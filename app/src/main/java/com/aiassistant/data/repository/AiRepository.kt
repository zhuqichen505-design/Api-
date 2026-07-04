package com.aiassistant.data.repository

import android.util.Log
import com.aiassistant.data.local.*
import com.aiassistant.data.remote.RetrofitClient
import com.aiassistant.domain.model.*
import com.aiassistant.utils.CryptoManager
import com.aiassistant.utils.FileUtils
import com.aiassistant.utils.PersonalizationManager
import com.aiassistant.utils.TavilySearchManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Call
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class AiRepository(
    private val folderDao: FolderDao,
    private val apiConfigDao: ApiConfigDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val usageStatDao: UsageStatDao,
    private val environmentVariableDao: EnvironmentVariableDao,
    private val promptTemplateDao: PromptTemplateDao,
    private val conversationBranchDao: ConversationBranchDao,
    private val selectedModelDao: SelectedModelDao,
    private val cryptoManager: CryptoManager,
    private val personalizationManager: PersonalizationManager,
    private val tavilySearchManager: TavilySearchManager
) {
    private val gson = Gson()
    private val tag = "AiRepository"
    private val activeStreamingCalls = ConcurrentHashMap<Long, Call>()

    fun cancelActiveRequest(conversationId: Long) {
        activeStreamingCalls.remove(conversationId)?.cancel()
    }

    private fun isRequestCancellation(error: Throwable): Boolean {
        if (error is CancellationException) return true
        if (error.message.equals("Canceled", ignoreCase = true)) return true
        return error.cause?.let(::isRequestCancellation) == true
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
        // 检查apiKey是否已经加密（通过尝试解密来判断）
        val apiKeyToSave = if (config.id != 0L && isAlreadyEncrypted(config.apiKey)) {
            // 如果是编辑现有配置且key已经加密，保持原值
            config.apiKey
        } else {
            // 否则加密
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
        return try {
            // 尝试解密，如果成功说明已经加密过
            cryptoManager.decrypt(value)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteApiConfig(config: ApiConfig) = apiConfigDao.deleteConfig(config)

    suspend fun setDefaultConfig(id: Long) {
        apiConfigDao.clearDefaultConfigs()
        apiConfigDao.setDefaultConfig(id)
    }

    suspend fun getDecryptedConfig(id: Long): ApiConfig? {
        val config = apiConfigDao.getConfigById(id) ?: return null
        return try {
            config.copy(apiKey = cryptoManager.decrypt(config.apiKey))
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

    // 直接获取模型列表（不保存配置）
    suspend fun fetchAvailableModelsDirect(baseUrl: String, apiKey: String, apiType: String = "openai"): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedBaseUrl = normalizeApiBaseUrl(baseUrl, apiType)
                val models = fetchModelsFromEndpoint(normalizedBaseUrl, apiKey, apiType)
                val cleanedModels = sanitizeModelNames(models)
                if (cleanedModels.isEmpty()) {
                    Result.failure(Exception("该API未返回模型列表，请手动输入模型名称"))
                } else {
                    Result.success(cleanedModels)
                }
            } catch (e: Exception) {
                Log.e(tag, "获取模型列表失败", e)
                Result.failure(Exception(e.message ?: "网络错误，请检查API地址是否正确"))
            }
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

            val body = gson.fromJson(bodyText, ModelsResponse::class.java)
            return body?.data
                ?.mapNotNull { sanitizeModelName(it.id) }
                ?.distinct()
                ?.sorted()
                .orEmpty()
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
        val conversation = Conversation(
            title = title,
            folderId = folderId,
            apiConfigId = apiConfigId,
            modelName = modelName,
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
        conversationDao.deleteConversationById(id)
    }

    suspend fun destroyPrivateConversation(id: Long) {
        messageDao.deleteMessagesByConversation(id)
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

    private fun updateTag(rawTags: String?, tag: String, enabled: Boolean): String? {
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

    // ============ 消息相关 ============

    fun getMessages(conversationId: Long): Flow<List<Message>> =
        messageDao.getMessagesByConversation(conversationId)

    suspend fun getMessagesList(conversationId: Long): List<Message> =
        messageDao.getMessagesList(conversationId)

    suspend fun saveMessage(message: Message): Long {
        val id = messageDao.insertMessage(message)
        updateConversationStats(message.conversationId)
        return id
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
                    "anthropic" -> sendAnthropicMessage(config, conversationId, historyMessages, userMessage, attachments, null, null, 1, onToken, onThinkingToken, onComplete, onError)
                    else -> sendOpenAIMessage(config, conversationId, historyMessages, userMessage, attachments, null, null, 1, onToken, onThinkingToken, onComplete, onError)
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
        onComplete: (String, String?, Any?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // 获取历史消息构建上下文
            val historyMessages = getMessagesList(conversationId)

            // 根据API类型调用不同的方法
            when (config.apiType) {
                "anthropic" -> sendAnthropicMessage(config, conversationId, historyMessages, userMessage, attachments, options, assistantVariantGroupId, assistantVariantIndex, onToken, onThinkingToken, onComplete, onError)
                else -> sendOpenAIMessage(config, conversationId, historyMessages, userMessage, attachments, options, assistantVariantGroupId, assistantVariantIndex, onToken, onThinkingToken, onComplete, onError)
            }
        } catch (e: Exception) {
            if (isRequestCancellation(e)) throw CancellationException("请求已取消", e)
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
        onComplete: (String, String?, Any?) -> Unit,
        onError: (String) -> Unit
    ) {
        val contextMessages = historyMessages.dropLastCurrentUserMessage(userMessage)
        val conversation = getConversationById(conversationId)
        val effectiveOptions = resolveChatRequestOptions(config, options)
        val requestModel = resolveRequestModel(config, effectiveOptions)
        val contextBundle = buildContextBundle(contextMessages, requestModel)
        val enrichedUserMessage = enrichUserMessageWithWebSearch(userMessage, effectiveOptions)
        val chatMessages = mutableListOf<ChatMessage>()

        buildEffectiveSystemPrompt(conversation?.systemPrompt, contextBundle.summary, effectiveOptions)?.let {
            chatMessages.add(ChatMessage(role = "system", content = it))
        }

        contextBundle.recentMessages.forEach { msg ->
            if (msg.role == "user" || msg.role == "assistant") {
                chatMessages.add(ChatMessage(role = msg.role, content = compactMessageForHistory(msg.content)))
            }
        }

        // 构建当前用户消息（支持多模态）
        val userContent = buildUserMessage(enrichedUserMessage, attachments)
        chatMessages.add(ChatMessage(role = "user", content = userContent))

        // 创建请求 - OpenAI格式不发送top_k
        val providerToggles = buildOpenAiProviderToggles(
            config = config,
            options = effectiveOptions,
            allowNativeWebSearch = !tavilySearchManager.isReady()
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
                var totalTokens = 0
                var inputTokens = 0
                var outputTokens = 0
                var thinkingTokens = 0
                var cachedTokens = 0

                // 读取SSE流
                responseBody.byteStream().bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val lineStr = line?.trim() ?: continue
                        if (lineStr.startsWith("data:")) {
                            val data = lineStr.removePrefix("data:").trimStart()
                            if (data == "[DONE]") break

                            try {
                                val chunk = gson.fromJson(data, ChatCompletionChunk::class.java)
                                val delta = chunk.choices?.firstOrNull()?.delta

                                // 处理普通内容
                                delta?.content?.let { content ->
                                    contentBuilder.append(content)
                                    onToken(content)
                                }

                                // 处理思考内容（DeepSeek/Qwen/Claude 兼容网关等字段名不完全一致）
                                val thinkingChunk = delta?.reasoning_content
                                    ?: delta?.reasoning
                                    ?: delta?.thinking
                                    ?: delta?.thinking_content
                                thinkingChunk?.let { thinking ->
                                    thinkingBuilder.append(thinking)
                                    onThinkingToken(thinking)
                                }

                                // 处理usage
                                chunk.usage?.let { usage ->
                                    inputTokens = usage.prompt_tokens ?: inputTokens
                                    val rawCompletionTokens = usage.completion_tokens ?: outputTokens
                                    thinkingTokens = usage.completion_tokens_details?.reasoning_tokens ?: thinkingTokens
                                    outputTokens = (rawCompletionTokens - thinkingTokens).coerceAtLeast(0)
                                    cachedTokens = usage.prompt_tokens_details?.cached_tokens ?: cachedTokens
                                    totalTokens = usage.total_tokens ?: (inputTokens + outputTokens + thinkingTokens)
                                }
                            } catch (e: Exception) {
                                Log.w(tag, "解析chunk失败: $data", e)
                            }
                        }
                    }
                }

                val responseTime = System.currentTimeMillis() - startTime
                val fullContent = contentBuilder.toString()
                val fullThinking = thinkingBuilder.toString().ifEmpty { null }
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
                    responseTime = responseTime
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

                onComplete(fullContent, fullThinking, null)
            } else {
                val errorBody = okResponse.body?.string() ?: "未知错误"
                val errorMsg = try {
                    val error = gson.fromJson(errorBody, ApiError::class.java)
                    error.message ?: errorBody
                } catch (e: Exception) {
                    errorBody
                }
                throw Exception("API错误: $errorMsg")
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
        onComplete: (String, String?, Any?) -> Unit,
        onError: (String) -> Unit
    ) {
        val contextMessages = historyMessages.dropLastCurrentUserMessage(userMessage)

        // 获取系统提示
        val conversation = getConversationById(conversationId)
        val effectiveOptions = resolveChatRequestOptions(config, options)
        val requestModel = resolveRequestModel(config, effectiveOptions)
        val contextBundle = buildContextBundle(contextMessages, requestModel)
        val systemPrompt = buildEffectiveSystemPrompt(conversation?.systemPrompt, contextBundle.summary, effectiveOptions)
        val anthropicMessages = mutableListOf<AnthropicMessage>()

        // 添加历史消息
        contextBundle.recentMessages.dropWhile { it.role == "assistant" }.forEach { msg ->
            when (msg.role) {
                "user" -> addAnthropicHistoryMessage(anthropicMessages, "user", compactMessageForHistory(msg.content))
                "assistant" -> addAnthropicHistoryMessage(anthropicMessages, "assistant", compactMessageForHistory(msg.content))
            }
        }

        // 构建当前用户消息（支持多模态）
        val enrichedUserMessage = enrichUserMessageWithWebSearch(userMessage, effectiveOptions)
        val userContent = buildAnthropicUserMessage(enrichedUserMessage, attachments)
        if (userContent is String) {
            addAnthropicHistoryMessage(anthropicMessages, "user", userContent)
        } else {
            anthropicMessages.add(AnthropicMessage(role = "user", content = userContent))
        }

        // 创建请求 - Anthropic格式支持top_k
        val request = AnthropicRequest(
            model = requestModel,
            messages = anthropicMessages,
            max_tokens = effectiveOptions.maxTokens ?: config.maxTokens,
            system = systemPrompt,
            temperature = requestTemperature(config, effectiveOptions),
            top_p = effectiveOptions.topP,
            top_k = if (config.topK != 50) config.topK else null,
            stream = true,
            stop_sequences = parseStopSequences(config.stopSequences),
            thinking = if (effectiveOptions.enableThinking == true) {
                AnthropicThinking(budget_tokens = thinkingBudgetForEffort(effectiveOptions.thinkingEffort, config.thinkingBudget))
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

                // 读取SSE流
                responseBody.byteStream().bufferedReader().use { reader ->
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        val lineStr = line?.trim() ?: continue

                        // 处理event行
                        if (lineStr.startsWith("event: ")) {
                            continue
                        }

                        // 处理data行
                        if (lineStr.startsWith("data:")) {
                            val data = lineStr.removePrefix("data:").trimStart()

                            try {
                                val event = gson.fromJson(data, AnthropicStreamEvent::class.java)

                                when (event.type) {
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
                                }
                            } catch (e: Exception) {
                                Log.w(tag, "解析Anthropic event失败: $data", e)
                            }
                        }
                    }
                }

                val responseTime = System.currentTimeMillis() - startTime
                val fullContent = contentBuilder.toString()
                val fullThinking = thinkingBuilder.toString().ifEmpty { null }
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
                    responseTime = responseTime
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

                onComplete(fullContent, fullThinking, null)
            } else {
                val errorBody = okResponse.body?.string() ?: "未知错误"
                val errorMsg = try {
                    // 尝试解析Anthropic错误格式
                    val errorJson = gson.fromJson(errorBody, AnthropicError::class.java)
                    errorJson.error?.message ?: errorBody
                } catch (e: Exception) {
                    errorBody
                }
                throw Exception("API错误: $errorMsg")
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
            enableWebSearch = overrides?.enableWebSearch ?: config.enableWebSearch
        )
    }

    private fun normalizeThinkingEffort(effort: String?, config: ApiConfig): String {
        val normalized = effort?.lowercase()
        return if (isDeepSeekConfig(config)) {
            when (normalized) {
                "max" -> "max"
                else -> "high"
            }
        } else {
            when (normalized) {
                "low", "medium", "high" -> normalized
                else -> "medium"
            }
        }
    }

    private fun thinkingBudgetForEffort(effort: String?, configuredBudget: Int): Int {
        val base = configuredBudget.coerceIn(1024, 32768)
        return when (effort?.lowercase()) {
            "low" -> (base / 2).coerceIn(1024, 32768)
            "max" -> 32768
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
        if (options.enableThinking == true && (isDeepSeekConfig(config) || isMiMoConfig(config))) {
            return null
        }
        return options.temperature?.coerceIn(0f, temperatureMaxForConfig(config))
    }

    private fun resolveRequestModel(config: ApiConfig, options: ChatRequestOptions): String {
        val model = config.modelName
        if (options.enableThinking != true) return model

        val identity = listOf(config.provider, config.baseUrl, model)
            .joinToString(" ")
            .lowercase()
        val isOfficialDeepSeek = "api.deepseek.com" in identity || config.provider.equals("deepseek", ignoreCase = true)
        val isDeepSeekChat = model.equals("deepseek-chat", ignoreCase = true)
        val isAlreadyReasoner = "reasoner" in model.lowercase()

        return if (isOfficialDeepSeek && isDeepSeekChat && !isAlreadyReasoner) {
            "deepseek-reasoner"
        } else {
            model
        }
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
        val isOpenAiReasoningModel = Regex("""(^|[-_/])(o[134]|gpt-5)""")
            .containsMatchIn(config.modelName.lowercase())

        return OpenAiProviderToggles(
            includeTopK = options != null && isMiMo,
            includeGenericSearch = wantsSearch && !isDeepSeek && !isOpenAi,
            includeOpenAiSearchOptions = wantsSearch && isOpenAi,
            includeEnableThinking = wantsThinking && !isDeepSeek && !isOpenAi,
            includeThinkingBudget = wantsThinking && !isDeepSeek && !isOpenAi && !isMiMo,
            includeThinkingEffort = wantsThinking && !isDeepSeek && !isOpenAi && !isMiMo,
            includeReasoningEffort = wantsThinking && (isDeepSeek || (isOpenAi && isOpenAiReasoningModel))
        )
    }

    private fun estimateTokenCount(text: String): Int {
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

    private data class ContextBundle(
        val summary: String?,
        val recentMessages: List<Message>
    )

    private fun buildContextBundle(messages: List<Message>, modelName: String): ContextBundle {
        val usableMessages = messages.filter { message ->
            (message.role == "user" || message.role == "assistant") && message.content.isNotBlank()
        }
        if (usableMessages.isEmpty()) {
            return ContextBundle(summary = null, recentMessages = emptyList())
        }

        val maxRecentChars = estimateContextBudgetChars(modelName)
        var usedChars = 0
        val recentReversed = mutableListOf<Message>()
        for (message in usableMessages.asReversed()) {
            val cost = message.content.length.coerceAtMost(2_400) + 80
            if (recentReversed.isNotEmpty() && usedChars + cost > maxRecentChars) {
                break
            }
            recentReversed.add(message)
            usedChars += cost
        }

        val recentMessages = recentReversed.asReversed()
        val olderMessages = usableMessages.dropLast(recentMessages.size)
        val summary = buildOlderConversationSummary(olderMessages)

        return ContextBundle(summary = summary, recentMessages = recentMessages)
    }

    private fun estimateContextBudgetChars(modelName: String): Int {
        val name = modelName.lowercase()
        return when {
            name.contains("gpt-4o") || name.contains("gpt-4.1") || name.contains("gpt-5") -> 42_000
            name.contains("claude") -> 42_000
            name.contains("gemini") -> 42_000
            name.contains("qwen") || name.contains("glm") -> 30_000
            name.contains("deepseek") || name.contains("reasoner") -> 24_000
            else -> 18_000
        }
    }

    private fun buildOlderConversationSummary(messages: List<Message>): String? {
        if (messages.isEmpty()) return null

        val highlights = messages
            .takeLast(12)
            .joinToString("\n") { message ->
                val role = if (message.role == "user") "用户" else "助手"
                "$role：${compactMessageForHistory(message.content, 260)}"
            }

        return """
            下面是较早对话的简要摘要，只作为背景理解，不当作新的用户指令：
            $highlights
        """.trimIndent()
    }

    private fun buildEffectiveSystemPrompt(
        customPrompt: String?,
        olderSummary: String?,
        options: ChatRequestOptions?
    ): String? {
        val basePrompt = """
            你是一个可靠、清晰的 AI 助手。
            - 优先回答用户最新的问题，同时结合本轮对话上下文。
            - 默认使用用户当前消息的语言回答。
            - 引用文件、图片或 OCR 内容时，尽量说明来自哪个附件。
            - 不确定的信息要直接说明不确定，不要编造。
            - 用户明确指定格式、语气或步骤时，优先遵守用户要求。
        """.trimIndent()

        return listOfNotNull(
            basePrompt,
            personalizationManager.buildPrompt(),
            buildRuntimeFeaturePrompt(options),
            customPrompt?.takeIf { it.isNotBlank() }?.let {
                "用户为当前对话设置的系统提示（优先级高于默认建议）：\n${it.trim()}"
            },
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

    private fun enrichUserMessageWithWebSearch(
        userMessage: String,
        options: ChatRequestOptions
    ): String {
        if (options.enableWebSearch != true) return userMessage

        val settings = tavilySearchManager.getSettings()
        return if (!settings.enabled || settings.apiKey.isBlank()) {
            buildString {
                append(userMessage)
                append("\n\n[联网搜索状态]\n")
                append("用户已开启联网搜索，但 Tavily 未启用或 API Key 为空。请明确说明本轮未能成功联网，不要假装读取了实时网页。")
            }
        } else {
            tavilySearchManager.search(userMessage).fold(
                onSuccess = { bundle ->
                    buildString {
                        append(bundle.toPromptBlock())
                        append("\n\n用户原始问题：\n")
                        append(userMessage)
                    }
                },
                onFailure = { error ->
                    buildString {
                        append(userMessage)
                        append("\n\n[联网搜索状态]\n")
                        append("Tavily 搜索失败：")
                        append(error.message ?: "未知错误")
                        append("\n请明确说明本轮未能成功联网，并基于已有上下文谨慎回答。")
                    }
                }
            )
        }

        if (!settings.enabled || settings.apiKey.isBlank()) {
            return buildString {
                append(userMessage)
                append("\n\n[联网搜索状态]\n")
                append("用户已开启联网搜索，但 Tavily 未启用或 API Key 为空。请明确说明本轮未能成功联网，不要假装读取了实时网页。")
            }
        }

        return tavilySearchManager.search(userMessage).fold(
            onSuccess = { bundle ->
                buildString {
                    append(bundle.toPromptBlock())
                    append("\n\n用户原始问题：\n")
                    append(userMessage)
                }
            },
            onFailure = { error ->
                buildString {
                    append(userMessage)
                    append("\n\n[联网搜索状态]\n")
                    append("Tavily 搜索失败：")
                    append(error.message ?: "未知错误")
                    append("\n请明确说明本轮未能成功联网，并基于已有上下文谨慎回答。")
                }
            }
        )
    }

    private fun compactMessageForHistory(content: String, limit: Int = 2_400): String {
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
            val conversation = getConversationById(conversationId) ?: return@withContext null
            val config = getDecryptedConfig(conversation.apiConfigId) ?: return@withContext null
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
            val prompt = """
                请根据下面这段对话，生成一个简短中文标题。
                要求：不超过12个汉字，不要引号，不要句号，只输出标题。

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

    private suspend fun generateOpenAITitle(config: ApiConfig, prompt: String): String? {
        val request = ChatCompletionRequest(
            model = config.modelName,
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            temperature = 0.2f,
            max_tokens = 32,
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
            max_tokens = 32,
            temperature = 0.2f
        )
        val response = RetrofitClient.getService(config.baseUrl)
            .anthropicMessages(apiKey = config.apiKey, request = request)
            .execute()
        if (!response.isSuccessful) return null
        return response.body()?.content?.firstOrNull()?.text
    }

    private fun sanitizeGeneratedTitle(rawTitle: String?): String? {
        val title = rawTitle
            ?.lineSequence()
            ?.firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.trim('"', '\'', '“', '”', '「', '」', '。', '.', '：', ':')
            ?.take(30)
            ?.trim()
        return title?.ifBlank { null }
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

    suspend fun getBranchByChild(childId: Long): ConversationBranch? =
        conversationBranchDao.getBranchByChild(childId)

    suspend fun deleteBranch(branch: ConversationBranch) =
        conversationBranchDao.deleteBranch(branch)

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
        modelCapabilities: Map<String, String> = emptyMap()
    ) {
        selectedModelDao.deleteModelsByConfig(apiConfigId)
        selectedModelDao.insertModels(
            sanitizeModelNames(modelNames).mapIndexed { index, modelName ->
                SelectedModel(
                    apiConfigId = apiConfigId,
                    modelName = modelName,
                    isEnabled = enabledModelNames.contains(modelName),
                    capability = modelCapabilities[modelName] ?: "auto",
                    sortOrder = index
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
}
