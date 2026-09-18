package com.aiassistant.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.aiassistant.data.local.*
import com.aiassistant.data.remote.RetrofitClient
import com.aiassistant.domain.model.*
import com.aiassistant.tools.EnrichedPromptResult
import com.aiassistant.utils.CryptoManager
import com.aiassistant.utils.FileUtils
import com.aiassistant.utils.PersonalizationManager
import com.aiassistant.utils.SmartMemoryExtractor
import com.aiassistant.utils.TavilySearchManager
import com.aiassistant.utils.TimelineMemoryHelper
import com.aiassistant.utils.TimelineReconcileResult
import com.aiassistant.utils.TimelineEventItem
import com.aiassistant.utils.TimelineCategory
import com.aiassistant.utils.AtemporalSettingItem
import com.aiassistant.utils.AdvancedMemoryEngine
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class ApiException(val statusCode: Int, message: String, val errorBody: String? = null) : Exception(message)

class AiRepository(
    private val folderDao: FolderDao,
    private val apiConfigDao: ApiConfigDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val usageStatDao: UsageStatDao,
    private val environmentVariableDao: EnvironmentVariableDao,
    private val promptTemplateDao: PromptTemplateDao,
    private val memoryDao: MemoryDao,
    private val conversationBranchDao: ConversationBranchDao,
    private val selectedModelDao: SelectedModelDao,
    private val cryptoManager: CryptoManager,
    private val personalizationManager: PersonalizationManager,
    private val tavilySearchManager: TavilySearchManager,
    private val echoToolHub: com.aiassistant.tools.EchoToolHub? = null,
    private val worldBookDao: WorldBookDao? = null
) {
    private val gson = Gson()
    private val tag = "AiRepository"
    private val activeStreamingCalls = ConcurrentHashMap<Long, Call>()
    private val modelContextWindowCache = ConcurrentHashMap<String, Int>()
    private val runtimeContextWindowLimitCache = ConcurrentHashMap<String, Int>()

    companion object {
        const val SUMMARY_BUDGET_RATIO = 0.14f
        const val MEMORY_BUDGET_RATIO = 0.08f
        const val SYSTEM_PROMPT_TOKEN_RESERVE = 900
        const val MIN_RECENT_CONTEXT_TOKENS = 16_000

        const val MIN_SUMMARY_SOURCE_MESSAGES = 16
        const val MIN_SUMMARY_SOURCE_TOKENS = 8_000
        const val SUMMARY_PROMPT_MIN_TOKENS = 300
        const val SUMMARY_PROMPT_MAX_TOKENS = 1_200
        const val SUMMARY_COMPLETION_MIN_TOKENS = 256
        const val SUMMARY_COMPLETION_MAX_TOKENS = 1_200
        const val SUMMARY_TRANSCRIPT_MESSAGE_LIMIT = 80
        const val SUMMARY_TRANSCRIPT_HEAD_COUNT = 20
        const val SUMMARY_TRANSCRIPT_CHAR_LIMIT = 1_200
        const val EXTRACTIVE_SUMMARY_MESSAGE_LIMIT = 12
        const val EXTRACTIVE_SUMMARY_CHAR_LIMIT = 500

        const val MEMORY_CAPTURE_FRESHNESS_MS = 10 * 60 * 1000L
        const val MEMORY_CAPTURE_CONFIDENCE = 0.72f
        const val MEMORY_RELEVANCE_THRESHOLD = 0.45f
        const val CONVERSATION_MEMORY_BOOST = 0.45f
        const val USER_MEMORY_BOOST = 0.35f
        const val MEMORY_CONFIDENCE_WEIGHT = 0.18f
        const val MEMORY_TERM_OVERLAP_WEIGHT = 0.32f
        const val MEMORY_RECENCY_WEIGHT = 0.12f
        const val MEMORY_RECENCY_WINDOW_MS = 14f * 24f * 60f * 60f * 1000f
        const val DEFAULT_UNKNOWN_CONTEXT_WINDOW_TOKENS = 256_000
        const val CONTEXT_OVERFLOW_RETRY_WINDOW_TOKENS = 32_000

        /**
         * 解析具名或纯文本 API Key 列表（支持 [名称] sk-xxx 与 名称:::sk-xxx 格式）
         */
        fun parseNamedApiKeys(rawKey: String?): List<NamedApiKey> {
            if (rawKey.isNullOrBlank()) return emptyList()
            val lines = rawKey.split(Regex("[\\n,;]+")).map { it.trim() }.filter { it.isNotEmpty() }
            val result = mutableListOf<NamedApiKey>()
            for (line in lines) {
                // 格式 1: [名称] key
                val bracketMatch = Regex("""^[\[【]([^\[\]【】]+)[\]】]\s*(.+)$""").find(line)
                if (bracketMatch != null) {
                    val name = bracketMatch.groupValues[1].trim()
                    val key = bracketMatch.groupValues[2].trim()
                    if (key.isNotEmpty()) {
                        result.add(NamedApiKey(name = name, key = key))
                        continue
                    }
                }
                // 格式 2: 名称:::key
                val colonMatch = Regex("""^([^:]+):::\s*(.+)$""").find(line)
                if (colonMatch != null) {
                    val name = colonMatch.groupValues[1].trim()
                    val key = colonMatch.groupValues[2].trim()
                    if (key.isNotEmpty()) {
                        result.add(NamedApiKey(name = name, key = key))
                        continue
                    }
                }
                // 格式 3: 纯 key
                result.add(NamedApiKey(name = "", key = line))
            }
            return result
        }

        /**
         * 格式化具名 API Key 为多行持久化文本
         */
        fun formatNamedApiKeys(keys: List<NamedApiKey>): String {
            return keys.filter { it.key.isNotBlank() }.joinToString("\n") { item ->
                val cleanKey = item.key.trim()
                val cleanName = item.name.trim()
                if (cleanName.isNotBlank()) {
                    "[$cleanName] $cleanKey"
                } else {
                    cleanKey
                }
            }
        }

        /**
         * 提取纯净 API Key 列表（自动剥离名称前缀，杜绝网络层脏标头）
         */
        fun parseApiKeys(rawKey: String?): List<String> {
            if (rawKey.isNullOrBlank()) return emptyList()
            return parseNamedApiKeys(rawKey)
                .map { it.key.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        }

        fun isMainlyEnglish(text: String?): Boolean {
            if (text.isNullOrBlank()) return false
            val nonWhitespace = text.filterNot { it.isWhitespace() }
            if (nonWhitespace.length < 15) return false

            var chineseCount = 0
            var latinCount = 0
            for (ch in nonWhitespace) {
                if (ch in '\u4e00'..'\u9fa5') {
                    chineseCount++
                } else if ((ch in 'a'..'z') || (ch in 'A'..'Z')) {
                    latinCount++
                }
            }
            return latinCount >= 30 && chineseCount < (latinCount * 0.15)
        }

        fun normalizeThinkingEffort(effort: String?, providerType: String = "openai"): String {
            val normalized = effort?.lowercase()
            return if (providerType.equals("deepseek", true) || providerType.equals("deepseek_fixed", true)) {
                when (normalized) {
                    "max", "ultra" -> "max"
                    else -> "high"
                }
            } else {
                when (normalized) {
                    "low", "medium", "high" -> normalized
                    "ultra", "max" -> "high"
                    else -> "medium"
                }
            }
        }

        fun thinkingBudgetForEffort(effort: String?, configuredBudget: Int): Int {
            val base = configuredBudget.coerceIn(1024, 32768)
            return when (effort?.lowercase()) {
                "low" -> (base / 2).coerceIn(1024, 32768)
                "max", "ultra" -> 32768
                "high" -> (base * 2).coerceIn(1024, 32768)
                else -> base
            }
        }

        fun isTimeoutException(e: Throwable): Boolean {
            if (e is java.net.SocketTimeoutException) return true
            var current: Throwable? = e
            while (current != null) {
                if (current is java.net.SocketTimeoutException) return true
                val msg = current.message?.lowercase().orEmpty()
                if (msg.contains("timeout") || msg.contains("timed out") || msg.contains("time out")) return true
                current = current.cause
            }
            return false
        }

        fun estimateTokenCount(text: String): Int {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return 0
            val cjkCount = trimmed.count { Character.UnicodeScript.of(it.code) in setOf(
                Character.UnicodeScript.HAN,
                Character.UnicodeScript.HIRAGANA,
                Character.UnicodeScript.KATAKANA,
                Character.UnicodeScript.HANGUL
            ) }
            val asciiCount = trimmed.length - cjkCount
            return (cjkCount / 1.7f + asciiCount / 4.0f).toInt().coerceAtLeast(1)
        }

        fun sanitizeGeneratedTitle(rawTitle: String?): String? {
            if (rawTitle.isNullOrBlank()) return null

            // 1. 剔除思考过程标签（支持 <think>...</think> 或未闭合的 <think>）
            var cleaned = rawTitle.replace(Regex("(?s)<think>.*?</think>"), "").trim()
            if (cleaned.contains("<think>")) {
                cleaned = cleaned.substringAfterLast("</think>", cleaned.substringBefore("<think>")).trim()
            }

            // 2. 取第一行非空文字
            var firstLine = cleaned
                .lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() } ?: return null

            // 3. 剥离常见前缀（例如 "标题：", "对话标题：", "建议标题：", "Title:", "1. " 等）
            val prefixRegex = Regex("^(?:[0-9]+[\\.、]|[-*]\\s*|对话标题[：:]|标题[：:]|建议标题[：:]|Title[：:]|Topic[：:])\\s*", RegexOption.IGNORE_CASE)
            firstLine = firstLine.replace(prefixRegex, "").trim()

            // 4. 剥离首尾引号、标点与多余符号
            firstLine = firstLine
                .trim('"', '\'', '“', '”', '「', '」', '《', '》', '【', '】', '`', '。', '.', '：', ':', '！', '!', '？', '?')
                .trim()

            // 5. 限制在18个汉字/字符以内
            val title = firstLine.take(18).trim()
            return title.ifBlank { null }
        }

        fun generateDuplicateTitle(originalTitle: String): String {
            val trimmed = originalTitle.trim()
            if (trimmed.isEmpty()) return "未命名对话 (副本)"
            val copyRegex = Regex("""^(.*?)\s*\(副本(?:\s*(\d+))?\)$""")
            val match = copyRegex.find(trimmed)
            return if (match != null) {
                val baseName = match.groupValues[1].trim()
                val copyIndex = match.groupValues[2].toIntOrNull() ?: 1
                "$baseName (副本 ${copyIndex + 1})"
            } else {
                "$trimmed (副本)"
            }
        }

        fun isNetworkFluctuationException(e: Throwable): Boolean {
            if (isTimeoutException(e)) return true
            var current: Throwable? = e
            while (current != null) {
                if (current is java.net.UnknownHostException ||
                    current is java.net.ConnectException ||
                    current is java.net.NoRouteToHostException ||
                    current is java.net.SocketException ||
                    current is javax.net.ssl.SSLException ||
                    current is java.net.SocketTimeoutException
                ) {
                    return true
                }
                val msg = current.message?.lowercase().orEmpty()
                if (msg.contains("connection abort") ||
                    msg.contains("unexpected end of stream") ||
                    msg.contains("stream was reset") ||
                    msg.contains("broken pipe") ||
                    msg.contains("network is unreachable") ||
                    msg.contains("connection reset") ||
                    msg.contains("connection closed") ||
                    msg.contains("unable to resolve host") ||
                    msg.contains("failed to connect") ||
                    msg.contains("route to host") ||
                    msg.contains("end of stream")
                ) {
                    return true
                }
                if (current is java.io.EOFException) {
                    return true
                }
                current = current.cause
            }
            return false
        }

        /**
         * OpenAI 流式 Chunk 解析结果数据载体
         */
        data class OpenAiStreamChunkResult(
            val contentDelta: String? = null,
            val thinkingDelta: String? = null,
            val finishReason: String? = null,
            val isDone: Boolean = false,
            val usage: Usage? = null,
            val inlineErrorMessage: String? = null
        )

        /**
         * 健壮解析单行流式数据：
         * 1. 兼容响应 Content-Type 不是 text/event-stream 的情况
         * 2. 兼容没有 data: [DONE] 的自然 EOF 终止
         * 3. 兼容没有 finish_reason 的非标准 chunk
         * 4. 兼容行首 BOM (\uFEFF)、多余空白、空行与 SSE 注释行 (: ping / : keepalive)
         * 5. 兼容 NDJSON 格式 ({...}) 与非标准 choices[0].message
         */
        fun parseOpenAiStreamLine(rawLine: String, gson: com.google.gson.Gson = com.google.gson.Gson()): OpenAiStreamChunkResult? {
            var line = rawLine
            if (line.startsWith("\uFEFF")) {
                line = line.removePrefix("\uFEFF")
            }
            line = line.trim()
            if (line.isEmpty()) return null

            // 忽略 SSE 注释行（: keepalive, : ping）以及 event / id / retry 字段行
            if (line.startsWith(":")) return null
            if (line.startsWith("event:", ignoreCase = true) ||
                line.startsWith("id:", ignoreCase = true) ||
                line.startsWith("retry:", ignoreCase = true)) {
                return null
            }

            // 提取有效数据载荷：兼容 SSE "data: ..." 与 NDJSON "{...}"
            val data = when {
                line.startsWith("data:", ignoreCase = true) -> line.substring(5).trim()
                line.startsWith("{") && line.endsWith("}") -> line
                else -> return null
            }

            if (data.isBlank()) return null

            // 识别结束标记
            if (data == "[DONE]") {
                return OpenAiStreamChunkResult(isDone = true)
            }

            // 检测流式返回的内联错误（如 200 OK 建立连接后第一包返回 error）
            if (data.contains("\"error\"") && (data.contains("\"message\"") || data.contains("\"code\""))) {
                try {
                    val errObj = com.google.gson.JsonParser.parseString(data).asJsonObject
                    if (errObj.has("error")) {
                        val err = errObj.get("error")
                        val msg = if (err.isJsonObject) err.asJsonObject.get("message")?.asString else err.asString
                        if (!msg.isNullOrBlank()) {
                            return OpenAiStreamChunkResult(inlineErrorMessage = msg)
                        }
                    }
                } catch (_: Exception) {}
            }

            return try {
                val chunk = gson.fromJson(data, ChatCompletionChunk::class.java)
                val choice = chunk.choices?.firstOrNull()
                val finishReason = choice?.finish_reason

                var contentDelta = choice?.delta?.content
                var thinkingDelta = choice?.delta?.reasoning_content
                    ?: choice?.delta?.reasoning_content_camel
                    ?: choice?.delta?.reasoningContent
                    ?: choice?.delta?.reasoning
                    ?: choice?.delta?.thinking
                    ?: choice?.delta?.thinking_content
                    ?: choice?.delta?.thought

                // 兼容非标准将输出置于 choices[0].message 的中转网关
                if (contentDelta == null && thinkingDelta == null && data.contains("\"message\"")) {
                    try {
                        val jsonObj = com.google.gson.JsonParser.parseString(data).asJsonObject
                        val firstChoice = jsonObj.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
                        val msgObj = firstChoice?.getAsJsonObject("message")
                        if (msgObj != null) {
                            if (msgObj.has("content") && !msgObj.get("content").isJsonNull) {
                                contentDelta = msgObj.get("content").asString
                            }
                            if (msgObj.has("reasoning_content") && !msgObj.get("reasoning_content").isJsonNull) {
                                thinkingDelta = msgObj.get("reasoning_content").asString
                            }
                        }
                    } catch (_: Exception) {}
                }

                OpenAiStreamChunkResult(
                    contentDelta = contentDelta,
                    thinkingDelta = thinkingDelta,
                    finishReason = finishReason,
                    usage = chunk.usage
                )
            } catch (_: Exception) {
                null
            }
        }

        fun extractRootBaseTitle(rawTitle: String): String {
            var current = rawTitle.trim()
            val branchSuffixRegex = Regex("""[\s_]*[\(（]分支[\s_]*\d*[\)）]\s*$""")
            while (true) {
                val next = current.replace(branchSuffixRegex, "").trim()
                if (next == current || next.isEmpty()) break
                current = next
            }
            return current.ifBlank { "对话" }
        }

        fun calculateNextBranchTitle(rootBaseTitle: String, existingTitles: List<String>): String {
            val escapedBase = Regex.escape(rootBaseTitle)
            val branchIndexRegex = Regex("""^$escapedBase[\s_]*[\(（]分支[\s_]*(\d*)[\)）]$""")
            var maxIndex = 0
            var hasUnnumberedBranch = false

            for (title in existingTitles) {
                val trimmed = title.trim()
                val match = branchIndexRegex.find(trimmed)
                if (match != null) {
                    val numStr = match.groupValues[1]
                    if (numStr.isNotBlank()) {
                        val num = numStr.toIntOrNull() ?: 0
                        if (num > maxIndex) maxIndex = num
                    } else {
                        hasUnnumberedBranch = true
                    }
                }
            }

            val nextIndex = if (maxIndex > 0) {
                maxIndex + 1
            } else if (hasUnnumberedBranch) {
                2
            } else {
                1
            }

            return "$rootBaseTitle (分支 $nextIndex)"
        }

        fun formatWorldBookPrompt(entries: List<WorldBookEntry>): String {
            if (entries.isEmpty()) return ""
            val lines = entries.map { entry ->
                "- 【${entry.name}】${entry.content.trim()}"
            }
            return "【世界书设定】\n" + lines.joinToString("\n")
        }
    }

    fun cancelActiveRequest(conversationId: Long) {
        activeStreamingCalls.remove(conversationId)?.cancel()
    }

    private fun isRequestCancellation(error: Throwable): Boolean {
        if (error is CancellationException) return true
        if (error.message.equals("Canceled", ignoreCase = true)) return true
        return error.cause?.let(::isRequestCancellation) == true
    }

    private fun isContextLimitError(error: Throwable): Boolean {
        val messages = mutableListOf<String>()
        var current: Throwable? = error
        while (current != null) {
            current.message?.let(messages::add)
            current = current.cause
        }
        val message = messages.joinToString(" ").lowercase()
        if (message.isBlank()) return false
        val hasTokenOrContext = listOf("token", "context", "上下文", "长度", "prompt", "input").any { it in message }
        val hasOverflow = listOf(
            "exceed",
            "exceeded",
            "too long",
            "too many",
            "maximum",
            "max",
            "length",
            "context_length_exceeded",
            "reduce",
            "超出",
            "超过",
            "过长"
        ).any { it in message }
        return hasTokenOrContext && hasOverflow
    }

    private fun extractContextWindowFromError(message: String): Int? {
        return Regex("""(?<!\d)([1-9]\d{3,6})(?!\d)\s*(?:tokens?|token|上下文|长度)?""")
            .findAll(message.lowercase())
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .filter { it in 4_000..2_000_000 }
            .minOrNull()
    }

    // ============ 文件夹相关 ============

    fun getAllFolders(): Flow<List<Folder>> = folderDao.getAllFolders()

    fun getRootFolders(): Flow<List<Folder>> = folderDao.getRootFolders()

    fun getSubFolders(parentId: Long): Flow<List<Folder>> = folderDao.getSubFolders(parentId)

    suspend fun getFolderById(id: Long): Folder? = folderDao.getFolderById(id)

    suspend fun createFolder(name: String, parentId: Long? = null, icon: String = "folder", color: Int = 0): Long {
        val folder = Folder(
            name = name,
            parentId = parentId,
            icon = icon,
            color = color
        )
        return folderDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: Folder) {
        folderDao.updateFolder(folder.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteFolder(id: Long, moveConversationsToRoot: Boolean = true) {
        if (moveConversationsToRoot) {
            folderDao.unassignConversationsFromFolder(id)
        }
        folderDao.deleteFolderById(id)
    }

    suspend fun getConversationCountInFolder(folderId: Long): Int {
        return folderDao.getConversationCount(folderId)
    }

    // ============ API配置相关 ============

    fun getAllApiConfigs(): Flow<List<ApiConfig>> = apiConfigDao.getAllConfigs()

    suspend fun getApiConfigById(id: Long): ApiConfig? = apiConfigDao.getConfigById(id)

    suspend fun getDefaultApiConfig(): ApiConfig? = apiConfigDao.getDefaultConfig()

    suspend fun saveApiConfig(config: ApiConfig): Long {
        // 检查apiKey是否已经加密（以 enc:v1: 标头为准）
        val apiKeyToSave = if (isAlreadyEncrypted(config.apiKey)) {
            // 如果key已经带有 enc:v1: 密文标头，保持原密文
            config.apiKey
        } else {
            // 否则加密并打上 enc:v1: 标头
            cryptoManager.encrypt(config.apiKey)
        }

        // 自动补全URL
        val normalizedConfig = config.copy(apiKey = apiKeyToSave)
        val finalConfig = normalizeUrl(normalizedConfig)

        return if (config.id == 0L) {
            val existing = apiConfigDao.getConfigByIdentity(
                name = finalConfig.name,
                provider = finalConfig.provider,
                baseUrl = finalConfig.baseUrl,
                apiType = finalConfig.apiType,
                modelName = finalConfig.modelName
            )
            if (existing != null) {
                apiConfigDao.updateConfig(
                    finalConfig.copy(
                        id = existing.id,
                        isDefault = existing.isDefault,
                        createdAt = existing.createdAt
                    )
                )
                existing.id
            } else {
                apiConfigDao.insertConfig(finalConfig)
            }
        } else {
            apiConfigDao.updateConfig(finalConfig)
            config.id
        }
    }

    // 自动补全URL
    private fun normalizeUrl(config: ApiConfig): ApiConfig {
        var baseUrl = config.baseUrl.trim()

        // 确保以http://或https://开头
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "https://$baseUrl"
        }

        // 移除末尾的斜杠
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.dropLast(1)
        }

        // 根据API类型自动补全路径
        when (config.apiType) {
            "anthropic" -> {
                // Anthropic API
                if (!baseUrl.endsWith("/v1") && !baseUrl.endsWith("/anthropic")) {
                    baseUrl = "$baseUrl/v1"
                }
            }
            "openai" -> {
                // OpenAI兼容格式
                if (!baseUrl.endsWith("/v1")) {
                    baseUrl = "$baseUrl/v1"
                }
            }
        }

        return config.copy(baseUrl = baseUrl)
    }

    // 检查是否已经加密
    private fun isAlreadyEncrypted(value: String): Boolean {
        return cryptoManager.isEncrypted(value)
    }

    suspend fun deleteApiConfig(config: ApiConfig) = apiConfigDao.deleteConfig(config)

    suspend fun setDefaultConfig(id: Long) {
        apiConfigDao.clearDefaultConfigs()
        apiConfigDao.setDefaultConfig(id)
    }

    suspend fun getDecryptedConfig(id: Long): ApiConfig? {
        val config = apiConfigDao.getConfigById(id) ?: return null
        return try {
            val decryptedKey = cryptoManager.decrypt(config.apiKey)
            if (!cryptoManager.isEncrypted(config.apiKey) && config.apiKey.isNotBlank()) {
                val newEncryptedKey = cryptoManager.encrypt(decryptedKey)
                apiConfigDao.updateConfig(config.copy(apiKey = newEncryptedKey))
            }
            config.copy(apiKey = decryptedKey)
        } catch (e: Exception) {
            Log.e(tag, "解密API Key失败", e)
            config // 返回原始配置，让调用者处理
        }
    }

    // 获取可用模型列表
    suspend fun fetchAvailableModels(configId: Long): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val config = getDecryptedConfig(configId)
                    ?: return@withContext Result.failure(Exception("API配置不存在"))

                val models = fetchModelsFromEndpoint(
                    baseUrl = normalizeApiBaseUrl(config.baseUrl, config.apiType),
                    apiKey = config.apiKey,
                    apiType = config.apiType
                )
                val cleanedModels = sanitizeModelNames(models)
                if (cleanedModels.isEmpty()) {
                    Result.failure(Exception("该API未返回模型列表，请手动输入模型名称"))
                } else {
                    Result.success(cleanedModels)
                }
            } catch (e: Exception) {
                Log.e(tag, "获取模型列表失败", e)
                // 返回预设模型列表作为备选
                val config = getApiConfigById(configId)
                val presetModels = config?.let { getPresetModels(it.apiType, it.provider) } ?: emptyList()
                if (presetModels.isNotEmpty()) {
                    Result.success(sanitizeModelNames(presetModels))
                } else {
                    Result.failure(Exception("网络错误，请检查API地址是否正确"))
                }
            }
        }
    }

    // 直接获取模型列表（不保存配置，支持多Key轮询尝试）
    suspend fun fetchAvailableModelsDirect(baseUrl: String, apiKey: String, apiType: String = "openai"): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            val allKeys = parseApiKeys(apiKey).ifEmpty { listOf(apiKey) }
            var lastException: Exception? = null

            for (key in allKeys) {
                try {
                    val normalizedBaseUrl = normalizeApiBaseUrl(baseUrl, apiType)
                    val models = fetchModelsFromEndpoint(normalizedBaseUrl, key, apiType)
                    val cleanedModels = sanitizeModelNames(models)
                    if (cleanedModels.isNotEmpty()) {
                        return@withContext Result.success(cleanedModels)
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Key获取模型列表失败: ${e.message}，尝试下一Key")
                    lastException = e
                }
            }
            Result.failure(lastException ?: Exception("未能获取到模型列表，请手动输入模型名称"))
        }
    }

    private fun normalizeApiBaseUrl(rawBaseUrl: String, apiType: String): String {
        var baseUrl = rawBaseUrl.trim()
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "https://$baseUrl"
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.dropLast(1)
        }
        return when (apiType) {
            "anthropic" -> {
                if (baseUrl.endsWith("/v1") || baseUrl.endsWith("/anthropic")) baseUrl else "$baseUrl/v1"
            }
            else -> {
                if (baseUrl.endsWith("/v1")) baseUrl else "$baseUrl/v1"
            }
        }
    }

    private fun fetchModelsFromEndpoint(baseUrl: String, apiKey: String, apiType: String): List<String> {
        val headers = if (apiType == "anthropic") {
            mapOf(
                "x-api-key" to apiKey,
                "anthropic-version" to "2023-06-01"
            )
        } else {
            mapOf("Authorization" to RetrofitClient.formatApiKey(apiKey))
        }

        RetrofitClient.getJson(baseUrl, "models", headers).use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = bodyText.ifBlank { response.message }
                throw Exception("获取模型列表失败 (${response.code}): $detail")
            }

            val dynamicModels = parseModelNamesAndCacheContextWindows(bodyText)
            if (dynamicModels.isNotEmpty()) return dynamicModels

            val body = gson.fromJson(bodyText, ModelsResponse::class.java)
            return body?.data
                ?.onEach { cacheModelContextWindow(it) }
                ?.mapNotNull { sanitizeModelName(it.id.ifBlank { it.name.orEmpty() }) }
                ?.distinct()
                ?.sorted()
                .orEmpty()
        }
    }

    private fun parseModelNamesAndCacheContextWindows(bodyText: String): List<String> {
        return runCatching {
            val root = JsonParser.parseString(bodyText).asJsonObject
            val modelElements = when {
                root.get("data")?.isJsonArray == true -> root.getAsJsonArray("data")
                root.get("models")?.isJsonArray == true -> root.getAsJsonArray("models")
                root.get("model")?.isJsonArray == true -> root.getAsJsonArray("model")
                else -> return@runCatching emptyList()
            }

            modelElements.mapNotNull { element ->
                val modelObject = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val modelName = firstString(modelObject, "id", "name", "model", "model_name")
                    ?.let(::sanitizeModelName)
                    ?: return@mapNotNull null
                val aliases = listOfNotNull(
                    modelName,
                    firstString(modelObject, "id")?.let(::sanitizeModelName),
                    firstString(modelObject, "name")?.let(::sanitizeModelName)
                ).distinct()
                extractContextWindowFromModelJson(modelObject)?.let { limit ->
                    aliases.forEach { alias ->
                        modelContextWindowCache[alias.lowercase()] = limit
                    }
                }
                modelName
            }.distinct().sorted()
        }.getOrElse {
            Log.w(tag, "动态解析模型列表失败", it)
            emptyList()
        }
    }

    private fun firstString(obj: JsonObject, vararg names: String): String? {
        return names.firstNotNullOfOrNull { name ->
            obj.get(name)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString
        }
    }

    private fun extractContextWindowFromModelJson(obj: JsonObject): Int? {
        val directKeys = listOf(
            "context_length",
            "context_window",
            "max_context_length",
            "max_context_window",
            "max_model_len",
            "max_model_length",
            "max_sequence_length",
            "max_position_embeddings",
            "max_input_tokens",
            "input_token_limit",
            "n_ctx",
            "contextLength",
            "contextWindow",
            "maxContextLength",
            "maxContextWindow",
            "maxModelLen",
            "maxInputTokens"
        )
        directKeys.firstNotNullOfOrNull { key ->
            parseContextWindowValue(obj.get(key))
        }?.let { return it }

        val nestedKeys = listOf("metadata", "limits", "capabilities", "model_info", "config", "parameters")
        return nestedKeys.firstNotNullOfOrNull { key ->
            obj.get(key)?.takeIf { it.isJsonObject }?.asJsonObject?.let(::extractContextWindowFromModelJson)
        }
    }

    private fun parseContextWindowValue(value: JsonElement?): Int? {
        if (value == null || value.isJsonNull) return null
        val parsed = when {
            value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> value.asLong.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            value.isJsonPrimitive && value.asJsonPrimitive.isString -> parseContextWindowFromText(value.asString)
            else -> null
        }
        return parsed?.takeIf { it >= 4_000 }?.coerceIn(4_000, 2_000_000)
    }

    private fun cacheModelContextWindow(modelInfo: ModelInfo) {
        val limit = listOfNotNull(
            modelInfo.context_length,
            modelInfo.context_window,
            modelInfo.max_context_length,
            modelInfo.max_context_window,
            modelInfo.max_input_tokens,
            modelInfo.input_token_limit,
            modelInfo.contextLength,
            modelInfo.contextWindow,
            modelInfo.maxContextLength,
            modelInfo.maxContextWindow
        ).firstOrNull { it > 0 } ?: return

        listOf(modelInfo.id, modelInfo.name.orEmpty())
            .mapNotNull(::sanitizeModelName)
            .forEach { modelName ->
                modelContextWindowCache[modelName.lowercase()] = limit.coerceIn(4_000, 2_000_000)
            }
    }

    private fun sanitizeModelNames(models: List<String>): List<String> {
        return models.mapNotNull { sanitizeModelName(it) }.distinct()
    }

    private fun sanitizeModelName(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        val blockedEdges = setOf('"', '“', '”', '\'', '`', ']', '[', '\\')
        if (value.first() in blockedEdges || value.last() in blockedEdges) return null
        if (value.any { it.isISOControl() }) return null
        return value
    }

    // 获取预设模型列表
    private fun getPresetModels(apiType: String, provider: String): List<String> {
        return when {
            apiType == "anthropic" -> listOf(
                "claude-3-5-sonnet-20241022",
                "claude-3-5-haiku-20241022",
                "claude-3-opus-20240229",
                "claude-3-sonnet-20240229",
                "claude-3-haiku-20240307"
            )
            provider.contains("DeepSeek", ignoreCase = true) -> listOf(
                "deepseek-chat",
                "deepseek-reasoner"
            )
            provider.contains("OpenAI", ignoreCase = true) -> listOf(
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-4-turbo",
                "gpt-4",
                "gpt-3.5-turbo"
            )
            provider.contains("MiMo", ignoreCase = true) -> listOf(
                "mimo"
            )
            else -> emptyList()
        }
    }

    // ============ 对话相关 ============

    fun getAllConversations(): Flow<List<Conversation>> = conversationDao.getAllConversations()

    fun getUnfiledConversations(): Flow<List<Conversation>> = conversationDao.getUnfiledConversations()

    fun getPinnedConversations(): Flow<List<Conversation>> = conversationDao.getPinnedConversations()

    fun getHiddenConversations(): Flow<List<Conversation>> = conversationDao.getHiddenConversations()

    fun getConversationsByFolder(folderId: Long): Flow<List<Conversation>> =
        conversationDao.getConversationsByFolder(folderId)

    fun searchConversations(query: String): Flow<List<Conversation>> =
        conversationDao.searchConversations(query)

    suspend fun getConversationById(id: Long): Conversation? =
        conversationDao.getConversationById(id)

    suspend fun createConversation(
        title: String,
        apiConfigId: Long,
        modelName: String,
        folderId: Long? = null,
        systemPrompt: String? = null,
        tags: String? = null
    ): Long {
        val config = apiConfigDao.getConfigById(apiConfigId)
        val conversation = Conversation(
            title = title,
            folderId = folderId,
            apiConfigId = apiConfigId,
            modelName = modelName,
            temperature = config?.temperature ?: 0.95f,
            maxTokens = 50000,
            topP = config?.topP ?: 1.0f,
            enableThinking = true,
            thinkingEffort = config?.thinkingEffort ?: "high",
            enableWebSearch = false,
            systemPrompt = systemPrompt,
            tags = tags
        )
        return conversationDao.insertConversation(conversation)
    }

    suspend fun resolveDefaultModelName(config: ApiConfig): String = withContext(Dispatchers.IO) {
        val savedModels = selectedModelDao.getModelsByConfig(config.id).first()
            .filter { sanitizeModelName(it.modelName) != null }
        val enabledModels = savedModels.filter { it.isEnabled }
        val configDefault = sanitizeModelName(config.modelName)

        enabledModels.firstOrNull { it.modelName == configDefault }?.modelName
            ?: savedModels.firstOrNull { it.modelName == configDefault }?.modelName
            ?: enabledModels.firstOrNull()?.modelName
            ?: savedModels.firstOrNull()?.modelName
            ?: configDefault
            ?: config.modelName
    }

    suspend fun deleteConversation(id: Long) {
        messageDao.deleteMessagesByConversation(id)
        memoryDao.deleteConversationMemories(id)
        conversationDao.deleteConversationById(id)
    }

    suspend fun destroyPrivateConversation(id: Long) {
        messageDao.deleteMessagesByConversation(id)
        memoryDao.deleteConversationMemories(id)
        conversationDao.deleteConversationById(id)
    }

    suspend fun setConversationHidden(conversationId: Long, hidden: Boolean) {
        val conversation = conversationDao.getConversationById(conversationId) ?: return
        conversationDao.updateTags(conversationId, updateTag(conversation.tags, "hidden", hidden))
    }

    suspend fun setConversationsHidden(conversationIds: Collection<Long>, hidden: Boolean) {
        conversationIds.forEach { setConversationHidden(it, hidden) }
    }

    fun hasConversationTag(conversation: Conversation?, tag: String): Boolean {
        return conversation?.tags
            ?.split(',', ';', '|', ' ')
            ?.map { it.trim() }
            ?.any { it.equals(tag, ignoreCase = true) } == true
    }

    fun updateTag(rawTags: String?, tag: String, enabled: Boolean): String? {
        val tags = rawTags
            ?.split(',', ';', '|', ' ')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toMutableSet()
            ?: mutableSetOf()
        if (enabled) {
            tags += tag
        } else {
            tags.removeAll { it.equals(tag, ignoreCase = true) }
        }
        return tags.takeIf { it.isNotEmpty() }?.joinToString(",")
    }

    suspend fun moveToFolder(conversationId: Long, folderId: Long?) {
        conversationDao.moveToFolder(conversationId, folderId)
    }

    suspend fun setPinned(conversationId: Long, isPinned: Boolean) {
        conversationDao.setPinned(conversationId, isPinned)
    }

    suspend fun updateConversationTitle(conversationId: Long, title: String) {
        val conv = conversationDao.getConversationById(conversationId) ?: return
        conversationDao.updateConversation(conv.copy(title = title, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateConversation(conversation: Conversation) {
        conversationDao.updateConversation(conversation)
    }

    // ============ 模型长记忆相关 ============

    fun getAllMemories(): Flow<List<MemoryItem>> = memoryDao.getAllMemories()

    fun getConversationMemories(conversationId: Long): Flow<List<MemoryItem>> =
        memoryDao.getConversationMemoriesFlow(conversationId)

    suspend fun addConversationMemory(conversationId: Long, content: String): Long {
        val now = System.currentTimeMillis()
        val trimmed = content.trim()
        val item = MemoryItem(
            scope = "conversation",
            conversationId = conversationId,
            content = trimmed,
            keywords = tokenizeForMemory(trimmed).take(18).joinToString(",").ifBlank { null },
            confidence = 1.0f,
            isEnabled = true,
            createdAt = now,
            updatedAt = now
        )
        return memoryDao.insertMemory(item)
    }

    suspend fun addUserMemory(content: String): Long {
        val now = System.currentTimeMillis()
        val trimmed = content.trim()
        val item = MemoryItem(
            scope = "user",
            conversationId = null,
            content = trimmed,
            keywords = tokenizeForMemory(trimmed).take(18).joinToString(",").ifBlank { null },
            confidence = 1.0f,
            isEnabled = true,
            createdAt = now,
            updatedAt = now
        )
        return memoryDao.insertMemory(item)
    }

    suspend fun clearConversationMemories(conversationId: Long) {
        memoryDao.deleteConversationMemories(conversationId)
    }

    fun searchMemories(query: String): Flow<List<MemoryItem>> = memoryDao.searchMemories(query)

    suspend fun getMemoryById(id: Long): MemoryItem? = memoryDao.getMemoryById(id)

    suspend fun insertMemory(memory: MemoryItem): Long = memoryDao.insertMemory(memory)

    suspend fun updateMemory(memory: MemoryItem) = memoryDao.updateMemory(memory)

    suspend fun deleteMemory(id: Long) = memoryDao.deleteMemoryById(id)

    suspend fun deleteMemory(memory: MemoryItem) = deleteMemory(memory.id)

    suspend fun deleteAllMemories() = memoryDao.deleteAllMemories()

    suspend fun clearAllMemories() = deleteAllMemories()

    suspend fun saveMemory(memory: MemoryItem): Long {
        return if (memory.id > 0L) {
            updateMemory(memory)
            memory.id
        } else {
            insertMemory(memory)
        }
    }

    suspend fun setMemoryEnabled(id: Long, isEnabled: Boolean) = memoryDao.setMemoryEnabled(id, isEnabled)

    suspend fun saveConfirmedMemory(
        content: String,
        scope: String,
        conversationId: Long?,
        sourceMessageId: Long? = null
    ): Long {
        val now = System.currentTimeMillis()
        val item = MemoryItem(
            scope = scope,
            conversationId = if (scope == "conversation") conversationId else null,
            content = content,
            keywords = tokenizeForMemory(content).take(18).joinToString(","),
            sourceMessageId = sourceMessageId,
            confidence = 1.0f,
            isEnabled = true,
            createdAt = now,
            updatedAt = now
        )
        return memoryDao.insertMemory(item)
    }

    // ============ 世界书 (World Book / Lorebook) 相关 ============
    val worldBookDaoInstance: WorldBookDao? get() = worldBookDao

    fun getAllWorldBooks(): Flow<List<WorldBook>> =
        worldBookDao?.getAllBooks() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getAllWorldBooksList(): List<WorldBook> =
        worldBookDao?.getAllBooksList() ?: emptyList()

    suspend fun getWorldBookById(id: Long): WorldBook? =
        worldBookDao?.getBookById(id)

    suspend fun insertWorldBook(book: WorldBook): Long =
        worldBookDao?.insertBook(book) ?: 0L

    suspend fun updateWorldBook(book: WorldBook) {
        worldBookDao?.updateBook(book)
    }

    suspend fun deleteWorldBook(id: Long) {
        worldBookDao?.deleteBookById(id)
    }

    suspend fun setWorldBookEnabled(id: Long, isEnabled: Boolean) {
        worldBookDao?.setBookEnabled(id, isEnabled)
    }

    fun getWorldBookEntries(bookId: Long): Flow<List<WorldBookEntry>> =
        worldBookDao?.getEntriesForBook(bookId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getWorldBookEntriesList(bookId: Long): List<WorldBookEntry> =
        worldBookDao?.getEntriesForBookList(bookId) ?: emptyList()

    suspend fun getWorldBookEntryById(id: Long): WorldBookEntry? =
        worldBookDao?.getEntryById(id)

    suspend fun insertWorldBookEntry(entry: WorldBookEntry): Long =
        worldBookDao?.insertEntry(entry) ?: 0L

    suspend fun updateWorldBookEntry(entry: WorldBookEntry) {
        worldBookDao?.updateEntry(entry)
    }

    suspend fun deleteWorldBookEntry(id: Long) {
        worldBookDao?.deleteEntryById(id)
    }

    suspend fun setWorldBookEntryEnabled(id: Long, isEnabled: Boolean) {
        worldBookDao?.setEntryEnabled(id, isEnabled)
    }

    fun searchWorldBookEntries(query: String): Flow<List<WorldBookEntry>> =
        worldBookDao?.searchEntries(query) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun matchWorldBookEntries(
        text: String,
        activeBookIds: List<Long>? = null,
        maxTokenBudget: Int = 1500
    ): List<WorldBookEntry> {
        val dao = worldBookDao ?: return emptyList()
        val candidateEntries = if (!activeBookIds.isNullOrEmpty()) {
            dao.getActiveEntriesForBooks(activeBookIds)
        } else {
            dao.getActiveEntriesFromEnabledBooks()
        }
        if (candidateEntries.isEmpty()) return emptyList()

        val matched = candidateEntries.filter { it.matchesText(text) }
        val sorted = matched.sortedWith(
            compareByDescending<WorldBookEntry> { it.priority }
                .thenByDescending { it.updatedAt }
        )

        val results = mutableListOf<WorldBookEntry>()
        var estimatedTokens = 0
        for (entry in sorted) {
            val cost = (entry.name.length + entry.content.length) / 2 + 10
            if (results.isNotEmpty() && estimatedTokens + cost > maxTokenBudget) break
            results.add(entry)
            estimatedTokens += cost
        }
        return results
    }

    // ============ 消息相关 ============

    fun getMessages(conversationId: Long): Flow<List<Message>> =
        messageDao.getMessagesByConversation(conversationId)

    suspend fun getMessagesList(conversationId: Long): List<Message> =
        messageDao.getMessagesList(conversationId)

    suspend fun getConversationContextUsage(
        conversationId: Long,
        modelNameOverride: String? = null,
        maxOutputTokens: Int? = null
    ): ConversationContextUsage = withContext(Dispatchers.IO) {
        val conversation = getConversationById(conversationId) ?: return@withContext ConversationContextUsage()
        val messages = getMessagesList(conversationId).collapseVariantsForHistory()
        val modelName = modelNameOverride
            ?.takeIf { it.isNotBlank() }
            ?: conversation.modelName

        buildContextUsageSnapshot(
            conversation = conversation,
            messages = messages,
            modelName = modelName,
            maxOutputTokens = maxOutputTokens
        )
    }

    suspend fun compressConversationContext(
        conversationId: Long,
        modelNameOverride: String? = null,
        maxOutputTokens: Int? = null
    ): Result<ConversationContextUsage> = withContext(Dispatchers.IO) {
        runCatching {
            val conversation = getConversationById(conversationId)
                ?: throw IllegalStateException("对话不存在")
            val config = getDecryptedConfig(conversation.apiConfigId)
                ?: throw IllegalStateException("API配置不存在")
            val messages = getMessagesList(conversationId).collapseVariantsForHistory()
            val modelName = modelNameOverride
                ?.takeIf { it.isNotBlank() }
                ?: resolveRequestModel(config.copy(modelName = conversation.modelName), resolveChatRequestOptions(config, null))

            val snapshot = buildContextUsageSnapshot(
                conversation = conversation,
                messages = messages,
                modelName = modelName,
                maxOutputTokens = maxOutputTokens
            )
            val usableMessages = messages.filter { message ->
                (message.role == "user" || message.role == "assistant") && message.content.isNotBlank() && !message.isExcluded
            }
            if (usableMessages.size < 4) {
                return@runCatching snapshot
            }

            val tokenBudget = (snapshot.promptBudgetTokens * SUMMARY_BUDGET_RATIO)
                .toInt()
                .coerceIn(600, 1_800)

            // 开源最佳实践（ConversationSummaryBufferMemory 范式）：
            // 保留最近 16 条左右鲜活活跃对话（8 轮完整上下文），仅将其余超出活跃窗口的较早历史消息压缩归约进滚动摘要
            val keepRecentCount = 16.coerceAtMost(usableMessages.size)
            val olderMessages = usableMessages.dropLast(keepRecentCount)

            ensureRollingSummary(
                conversation = conversation,
                config = config.copy(modelName = modelName),
                modelName = modelName,
                olderMessages = olderMessages,
                tokenBudget = tokenBudget
            )

            val refreshedConversation = getConversationById(conversationId) ?: conversation
            buildContextUsageSnapshot(
                conversation = refreshedConversation,
                messages = messages,
                modelName = modelName,
                maxOutputTokens = maxOutputTokens
            )
        }
    }

    suspend fun updateConversationModelAvatar(conversationId: Long, avatarUri: String?) = withContext(Dispatchers.IO) {
        conversationDao.updateModelAvatarUri(conversationId, avatarUri)
    }

    suspend fun saveMessage(message: Message): Long {
        val id = messageDao.insertMessage(message)
        updateConversationStats(message.conversationId)
        captureMemoryCandidate(message.copy(id = id))
        return id
    }

    suspend fun updateMessage(message: Message) {
        messageDao.updateMessage(message)
    }

    suspend fun updateTranslatedThinking(messageId: Long, translatedThinking: String?) {
        messageDao.updateTranslatedThinking(messageId, translatedThinking)
    }

    suspend fun deleteMessage(message: Message) {
        messageDao.deleteMessage(message)
        updateConversationStats(message.conversationId)
    }

    suspend fun deleteMessagesFrom(conversationId: Long, createdAt: Long) {
        messageDao.deleteMessagesFrom(conversationId, createdAt)
        updateConversationStats(conversationId)
    }

    private suspend fun updateConversationStats(conversationId: Long) {
        val messages = messageDao.getMessagesList(conversationId)
        val totalTokens = messages.sumOf { it.tokenCount }
        conversationDao.updateStats(conversationId, messages.size, totalTokens)
        conversationDao.updateTimestamp(conversationId)
    }

    // ============ AI API调用 ============

    suspend fun sendChatMessage(
        configId: Long,
        conversationId: Long,
        userMessage: String,
        attachments: List<Attachment> = emptyList(),
        onToken: (String) -> Unit,
        onThinkingToken: (String) -> Unit = {},
        onComplete: (String, String?, Any?) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val config = getDecryptedConfig(configId)
                    ?: throw Exception("API配置不存在")

                // 获取历史消息构建上下文
                val historyMessages = getMessagesList(conversationId)

                // 根据API类型调用不同的方法
                when (config.apiType) {
                    "anthropic" -> sendAnthropicMessage(config, conversationId, historyMessages, userMessage, attachments, null, null, 1, onToken, onThinkingToken, onComplete)
                    else -> sendOpenAIMessage(config, conversationId, historyMessages, userMessage, attachments, null, null, 1, onToken, onThinkingToken, onComplete)
                }
            } catch (e: Exception) {
                if (isRequestCancellation(e)) throw CancellationException("请求已取消", e)
                Log.e(tag, "发送消息失败", e)
                onError(e.message ?: "未知错误")

                // 记录失败统计
                try {
                    val failedConfig = getApiConfigById(configId)
                    failedConfig?.let { cfg ->
                        val stat = ApiUsageStat(
                            apiConfigId = configId,
                            provider = cfg.provider,
                            modelName = cfg.modelName,
                            success = false,
                            errorMessage = e.message
                        )
                        usageStatDao.insertStat(stat)
                    }
                } catch (statEx: Exception) {
                    Log.e(tag, "记录失败统计异常", statEx)
                }
            }
        }
    }

    // 使用自定义配置发送消息（支持临时切换模型和参数）
    suspend fun sendChatMessageWithConfig(
        config: ApiConfig,
        conversationId: Long,
        userMessage: String,
        attachments: List<Attachment> = emptyList(),
        options: ChatRequestOptions? = null,
        assistantVariantGroupId: String? = null,
        assistantVariantIndex: Int = 1,
        onToken: (String) -> Unit,
        onThinkingToken: (String) -> Unit = {},
        onStatusUpdate: ((String) -> Unit)? = null,
        onResetBuffer: (() -> Unit)? = null,
        onComplete: (String, String?, Any?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            dispatchChatMessageWithConfig(
                config = config,
                conversationId = conversationId,
                userMessage = userMessage,
                attachments = attachments,
                options = options,
                assistantVariantGroupId = assistantVariantGroupId,
                assistantVariantIndex = assistantVariantIndex,
                onToken = onToken,
                onThinkingToken = onThinkingToken,
                onStatusUpdate = onStatusUpdate,
                onResetBuffer = onResetBuffer,
                onComplete = onComplete
            )
        } catch (e: Exception) {
            if (isRequestCancellation(e)) throw CancellationException("请求已取消", e)
            if (isContextLimitError(e) && retryWithCompressedContext(
                    config = config,
                    conversationId = conversationId,
                    userMessage = userMessage,
                    attachments = attachments,
                    options = options,
                    assistantVariantGroupId = assistantVariantGroupId,
                    assistantVariantIndex = assistantVariantIndex,
                    onToken = onToken,
                    onThinkingToken = onThinkingToken,
                    onResetBuffer = onResetBuffer,
                    onComplete = onComplete,
                    originalError = e
                )
            ) {
                return
            }
            Log.e(tag, "发送消息失败", e)
            onError(e.message ?: "未知错误")

            // 记录失败统计
            try {
                val stat = ApiUsageStat(
                    apiConfigId = config.id,
                    provider = config.provider,
                    modelName = config.modelName,
                    success = false,
                    errorMessage = e.message
                )
                usageStatDao.insertStat(stat)
            } catch (statEx: Exception) {
                Log.e(tag, "记录失败统计异常", statEx)
            }
        }
    }

    private suspend fun dispatchChatMessageWithConfig(
        config: ApiConfig,
        conversationId: Long,
        userMessage: String,
        attachments: List<Attachment>,
        options: ChatRequestOptions?,
        assistantVariantGroupId: String?,
        assistantVariantIndex: Int,
        onToken: (String) -> Unit,
        onThinkingToken: (String) -> Unit,
        onStatusUpdate: ((String) -> Unit)? = null,
        onResetBuffer: (() -> Unit)? = null,
        onComplete: (String, String?, Any?) -> Unit
    ) {
        val historyMessages = getMessagesList(conversationId)
        val allKeys = parseApiKeys(config.apiKey).ifEmpty { listOf(config.apiKey) }
        var lastException: Exception? = null
        var hasEmittedTokens = false

        val wrappedOnToken: (String) -> Unit = { token ->
            hasEmittedTokens = true
            onToken(token)
        }
        val wrappedOnThinkingToken: (String) -> Unit = { token ->
            hasEmittedTokens = true
            onThinkingToken(token)
        }

        for ((keyIndex, currentKey) in allKeys.withIndex()) {
            val keyConfig = config.copy(apiKey = currentKey)
            var attempt = 0
            val maxTimeoutAttempts = 3 // 遇网络波动/超时在未收到任何内容时最多重试3次
            val retryDelays = longArrayOf(1000L, 2000L, 5000L) // 需求 5：退避 1s / 2s / 5s

            while (attempt <= maxTimeoutAttempts) {
                try {
                    when (keyConfig.apiType) {
                        "anthropic" -> sendAnthropicMessage(
                            keyConfig, conversationId, historyMessages, userMessage, attachments,
                            options, assistantVariantGroupId, assistantVariantIndex,
                            wrappedOnToken, wrappedOnThinkingToken, onComplete
                        )
                        else -> sendOpenAIMessage(
                            keyConfig, conversationId, historyMessages, userMessage, attachments,
                            options, assistantVariantGroupId, assistantVariantIndex,
                            wrappedOnToken, wrappedOnThinkingToken, onComplete
                        )
                    }
                    return // 请求成功完成
                } catch (e: Exception) {
                    if (isRequestCancellation(e)) throw e
                    lastException = e

                    // 需求 5：如果已经收到部分内容，坚决不自动重试，避免向用户重复输出
                    if (hasEmittedTokens) {
                        Log.w(tag, "Key[$keyIndex] 已向用户输出部分内容，根据保护策略不再自动重试以避免重复输出: ${e.message}")
                        throw e
                    }

                    // 只有在完全没有收到任何内容时才自动重试
                    if (isNetworkFluctuationException(e)) {
                        attempt++
                        if (attempt <= maxTimeoutAttempts) {
                            val delayMs = retryDelays.getOrElse(attempt - 1) { 5000L }
                            val retryText = "网络波动，正在尝试重新连接 ($attempt/$maxTimeoutAttempts)..."
                            Log.w(tag, "Key[$keyIndex] $retryText (退避等待 ${delayMs}ms) - 异常: ${e.javaClass.simpleName}: ${e.message}")
                            onStatusUpdate?.invoke(retryText)
                            kotlinx.coroutines.delay(delayMs)
                            continue
                        } else {
                            val failText = if (keyIndex + 1 < allKeys.size) {
                                "网络重连重试已达 $maxTimeoutAttempts 次，自动尝试下一个 Key (${keyIndex + 2}/${allKeys.size})..."
                            } else {
                                "网络波动，重连重试已达 $maxTimeoutAttempts 次"
                            }
                            Log.w(tag, failText)
                            onStatusUpdate?.invoke(failText)
                            break
                        }
                    } else {
                        // 无论客户端参数/模型错误(400, 404, 422)还是服务端错误(401, 403, 429, 500)，只要还有备用Key且未收到内容就自动尝试下一个Key
                        if (keyIndex + 1 < allKeys.size) {
                            val nextIdx = keyIndex + 2
                            val failText = "当前 Key 异常(${e.message?.take(40)})，正在自动尝试备用 Key ($nextIdx/${allKeys.size})..."
                            Log.w(tag, "Key[$keyIndex] 请求报错: ${e.message}，自动尝试备用 Key")
                            onStatusUpdate?.invoke(failText)
                            break
                        } else {
                            val failText = "Key[${keyIndex + 1}] 请求报错: ${e.message}"
                            Log.w(tag, failText)
                            onStatusUpdate?.invoke(failText)
                            throw e
                        }
                    }
                }
            }
        }

        // 所有 Key 都尝试失败
        throw lastException ?: Exception("所有 API Key 均连接失败或报错")
    }

    private suspend fun retryWithCompressedContext(
        config: ApiConfig,
        conversationId: Long,
        userMessage: String,
        attachments: List<Attachment>,
        options: ChatRequestOptions?,
        assistantVariantGroupId: String?,
        assistantVariantIndex: Int,
        onToken: (String) -> Unit,
        onThinkingToken: (String) -> Unit,
        onResetBuffer: (() -> Unit)? = null,
        onComplete: (String, String?, Any?) -> Unit,
        originalError: Exception
    ): Boolean {
        val retryWindow = extractContextWindowFromError(originalError.message.orEmpty())
            ?: CONTEXT_OVERFLOW_RETRY_WINDOW_TOKENS
        val requestModel = config.modelName
        runtimeContextWindowLimitCache[requestModel.lowercase()] = retryWindow
        runCatching {
            compressConversationContext(
                conversationId = conversationId,
                modelNameOverride = requestModel,
                maxOutputTokens = options?.maxTokens
            )
        }.onFailure {
            Log.w(tag, "上下文超限后自动压缩失败，仍尝试缩小窗口重试", it)
        }

        val retryOptions = (options ?: ChatRequestOptions()).copy(
            contextWindowOverrideTokens = retryWindow
        )
        return runCatching {
            dispatchChatMessageWithConfig(
                config = config,
                conversationId = conversationId,
                userMessage = userMessage,
                attachments = attachments,
                options = retryOptions,
                assistantVariantGroupId = assistantVariantGroupId,
                assistantVariantIndex = assistantVariantIndex,
                onToken = onToken,
                onThinkingToken = onThinkingToken,
                onResetBuffer = onResetBuffer,
                onComplete = onComplete
            )
        }.onFailure {
            Log.e(tag, "上下文压缩重试仍失败", it)
        }.isSuccess
    }

    // OpenAI格式发送消息
    private suspend fun sendOpenAIMessage(
        config: ApiConfig,
        conversationId: Long,
        historyMessages: List<Message>,
        userMessage: String,
        attachments: List<Attachment>,
        options: ChatRequestOptions? = null,
        assistantVariantGroupId: String? = null,
        assistantVariantIndex: Int = 1,
        onToken: (String) -> Unit,
        onThinkingToken: (String) -> Unit,
        onComplete: (String, String?, Any?) -> Unit
    ) {
        val contextMessages = historyMessages.dropLastCurrentUserMessage(userMessage)
        val conversation = getConversationById(conversationId)
        val effectiveOptions = resolveChatRequestOptions(config, options)
        val requestModel = resolveRequestModel(config, effectiveOptions)
        val contextBundle = buildContextBundle(
            conversation = conversation,
            config = config,
            messages = contextMessages,
            modelName = requestModel,
            maxOutputTokens = effectiveOptions.maxTokens,
            contextWindowOverrideTokens = effectiveOptions.contextWindowOverrideTokens,
            currentUserMessage = userMessage,
            options = effectiveOptions
        )
        val enrichedResult = enrichUserMessageWithWebSearch(userMessage, effectiveOptions)
        val enrichedUserMessage = enrichedResult.enrichedPrompt
        val toolCalls = enrichedResult.toolCalls
        val toolCallsJson = if (toolCalls.isNotEmpty()) gson.toJson(toolCalls) else null
        val chatMessages = mutableListOf<ChatMessage>()

        val isRoleplayConv = conversation != null && (hasConversationTag(conversation, "roleplay") || hasConversationTag(conversation, "story"))
        val promptResolution = resolveSystemPromptWithPriority(conversation, effectiveOptions, isRoleplayConv)

        val worldBookBlock = if (!isRoleplayConv && effectiveOptions.enableWorldBook == true && userMessage.isNotBlank()) {
            val bookIds = effectiveOptions.activeWorldBookIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() }
            val matchedEntries = matchWorldBookEntries(userMessage, bookIds)
            if (matchedEntries.isNotEmpty()) {
                "<world_book_lore>\n${formatWorldBookPrompt(matchedEntries)}\n</world_book_lore>"
            } else null
        } else null

        buildEffectiveSystemPrompt(
            customPrompt = promptResolution.first,
            olderSummary = contextBundle.summary,
            memoryBlock = contextBundle.memoryBlock,
            options = effectiveOptions,
            isConversationSpecific = promptResolution.second,
            isRoleplay = isRoleplayConv,
            worldBookBlock = worldBookBlock
        )?.let {
            chatMessages.add(ChatMessage(role = "system", content = it))
        }

        contextBundle.recentMessages.forEach { msg ->
            if (msg.role == "user" || msg.role == "assistant") {
                chatMessages.add(ChatMessage(role = msg.role, content = compactMessageForHistory(msg.content)))
            }
        }

        // 双端注水机制 (Dual-Anchor Prompting)：当长历史超过6轮或存在摘要时，注入尾部系统强化声明
        val customPrompt = promptResolution.first
        if (!isRoleplayConv && !customPrompt.isNullOrBlank() && (contextBundle.recentMessages.size >= 6 || contextBundle.summary != null)) {
            val tailOverride = "[System Override Directive / 核心指令强化声明]\n" +
                "请注意：用户已对当前对话设定了最新的行为规范与提示词要求。\n" +
                "无论前序历史对话风格如何，你必须立即完全遵循以下最新指令，放弃先前的惯性回复模式：\n" +
                customPrompt.trim()
            chatMessages.add(ChatMessage(role = "system", content = tailOverride))
        }

        // 构建当前用户消息（支持多模态）
        val userContent = buildUserMessage(enrichedUserMessage, attachments)
        chatMessages.add(ChatMessage(role = "user", content = userContent))

        // 创建请求 - OpenAI格式不发送top_k
        val searchIsReady = echoToolHub?.let {
            when (it.getSearchEngine()) {
                com.aiassistant.tools.search.SearchEngineType.EXA -> true
                com.aiassistant.tools.search.SearchEngineType.TAVILY -> tavilySearchManager.isReady()
            }
        } ?: tavilySearchManager.isReady()

        val providerToggles = buildOpenAiProviderToggles(
            config = config,
            options = effectiveOptions,
            allowNativeWebSearch = !searchIsReady
        )
        val request = ChatCompletionRequest(
            model = requestModel,
            messages = chatMessages,
            temperature = requestTemperature(config, effectiveOptions),
            max_tokens = effectiveOptions.maxTokens,
            top_p = effectiveOptions.topP,
            top_k = if (providerToggles.includeTopK) config.topK else null,
            stream = true,
            frequency_penalty = config.frequencyPenalty.takeIf { it != 0.0f },
            presence_penalty = config.presencePenalty.takeIf { it != 0.0f },
            stop = parseStopSequences(config.stopSequences),
            seed = config.seed,
            response_format = config.responseFormat?.let { ResponseFormat(it) },
            stream_options = StreamOptions(include_usage = true),
            web_search_options = if (providerToggles.includeOpenAiSearchOptions) WebSearchOptions(
                search_context_size = config.searchContextSize
            ) else null,
            enable_search = if (providerToggles.includeGenericSearch) true else null,
            web_search = if (providerToggles.includeGenericSearch) true else null,
            search_context_size = if (providerToggles.includeGenericSearch) config.searchContextSize else null,
            enable_thinking = if (providerToggles.includeEnableThinking) true else null,
            thinking_budget = if (providerToggles.includeThinkingBudget) thinkingBudgetForEffort(effectiveOptions.thinkingEffort, config.thinkingBudget) else null,
            thinking_effort = if (providerToggles.includeThinkingEffort) effectiveOptions.thinkingEffort else null,
            reasoning_effort = if (providerToggles.includeReasoningEffort) effectiveOptions.thinkingEffort else null
        )

        val auth = RetrofitClient.formatApiKey(config.apiKey)

        // 发送流式请求
        val startTime = System.currentTimeMillis()
        var activeCall: Call? = null
        try {
            val response = RetrofitClient.postJson(
                baseUrl = config.baseUrl,
                path = "chat/completions",
                headers = mapOf(
                    "Authorization" to auth,
                    "Accept" to "text/event-stream",
                    "Cache-Control" to "no-cache"
                ),
                json = gson.toJson(request),
                onCallCreated = { call ->
                    activeCall = call
                    activeStreamingCalls[conversationId] = call
                }
            )

            response.use { okResponse ->
            if (okResponse.isSuccessful) {
                val responseBody = okResponse.body
                    ?: throw Exception("响应体为空")

                val contentBuilder = StringBuilder()
                val thinkingBuilder = StringBuilder()
                var isInThinkTag = false
                var totalTokens = 0
                var inputTokens = 0
                var outputTokens = 0
                var thinkingTokens = 0
                var cachedTokens = 0

                var hasReceivedDone = false
                var lastFinishReason: String? = null
                var streamReadException: Exception? = null
                val jsonAccumulator = StringBuilder()

                // 健壮读取 SSE / NDJSON 流：兼容 BOM、缺失[DONE]、缺失finish_reason、空行、注释及纯JSON
                try {
                    responseBody.byteStream().bufferedReader(Charsets.UTF_8).use { reader ->
                        while (true) {
                            val nextLine = try {
                                reader.readLine()
                            } catch (ioEx: Exception) {
                                if (isRequestCancellation(ioEx)) throw ioEx
                                streamReadException = ioEx
                                Log.w(tag, "流式网络读取中断 (${ioEx.javaClass.simpleName}): ${ioEx.message}")
                                null
                            } ?: break

                            var lineStr = nextLine
                            if (lineStr.startsWith("\uFEFF")) {
                                lineStr = lineStr.removePrefix("\uFEFF")
                            }
                            lineStr = lineStr.trim()
                            if (lineStr.isEmpty() || lineStr.startsWith(":")) continue

                            // 兼容多行换行缩进的完整 JSON 返回
                            val isPartialJsonStart = (lineStr.startsWith("{") && !lineStr.endsWith("}"))
                            if (jsonAccumulator.isNotEmpty() || isPartialJsonStart) {
                                jsonAccumulator.append(lineStr).append("\n")
                                if (lineStr.endsWith("}") || lineStr == "}") {
                                    lineStr = jsonAccumulator.toString().trim()
                                    jsonAccumulator.clear()
                                } else {
                                    continue
                                }
                            }

                            val chunkResult = parseOpenAiStreamLine(lineStr, gson) ?: continue

                            if (chunkResult.isDone) {
                                hasReceivedDone = true
                                break
                            }

                            // 检测流式返回的内联错误
                            if (!chunkResult.inlineErrorMessage.isNullOrBlank()) {
                                if (contentBuilder.isNotEmpty() || thinkingBuilder.isNotEmpty()) {
                                    Log.w(tag, "流式接收过程中遇到内联错误: ${chunkResult.inlineErrorMessage}，但已接收到部分内容，保留已收到内容平稳完成")
                                    break
                                } else {
                                    throw ApiException(400, "API流式返回错误: ${chunkResult.inlineErrorMessage}")
                                }
                            }

                            if (!chunkResult.finishReason.isNullOrBlank()) {
                                lastFinishReason = chunkResult.finishReason
                            }

                            chunkResult.thinkingDelta?.let { thinking ->
                                thinkingBuilder.append(thinking)
                                onThinkingToken(thinking)
                            }

                            chunkResult.contentDelta?.let { content ->
                                if (isInThinkTag) {
                                    if (content.contains("</think>")) {
                                        val parts = content.split("</think>", limit = 2)
                                        val inside = parts[0]
                                        val after = parts.getOrNull(1).orEmpty()
                                        if (inside.isNotEmpty()) {
                                            thinkingBuilder.append(inside)
                                            onThinkingToken(inside)
                                        }
                                        isInThinkTag = false
                                        if (after.isNotEmpty()) {
                                            contentBuilder.append(after)
                                            onToken(after)
                                        }
                                    } else {
                                        thinkingBuilder.append(content)
                                        onThinkingToken(content)
                                    }
                                } else if (content.contains("<think>")) {
                                    val parts = content.split("<think>", limit = 2)
                                    val before = parts[0]
                                    val insideAndAfter = parts.getOrNull(1).orEmpty()
                                    if (before.isNotEmpty()) {
                                        contentBuilder.append(before)
                                        onToken(before)
                                    }
                                    if (insideAndAfter.contains("</think>")) {
                                        val subParts = insideAndAfter.split("</think>", limit = 2)
                                        val inside = subParts[0]
                                        val after = subParts.getOrNull(1).orEmpty()
                                        if (inside.isNotEmpty()) {
                                            thinkingBuilder.append(inside)
                                            onThinkingToken(inside)
                                        }
                                        isInThinkTag = false
                                        if (after.isNotEmpty()) {
                                            contentBuilder.append(after)
                                            onToken(after)
                                        }
                                    } else {
                                        isInThinkTag = true
                                        if (insideAndAfter.isNotEmpty()) {
                                            thinkingBuilder.append(insideAndAfter)
                                            onThinkingToken(insideAndAfter)
                                        }
                                    }
                                } else {
                                    contentBuilder.append(content)
                                    onToken(content)
                                }
                            }

                            chunkResult.usage?.let { usage ->
                                inputTokens = usage.prompt_tokens ?: inputTokens
                                val rawCompletionTokens = usage.completion_tokens ?: outputTokens
                                thinkingTokens = usage.completion_tokens_details?.reasoning_tokens ?: thinkingTokens
                                outputTokens = (rawCompletionTokens - thinkingTokens).coerceAtLeast(0)
                                val parsedCached = extractCachedTokensFromUsage(usage)
                                if (parsedCached > 0) {
                                    cachedTokens = parsedCached
                                }
                                totalTokens = usage.total_tokens ?: (inputTokens + outputTokens + thinkingTokens)
                            }
                        }
                    }
                } catch (streamEx: Exception) {
                    if (isRequestCancellation(streamEx)) throw streamEx
                    streamReadException = streamEx
                    Log.w(tag, "流式解析外层异常: ${streamEx.javaClass.simpleName}: ${streamEx.message}")
                }

                val responseTime = System.currentTimeMillis() - startTime
                var fullContent = contentBuilder.toString()
                var fullThinking = thinkingBuilder.toString().ifEmpty { null }

                // 兜底提取：若思考内容未被流式单独捕获但文本中带有 <think> 标签，将其分离并持久化到 thinkingContent
                if (fullThinking == null && fullContent.contains("<think>", ignoreCase = true)) {
                    val thinkRegex = Regex("<think>([\\s\\S]*?)(?:</think>|$)", RegexOption.IGNORE_CASE)
                    val match = thinkRegex.find(fullContent)
                    if (match != null) {
                        val extracted = match.groupValues[1].trim()
                        if (extracted.isNotEmpty()) {
                            fullThinking = extracted
                        }
                        fullContent = fullContent.replace(match.value, "").trim()
                    }
                }

                val hasReceivedContent = fullContent.isNotBlank() || !fullThinking.isNullOrBlank()

                // 需求 2：只在“完全没有收到任何 delta.content”（且无思考、无工具调用）时，才判定为失败
                if (!hasReceivedContent && toolCalls.isEmpty()) {
                    streamReadException?.let { throw it }
                    throw ApiException(500, "模型回复内容为空 (empty response detected)，未收到任何有效的文本或思考内容")
                }

                // 需求 3：如果流结束时缺少 finish_reason 或 [DONE]，不要抛异常，只记录 warning 并给结果标记 finished: false
                val isFinished = (hasReceivedDone || !lastFinishReason.isNullOrBlank()) && streamReadException == null
                if (!isFinished) {
                    Log.w(tag, "流式响应结束但缺少 finish_reason 或 [DONE] (finished: false, finishReason: $lastFinishReason, hasDone: $hasReceivedDone, streamException: ${streamReadException?.message}), 内容正常保留输出 (${fullContent.length} 字符)")
                }

                val finalThinkingTokens = thinkingTokens.takeIf { it > 0 } ?: estimateTokenCount(fullThinking.orEmpty())
                val finalOutputTokens = outputTokens.takeIf { it > 0 } ?: estimateTokenCount(fullContent)
                val finalInputTokens = inputTokens.takeIf { it > 0 }
                    ?: chatMessages.sumOf { estimateTokenCount(it.content.toString()) }
                val finalTotalTokens = totalTokens.takeIf { it > 0 }
                    ?: (finalInputTokens + finalOutputTokens + finalThinkingTokens)

                // 保存助手消息
                val assistantMsg = Message(
                    conversationId = conversationId,
                    role = "assistant",
                    content = fullContent,
                    thinkingContent = fullThinking,
                    variantGroupId = assistantVariantGroupId,
                    variantIndex = assistantVariantIndex,
                    tokenCount = finalTotalTokens,
                    thinkingTokens = finalThinkingTokens,
                    responseTime = responseTime,
                    toolCalls = toolCallsJson
                )
                saveMessage(assistantMsg)

                // 记录使用统计
                val stat = ApiUsageStat(
                    apiConfigId = config.id,
                    provider = config.provider,
                    modelName = requestModel,
                    inputTokens = finalInputTokens,
                    outputTokens = finalOutputTokens,
                    thinkingTokens = finalThinkingTokens,
                    totalTokens = finalTotalTokens,
                    cachedTokens = cachedTokens,
                    responseTime = responseTime,
                    success = true
                )
                try {
                    usageStatDao.insertStat(stat)
                } catch (statEx: Exception) {
                    Log.e(tag, "记录成功统计异常", statEx)
                }

                onComplete(fullContent, fullThinking, toolCalls)
            } else {
                val errorBody = okResponse.body?.string() ?: "未知错误"
                val errorMsg = parseApiErrorMessage(errorBody)
                val statusCode = okResponse.code
                throw ApiException(statusCode, "API错误 ($statusCode): $errorMsg", errorBody)
            }
        }
        } finally {
            activeCall?.let { activeStreamingCalls.remove(conversationId, it) }
        }
    }

    // Anthropic格式发送消息
    private suspend fun sendAnthropicMessage(
        config: ApiConfig,
        conversationId: Long,
        historyMessages: List<Message>,
        userMessage: String,
        attachments: List<Attachment>,
        options: ChatRequestOptions? = null,
        assistantVariantGroupId: String? = null,
        assistantVariantIndex: Int = 1,
        onToken: (String) -> Unit,
        onThinkingToken: (String) -> Unit,
        onComplete: (String, String?, Any?) -> Unit
    ) {
        val contextMessages = historyMessages.dropLastCurrentUserMessage(userMessage)

        // 获取系统提示
        val conversation = getConversationById(conversationId)
        val effectiveOptions = resolveChatRequestOptions(config, options)
        val requestModel = resolveRequestModel(config, effectiveOptions)
        val contextBundle = buildContextBundle(
            conversation = conversation,
            config = config,
            messages = contextMessages,
            modelName = requestModel,
            maxOutputTokens = effectiveOptions.maxTokens,
            contextWindowOverrideTokens = effectiveOptions.contextWindowOverrideTokens,
            currentUserMessage = userMessage,
            options = effectiveOptions
        )
        val isRoleplayConv = conversation != null && (hasConversationTag(conversation, "roleplay") || hasConversationTag(conversation, "story"))
        val promptResolution = resolveSystemPromptWithPriority(conversation, effectiveOptions, isRoleplayConv)

        val worldBookBlock = if (!isRoleplayConv && effectiveOptions.enableWorldBook == true && userMessage.isNotBlank()) {
            val bookIds = effectiveOptions.activeWorldBookIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() }
            val matchedEntries = matchWorldBookEntries(userMessage, bookIds)
            if (matchedEntries.isNotEmpty()) {
                "<world_book_lore>\n${formatWorldBookPrompt(matchedEntries)}\n</world_book_lore>"
            } else null
        } else null

        val systemPrompt = buildEffectiveSystemPrompt(
            customPrompt = promptResolution.first,
            olderSummary = contextBundle.summary,
            memoryBlock = contextBundle.memoryBlock,
            options = effectiveOptions,
            isConversationSpecific = promptResolution.second,
            isRoleplay = isRoleplayConv,
            worldBookBlock = worldBookBlock
        )
        val anthropicMessages = mutableListOf<AnthropicMessage>()

        // 添加历史消息
        contextBundle.recentMessages.dropWhile { it.role == "assistant" }.forEach { msg ->
            when (msg.role) {
                "user" -> addAnthropicHistoryMessage(anthropicMessages, "user", compactMessageForHistory(msg.content))
                "assistant" -> addAnthropicHistoryMessage(anthropicMessages, "assistant", compactMessageForHistory(msg.content))
            }
        }

        // 构建当前用户消息（支持多模态）
        val enrichedResult = enrichUserMessageWithWebSearch(userMessage, effectiveOptions)
        val enrichedUserMessage = enrichedResult.enrichedPrompt
        val toolCalls = enrichedResult.toolCalls
        val toolCallsJson = if (toolCalls.isNotEmpty()) gson.toJson(toolCalls) else null
        val userContent = buildAnthropicUserMessage(enrichedUserMessage, attachments)
        if (userContent is String) {
            addAnthropicHistoryMessage(anthropicMessages, "user", userContent)
        } else {
            anthropicMessages.add(AnthropicMessage(role = "user", content = userContent))
        }

        val thinkingBudget = if (effectiveOptions.enableThinking == true) {
            thinkingBudgetForEffort(effectiveOptions.thinkingEffort, config.thinkingBudget)
        } else null
        val configuredMaxTokens = effectiveOptions.maxTokens ?: config.maxTokens
        val requestMaxTokens = if (thinkingBudget != null && configuredMaxTokens <= thinkingBudget) {
            thinkingBudget + 2048
        } else {
            configuredMaxTokens
        }

        // 创建请求 - Anthropic格式支持top_k
        val request = AnthropicRequest(
            model = requestModel,
            messages = anthropicMessages,
            max_tokens = requestMaxTokens,
            system = systemPrompt,
            temperature = requestTemperature(config, effectiveOptions),
            top_p = effectiveOptions.topP,
            top_k = if (config.topK != 50) config.topK else null,
            stream = true,
            stop_sequences = parseStopSequences(config.stopSequences),
            thinking = if (thinkingBudget != null) {
                AnthropicThinking(budget_tokens = thinkingBudget)
            } else null
        )

        // 发送流式请求
        val startTime = System.currentTimeMillis()
        var activeCall: Call? = null
        try {
            val response = RetrofitClient.postJson(
                baseUrl = config.baseUrl,
                path = "messages",
                headers = mapOf(
                    "x-api-key" to config.apiKey,
                    "anthropic-version" to "2023-06-01",
                    "Accept" to "text/event-stream",
                    "Cache-Control" to "no-cache"
                ),
                json = gson.toJson(request),
                onCallCreated = { call ->
                    activeCall = call
                    activeStreamingCalls[conversationId] = call
                }
            )

            response.use { okResponse ->
            if (okResponse.isSuccessful) {
                val responseBody = okResponse.body
                    ?: throw Exception("响应体为空")

                val contentBuilder = StringBuilder()
                val thinkingBuilder = StringBuilder()
                var totalTokens = 0
                var inputTokens = 0
                var outputTokens = 0
                var cachedTokens = 0

                var anthropicStreamReadException: Exception? = null

                // 读取SSE流
                try {
                    responseBody.byteStream().bufferedReader(Charsets.UTF_8).use { reader ->
                        while (true) {
                            val nextLine = try {
                                reader.readLine()
                            } catch (ioEx: Exception) {
                                if (isRequestCancellation(ioEx)) throw ioEx
                                anthropicStreamReadException = ioEx
                                Log.w(tag, "Anthropic流式读取遇到IO中断 (${ioEx.javaClass.simpleName}): ${ioEx.message}")
                                null
                            } ?: break

                            var lineStr = nextLine
                            if (lineStr.startsWith("\uFEFF")) {
                                lineStr = lineStr.removePrefix("\uFEFF")
                            }
                            lineStr = lineStr.trim()
                            if (lineStr.isEmpty() || lineStr.startsWith(":") || lineStr.startsWith("event: ")) continue

                            if (lineStr.startsWith("data:")) {
                                val data = lineStr.removePrefix("data:").trimStart()
                                if (data == "[DONE]") break

                                try {
                                    val event = gson.fromJson(data, AnthropicStreamEvent::class.java)

                                    when (event.type) {
                                        "message_start" -> {
                                            event.message?.usage?.let { usage ->
                                                inputTokens = usage.input_tokens ?: inputTokens
                                                cachedTokens = (usage.cache_read_input_tokens ?: 0) + (usage.cache_creation_input_tokens ?: 0)
                                            }
                                        }
                                        "content_block_delta" -> {
                                            // 处理文本内容
                                            event.delta?.text?.let { text ->
                                                contentBuilder.append(text)
                                                onToken(text)
                                            }
                                            // 处理思考内容
                                            event.delta?.thinking?.let { thinking ->
                                                thinkingBuilder.append(thinking)
                                                onThinkingToken(thinking)
                                            }
                                        }
                                        "message_delta" -> {
                                            event.usage?.let { usage ->
                                                inputTokens = usage.input_tokens ?: inputTokens
                                                outputTokens = usage.output_tokens ?: outputTokens
                                                cachedTokens = usage.cache_read_input_tokens ?: cachedTokens
                                                totalTokens = inputTokens + outputTokens
                                            }
                                        }
                                        "error" -> {
                                            val errDetail = event.error?.message ?: "Anthropic流式返回未知错误"
                                            if (contentBuilder.isNotEmpty() || thinkingBuilder.isNotEmpty()) {
                                                Log.w(tag, "Anthropic流式输出过程中遇到错误: $errDetail，保留已输出内容平稳完成")
                                                break
                                            } else {
                                                throw ApiException(400, "Anthropic错误: $errDetail", data)
                                            }
                                        }
                                    }
                                } catch (e: ApiException) {
                                    throw e
                                } catch (e: Exception) {
                                    Log.w(tag, "解析Anthropic chunk失败: $data", e)
                                }
                            }
                        }
                    }
                } catch (streamEx: Exception) {
                    if (isRequestCancellation(streamEx)) throw streamEx
                    anthropicStreamReadException = streamEx
                    Log.w(tag, "Anthropic流式读取外层异常: ${streamEx.javaClass.simpleName}: ${streamEx.message}")
                }

                val responseTime = System.currentTimeMillis() - startTime
                var fullContent = contentBuilder.toString()
                var fullThinking = thinkingBuilder.toString().ifEmpty { null }

                if (fullThinking == null && fullContent.contains("<think>", ignoreCase = true)) {
                    val thinkRegex = Regex("<think>([\\s\\S]*?)(?:</think>|$)", RegexOption.IGNORE_CASE)
                    val match = thinkRegex.find(fullContent)
                    if (match != null) {
                        val extracted = match.groupValues[1].trim()
                        if (extracted.isNotEmpty()) {
                            fullThinking = extracted
                        }
                        fullContent = fullContent.replace(match.value, "").trim()
                    }
                }

                val hasReceivedContent = fullContent.isNotBlank() || !fullThinking.isNullOrBlank()

                if (!hasReceivedContent && toolCalls.isEmpty()) {
                    anthropicStreamReadException?.let { throw it }
                    throw ApiException(500, "Anthropic 模型回复内容为空 (empty response detected)，可能触发限制或API Key异常")
                }
                val finalThinkingTokens = estimateTokenCount(fullThinking.orEmpty())
                val finalOutputTokens = outputTokens.takeIf { it > 0 } ?: estimateTokenCount(fullContent)
                val finalInputTokens = inputTokens.takeIf { it > 0 }
                    ?: anthropicMessages.sumOf { estimateTokenCount(it.content.toString()) }
                val finalTotalTokens = totalTokens.takeIf { it > 0 }
                    ?: (finalInputTokens + finalOutputTokens + finalThinkingTokens)

                // 保存助手消息
                val assistantMsg = Message(
                    conversationId = conversationId,
                    role = "assistant",
                    content = fullContent,
                    thinkingContent = fullThinking,
                    variantGroupId = assistantVariantGroupId,
                    variantIndex = assistantVariantIndex,
                    tokenCount = finalTotalTokens,
                    thinkingTokens = finalThinkingTokens,
                    responseTime = responseTime,
                    toolCalls = toolCallsJson
                )
                saveMessage(assistantMsg)

                // 记录使用统计
                val stat = ApiUsageStat(
                    apiConfigId = config.id,
                    provider = config.provider,
                    modelName = requestModel,
                    inputTokens = finalInputTokens,
                    outputTokens = finalOutputTokens,
                    thinkingTokens = finalThinkingTokens,
                    totalTokens = finalTotalTokens,
                    cachedTokens = cachedTokens,
                    responseTime = responseTime,
                    success = true
                )
                try {
                    usageStatDao.insertStat(stat)
                } catch (statEx: Exception) {
                    Log.e(tag, "记录成功统计异常", statEx)
                }

                onComplete(fullContent, fullThinking, toolCalls)
            } else {
                val errorBody = okResponse.body?.string() ?: "未知错误"
                val errorMsg = parseApiErrorMessage(errorBody)
                val statusCode = okResponse.code
                throw ApiException(statusCode, "API错误 ($statusCode): $errorMsg", errorBody)
            }
        }
        } finally {
            activeCall?.let { activeStreamingCalls.remove(conversationId, it) }
        }
    }

    private data class OpenAiProviderToggles(
        val includeTopK: Boolean,
        val includeGenericSearch: Boolean,
        val includeOpenAiSearchOptions: Boolean,
        val includeEnableThinking: Boolean,
        val includeThinkingBudget: Boolean,
        val includeThinkingEffort: Boolean,
        val includeReasoningEffort: Boolean
    )

    private fun resolveChatRequestOptions(
        config: ApiConfig,
        overrides: ChatRequestOptions?
    ): ChatRequestOptions {
        return ChatRequestOptions(
            temperature = (overrides?.temperature ?: config.temperature).coerceIn(0f, temperatureMaxForConfig(config)),
            maxTokens = overrides?.maxTokens ?: config.maxTokens,
            topP = (overrides?.topP ?: config.topP).coerceIn(0f, 1f),
            enableThinking = overrides?.enableThinking ?: config.enableThinking,
            thinkingEffort = normalizeThinkingEffort(overrides?.thinkingEffort ?: config.thinkingEffort, config),
            enableWebSearch = overrides?.enableWebSearch ?: config.enableWebSearch,
            enableSessionMemory = overrides?.enableSessionMemory,
            enableExternalMemory = overrides?.enableExternalMemory,
            enableWorldBook = overrides?.enableWorldBook,
            activeWorldBookIds = overrides?.activeWorldBookIds,
            overrideSystemPrompt = overrides?.overrideSystemPrompt == true,
            systemPromptOverride = overrides?.systemPromptOverride,
            contextWindowOverrideTokens = overrides?.contextWindowOverrideTokens
        )
    }

    fun resolveSystemPromptWithPriority(
        conversation: Conversation?,
        options: ChatRequestOptions,
        isRoleplay: Boolean = conversation != null && (hasConversationTag(conversation, "roleplay") || hasConversationTag(conversation, "story"))
    ): Pair<String?, Boolean> {
        if (options.overrideSystemPrompt && !options.systemPromptOverride.isNullOrBlank()) {
            return Pair(options.systemPromptOverride, true)
        }
        if (isRoleplay) {
            // 角色扮演拥有完全独立的上下文，不读取普通全局提示词
            return Pair(conversation?.systemPrompt, true)
        }
        val convPrompt = conversation?.systemPrompt?.trim()
        if (!convPrompt.isNullOrBlank()) {
            // 对话本身有系统提示词 -> 全局提示词 100% 0作用
            return Pair(convPrompt, true)
        }
        // 对话本身无系统提示词 -> 自动继承使用全局提示词作为兜底
        val globalPrompt = personalizationManager.getSettings().globalSystemPrompt.trim()
        return if (globalPrompt.isNotBlank()) {
            Pair(globalPrompt, false)
        } else {
            Pair(null, false)
        }
    }

    private fun effectiveSystemPrompt(
        conversation: Conversation?,
        options: ChatRequestOptions
    ): String? {
        return resolveSystemPromptWithPriority(conversation, options).first
    }

    private fun normalizeThinkingEffort(effort: String?, config: ApiConfig): String {
        return normalizeThinkingEffort(effort, if (isDeepSeekConfig(config)) "deepseek" else "openai")
    }

    private fun thinkingBudgetForEffort(effort: String?, configuredBudget: Int): Int {
        val base = configuredBudget.coerceIn(1024, 32768)
        return when (effort?.lowercase()) {
            "low" -> (base / 2).coerceIn(1024, 32768)
            "max", "ultra" -> 32768
            "high" -> (base * 2).coerceIn(1024, 32768)
            else -> base
        }
    }

    private fun isDeepSeekConfig(config: ApiConfig): Boolean {
        val identity = listOf(config.provider, config.baseUrl, config.modelName).joinToString(" ").lowercase()
        return "deepseek" in identity
    }

    private fun isMiMoConfig(config: ApiConfig): Boolean {
        val identity = listOf(config.provider, config.baseUrl, config.modelName).joinToString(" ").lowercase()
        return "mimo" in identity || "xiaomi" in identity
    }

    private fun temperatureMaxForConfig(config: ApiConfig): Float {
        return if (isDeepSeekConfig(config)) 2f else 1f
    }

    private fun requestTemperature(config: ApiConfig, options: ChatRequestOptions): Float? {
        val identity = listOf(config.provider, config.baseUrl, config.modelName).joinToString(" ").lowercase()
        val isAnthropic = config.apiType == "anthropic" || config.provider.equals("anthropic", ignoreCase = true) || "anthropic" in identity || "claude" in identity
        if (options.enableThinking == true) {
            if (isAnthropic) {
                return 1.0f
            }
            if (isDeepSeekConfig(config) || isMiMoConfig(config) || Regex("""(^|[-_/])(o[134]|gpt-5|r1)""").containsMatchIn(config.modelName.lowercase())) {
                return null
            }
        }
        return options.temperature?.coerceIn(0f, temperatureMaxForConfig(config))
    }

    private fun resolveRequestModel(config: ApiConfig, options: ChatRequestOptions): String {
        return config.modelName
    }

    fun parseApiErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "未知错误"
        return runCatching {
            val json = JsonParser.parseString(errorBody).asJsonObject
            when {
                json.has("error") -> {
                    val errorElem = json.get("error")
                    when {
                        errorElem.isJsonObject -> {
                            val errObj = errorElem.asJsonObject
                            errObj.get("message")?.asString
                                ?: errObj.get("msg")?.asString
                                ?: errObj.toString()
                        }
                        errorElem.isJsonPrimitive -> errorElem.asString
                        else -> errorElem.toString()
                    }
                }
                json.has("message") -> json.get("message").asString
                json.has("detail") -> {
                    val detail = json.get("detail")
                    if (detail.isJsonPrimitive) detail.asString else detail.toString()
                }
                json.has("msg") -> json.get("msg").asString
                else -> errorBody
            }
        }.getOrDefault(errorBody)
    }

    private fun buildOpenAiProviderToggles(
        config: ApiConfig,
        options: ChatRequestOptions?,
        allowNativeWebSearch: Boolean = true
    ): OpenAiProviderToggles {
        val identity = listOf(config.provider, config.baseUrl, config.modelName)
            .joinToString(" ")
            .lowercase()
        val wantsSearch = options?.enableWebSearch == true && allowNativeWebSearch
        val wantsThinking = options?.enableThinking == true
        val isDeepSeek = "deepseek" in identity
        val isMiMo = "mimo" in identity || "xiaomi" in identity
        val isOpenAi = "openai" in identity || "api.openai.com" in identity
        val capability = com.aiassistant.domain.model.ModelCapabilityEngine.evaluateModel(
            config.modelName, config.provider, config.baseUrl
        )
        val isOpenAiReasoningModel = capability.reasoningProviderType == "openai" ||
            Regex("""(^|[-_/])(o[134]|gpt-5)""").containsMatchIn(config.modelName.lowercase())

        return OpenAiProviderToggles(
            includeTopK = options != null && isMiMo,
            includeGenericSearch = wantsSearch && !isDeepSeek && !isOpenAi,
            includeOpenAiSearchOptions = wantsSearch && isOpenAi,
            includeEnableThinking = wantsThinking && !isDeepSeek && !isOpenAiReasoningModel,
            includeThinkingBudget = wantsThinking && !isDeepSeek && !isOpenAiReasoningModel && !isMiMo,
            includeThinkingEffort = wantsThinking && !isDeepSeek && !isOpenAiReasoningModel && !isMiMo,
            includeReasoningEffort = wantsThinking && isOpenAiReasoningModel
        )
    }

    private data class ContextBundle(
        val summary: String?,
        val memoryBlock: String?,
        val recentMessages: List<Message>
    )

    private suspend fun buildContextUsageSnapshot(
        conversation: Conversation,
        messages: List<Message>,
        modelName: String,
        maxOutputTokens: Int?
    ): ConversationContextUsage {
        val usableMessages = messages.filter { message ->
            (message.role == "user" || message.role == "assistant") && message.content.isNotBlank() && !message.isExcluded
        }

        val contextWindow = estimateModelContextWindowTokens(modelName)
        val promptBudget = estimatePromptBudgetTokens(modelName, maxOutputTokens)
        val summaryBudget = (promptBudget * SUMMARY_BUDGET_RATIO).toInt().coerceIn(600, 1_800)
        val memoryBudget = (promptBudget * MEMORY_BUDGET_RATIO).toInt().coerceIn(300, 1_200)
        val recentBudget = (
            promptBudget - summaryBudget - memoryBudget - SYSTEM_PROMPT_TOKEN_RESERVE
        ).coerceAtLeast(MIN_RECENT_CONTEXT_TOKENS)

        val hasRollingSummary = !conversation.rollingSummary.isNullOrBlank()
        val summarizedThrough = conversation.summaryUpdatedMessageId ?: 0L

        // 开源规范（ConversationSummaryBufferMemory 机制）：
        // 若存在滚动摘要且记录了截断点，活跃上下文仅包含截断点之后的新消息与显式钉住 (isPinned) 的消息；
        // 早期历史消息已被归约为滚动摘要，不再作为原文消耗上下文输入预算。
        val activeCandidateMessages = if (hasRollingSummary && summarizedThrough > 0L) {
            usableMessages.filter { it.id > summarizedThrough || it.isPinned }
        } else {
            usableMessages
        }

        var recentTokens = 0
        var recentCount = 0
        for (message in usableMessages.asReversed()) {
            val cost = estimateTokenCount(compactMessageForHistory(message.content)) + 24
            if (recentCount > 0 && recentTokens + cost > recentBudget && !message.isPinned) break
            recentTokens += cost
            recentCount++
        }

        val olderCount = (usableMessages.size - recentCount).coerceAtLeast(0)
        val lastOlderMessageId = if (usableMessages.size >= 4) {
            usableMessages.dropLast(2).lastOrNull()?.id
        } else null

        val summaryTokens = if (olderCount > 0 && hasRollingSummary) {
            conversation.rollingSummary?.let { estimateTokenCount(compactTextToTokenBudget(it, summaryBudget)) } ?: 0
        } else 0

        val latestUserMessage = usableMessages.lastOrNull { it.role == "user" }?.content.orEmpty()
        val memoryBlock = buildRelevantMemoryBlock(conversation, latestUserMessage, memoryBudget)
        val memoryTokens = memoryBlock?.let(::estimateTokenCount) ?: 0
        val memoryItemCount = memoryDao.getCandidateMemories(conversation.id).size

        val estimatedInputTokens = (
            SYSTEM_PROMPT_TOKEN_RESERVE +
                recentTokens +
                summaryTokens.coerceAtMost(summaryBudget) +
                memoryTokens.coerceAtMost(memoryBudget)
        ).coerceAtLeast(0)
        val calculatedUsagePercent = (estimatedInputTokens / promptBudget.toFloat()).coerceIn(0f, 1f)

        // 需求 6：修复始终提示“有较早信息尚未进入摘要”
        // 判定准则（符合 ConversationSummaryBufferMemory 规范）：
        // 1. 只有当存在真正溢出活跃预算的较早历史消息（olderCount > 0 且其中包含未归约的消息）；
        // 2. 或当前活跃上下文占用率达到中高负载水位（>= 50% 且存在可压缩消息）时，才激活可压缩预警状态；
        // 3. 当处于健康短对话或历史完全包含在活跃窗口内时，不激活可压缩（杜绝无意义红点与误报提示）。
        val hasPendingOlderOverflow = olderCount > 0 && lastOlderMessageId != null && (summarizedThrough == 0L || summarizedThrough < lastOlderMessageId)
        val isHighContextPressure = calculatedUsagePercent >= 0.50f && usableMessages.size >= 6 && lastOlderMessageId != null && (summarizedThrough == 0L || summarizedThrough < lastOlderMessageId)
        val canCompress = hasPendingOlderOverflow || isHighContextPressure

        return ConversationContextUsage(
            contextWindowTokens = contextWindow,
            promptBudgetTokens = promptBudget,
            estimatedInputTokens = estimatedInputTokens,
            usagePercent = calculatedUsagePercent,
            recentMessageCount = recentCount,
            olderMessageCount = olderCount,
            recentTokens = recentTokens,
            summaryTokens = summaryTokens,
            memoryTokens = memoryTokens,
            memoryItemCount = memoryItemCount,
            hasRollingSummary = hasRollingSummary,
            summaryUpdatedAt = conversation.summaryUpdatedAt,
            compressedThroughMessageId = conversation.summaryUpdatedMessageId,
            canCompress = canCompress
        )
    }

    private suspend fun buildContextBundle(
        conversation: Conversation?,
        config: ApiConfig,
        messages: List<Message>,
        modelName: String,
        maxOutputTokens: Int?,
        contextWindowOverrideTokens: Int? = null,
        currentUserMessage: String,
        options: ChatRequestOptions? = null
    ): ContextBundle {
        val usableMessages = messages.filter { message ->
            (message.role == "user" || message.role == "assistant") && message.content.isNotBlank() && !message.isExcluded
        }

        val promptBudget = estimatePromptBudgetTokens(
            modelName = modelName,
            maxOutputTokens = maxOutputTokens,
            contextWindowOverrideTokens = contextWindowOverrideTokens
        )
        val summaryBudget = (promptBudget * SUMMARY_BUDGET_RATIO).toInt().coerceIn(600, 1_800)
        val memoryBudget = (promptBudget * MEMORY_BUDGET_RATIO).toInt().coerceIn(300, 1_200)
        val recentBudget = (
            promptBudget - summaryBudget - memoryBudget - SYSTEM_PROMPT_TOKEN_RESERVE
        ).coerceAtLeast(MIN_RECENT_CONTEXT_TOKENS)

        // 上下文按固定优先级组装：长期记忆和滚动摘要先占预算，剩余预算留给最近原文。
        val memoryBlock = conversation?.let {
            buildRelevantMemoryBlock(it, currentUserMessage, memoryBudget, options)
        }

        val hasRollingSummary = !conversation?.rollingSummary.isNullOrBlank()
        val summarizedThrough = conversation?.summaryUpdatedMessageId ?: 0L

        // 真实 ConversationSummaryBufferMemory 原则：
        // 优先保证最近活跃上下文完整（从最新消息倒序向前装填，只要在 recentBudget 预算内，保留完整原始对话）
        var usedTokens = 0
        val recentReversed = mutableListOf<Message>()
        for (message in usableMessages.asReversed()) {
            val compact = compactMessageForHistory(message.content)
            val cost = estimateTokenCount(compact) + 24
            if (recentReversed.isNotEmpty() && usedTokens + cost > recentBudget && !message.isPinned) {
                break
            }
            recentReversed.add(message)
            usedTokens += cost
        }

        val recentMessages = recentReversed.asReversed()
        val olderMessages = usableMessages.dropLast(recentMessages.size)
        val summary = if (olderMessages.isEmpty()) {
            // 如果全部历史对话均已完整包含在最近活跃上下文中，无需注入摘要，保留 100% 原始对话细节
            null
        } else if (hasRollingSummary && summarizedThrough >= (olderMessages.lastOrNull()?.id ?: 0L)) {
            conversation?.rollingSummary
        } else {
            ensureRollingSummary(
                conversation = conversation,
                config = config,
                modelName = modelName,
                olderMessages = olderMessages,
                tokenBudget = summaryBudget
            )
        }

        return ContextBundle(
            summary = summary,
            memoryBlock = memoryBlock,
            recentMessages = recentMessages
        )
    }

    private fun estimatePromptBudgetTokens(
        modelName: String,
        maxOutputTokens: Int?,
        contextWindowOverrideTokens: Int? = null
    ): Int {
        val contextWindow = contextWindowOverrideTokens
            ?.coerceIn(4_000, 2_000_000)
            ?: estimateModelContextWindowTokens(modelName)
        val outputReserve = (maxOutputTokens ?: 4_096).coerceIn(512, 32_768)
        return (contextWindow - outputReserve - 1_024)
            .coerceAtLeast(3_000)
            .coerceAtMost((contextWindow - 512).coerceAtLeast(3_000))
    }

    private fun estimateModelContextWindowTokens(modelName: String): Int {
        val name = modelName.lowercase()
        runtimeContextWindowLimitCache[name]?.let { return it }

        val explicitLimit = parseContextWindowFromText(name)
        if (explicitLimit != null) {
            return explicitLimit.coerceIn(4_000, 2_000_000)
        }

        modelContextWindowCache[name]
            ?: modelContextWindowCache.entries.firstOrNull { (cachedName, _) ->
                cachedName == name ||
                    name.endsWith("/$cachedName") ||
                    cachedName.endsWith("/$name")
            }?.value
            ?.coerceIn(4_000, 2_000_000)
            ?.takeIf { it >= 128_000 }
            ?.let { return it }

        return com.aiassistant.domain.model.ModelCapabilityEngine.evaluateModel(modelName).contextWindowTokens
    }

    private fun parseContextWindowFromText(value: String): Int? {
        val normalized = value.lowercase()
        Regex("""(?<!\d)(\d+(?:\.\d+)?)\s*(m|k)\b""")
            .findAll(normalized)
            .mapNotNull { match ->
                val number = match.groupValues[1].toFloatOrNull() ?: return@mapNotNull null
                val multiplier = if (match.groupValues[2] == "m") 1_000_000 else 1_000
                (number * multiplier).toInt()
            }
            .filter { it in 4_000..2_000_000 }
            .maxOrNull()
            ?.let { return it }

        // 避免误匹配年份数字（如 2024、2025、202405）
        return Regex("""(?<!\d)(?:ctx|context|window|tokens?)?[-_]?([1-9]\d{4,6})(?!\d)""")
            .findAll(normalized)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .filter { it in 16_000..2_000_000 }
            .maxOrNull()
    }

    private suspend fun ensureRollingSummary(
        conversation: Conversation?,
        config: ApiConfig,
        modelName: String,
        olderMessages: List<Message>,
        tokenBudget: Int
    ): String? {
        if (conversation == null || olderMessages.isEmpty()) return null

        val lastOlderMessageId = olderMessages.lastOrNull()?.id ?: return null
        val existingSummary = conversation.rollingSummary?.takeIf { it.isNotBlank() }
        val summarizedThrough = conversation.summaryUpdatedMessageId ?: 0L
        if (existingSummary != null && summarizedThrough >= lastOlderMessageId) {
            return compactTextToTokenBudget(existingSummary, tokenBudget)
        }

        // 只有旧消息足够多时才额外发起摘要请求，避免短对话产生无意义的二次调用或过早归约。
        val pendingMessages = olderMessages.filter { it.id > summarizedThrough }
            .ifEmpty { olderMessages }
        if (
            pendingMessages.size < MIN_SUMMARY_SOURCE_MESSAGES &&
            pendingMessages.sumOf { estimateTokenCount(it.content) } < MIN_SUMMARY_SOURCE_TOKENS
        ) {
            return existingSummary
        }

        val generated = runCatching {
            withTimeoutOrNull(15_000L) {
                generateRollingSummary(config, modelName, existingSummary, pendingMessages, tokenBudget)
            }
        }.onFailure {
            Log.w(tag, "Rolling summary generation failed", it)
        }.getOrNull()

        val finalSummary = generated
            ?.takeIf { it.isNotBlank() }
            ?.let { compactTextToTokenBudget(it, tokenBudget) }
            ?: existingSummary
            ?: buildExtractiveConversationSummary(olderMessages, tokenBudget)

        if (!finalSummary.isNullOrBlank() && olderMessages.size >= MIN_SUMMARY_SOURCE_MESSAGES) {
            conversationDao.updateRollingSummary(
                conversationId = conversation.id,
                summary = finalSummary,
                messageId = lastOlderMessageId
            )
        }

        return finalSummary
    }

    private suspend fun generateRollingSummary(
        config: ApiConfig,
        modelName: String,
        existingSummary: String?,
        pendingMessages: List<Message>,
        tokenBudget: Int
    ): String? {
        val transcript = buildSummaryTranscript(pendingMessages, maxMessages = SUMMARY_TRANSCRIPT_MESSAGE_LIMIT)
        val prompt = AdvancedMemoryEngine.buildStructuredSummaryPrompt(
            existingSummary = existingSummary,
            transcript = transcript,
            tokenBudget = tokenBudget.coerceIn(SUMMARY_PROMPT_MIN_TOKENS, SUMMARY_PROMPT_MAX_TOKENS)
        )

        return if (config.apiType == "anthropic") {
            val request = AnthropicRequest(
                model = modelName,
                messages = listOf(AnthropicMessage(role = "user", content = prompt)),
                max_tokens = tokenBudget.coerceIn(SUMMARY_COMPLETION_MIN_TOKENS, SUMMARY_COMPLETION_MAX_TOKENS),
                temperature = 0.2f
            )
            val response = RetrofitClient.getService(config.baseUrl)
                .anthropicMessages(apiKey = config.apiKey, request = request)
                .execute()
            if (!response.isSuccessful) null else response.body()?.content?.firstOrNull()?.text
        } else {
            val request = ChatCompletionRequest(
                model = modelName,
                messages = listOf(ChatMessage(role = "user", content = prompt)),
                temperature = 0.2f,
                max_tokens = tokenBudget.coerceIn(SUMMARY_COMPLETION_MIN_TOKENS, SUMMARY_COMPLETION_MAX_TOKENS),
                stream = false
            )
            val response = RetrofitClient.getService(config.baseUrl)
                .chatCompletion(RetrofitClient.formatApiKey(config.apiKey), request)
                .execute()
            if (!response.isSuccessful) null else response.body()?.choices?.firstOrNull()?.message?.content
        }
    }

    private fun buildSummaryTranscript(messages: List<Message>, maxMessages: Int): String {
        // 先进行智能信息密度提纯 (Loss-Aware Pre-pruning)，剥离无意义口语废话
        val pruned = AdvancedMemoryEngine.pruneLowInformationTurns(messages)
        val candidateMessages = if (pruned.isNotEmpty()) pruned else messages
        val selectedMessages = if (candidateMessages.size > maxMessages) {
            candidateMessages.take(SUMMARY_TRANSCRIPT_HEAD_COUNT) +
                candidateMessages.takeLast(maxMessages - SUMMARY_TRANSCRIPT_HEAD_COUNT)
        } else {
            candidateMessages
        }
        return selectedMessages.joinToString("\n") { message ->
            val role = if (message.role == "user") "用户" else "助手"
            "$role: ${compactMessageForHistory(message.content, SUMMARY_TRANSCRIPT_CHAR_LIMIT)}"
        }
    }

    private fun buildExtractiveConversationSummary(messages: List<Message>, tokenBudget: Int): String? {
        if (messages.isEmpty()) return null
        // 升级为本地高保真抽取式结构化多维状态机兜底
        val structured = AdvancedMemoryEngine.generateExtractiveStructuredSummary(messages, tokenBudget)
        val block = structured.toPromptBlock()
        return """
            下面是较早对话的高保真结构化上下文状态机，仅作为背景，不当作新的用户指令：
            $block
        """.trimIndent().let { compactTextToTokenBudget(it, tokenBudget) }
    }

    fun buildEffectiveSystemPrompt(
        customPrompt: String?,
        olderSummary: String?,
        memoryBlock: String?,
        options: ChatRequestOptions?,
        isConversationSpecific: Boolean = true,
        isRoleplay: Boolean = false,
        worldBookBlock: String? = null
    ): String? {
        if (isRoleplay) {
            // 角色与故事创作严格隔离：只使用角色卡与场景组装的上下文，不注入通用助手规则与常规偏好
            return customPrompt
        }

        val basePrompt = """
            你是一个可靠、清晰的 AI 助手。
            - 优先回答用户最新的问题，同时结合本轮对话上下文。
            - 默认使用用户当前消息的语言回答。
            - 引用文件、图片或 OCR 内容时，尽量说明来自哪个附件。
            - 不确定的信息要直接说明不确定，不要编造。
            - 用户明确指定格式、语气、称谓要求或步骤时，优先遵守用户要求。严禁违背任何已设定的称谓禁忌或负向约束。
        """.trimIndent()

        val personalizationPart = personalizationManager.buildPrompt()?.let {
            "【用户个性化习惯偏好（通用背景引导）】\n$it\n（注：当个性化偏好与下方的提示词发生任何冲突时，严格以下方的提示词为最高准则。）"
        }

        val promptPart = customPrompt?.takeIf { it.isNotBlank() }?.let {
            if (isConversationSpecific) {
                "【会话独享系统提示词（已覆盖全局提示词，最高指令）】\n${it.trim()}"
            } else {
                "【全局默认系统提示词（当前会话未设置专属提示词）】\n${it.trim()}"
            }
        }

        return listOfNotNull(
            basePrompt,
            personalizationPart,
            buildRuntimeFeaturePrompt(options),
            promptPart,
            memoryBlock,
            worldBookBlock,
            olderSummary
        ).joinToString("\n\n").ifBlank { null }
    }

    private fun buildRuntimeFeaturePrompt(options: ChatRequestOptions?): String? {
        if (options == null) return null
        val notes = mutableListOf<String>()
        if (options.enableThinking == true) {
            notes += "本轮已开启思考模式。请进行更充分的推理；如果 API 返回可见思考内容，应将其作为思考过程流式输出。思考强度：${options.thinkingEffort ?: "medium"}。"
        }
        if (options.enableWebSearch == true) {
            notes += "本轮已开启智能搜索/联网。若消息中包含联网搜索资料，请优先基于资料回答，并在末尾列出来源；若资料不足，请明确说明不确定。"
        }
        return notes.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private suspend fun enrichUserMessageWithWebSearch(
        userMessage: String,
        options: ChatRequestOptions
    ): EnrichedPromptResult {
        val hub = echoToolHub
        if (hub != null) {
            return hub.enrichUserPrompt(userMessage, options)
        }

        if (options.enableWebSearch != true) return EnrichedPromptResult(userMessage, emptyList())

        val settings = tavilySearchManager.getSettings()
        return if (!settings.enabled || settings.apiKey.isBlank()) {
            val prompt = buildString {
                append(userMessage)
                append("\n\n[联网搜索状态]\n")
                append("用户已开启联网搜索，但 Tavily 未启用或 API Key 为空。请明确说明本轮未能成功联网，不要假装读取了实时网页。")
            }
            EnrichedPromptResult(prompt, emptyList())
        } else {
            tavilySearchManager.search(userMessage).fold(
                onSuccess = { bundle ->
                    val prompt = buildString {
                        append(bundle.toPromptBlock())
                        append("\n\n用户原始问题：\n")
                        append(userMessage)
                    }
                    val record = ToolCallRecord(
                        toolType = "WEB_SEARCH",
                        toolName = "Tavily 联网搜索",
                        iconName = "Search",
                        summary = "已检索到 ${bundle.results.size} 条网络网页资料",
                        detailContent = bundle.toPromptBlock(),
                        isSuccess = true
                    )
                    EnrichedPromptResult(prompt, listOf(record))
                },
                onFailure = { error ->
                    val prompt = buildString {
                        append(userMessage)
                        append("\n\n[联网搜索状态]\n")
                        append("Tavily 搜索失败：")
                        append(error.message ?: "未知错误")
                        append("\n请明确说明本轮未能成功联网，并基于已有上下文谨慎回答。")
                    }
                    EnrichedPromptResult(prompt, emptyList())
                }
            )
        }
    }

    private fun compactMessageForHistory(content: String, limit: Int = Int.MAX_VALUE): String {
        val normalized = content
            .lineSequence()
            .map { it.trimEnd() }
            .joinToString("\n")
            .trim()
        return if (normalized.length <= limit) {
            normalized
        } else {
            normalized.take(limit) + "\n...[内容过长，已截断]"
        }
    }

    private fun compactTextToTokenBudget(text: String, tokenBudget: Int): String {
        val normalized = text.trim()
        if (estimateTokenCount(normalized) <= tokenBudget) return normalized

        var charLimit = (tokenBudget * 2.4f).toInt().coerceAtLeast(400)
        while (charLimit > 400) {
            val compact = normalized.take(charLimit).trimEnd()
            if (estimateTokenCount(compact) <= tokenBudget) {
                return "$compact\n...[summary truncated]"
            }
            charLimit = (charLimit * 0.82f).toInt()
        }
        return normalized.take(charLimit).trimEnd() + "\n...[summary truncated]"
    }

    private suspend fun captureMemoryCandidate(message: Message) {
        if (message.role != "user" || message.content.isBlank() || message.id == 0L) return
        if (System.currentTimeMillis() - message.createdAt > MEMORY_CAPTURE_FRESHNESS_MS) return
        if (memoryDao.getBySourceMessage(message.id) != null) return

        if (!personalizationManager.getSettings().autoMemoryEnabled) return

        val conversation = conversationDao.getConversationById(message.conversationId) ?: return
        // 严格隔离：私密对话、角色扮演/故事创作会话均不写入全局长期记忆，防止小说情节与角色设定污染全局用户偏好
        if (hasConversationTag(conversation, "private") ||
            hasConversationTag(conversation, "roleplay") ||
            hasConversationTag(conversation, "story")
        ) return

        val candidate = extractMemoryCandidate(
            content = message.content,
            conversationId = message.conversationId,
            messageId = message.id
        ) ?: return

        val memoryContent = candidate.distilledContent
        val scope = candidate.suggestedScope
        val scopedConversationId = if (scope == "conversation") message.conversationId else null
        val keywords = tokenizeForMemory(memoryContent).take(18).joinToString(",")
        val existing = memoryDao.getByScopeAndContent(scope, memoryContent)
        val now = System.currentTimeMillis()

        // 仅自动保存经 SmartMemoryExtractor 或辅助模型提炼的高置信度偏好或项目背景，彻底杜绝噪音
        if (existing != null) {
            memoryDao.updateMemory(
                existing.copy(
                    confidence = maxOf(existing.confidence, MEMORY_CAPTURE_CONFIDENCE),
                    keywords = keywords.ifBlank { existing.keywords },
                    updatedAt = now
                )
            )
            return
        }

        // 冲突检测与版本更替：检测是否有与当前事实发生排他性属性冲突的旧记忆
        val existingCandidates = memoryDao.getCandidateMemories(message.conversationId)
            .filter { it.scope == scope && (scopedConversationId == null || it.conversationId == scopedConversationId) }
        val conflictedItem = existingCandidates.firstOrNull { AdvancedMemoryEngine.detectConflict(memoryContent, it) }
        if (conflictedItem != null) {
            memoryDao.updateMemory(
                conflictedItem.copy(
                    content = memoryContent,
                    keywords = keywords.ifBlank { conflictedItem.keywords },
                    confidence = maxOf(conflictedItem.confidence, MEMORY_CAPTURE_CONFIDENCE),
                    sourceMessageId = message.id,
                    updatedAt = now
                )
            )
            return
        }

        memoryDao.insertMemory(
            MemoryItem(
                scope = scope,
                conversationId = scopedConversationId,
                content = memoryContent,
                keywords = keywords.ifBlank { null },
                sourceMessageId = message.id,
                confidence = MEMORY_CAPTURE_CONFIDENCE,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun extractMemoryCandidate(
        content: String,
        conversationId: Long = 0L,
        messageId: Long? = null
    ): PendingMemoryCandidate? = withContext(Dispatchers.IO) {
        val settings = personalizationManager.getSettings()
        // 1. 如果启用了辅助模型，尝试使用指定的辅助模型进行提炼
        if (settings.auxiliaryMemoryEnabled && settings.auxiliaryMemoryApiConfigId > 0L) {
            try {
                val candidate = extractMemoryWithAuxiliaryModel(
                    content = content,
                    conversationId = conversationId,
                    messageId = messageId,
                    apiConfigId = settings.auxiliaryMemoryApiConfigId,
                    modelName = settings.auxiliaryMemoryModel,
                    customPrompt = settings.auxiliaryMemoryPrompt
                )
                if (candidate != null) {
                    return@withContext candidate
                }
            } catch (e: Exception) {
                Log.w(tag, "辅助模型提取记忆失败，自动降级为本地规则提取: ${e.message}")
            }
        }

        // 2. 本地纯规则提取兜底（当辅助模型未开启、不可用、超时、报错或返回空时无缝生效）
        SmartMemoryExtractor.extractCandidate(
            content = content,
            conversationId = conversationId,
            messageId = messageId
        )
    }

    private suspend fun extractMemoryWithAuxiliaryModel(
        content: String,
        conversationId: Long,
        messageId: Long?,
        apiConfigId: Long,
        modelName: String,
        customPrompt: String
    ): PendingMemoryCandidate? = withContext(Dispatchers.IO) {
        val rawConfig = getDecryptedConfig(apiConfigId) ?: return@withContext null
        val targetModel = modelName.trim().ifBlank { rawConfig.modelName }
        val config = rawConfig.copy(modelName = targetModel)

        val promptTemplate = customPrompt.trim().ifBlank {
            PersonalizationManager.DEFAULT_AUXILIARY_MEMORY_PROMPT
        }
        val prompt = """
            $promptTemplate

            【待识别内容】
            $content
        """.trimIndent()

        val responseText = try {
            if (config.apiType == "anthropic") {
                generateAnthropicMemoryExtraction(config, prompt)
            } else {
                generateOpenAIMemoryExtraction(config, prompt)
            }
        } catch (e: Exception) {
            Log.w(tag, "调用辅助模型API提取记忆异常: ${e.message}")
            null
        }

        if (responseText.isNullOrBlank()) return@withContext null
        // 彻底剥离思考标签，防止思考过程或草稿污染记忆事实
        val stripped = TimelineMemoryHelper.stripThinkingTags(responseText)
        val cleaned = stripped.trim().removePrefix("```").removeSuffix("```").trim()
        if (cleaned.contains("IGNORE", ignoreCase = true) || cleaned.length < 3) {
            return@withContext null
        }

        // 过滤开场白、客套话与空白，获取首条有效记忆内容
        val validLines = cleaned.lines().map { it.trim() }.filter {
            it.isNotBlank() && !it.startsWith("<") && !it.startsWith("好的") && !it.startsWith("以下是") && !it.startsWith("提炼结果")
        }
        val rawDistilled = validLines.firstOrNull()?.trim() ?: return@withContext null
        val (finalContent, category) = SmartMemoryExtractor.refineMemoryContent(rawDistilled)
        if (finalContent.isBlank()) return@withContext null

        PendingMemoryCandidate(
            distilledContent = finalContent,
            originalSnippet = content.take(80),
            suggestedScope = if (SmartMemoryExtractor.isConversationScoped(content)) "conversation" else "user",
            conversationId = conversationId,
            sourceMessageId = messageId,
            category = category
        )
    }

    private suspend fun generateOpenAIMemoryExtraction(config: ApiConfig, prompt: String): String? {
        val allKeys = parseApiKeys(config.apiKey).ifEmpty { listOf(config.apiKey) }
        var lastException: Exception? = null
        for (key in allKeys) {
            try {
                val request = ChatCompletionRequest(
                    model = config.modelName,
                    messages = listOf(ChatMessage(role = "user", content = prompt)),
                    temperature = 0.1f,
                    max_tokens = 1024,
                    stream = false
                )
                val response = RetrofitClient.getService(config.baseUrl)
                    .chatCompletion(RetrofitClient.formatApiKey(key), request)
                    .execute()
                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string()?.take(200).orEmpty()
                    throw Exception("HTTP ${response.code()}: $err")
                }
                val choice = response.body()?.choices?.firstOrNull()
                val text = choice?.message?.content?.ifBlank { null }
                    ?: choice?.message?.reasoning_content?.ifBlank { null }
                if (!text.isNullOrBlank()) return text
            } catch (e: Exception) {
                lastException = e
                Log.w(tag, "辅助模型提取(OpenAI) Key报错: ${e.message}，尝试下一Key")
            }
        }
        if (lastException != null) throw lastException
        return null
    }

    private suspend fun generateAnthropicMemoryExtraction(config: ApiConfig, prompt: String): String? {
        val allKeys = parseApiKeys(config.apiKey).ifEmpty { listOf(config.apiKey) }
        var lastException: Exception? = null
        for (key in allKeys) {
            try {
                val request = AnthropicRequest(
                    model = config.modelName,
                    messages = listOf(AnthropicMessage(role = "user", content = prompt)),
                    max_tokens = 1024,
                    temperature = 0.1f
                )
                val response = RetrofitClient.getService(config.baseUrl)
                    .anthropicMessages(
                        apiKey = key.removePrefix("Bearer ").trim(),
                        request = request
                    )
                    .execute()
                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string()?.take(200).orEmpty()
                    throw Exception("HTTP ${response.code()}: $err")
                }
                val contents = response.body()?.content
                val text = contents?.firstOrNull { it.type == "text" }?.text
                if (!text.isNullOrBlank()) return text
            } catch (e: Exception) {
                lastException = e
                Log.w(tag, "辅助模型提取(Anthropic) Key报错: ${e.message}，尝试下一Key")
            }
        }
        if (lastException != null) throw lastException
        return null
    }

    suspend fun testAuxiliaryMemoryExtraction(
        apiConfigId: Long,
        modelName: String,
        testText: String,
        customPrompt: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val rawConfig = getDecryptedConfig(apiConfigId)
                ?: return@withContext Result.failure(Exception("API配置不存在，请重新选择配置"))
            val targetModel = modelName.trim().ifBlank { rawConfig.modelName }
            val config = rawConfig.copy(modelName = targetModel)

            val promptTemplate = customPrompt.trim().ifBlank {
                PersonalizationManager.DEFAULT_AUXILIARY_MEMORY_PROMPT
            }
            val prompt = """
                $promptTemplate

                【待识别内容】
                $testText
            """.trimIndent()

            val raw = if (config.apiType == "anthropic") {
                generateAnthropicMemoryExtraction(config, prompt)
            } else {
                generateOpenAIMemoryExtraction(config, prompt)
            }

            if (raw.isNullOrBlank()) {
                val localFallback = SmartMemoryExtractor.extractCandidate(testText, 0L, null)
                if (localFallback != null) {
                    Result.success("【辅助模型返回空，自动降级为本地规则提炼】：\n${localFallback.distilledContent}")
                } else {
                    Result.success("【辅助模型与本地规则均判定该内容无需成为记忆（输出 IGNORE）】")
                }
            } else if (raw.contains("IGNORE", ignoreCase = true)) {
                Result.success("【辅助模型判定无需记录】：$raw")
            } else {
                Result.success("【辅助模型成功提取记忆】：\n$raw")
            }
        } catch (e: Exception) {
            val localFallback = SmartMemoryExtractor.extractCandidate(testText, 0L, null)
            val fallbackMsg = if (localFallback != null) {
                "\n\n[自动安全降级] 本地规则成功兜底提炼出记忆：\n${localFallback.distilledContent}"
            } else {
                "\n\n[自动安全降级] 本地规则兜底运行正常（内容无需入库）"
            }
            Result.failure(Exception("辅助模型调用报错: ${e.message}$fallbackMsg", e))
        }
    }

    /**
     * 读取会话全量历史，调用模型对时间推进轴与日常记忆进行提取、消灭相对时间词并校对推断
     */
    suspend fun reconcileConversationTimeline(
        conversationId: Long,
        activeConfigId: Long? = null,
        activeModelName: String? = null
    ): TimelineReconcileResult = withContext(Dispatchers.IO) {
        val messages = messageDao.getMessagesList(conversationId)
            .filter { !it.isExcluded && it.role != "system" && it.content.isNotBlank() }
            .sortedBy { it.createdAt }

        if (messages.isEmpty()) {
            return@withContext TimelineReconcileResult(
                currentStoryTime = "未确定",
                events = mutableListOf(),
                extractionSource = "AI_MODEL"
            )
        }

        // 解决问题 3：取消死板截断，全量分析历史对话，让脉络完整连贯
        // 当文本量在 60,000 字符内时全量送入；极超大文本时智能保留开端 30 条核心设定与最新连续大段对话
        val totalChars = messages.sumOf { it.content.length }
        val messagesToAnalyze = if (totalChars <= 60000) {
            messages
        } else {
            val head = messages.take(30)
            val headChars = head.sumOf { it.content.length }
            val tailBudget = 55000 - headChars
            val tail = mutableListOf<Message>()
            var acc = 0
            for (msg in messages.drop(30).reversed()) {
                if (acc + msg.content.length > tailBudget) break
                tail.add(msg)
                acc += msg.content.length
            }
            (head + tail.reversed()).sortedBy { it.createdAt }
        }

        // 解决问题 4：严格区分编剧指令与故事正文，从源头防止模型把指令误当剧情事实
        val formattedHistory = messagesToAnalyze.joinToString("\n") { msg ->
            val contentTrimmed = msg.content.trim()
            if (msg.role == "user") {
                if (TimelineMemoryHelper.isPureDirectorInstruction(contentTrimmed)) {
                    "【编剧写作指导/导演要求】: $contentTrimmed"
                } else {
                    "[用户发言/动作]: $contentTrimmed"
                }
            } else {
                "[助手演出的剧情正文]: $contentTrimmed"
            }
        }

        val settings = personalizationManager.getSettings()
        val conversation = conversationDao.getConversationById(conversationId)

        var rawConfig: ApiConfig? = null
        var targetModel: String = ""

        // 1. 优先使用专属【辅助记忆提炼模型】（若开启且已配置）
        if (settings.auxiliaryMemoryEnabled && settings.auxiliaryMemoryApiConfigId > 0L && settings.auxiliaryMemoryModel.isNotBlank()) {
            rawConfig = getDecryptedConfig(settings.auxiliaryMemoryApiConfigId)
            targetModel = settings.auxiliaryMemoryModel
            Log.d(tag, "时间轴提炼：优先调度辅助模型 [${targetModel}] (configId=${settings.auxiliaryMemoryApiConfigId})")
        }

        // 2. 若未启用辅助模型，依次寻找：ChatViewModel 当前活动配置 -> 会话绑定配置 -> 全局默认配置 -> 首个有效配置
        if (rawConfig == null || targetModel.isBlank()) {
            val candidateConfigId = activeConfigId?.takeIf { it > 0L }
                ?: conversation?.apiConfigId?.takeIf { it > 0L }
                ?: getDefaultApiConfig()?.id
                ?: getAllApiConfigs().first().firstOrNull()?.id
                ?: 0L

            if (candidateConfigId > 0L) {
                rawConfig = getDecryptedConfig(candidateConfigId)
            }
            if (rawConfig == null) {
                rawConfig = getDefaultApiConfig()?.let { getDecryptedConfig(it.id) }
                    ?: getAllApiConfigs().first().firstOrNull()?.let { getDecryptedConfig(it.id) }
            }

            targetModel = activeModelName?.ifBlank { null }
                ?: conversation?.modelName?.ifBlank { null }
                ?: rawConfig?.modelName.orEmpty()
        }

        if (rawConfig != null && targetModel.isBlank()) {
            targetModel = try {
                selectedModelDao.getEnabledModelsByConfig(rawConfig.id).first().firstOrNull()?.modelName.orEmpty()
            } catch (e: Exception) {
                ""
            }.ifBlank { rawConfig.modelName }
        }

        val prompt = """
            你是一个专业的小说时间线、剧情推进与常驻设定深度提炼专家。
            请通读以下完整的历史对话记录，梳理出故事内部真实的【单向推进叙事时间轴（In-Story Timeline）】、【在各时间点确立的规则与设定】以及【与时间无关的全局角色与世界固有常驻设定（Atemporal Settings）】。

            【特别指导核心准则】：

            1.【时间锚点极致敏感与精准捕捉法则（文学叙事时空深度挖掘）】：
               - 必须以极高敏感度嗅探剧情中所有的显式与隐式时间过渡，严禁遗漏任何细微的时序跃迁与暗线推移！
               - 包含但不限于：
                 ① 显式天数与时段：如 [第1天·清晨]、[第2天·晌午]、[第3天·黄昏]、[第4天·子时]；
                 ② 相对与自然时间跨度：如 [两周过后]、[半个月后]、[三日后·微雨]、[数月后·初冬]、[三年后·重逢]、[次日拂晓]；
                 ③ 季节轮替与阶段节气：如 [暑假开始]、[新学期伊始]、[深秋初雪]、[除夕之夜]、[惊蛰过后]；
                 ④ 篇章转折与时空锚点：如 [回忆·五年前]、[转折之夜]、[决战前夕]、[破晓时刻]；
               - 敏感捕捉文字中潜藏的暗线时间推移（如“聊到了掌灯时分”、“不知不觉窗外泛白”、“大雪封山已过七日”、“数日并进”），将其提炼为定位精准的规范时间标签！
               - 时序单向单调递增：剧情正文中若前文已是第2天，后文描写“第二天/次日/又过了一天”，必须合理推断累进为第3天；遇到“两周过后”等跨度词时，自然承接并推进入内部递增序列。

            2.【全方位剧情里程碑事件提炼（覆盖 5 大核心维度，拒绝遗漏重要进展）】：
               - 每一条时间线事件必须是【结构完整的剧情里程碑事实（Milestone Plot Event）】！
                 格式：主体（谁）在何处（何地）发生了什么关键转折/达成了什么共识/经历了什么重大事件。
               - 深度捕获 5 大关键维度：
                 ① 剧情重大转折与抉择：危机爆发、意外变故、重大抉择行动与产生的结果；
                 ② 感情线与人际质变：彼此从生疏到互信托付、建立同盟誓约、心结解开、发生争端或误会消除；
                 ③ 秘密揭露与重要发现：探明隐秘真相、识破真实身份、获悉关键情报或伏笔；
                 ④ 状态转变与阶段成果：获得关键信物/道具/武器、实力突破、负伤中毒或痊愈、处境重大改变；
                 ⑤ 关键约定与未决悬念：暗中达成的盟约、尚未解决的潜伏威胁、下一步核心目标。
               - 提炼 15~40 条结构完整事件，让整个故事脉络充实连贯（从初遇到展开、冲突、转折到当前停留节点）。

            3.【多维固有与常驻设定深度提炼（覆盖 6 大核心维度）】：
               - 敏锐从角色言行、反应、对话及背景中，捕捉具有长久约束力的常驻设定（每条凝练为 8~30 字明确规则事实）：
                 ① 角色特质与心结：核心性格、隐秘身世、心理执念、不可触犯的行为底线；
                 ② 习惯偏好与小动作：生活作息、特定饮食嗜好、思考/紧张时的肢体微动作、说话口癖；
                 ③ 生理特征与禁忌：过敏原、体质弱点、不可触碰的生理或心理禁区、旧伤残疾；
                 ④ 世界规则与法则限制：超自然/魔法体系代价、法律与禁令、特殊结界或社会制度；
                 ⑤ 人际羁绊与誓言契约：特定人物间的生死承诺、守护誓言、不可违背的关系契约；
                 ⑥ 专属信物与特殊器物：随身佩戴的特殊信物、标志性武器道具、契约印记。
               - 严禁原句抄录大段文学抒情描写或环境长句，必须凝练为规则属性的【原子设定事实】！

            4.【用户写作指令 `[...]` 与正文剧情严格解耦】：
               - 用户发送的中括号内容（如 `[让两人在雨夜再次相遇]`、`[推进剧情]`）是【编剧/导演的写作指令】，严禁将指令原话当作剧情事件记录！依据正文实际演出的事实进行提炼。

            请严格按照以下 JSON 格式输出，杜绝任何额外客套或解释：
            ```json
            {
              "currentStoryTime": "故事当前停留在的时间节点，如：第3天·夜晚、两周过后、暑假开始等",
              "timelineEvents": [
                {
                  "timeTag": "第1天·上午",
                  "category": "PLOT_EVENT",
                  "content": "两人在车站碰面并达成同行契约（客观完整的剧情里程碑总结）"
                },
                {
                  "timeTag": "两周过后",
                  "category": "PLOT_EVENT",
                  "content": "在旧城废墟完成第二阶段封印修复，同伴在战斗中展现了治愈能力"
                },
                {
                  "timeTag": "暑假开始",
                  "category": "RULE_CONSTRAINT",
                  "content": "学院全面封闭后山禁地，严禁未经许可擅入"
                }
              ],
              "atemporalSettings": [
                {
                  "category": "生理禁忌",
                  "content": "酒精严重过敏，滴酒不沾",
                  "targetScope": "session"
                },
                {
                  "category": "习惯偏好",
                  "content": "思考对策时习惯以指尖轻叩桌面",
                  "targetScope": "session"
                },
                {
                  "category": "角色特质",
                  "content": "极度重视契约承诺，一旦立誓绝不反悔",
                  "targetScope": "session"
                },
                {
                  "category": "世界规则",
                  "content": "帝国宵禁生效时，平民严禁携武器穿行中央广场",
                  "targetScope": "global"
                },
                {
                  "category": "专属信物",
                  "content": "随身携带刻有家族纹章的银质怀表",
                  "targetScope": "session"
                }
              ]
            }
            ```

            【历史对话记录】：
            $formattedHistory
        """.trimIndent()

        var modelException: Exception? = null
        var responseText: String? = null

        if (rawConfig != null && targetModel.isNotBlank()) {
            val config = rawConfig.copy(modelName = targetModel)
            Log.i(tag, "开始调用大模型深度分析全量时间线与设定: provider=${config.provider}, model=${config.modelName}")
            try {
                responseText = if (config.apiType == "anthropic") {
                    generateAnthropicTimelineAnalysis(config, prompt)
                } else {
                    generateOpenAITimelineAnalysis(config, prompt)
                }
                Log.i(tag, "大模型分析全量时间线响应成功，返回长度=${responseText?.length ?: 0}")
            } catch (e: Exception) {
                modelException = e
                Log.e(tag, "调用大模型分析全量时间线异常: ${e.message}", e)
            }
        } else {
            modelException = IllegalStateException("未找到可用的 API 配置或模型名称，请先配置模型 API")
        }

        // 读取已有故事时间，防止重新提炼时丢失或退回到未确定
        val existingStoryTime = memoryDao.getCandidateMemories(conversationId)
            .firstOrNull { it.content.startsWith("【当前故事时间】：") || it.content.startsWith("当前故事时间：") }
            ?.content?.substringAfter("：")?.trim()

        if (!responseText.isNullOrBlank()) {
            val parsedResult = TimelineMemoryHelper.parseModelOutput(responseText, fallbackCurrentTime = existingStoryTime)
            if (parsedResult.events.isNotEmpty() || parsedResult.atemporalSettings.isNotEmpty()) {
                parsedResult.extractionSource = "AI_MODEL"
                parsedResult.modelUsed = targetModel
                return@withContext parsedResult
            }
        }

        // 降级兜底：从消息中进行本地精纯时序扫描
        val fallback = fallbackLocalTimelineScan(messages)
        if (fallback.currentStoryTime.isBlank() || fallback.currentStoryTime == "未确定") {
            fallback.currentStoryTime = existingStoryTime?.takeIf { it.isNotBlank() && it != "未确定" } ?: "第 1 天·起始"
        }
        fallback.extractionSource = "LOCAL_FALLBACK"
        fallback.modelUsed = targetModel
        fallback.extractionErrorMessage = modelException?.message ?: "模型返回解析内容为空"
        return@withContext fallback
    }

    private suspend fun generateOpenAITimelineAnalysis(config: ApiConfig, prompt: String): String? {
        val normalizedUrl = normalizeApiBaseUrl(config.baseUrl, config.apiType)
        val isReasoning = Regex("""(^|[-_/])(o[134]|gpt-5|r1)""", RegexOption.IGNORE_CASE).containsMatchIn(config.modelName)
        val request = ChatCompletionRequest(
            model = config.modelName,
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            temperature = if (isReasoning) null else 0.2f,
            max_tokens = 8192,
            stream = false
        )
        val allKeys = parseApiKeys(config.apiKey).ifEmpty { listOf(config.apiKey) }
        var lastException: Exception? = null

        for (key in allKeys) {
            for (attempt in 1..2) {
                try {
                    val response = RetrofitClient.getAnalysisService(normalizedUrl)
                        .chatCompletion(RetrofitClient.formatApiKey(key), request)
                        .execute()
                    if (!response.isSuccessful) {
                        val errBody = response.errorBody()?.string()?.take(300).orEmpty()
                        throw Exception("HTTP ${response.code()}: $errBody")
                    }
                    val body = response.body()
                    if (body?.error != null) {
                        throw Exception(body.error.message ?: "OpenAI API 返回错误")
                    }
                    val choice = body?.choices?.firstOrNull()
                    val content = choice?.message?.content?.ifBlank { null }
                        ?: choice?.message?.reasoning_content?.ifBlank { null }
                    if (!content.isNullOrBlank()) return content
                } catch (e: Exception) {
                    lastException = e
                    Log.w(tag, "时间线分析 (OpenAI) Key报错或请求异常 (attempt $attempt): ${e.message}")
                    if (attempt < 2 && isNetworkFluctuationException(e)) {
                        kotlinx.coroutines.delay(2000L)
                        continue
                    }
                    break // 尝试下一个 Key
                }
            }
        }
        throw lastException ?: Exception("未能获取大模型有效输出")
    }

    private suspend fun generateAnthropicTimelineAnalysis(config: ApiConfig, prompt: String): String? {
        val normalizedUrl = normalizeApiBaseUrl(config.baseUrl, config.apiType)
        val request = AnthropicRequest(
            model = config.modelName,
            messages = listOf(AnthropicMessage(role = "user", content = prompt)),
            max_tokens = 8192,
            temperature = 0.2f
        )
        val allKeys = parseApiKeys(config.apiKey).ifEmpty { listOf(config.apiKey) }
        var lastException: Exception? = null

        for (key in allKeys) {
            for (attempt in 1..2) {
                try {
                    val response = RetrofitClient.getAnalysisService(normalizedUrl)
                        .anthropicMessages(
                            apiKey = key.removePrefix("Bearer ").trim(),
                            request = request
                        )
                        .execute()
                    if (!response.isSuccessful) {
                        val errBody = response.errorBody()?.string()?.take(300).orEmpty()
                        throw Exception("HTTP ${response.code()}: $errBody")
                    }
                    val content = response.body()?.content?.firstOrNull { it.type == "text" }?.text
                    if (!content.isNullOrBlank()) return content
                } catch (e: Exception) {
                    lastException = e
                    Log.w(tag, "时间线分析 (Anthropic) Key报错或请求异常 (attempt $attempt): ${e.message}")
                    if (attempt < 2 && isNetworkFluctuationException(e)) {
                        kotlinx.coroutines.delay(2000L)
                        continue
                    }
                    break // 尝试下一个 Key
                }
            }
        }
        throw lastException ?: Exception("未能获取大模型有效输出")
    }

    private fun fallbackLocalTimelineScan(messages: List<Message>): TimelineReconcileResult {
        val events = mutableListOf<TimelineEventItem>()
        val atemporalSettings = mutableListOf<AtemporalSettingItem>()
        var inferredCurrentTime = "未确定"
        val dayPattern = java.util.regex.Pattern.compile("""(?:第\s*(\d+)\s*天|DAY\s*(\d+))""", java.util.regex.Pattern.CASE_INSENSITIVE)

        var currentTrackedDay = 1
        var hasSeenEventsOnCurrentDay = false

        for (index in messages.indices) {
            val msg = messages[index]
            val content = msg.content.trim()
            if (content.isBlank()) continue

            // 1. 过滤纯用户导演指令（绝不将其原样塞入事件流）
            if (TimelineMemoryHelper.isPureDirectorInstruction(content)) {
                // 如果指令里提到天数推进
                val dMatch = dayPattern.matcher(content)
                if (dMatch.find()) {
                    val d = (dMatch.group(1) ?: dMatch.group(2))?.toIntOrNull() ?: currentTrackedDay
                    if (d > currentTrackedDay) {
                        currentTrackedDay = d
                        hasSeenEventsOnCurrentDay = false
                    }
                }
                val jumpDays = TimelineMemoryHelper.estimateTimeSpanJumpDays(content)
                if (jumpDays > 0) {
                    currentTrackedDay += jumpDays
                    hasSeenEventsOnCurrentDay = false
                }

                // 寻找紧随其后的助手正文，尝试提炼正文事实
                val nextAssistant = messages.getOrNull(index + 1)?.takeIf { it.role == "assistant" }
                if (nextAssistant != null && nextAssistant.content.isNotBlank()) {
                    val summary = nextAssistant.content.lines().firstOrNull { l ->
                        l.length in 10..120 && !l.startsWith("[") && !l.startsWith("【")
                    }?.trim()
                    if (!summary.isNullOrBlank()) {
                        val timeTag = when {
                            content.contains("两周") -> "两周过后"
                            content.contains("暑假") -> "暑假开始"
                            content.contains("一个月") || content.contains("次月") -> "一个月后"
                            content.contains("半年") -> "半年后"
                            content.contains("一年") -> "一年后"
                            else -> "第 $currentTrackedDay 天·剧情展开"
                        }
                        if (events.none { it.content == summary }) {
                            events.add(
                                TimelineEventItem(
                                    timeTag = timeTag,
                                    content = summary,
                                    category = TimelineCategory.PLOT_EVENT
                                )
                            )
                            hasSeenEventsOnCurrentDay = true
                        }
                    }
                }
                continue
            }

            // 2. 时序累进状态机：处理显式天数、相对次日与自然时间跨度
            val matcher = dayPattern.matcher(content)
            val spanJump = TimelineMemoryHelper.estimateTimeSpanJumpDays(content)
            if (spanJump > 0) {
                currentTrackedDay += spanJump
                hasSeenEventsOnCurrentDay = false
            } else if (matcher.find()) {
                val d = (matcher.group(1) ?: matcher.group(2))?.toIntOrNull() ?: currentTrackedDay
                if (d > currentTrackedDay) {
                    currentTrackedDay = d
                    hasSeenEventsOnCurrentDay = false
                } else if (d <= currentTrackedDay && hasSeenEventsOnCurrentDay && (content.contains("第二天") || content.contains("次日") || content.contains("翌日"))) {
                    // 关键累进：若已在第 2 天及以上，文本再次出现“第二天”，表示次日剧情，必须累加天数！
                    currentTrackedDay++
                    hasSeenEventsOnCurrentDay = false
                }
            } else if (content.contains("第二天") || content.contains("次日") || content.contains("翌日") || content.contains("又过了一天") || content.contains("隔天")) {
                if (hasSeenEventsOnCurrentDay || currentTrackedDay >= 1) {
                    currentTrackedDay++
                    hasSeenEventsOnCurrentDay = false
                }
            }

            // 3. 敏锐挖掘与时间无关的 6 维多维常驻设定 (atemporalSettings)
            // ① 生理禁忌
            val physiologicalKeywords = listOf("过敏", "畏寒", "怕冷", "旧伤", "夜盲", "不沾酒", "不能喝酒", "弱点", "体虚", "旧疾", "残疾", "毒素")
            // ② 习惯偏好
            val habitKeywords = listOf("习惯", "偏好", "喜欢喝", "喜欢吃", "总是会", "经常在", "随身带着", "习惯于", "嗜好", "口癖", "小动作")
            // ③ 身份过往与角色特质
            val identityKeywords = listOf("真实身份", "其实是", "隐姓埋名", "秘密", "身世", "曾经历过", "来自", "执念", "心结", "底线", "阵营")
            // ④ 世界规则与法则约束
            val ruleKeywords = listOf("禁止", "必须遵守", "世界规则", "法则", "结界", "严禁", "代价", "禁令", "宵禁", "律法", "禁区", "反噬")
            // ⑤ 人际羁绊与誓言契约
            val bondKeywords = listOf("保护", "绝不原谅", "唯一信任", "誓言", "承诺", "约定", "底线", "生死契约", "血契")
            // ⑥ 专属信物与特殊器物
            val itemKeywords = listOf("信物", "佩戴", "随身器物", "徽章", "玉佩", "戒指", "佩剑", "魔杖", "纹章", "刻印", "法器")

            val matchedCat = when {
                ruleKeywords.any { content.contains(it) } -> "世界规则"
                physiologicalKeywords.any { content.contains(it) } -> "生理禁忌"
                habitKeywords.any { content.contains(it) } -> "习惯偏好"
                identityKeywords.any { content.contains(it) } -> "角色特质"
                bondKeywords.any { content.contains(it) } -> "人际羁绊"
                itemKeywords.any { content.contains(it) } -> "专属信物"
                else -> null
            }

            if (matchedCat != null) {
                val candidateLine = content.lines().firstOrNull { l ->
                    listOf(physiologicalKeywords, habitKeywords, identityKeywords, ruleKeywords, bondKeywords, itemKeywords)
                        .flatten().any { l.contains(it) }
                }?.trim()

                // 严控设定提炼质量：长篇文学抒情长句不当作规则设定
                if (candidateLine != null && candidateLine.length <= 45 &&
                    !candidateLine.contains("目光") && !candidateLine.contains("神情") &&
                    !candidateLine.contains("微皱") && !candidateLine.contains("叹息")
                ) {
                    val cleanLine = candidateLine.replace("“", "").replace("”", "").replace("\"", "").trim()
                    if (cleanLine.length in 4..38 && atemporalSettings.none { it.content == cleanLine }) {
                        atemporalSettings.add(
                            AtemporalSettingItem(
                                category = matchedCat,
                                content = cleanLine,
                                isSelected = true,
                                targetScope = if (matchedCat == "世界规则") "global" else "session"
                            )
                        )
                    }
                }
            }

            // 4. 提取显式时间线事件或剧情转折正文（支持自然时间标签如 [两周过后]、[暑假开始]、[第3天]）
            if (content.startsWith("[") || content.startsWith("【")) {
                val item = TimelineMemoryHelper.parseContentToEvent(content)
                if (item.content.isNotBlank() && item.timeTag.isNotBlank() && events.none { it.content == item.content }) {
                    events.add(item)
                    hasSeenEventsOnCurrentDay = true
                }
            } else if (msg.role == "assistant" && content.length in 12..250 &&
                listOf("前往", "来到", "决定", "相遇", "发现", "答应", "拒绝", "战斗", "救下", "商量", "告别", "突破", "坦白", "重逢").any { content.contains(it) }) {
                val timeTag = "第 $currentTrackedDay 天·剧情节点"
                val rawSummary = content.lines().firstOrNull { l ->
                    listOf("前往", "来到", "决定", "相遇", "发现", "答应", "拒绝", "战斗", "救下", "商量", "告别", "突破", "坦白", "重逢").any { l.contains(it) }
                }?.trim()?.take(80) ?: content.take(60)
                val cleanSummary = rawSummary.replace("“", "").replace("”", "").replace("\"", "").trim()

                if (cleanSummary.isNotBlank() && events.none { it.content == cleanSummary }) {
                    events.add(
                        TimelineEventItem(
                            timeTag = timeTag,
                            content = cleanSummary,
                            category = TimelineCategory.PLOT_EVENT
                        )
                    )
                    hasSeenEventsOnCurrentDay = true
                }
            }
        }

        val normalized = TimelineMemoryHelper.normalizeMonotonicTimeline(events)
        if (currentTrackedDay > 1 || normalized.isNotEmpty()) {
            inferredCurrentTime = TimelineMemoryHelper.inferCurrentStoryTime(normalized).ifBlank { "第 $currentTrackedDay 天" }
        }

        return TimelineReconcileResult(
            currentStoryTime = inferredCurrentTime,
            events = normalized.toMutableList(),
            atemporalSettings = atemporalSettings
        )
    }

    private suspend fun buildRelevantMemoryBlock(
        conversation: Conversation,
        currentUserMessage: String,
        tokenBudget: Int,
        options: ChatRequestOptions? = null
    ): String? {
        if (hasConversationTag(conversation, "private")) return null

        val isRoleplay = hasConversationTag(conversation, "roleplay") || hasConversationTag(conversation, "story")
        // 角色扮演会话中严格隔离跨会话全局长期记忆，防止外部工作/代码等日常偏好污染小说剧情
        val extMemoryEnabled = if (isRoleplay) false else (options?.enableExternalMemory ?: conversation.enableExternalMemory ?: false)

        val candidates = memoryDao.getCandidateMemories(conversation.id).filter { it.isEnabled }
        if (candidates.isEmpty()) return null

        // 会话专属记忆在所有对话（包括普通对话、日常角色扮演）中只要开启均生效
        val sessionMemories = if (options?.enableSessionMemory == false) {
            emptyList()
        } else {
            candidates.filter { it.scope == "conversation" && it.conversationId == conversation.id }
        }
        val longTermMemories = if (extMemoryEnabled) {
            candidates.filter { it.scope == "user" }
        } else {
            emptyList()
        }

        fun isDirectiveOrConstraint(memory: MemoryItem): Boolean {
            val lower = memory.content.lowercase(java.util.Locale.ROOT)
            val constraintMarkers = listOf(
                "行为约束", "用户偏好", "约束", "规则", "规范",
                "不允许", "禁止", "切勿", "不要", "避免", "严禁", "不许", "不得", "不能",
                "必须", "始终", "永远", "称呼", "叫我", "自称", "身份", "尊称", "规矩",
                "雷区", "禁忌", "别叫", "不要叫", "格式要求", "不准", "特助", "老板"
            )
            return constraintMarkers.any { lower.contains(it) }
        }

        val (sessionDirectives, normalSessionMemories) = sessionMemories.partition { isDirectiveOrConstraint(it) }
        val (longTermDirectives, normalLongTermMemories) = longTermMemories.partition { isDirectiveOrConstraint(it) }
        val allDirectives = (sessionDirectives + longTermDirectives).distinctBy { it.content.trim() }

        val blocks = mutableListOf<String>()

        // 1. 核心行为准则与绝对约束（100% 无条件注入，最高约束级别，解决需求 4c）
        if (allDirectives.isNotEmpty()) {
            val directiveLines = allDirectives.map { "- [绝对准则] ${it.content.trim()}" }.joinToString("\n")
            blocks += """
                【核心行为准则与绝对约束（最高优先级，必须严格无条件遵守）】
                以下是用户已确认并生效的核心行为准则、称谓规范与输出禁令。在任何对话与输出中均拥有最高绝对效力，必须无条件执行：
                $directiveLines
                【执行铁律】：凡涉及上述禁止性称谓、措辞或行为（例如严禁使用“老板”、“特助”等任何称谓），在任何回复中绝对严禁出现，哪怕未被显式提醒也必须绝对回避！
            """.trimIndent()
        }

        if (normalSessionMemories.isNotEmpty()) {
            // 提取当前故事时间（若有记录）
            var currentStoryTime: String? = null
            val sessionLines = mutableListOf<String>()
            for (mem in normalSessionMemories) {
                val trimmed = mem.content.trim()
                if (trimmed.startsWith("【当前故事时间】：") || trimmed.startsWith("当前故事时间：")) {
                    currentStoryTime = trimmed.substringAfter("：").trim()
                } else {
                    sessionLines.add(trimmed)
                }
            }

            // 积极调度检测：如果用户提到“昨天”、“前天”、“上次”、“之前”、“哪天”、“那天”、“记得”、“吃过”等回忆关键词
            val timeRecallKeywords = listOf("昨天", "前天", "上次", "之前", "哪天", "那天", "记得", "吃过", "去过", "那时候", "上周", "前几天")
            val isTimeRecall = timeRecallKeywords.any { currentUserMessage.contains(it) }

            val timelineContext = if (sessionLines.any { it.startsWith("[") || it.startsWith("【") } || isTimeRecall || isRoleplay) {
                TimelineMemoryHelper.buildTimelinePromptContext(currentStoryTime, sessionLines)
            } else {
                val lines = sessionLines.map { "- $it" }.joinToString("\n")
                "【当前会话专属记忆与项目约束】：\n$lines"
            }

            blocks += "<session_timeline_memory>\n$timelineContext\n</session_timeline_memory>"
        }

        if (normalLongTermMemories.isNotEmpty()) {
            val queryTerms = tokenizeForMemory(currentUserMessage)
            val queryEntities = extractEntitiesFromQuery(currentUserMessage)
            val ranked = normalLongTermMemories
                .map { it to scoreMemory(it, queryTerms, queryEntities, conversation.id) }
                .filter { (_, score) -> score >= 0.22f }
                .sortedWith(compareByDescending<Pair<MemoryItem, Float>> { it.second }
                    .thenByDescending { it.first.updatedAt })

            val lines = mutableListOf<String>()
            var usedTokens = 0
            for ((memory, _) in ranked) {
                val line = "- ${memory.content}"
                val cost = estimateTokenCount(line) + 8
                if (lines.isNotEmpty() && usedTokens + cost > tokenBudget) break
                lines += line
                usedTokens += cost
            }
            if (lines.isNotEmpty()) {
                blocks += "<cross_session_long_term_memory>\n【跨会话长期背景与偏好参考（全局用户画像）】\n${lines.joinToString("\n")}\n</cross_session_long_term_memory>"
            }
        }

        if (blocks.isEmpty()) return null
        val memoryBody = blocks.joinToString("\n\n")
        return "<system_memory_context>\n$memoryBody\n\n【记忆与约束执行指引】：请严格优先执行上述核心行为准则与绝对约束；其余记忆、时间线与事实供你在构思方案和回答时自然参考与遵循，在未被用户明确询问时，无需机械复述这些条目。\n</system_memory_context>"
    }

    private fun scoreMemory(
        memory: MemoryItem,
        queryTerms: Set<String>,
        queryEntities: Set<String>,
        conversationId: Long
    ): Float {
        return AdvancedMemoryEngine.calculateHybridScore(
            memory = memory,
            queryTerms = queryTerms,
            queryEntities = queryEntities,
            conversationId = conversationId
        )
    }

    private fun extractEntitiesFromQuery(text: String): Set<String> {
        val entities = mutableSetOf<String>()
        Regex("""[“"《『【]([^“”"》』】]{2,20})[”"》』】]""").findAll(text).forEach {
            entities.add(it.groupValues[1].trim())
        }
        Regex("""\b[A-Z][a-zA-Z0-9_\-]{2,25}\b""").findAll(text).forEach {
            entities.add(it.value.trim())
        }
        return entities
    }

    private fun tokenizeForMemory(text: String): Set<String> {
        val lower = text.lowercase()
        val result = linkedSetOf<String>()
        Regex("[a-z0-9_\\-]{3,}").findAll(lower).forEach { result += it.value }
        Regex("[\\u4E00-\\u9FFF]{2,}").findAll(lower).forEach { match ->
            val value = match.value
            if (value.length <= 12) result += value
            value.windowed(2).forEach { result += it }
            if (value.length >= 3) value.windowed(3).forEach { result += it }
        }
        return result.take(120).toSet()
    }

    private fun addAnthropicHistoryMessage(
        messages: MutableList<AnthropicMessage>,
        role: String,
        content: String
    ) {
        if (content.isBlank()) return
        val last = messages.lastOrNull()
        if (last?.role == role && last.content is String) {
            messages[messages.lastIndex] = last.copy(content = "${last.content}\n\n$content")
        } else {
            messages.add(AnthropicMessage(role = role, content = content))
        }
    }

    // 构建OpenAI格式的用户消息（支持多模态）
    private fun buildUserMessage(text: String, attachments: List<Attachment>): Any {
        if (attachments.isEmpty()) {
            return text
        }

        val contentParts = mutableListOf<ContentPart>()

        // 构建完整的文本内容（包含文件内容）
        val fullText = StringBuilder()
        if (text.isNotBlank()) {
            fullText.append(text)
        }

        // 添加文本文件内容
        val textFiles = attachments.filter { it.textContent != null }
        if (textFiles.isNotEmpty()) {
            if (fullText.isNotEmpty()) fullText.append("\n\n")
            fullText.append("--- 附件内容 ---\n")
            textFiles.forEach { attachment ->
                fullText.append("\n文件: ${attachment.name}\n")
                fullText.append("```\n")
                fullText.append(attachment.textContent)
                fullText.append("\n```\n")
            }
        }

        // 添加文本部分
        if (fullText.isNotEmpty()) {
            contentParts.add(ContentPart(type = "text", text = fullText.toString()))
        }

        // 添加图片附件（多模态）
        attachments.filter { it.base64Data != null && FileUtils.isImage(it.mimeType, it.name) }.forEach { attachment ->
            contentParts.add(
                ContentPart(
                    type = "image_url",
                    image_url = ImageUrl(url = "data:${attachment.mimeType};base64,${attachment.base64Data}")
                )
            )
        }

        // 如果只有文本内容，直接返回字符串
        if (contentParts.size == 1 && contentParts[0].type == "text") {
            return contentParts[0].text ?: text
        }

        return contentParts
    }

    // 构建Anthropic格式的用户消息（支持多模态）
    private fun buildAnthropicUserMessage(text: String, attachments: List<Attachment>): Any {
        if (attachments.isEmpty()) {
            return text
        }

        val contentParts = mutableListOf<AnthropicContent>()

        // 构建完整的文本内容（包含文件内容）
        val fullText = StringBuilder()
        if (text.isNotBlank()) {
            fullText.append(text)
        }

        // 添加文本文件内容
        val textFiles = attachments.filter { it.textContent != null }
        if (textFiles.isNotEmpty()) {
            if (fullText.isNotEmpty()) fullText.append("\n\n")
            fullText.append("--- 附件内容 ---\n")
            textFiles.forEach { attachment ->
                fullText.append("\n文件: ${attachment.name}\n")
                fullText.append("```\n")
                fullText.append(attachment.textContent)
                fullText.append("\n```\n")
            }
        }

        // 添加文本部分
        if (fullText.isNotEmpty()) {
            contentParts.add(AnthropicContent(type = "text", text = fullText.toString()))
        }

        // 添加图片附件（多模态）
        attachments.filter { it.base64Data != null && FileUtils.isImage(it.mimeType, it.name) }.forEach { attachment ->
            contentParts.add(
                AnthropicContent(
                    type = "image",
                    source = AnthropicImageSource(
                        type = "base64",
                        media_type = attachment.mimeType,
                        data = attachment.base64Data!!
                    )
                )
            )
        }

        return contentParts
    }

    private fun parseStopSequences(json: String?): List<String>? {
        if (json.isNullOrBlank()) return null
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    private fun List<Message>.dropLastCurrentUserMessage(userMessage: String): List<Message> {
        val collapsed = collapseVariantsForHistory()
        val last = collapsed.lastOrNull()
        return if (last?.role == "user" && last.content == userMessage) {
            collapsed.dropLast(1)
        } else {
            collapsed
        }
    }

    private fun List<Message>.collapseVariantsForHistory(): List<Message> {
        val groups = filter { !it.variantGroupId.isNullOrBlank() }
            .groupBy { it.variantGroupId!! }
        val consumedGroups = mutableSetOf<String>()
        val result = mutableListOf<Message>()

        forEach { message ->
            val groupId = message.variantGroupId
            if (groupId.isNullOrBlank()) {
                result += message
                return@forEach
            }
            if (!consumedGroups.add(groupId)) return@forEach

            val selected = groups[groupId]
                .orEmpty()
                .maxWithOrNull(compareBy<Message> { it.variantIndex }.thenBy { it.createdAt })
                ?: message
            result += selected
        }

        return result
    }

    suspend fun generateConversationTitle(conversationId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val settings = personalizationManager.getSettings()
            if (!settings.autoNameEnabled) {
                return@withContext null
            }

            val conversation = getConversationById(conversationId) ?: return@withContext null

            // API 配置选择：若设置了独立自动命名配置则优先使用，否则使用当前对话的配置
            val targetConfigId = if (settings.autoNameApiConfigId > 0L) settings.autoNameApiConfigId else conversation.apiConfigId
            val rawConfig = getDecryptedConfig(targetConfigId) ?: getDecryptedConfig(conversation.apiConfigId) ?: return@withContext null

            // 模型选择：若设置了指定自动命名模型则覆盖配置模型，否则沿用配置模型
            val targetModel = settings.autoNameModel.trim().ifBlank { rawConfig.modelName }
            val config = rawConfig.copy(modelName = targetModel)

            val messages = getMessagesList(conversationId).take(8)
            if (messages.isEmpty()) return@withContext null

            val transcript = messages.joinToString("\n") { message ->
                val role = when (message.role) {
                    "user" -> "用户"
                    "assistant" -> "助手"
                    else -> message.role
                }
                "$role: ${message.content.take(500)}"
            }

            val customPrompt = settings.autoNamePrompt.trim().ifBlank {
                "请根据下面这段对话，生成一个简短精炼的中文标题。\n要求：严格在12个字以内，禁止使用任何标点符号、书名号或引号，禁止包含'标题'、'关于'等前缀废话，只直接输出最终标题。"
            }
            val prompt = """
                $customPrompt

                【对话内容】
                $transcript
            """.trimIndent()

            val title = if (config.apiType == "anthropic") {
                generateAnthropicTitle(config, prompt)
            } else {
                generateOpenAITitle(config, prompt)
            }

            sanitizeGeneratedTitle(title)
        } catch (e: Exception) {
            Log.w(tag, "自动生成标题失败", e)
            null
        }
    }

    suspend fun testAutoNaming(
        apiConfigId: Long,
        modelName: String,
        testText: String,
        customPrompt: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val rawConfig = getDecryptedConfig(apiConfigId) ?: return@withContext Result.failure(Exception("API配置不存在"))
            val targetModel = modelName.trim().ifBlank { rawConfig.modelName }
            val config = rawConfig.copy(modelName = targetModel)
            val promptTemplate = customPrompt.trim().ifBlank {
                "请根据下面这段用户发言或对话，生成一个简短精炼的中文标题。\n要求：严格在12个字以内，禁止使用任何标点符号、书名号或引号，禁止包含'标题'、'关于'等前缀废话，只直接输出最终标题。"
            }
            val prompt = """
                $promptTemplate

                【对话内容】
                用户: ${testText.take(500)}
            """.trimIndent()
            val raw = if (config.apiType == "anthropic") {
                generateAnthropicTitle(config, prompt)
            } else {
                generateOpenAITitle(config, prompt)
            }
            val sanitized = sanitizeGeneratedTitle(raw)
            if (sanitized.isNullOrBlank()) {
                Result.failure(Exception("未能生成有效标题，原始返回: ${raw ?: "空"}"))
            } else {
                Result.success(sanitized)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generateOpenAITitle(config: ApiConfig, prompt: String): String? {
        val request = ChatCompletionRequest(
            model = config.modelName,
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            temperature = 0.2f,
            max_tokens = 64,
            stream = false
        )
        val response = RetrofitClient.getService(config.baseUrl)
            .chatCompletion(RetrofitClient.formatApiKey(config.apiKey), request)
            .execute()
        if (!response.isSuccessful) return null
        return response.body()?.choices?.firstOrNull()?.message?.content
    }

    private suspend fun generateAnthropicTitle(config: ApiConfig, prompt: String): String? {
        val request = AnthropicRequest(
            model = config.modelName,
            messages = listOf(AnthropicMessage(role = "user", content = prompt)),
            max_tokens = 64,
            temperature = 0.2f
        )
        val response = RetrofitClient.getService(config.baseUrl)
            .anthropicMessages(apiKey = config.apiKey, request = request)
            .execute()
        if (!response.isSuccessful) return null
        return response.body()?.content?.firstOrNull()?.text
    }

    suspend fun executeQuickCompletion(config: ApiConfig, prompt: String, maxTokens: Int = 1000): String? = withContext(Dispatchers.IO) {
        try {
            if (config.apiType == "anthropic") {
                val request = AnthropicRequest(
                    model = config.modelName,
                    messages = listOf(AnthropicMessage(role = "user", content = prompt)),
                    max_tokens = maxTokens,
                    temperature = 0.3f
                )
                val response = RetrofitClient.getService(config.baseUrl)
                    .anthropicMessages(apiKey = config.apiKey, request = request)
                    .execute()
                if (!response.isSuccessful) null else response.body()?.content?.firstOrNull()?.text
            } else {
                val request = ChatCompletionRequest(
                    model = config.modelName,
                    messages = listOf(ChatMessage(role = "user", content = prompt)),
                    temperature = 0.3f,
                    max_tokens = maxTokens,
                    stream = false
                )
                val response = RetrofitClient.getService(config.baseUrl)
                    .chatCompletion(RetrofitClient.formatApiKey(config.apiKey), request)
                    .execute()
                if (!response.isSuccessful) null else response.body()?.choices?.firstOrNull()?.message?.content
            }
        } catch (e: Exception) {
            Log.e(tag, "executeQuickCompletion 失败", e)
            null
        }
    }

    suspend fun translateThinkingContent(
        thinkingText: String,
        targetApiConfigId: Long = 0L,
        targetModelName: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (thinkingText.isBlank()) {
                return@withContext Result.failure(Exception("待翻译内容为空"))
            }

            val rawConfig = if (targetApiConfigId > 0L) {
                getDecryptedConfig(targetApiConfigId)
            } else {
                val def = getDefaultApiConfig()
                if (def != null) {
                    getDecryptedConfig(def.id)
                } else {
                    val allConfigs = getAllApiConfigs().first()
                    allConfigs.firstOrNull()?.let { getDecryptedConfig(it.id) }
                }
            } ?: return@withContext Result.failure(Exception("未找到可用的 API 配置用于翻译"))

            val targetModel = targetModelName.trim().ifBlank { rawConfig.modelName }
            val effectiveConfig = rawConfig.copy(modelName = targetModel)

            val prompt = """
                请将以下 AI 模型的深度思考过程（Chain-of-Thought / 思维链）翻译为流畅、自然、符合中文表达习惯的简体中文。
                要求：
                1. 完整保留原有的思考逻辑、推理步骤、数学推导、代码标记和技术术语；
                2. 保持原有的思考语气（如自言自语、第一人称分析等）；
                3. 绝对不要添加任何额外的开场白、解释说明、翻译备注或结语，只直接输出翻译后的思考链正文。

                【待翻译思考过程】
                $thinkingText
            """.trimIndent()

            val maxOut = (thinkingText.length * 2).coerceIn(1000, 8192)
            val translated = executeQuickCompletion(effectiveConfig, prompt, maxTokens = maxOut)
            if (translated.isNullOrBlank()) {
                Result.failure(Exception("模型未返回有效翻译结果"))
            } else {
                Result.success(translated.trim())
            }
        } catch (e: Exception) {
            Log.e(tag, "思考链翻译异常", e)
            Result.failure(e)
        }
    }

    // ============ 统计相关 ============

    fun getAllUsageStats(): Flow<List<ApiUsageStat>> = usageStatDao.getAllStats()

    fun getUsageStatsByTimeRange(startTime: Long, endTime: Long): Flow<List<ApiUsageStat>> =
        usageStatDao.getStatsByTimeRange(startTime, endTime)

    suspend fun getUsageStatsListByTimeRange(startTime: Long, endTime: Long): List<ApiUsageStat> =
        usageStatDao.getStatsListByTimeRange(startTime, endTime)

    suspend fun getDailyStats(days: Int = 30): List<DailyStats> {
        val startTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        return usageStatDao.getDailyStatsSince(startTime)
    }

    suspend fun getModelStats(days: Int = 30): List<ModelStats> {
        val startTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        return usageStatDao.getModelStatsSince(startTime)
    }

    suspend fun getTotalTokens(days: Int = 30): Int {
        val startTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        return usageStatDao.getTotalTokensSince(startTime) ?: 0
    }

    suspend fun getThinkingTokens(days: Int = 30): Int {
        val startTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        return usageStatDao.getThinkingTokensSince(startTime) ?: 0
    }

    suspend fun getRequestCount(days: Int = 30): Int {
        val startTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        return usageStatDao.getRequestCountSince(startTime) ?: 0
    }

    suspend fun getAvgResponseTime(days: Int = 30): Long {
        val startTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        return usageStatDao.getAvgResponseTimeSince(startTime) ?: 0
    }

    // ============ 环境变量相关 ============

    fun getEnvironmentVariables(environment: String = "default"): Flow<List<EnvironmentVariable>> =
        environmentVariableDao.getVariablesByEnvironment(environment)

    fun getAllEnvironmentVariables(): Flow<List<EnvironmentVariable>> =
        environmentVariableDao.getAllVariables()

    suspend fun saveEnvironmentVariable(variable: EnvironmentVariable): Long {
        val encrypted = variable.copy(value = cryptoManager.encrypt(variable.value))
        return if (variable.id == 0L) {
            environmentVariableDao.insertVariable(encrypted)
        } else {
            environmentVariableDao.updateVariable(encrypted)
            variable.id
        }
    }

    suspend fun deleteEnvironmentVariable(variable: EnvironmentVariable) =
        environmentVariableDao.deleteVariable(variable)

    suspend fun getDecryptedVariable(name: String, environment: String): String? {
        val variable = environmentVariableDao.getVariableByName(name, environment) ?: return null
        return try {
            cryptoManager.decrypt(variable.value)
        } catch (e: Exception) {
            Log.e(tag, "解密环境变量失败", e)
            null
        }
    }

    suspend fun getAllEnvironments(): List<String> =
        environmentVariableDao.getAllEnvironments()

    // 替换字符串中的环境变量引用 {{VAR_NAME}}
    suspend fun resolveEnvironmentVariables(text: String, environment: String = "default"): String {
        var resolved = text
        val regex = "\\{\\{(.+?)\\}\\}".toRegex()
        regex.findAll(text).forEach { match ->
            val varName = match.groupValues[1]
            val value = getDecryptedVariable(varName, environment)
            if (value != null) {
                resolved = resolved.replace(match.value, value)
            }
        }
        return resolved
    }

    // ============ 提示词模板相关 ============

    fun getAllPromptTemplates(): Flow<List<PromptTemplate>> = promptTemplateDao.getAllTemplates()

    fun getPromptTemplatesByCategory(category: String): Flow<List<PromptTemplate>> =
        promptTemplateDao.getTemplatesByCategory(category)

    suspend fun getPromptTemplateById(id: Long): PromptTemplate? =
        promptTemplateDao.getTemplateById(id)

    suspend fun savePromptTemplate(template: PromptTemplate): Long {
        return if (template.id == 0L) {
            promptTemplateDao.insertTemplate(template)
        } else {
            promptTemplateDao.updateTemplate(template)
            template.id
        }
    }

    suspend fun deletePromptTemplate(template: PromptTemplate) =
        promptTemplateDao.deleteTemplate(template)

    suspend fun incrementTemplateUseCount(id: Long) =
        promptTemplateDao.incrementUseCount(id)

    suspend fun getAllTemplateCategories(): List<String> =
        promptTemplateDao.getAllCategories()

    // ============ 会话分支相关 ============

    fun getConversationBranches(parentId: Long): Flow<List<ConversationBranch>> =
        conversationBranchDao.getBranchesByParent(parentId)

    suspend fun createBranch(parentId: Long, branchMessageId: Long, childId: Long): Long {
        val branch = ConversationBranch(
            parentConversationId = parentId,
            branchMessageId = branchMessageId,
            childConversationId = childId
        )
        return conversationBranchDao.insertBranch(branch)
    }

    /**
     * 事务级完整创建会话分支：原子批量落库消息、继承活跃配置与会话设定、克隆角色扮演Session与记忆
     */
    suspend fun createBranchConversation(
        parentId: Long,
        branchMessageId: Long,
        sourceMessages: List<Message>,
        activeApiConfigId: Long? = null,
        activeModelName: String? = null
    ): Pair<Long, String> = withContext(Dispatchers.IO) {
        val originalConv = conversationDao.getConversationById(parentId)
            ?: throw IllegalStateException("原会话不存在: $parentId")

        val isParentHidden = hasConversationTag(originalConv, "hidden") ||
            originalConv.tags?.contains("hidden") == true
        val branchTags = if (isParentHidden) {
            updateTag(originalConv.tags, "hidden", true)
        } else {
            originalConv.tags
        }

        // 计算全局唯一、优雅单调自增的分支标题（彻底解决 XX(分支1) 重复问题，需求 1）
        val rootBaseTitle = extractRootBaseTitle(originalConv.title)
        val existingTitles = conversationDao.getTitlesStartingWith(rootBaseTitle)
        val branchTitle = calculateNextBranchTitle(rootBaseTitle, existingTitles)

        val effectiveApiConfigId = activeApiConfigId ?: originalConv.apiConfigId
        val effectiveModelName = (activeModelName?.takeIf { it.isNotBlank() }) ?: originalConv.modelName

        val db = com.aiassistant.AiAssistantApp.instance.database

        val newConversationId = db.withTransaction {
            // 1. 创建新会话实体（完整继承原会话的所有高级参数、滚动摘要、提示词与配置）
            val newConversation = Conversation(
                title = branchTitle,
                folderId = originalConv.folderId,
                apiConfigId = effectiveApiConfigId,
                modelName = effectiveModelName,
                systemPrompt = originalConv.systemPrompt,
                rollingSummary = originalConv.rollingSummary,
                summaryUpdatedMessageId = null,
                summaryUpdatedAt = originalConv.summaryUpdatedAt,
                totalTokens = 0,
                messageCount = 0,
                isPinned = false,
                tags = branchTags,
                temperature = originalConv.temperature,
                maxTokens = originalConv.maxTokens,
                topP = originalConv.topP,
                enableThinking = originalConv.enableThinking,
                thinkingEffort = originalConv.thinkingEffort,
                enableWebSearch = originalConv.enableWebSearch,
                enableSessionMemory = originalConv.enableSessionMemory,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newId = conversationDao.insertConversation(newConversation)

            // 2. 准备待复制的消息序列：完整保留纳入分支历史的单条消息所有历史版本（需求 7）
            val allParentMessages = messageDao.getMessagesList(parentId)
            val targetGroupIds = mutableSetOf<String>()
            val targetStandaloneIds = mutableSetOf<Long>()

            if (!sourceMessages.isNullOrEmpty()) {
                val targetIdx = sourceMessages.indexOfFirst { it.id == branchMessageId }
                val effectiveSlice = if (targetIdx >= 0) sourceMessages.subList(0, targetIdx + 1) else sourceMessages
                for (msg in effectiveSlice) {
                    if (!msg.variantGroupId.isNullOrBlank()) {
                        targetGroupIds.add(msg.variantGroupId!!)
                    } else {
                        targetStandaloneIds.add(msg.id)
                    }
                }
            } else {
                val branchMsg = allParentMessages.firstOrNull { it.id == branchMessageId }
                val maxTime = branchMsg?.createdAt ?: Long.MAX_VALUE
                for (msg in allParentMessages) {
                    if (msg.createdAt <= maxTime) {
                        if (!msg.variantGroupId.isNullOrBlank()) {
                            targetGroupIds.add(msg.variantGroupId!!)
                        } else {
                            targetStandaloneIds.add(msg.id)
                        }
                    }
                }
            }

            val messagesToCopy = allParentMessages.filter { msg ->
                (!msg.variantGroupId.isNullOrBlank() && msg.variantGroupId in targetGroupIds) ||
                (msg.id in targetStandaloneIds)
            }.sortedWith(compareBy<Message> { it.createdAt }.thenBy { it.variantIndex })

            val groupIdMapping = mutableMapOf<String, String>()
            targetGroupIds.forEach { oldGroup ->
                groupIdMapping[oldGroup] = "${oldGroup}_b${newId}"
            }

            val baseTime = System.currentTimeMillis() - (messagesToCopy.size * 1000L)
            val preparedMessages = messagesToCopy.mapIndexed { index, msg ->
                val newGroupId = msg.variantGroupId?.let { groupIdMapping[it] ?: "${it}_b${newId}" }
                msg.copy(
                    id = 0,
                    conversationId = newId,
                    variantGroupId = newGroupId,
                    variantIndex = msg.variantIndex,
                    createdAt = baseTime + (index * 1000L)
                )
            }

            // 批量一次性原子落库
            if (preparedMessages.isNotEmpty()) {
                messageDao.insertMessages(preparedMessages)
                val totalTokens = preparedMessages.sumOf { it.tokenCount }
                conversationDao.updateStats(newId, preparedMessages.size, totalTokens)
            }

            // 3. 继承隐藏属性确保生效
            if (isParentHidden) {
                conversationDao.updateTags(newId, branchTags)
            }

            // 4. 深度克隆 Roleplay 剧情会话与记忆
            try {
                val roleplayRepo = com.aiassistant.AiAssistantApp.instance.roleplayRepository
                val rpSession = roleplayRepo.getSessionByConversationId(parentId)
                if (rpSession != null) {
                    val clonedSessionId = roleplayRepo.insertSession(
                        rpSession.copy(
                            id = 0,
                            conversationId = newId,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    val sessionMemories = roleplayRepo.getMemoriesListBySession(rpSession.id)
                    sessionMemories.forEach { mem ->
                        roleplayRepo.insertMemory(
                            mem.copy(
                                id = 0,
                                sessionId = clonedSessionId
                            )
                        )
                    }
                }
            } catch (rpEx: Exception) {
                Log.e(tag, "克隆角色扮演会话异常", rpEx)
            }

            // 5. 复制普通会话专属记忆
            try {
                val convMemories = memoryDao.getConversationMemories(parentId)
                convMemories.forEach { mem ->
                    memoryDao.insertMemory(
                        mem.copy(
                            id = 0,
                            conversationId = newId
                        )
                    )
                }
            } catch (memEx: Exception) {
                Log.e(tag, "克隆会话专属记忆异常", memEx)
            }

            // 6. 记录分支关联表
            val branch = ConversationBranch(
                parentConversationId = parentId,
                branchMessageId = branchMessageId,
                childConversationId = newId
            )
            conversationBranchDao.insertBranch(branch)

            newId
        }

        Pair(newConversationId, branchTitle)
    }

    suspend fun getBranchByChild(childId: Long): ConversationBranch? =
        conversationBranchDao.getBranchByChild(childId)

    suspend fun deleteBranch(branch: ConversationBranch) =
        conversationBranchDao.deleteBranch(branch)

    /**
     * 完整复制整个对话生成新对话：
     * 包含全量消息流、模型与高级配置、会话专属记忆、角色卡与场景世界观关联及剧情专属记忆，并严格继承隐藏属性
     */
    suspend fun duplicateConversation(conversationId: Long): Long = withContext(Dispatchers.IO) {
        val originalConv = conversationDao.getConversationById(conversationId)
            ?: return@withContext -1L

        val isParentHidden = hasConversationTag(originalConv, "hidden") ||
            originalConv.tags?.contains("hidden") == true
        val duplicatedTags = if (isParentHidden) {
            updateTag(originalConv.tags, "hidden", true)
        } else {
            originalConv.tags
        }

        val duplicateTitle = generateDuplicateTitle(originalConv.title)
        val originalMessages = messageDao.getMessagesList(conversationId)

        val db = try {
            com.aiassistant.AiAssistantApp.instance.database
        } catch (e: Exception) {
            null
        }

        val performDuplicate = suspend {
            val now = System.currentTimeMillis()
            val newConversation = originalConv.copy(
                id = 0L,
                title = duplicateTitle,
                isPinned = false,
                tags = duplicatedTags,
                createdAt = now,
                updatedAt = now
            )
            val newId = conversationDao.insertConversation(newConversation)

            if (originalMessages.isNotEmpty()) {
                val baseTime = now - (originalMessages.size * 1000L)
                val preparedMessages = originalMessages.mapIndexed { index, msg ->
                    msg.copy(
                        id = 0L,
                        conversationId = newId,
                        createdAt = baseTime + (index * 1000L)
                    )
                }
                messageDao.insertMessages(preparedMessages)
                val totalTokens = preparedMessages.sumOf { it.tokenCount }
                conversationDao.updateStats(newId, preparedMessages.size, totalTokens)
            }

            if (isParentHidden) {
                conversationDao.updateTags(newId, duplicatedTags)
            }

            // 深度克隆 Roleplay 剧情会话与记忆
            try {
                val roleplayRepo = com.aiassistant.AiAssistantApp.instance.roleplayRepository
                val rpSession = roleplayRepo.getSessionByConversationId(conversationId)
                if (rpSession != null) {
                    val clonedSessionId = roleplayRepo.insertSession(
                        rpSession.copy(
                            id = 0L,
                            conversationId = newId,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    val sessionMemories = roleplayRepo.getMemoriesListBySession(rpSession.id)
                    sessionMemories.forEach { mem ->
                        roleplayRepo.insertMemory(
                            mem.copy(
                                id = 0L,
                                sessionId = clonedSessionId,
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                    }
                }
            } catch (rpEx: Exception) {
                Log.e(tag, "克隆角色扮演会话异常", rpEx)
            }

            // 复制普通会话专属记忆
            try {
                val convMemories = memoryDao.getConversationMemories(conversationId)
                convMemories.forEach { mem ->
                    memoryDao.insertMemory(
                        mem.copy(
                            id = 0L,
                            conversationId = newId,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
            } catch (memEx: Exception) {
                Log.e(tag, "克隆会话专属记忆异常", memEx)
            }

            newId
        }

        if (db != null) {
            db.withTransaction { performDuplicate() }
        } else {
            performDuplicate()
        }
    }

    // ============ 选择的模型相关 ============

    fun getSelectedModels(apiConfigId: Long): Flow<List<SelectedModel>> =
        selectedModelDao.getModelsByConfig(apiConfigId)

    fun getEnabledModels(apiConfigId: Long): Flow<List<SelectedModel>> =
        selectedModelDao.getEnabledModelsByConfig(apiConfigId)

    suspend fun getAllVisibleChatModelOptions(): List<ChatModelOption> = withContext(Dispatchers.IO) {
        val configs = apiConfigDao.getAllConfigs().first()
        configs.flatMap { config ->
            val savedModels = selectedModelDao.getModelsByConfig(config.id).first()
            val selectedModels = savedModels.filter { it.isEnabled }
            val options = when {
                selectedModels.isNotEmpty() -> selectedModels.map {
                    ChatModelOption(
                        apiConfigId = config.id,
                        configName = config.name,
                        provider = config.provider,
                        apiType = config.apiType,
                        modelName = it.modelName,
                        capability = it.capability
                    )
                }
                savedModels.isNotEmpty() -> savedModels.map {
                    ChatModelOption(
                        apiConfigId = config.id,
                        configName = config.name,
                        provider = config.provider,
                        apiType = config.apiType,
                        modelName = it.modelName,
                        capability = it.capability
                    )
                }
                else -> {
                    val modelNames = parseSavedModelNames(config.availableModels)
                        .ifEmpty { listOf(config.modelName) }
                    sanitizeModelNames(modelNames).map { model ->
                        ChatModelOption(
                            apiConfigId = config.id,
                            configName = config.name,
                            provider = config.provider,
                            apiType = config.apiType,
                            modelName = model,
                            capability = "auto"
                        )
                    }
                }
            }
            options
        }.distinctBy { "${it.apiConfigId}:${it.modelName}" }
    }

    private fun parseSavedModelNames(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(raw, type).orEmpty()
        } catch (_: Exception) {
            raw.split(",")
        }
    }

    suspend fun saveSelectedModel(model: SelectedModel): Long {
        return if (model.id == 0L) {
            selectedModelDao.insertModel(model)
        } else {
            selectedModelDao.updateModel(model)
            model.id
        }
    }

    suspend fun saveSelectedModels(models: List<SelectedModel>) =
        selectedModelDao.insertModels(models)

    suspend fun replaceSelectedModels(
        apiConfigId: Long,
        modelNames: List<String>,
        enabledModelNames: Set<String>,
        modelCapabilities: Map<String, String> = emptyMap(),
        modelSettings: Map<String, ModelCustomSettings> = emptyMap()
    ) {
        selectedModelDao.deleteModelsByConfig(apiConfigId)
        selectedModelDao.insertModels(
            sanitizeModelNames(modelNames).mapIndexed { index, modelName ->
                val custom = modelSettings[modelName]
                SelectedModel(
                    apiConfigId = apiConfigId,
                    modelName = modelName,
                    isEnabled = enabledModelNames.contains(modelName),
                    capability = modelCapabilities[modelName] ?: "auto",
                    sortOrder = index,
                    contextWindowTokens = custom?.contextWindowTokens,
                    supportsTools = custom?.supportsTools ?: true,
                    supportsVision = custom?.supportsVision ?: false,
                    supportsThinking = custom?.supportsThinking ?: true,
                    supportsWebSearch = custom?.supportsWebSearch ?: true
                )
            }
        )
    }

    suspend fun deleteSelectedModel(model: SelectedModel) =
        selectedModelDao.deleteModel(model)

    suspend fun setModelEnabled(id: Long, isEnabled: Boolean) =
        selectedModelDao.setModelEnabled(id, isEnabled)

    // ============ 使用统计增强 ============

    suspend fun getModelUsageSummary(days: Int = 30): List<ModelUsageSummary> {
        val startTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        return getModelUsageSummarySince(startTime)
    }

    suspend fun getModelUsageSummarySince(startTime: Long): List<ModelUsageSummary> {
        val results = usageStatDao.getModelUsageSummarySince(startTime)
        return results.map { result ->
            ModelUsageSummary(
                modelName = result.modelName,
                provider = result.provider,
                totalInputTokens = result.totalInputTokens,
                totalOutputTokens = result.totalOutputTokens,
                totalThinkingTokens = result.totalThinkingTokens,
                totalCachedTokens = result.totalCachedTokens,
                totalTokens = result.totalTokens,
                requestCount = result.requestCount,
                successCount = result.successCount,
                avgResponseTime = result.avgResponseTime,
                cacheHitRate = if (result.totalInputTokens > 0) {
                    (result.totalCachedTokens.toFloat() / result.totalInputTokens).coerceIn(0f, 1f)
                } else 0f
            )
        }
    }

    suspend fun getCachedTokens(days: Int = 30): Int {
        val startTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        return usageStatDao.getCachedTokensSince(startTime) ?: 0
    }

    private fun extractCachedTokensFromUsage(usage: Usage?): Int {
        if (usage == null) return 0
        return usage.prompt_tokens_details?.cached_tokens
            ?: usage.prompt_tokens_details?.cached_content_token_count
            ?: usage.prompt_tokens_details?.cache_read_input_tokens
            ?: usage.prompt_cache_hit_tokens
            ?: usage.cached_tokens
            ?: usage.cache_read_input_tokens
            ?: usage.cached_content_token_count
            ?: 0
    }
}
