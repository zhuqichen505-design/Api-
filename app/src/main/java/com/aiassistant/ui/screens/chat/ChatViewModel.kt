package com.aiassistant.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aiassistant.AiAssistantApp
import com.aiassistant.domain.model.*
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(private val conversationId: Long) : ViewModel() {
    private val repository = AiAssistantApp.instance.repository
    private val gson = Gson()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _currentResponse = MutableStateFlow("")
    val currentResponse: StateFlow<String> = _currentResponse.asStateFlow()

    private val _currentThinking = MutableStateFlow("")
    val currentThinking: StateFlow<String> = _currentThinking.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 可用模型列表，包含 API 配置来源，允许在同一对话里跨 API 切换。
    private val _availableModelOptions = MutableStateFlow<List<ChatModelOption>>(emptyList())
    val availableModelOptions: StateFlow<List<ChatModelOption>> = _availableModelOptions.asStateFlow()

    // 当前选择的模型（临时，仅当前对话有效）
    private val _currentModel = MutableStateFlow<String?>(null)
    val currentModel: StateFlow<String?> = _currentModel.asStateFlow()

    private val _currentModelOption = MutableStateFlow<ChatModelOption?>(null)
    val currentModelOption: StateFlow<ChatModelOption?> = _currentModelOption.asStateFlow()

    // 临时设置（仅当前对话有效）
    private val _tempSettings = MutableStateFlow(TempChatSettings())
    val tempSettings: StateFlow<TempChatSettings> = _tempSettings.asStateFlow()

    // 是否使用临时设置
    private val _useTempSettings = MutableStateFlow(true)
    val useTempSettings: StateFlow<Boolean> = _useTempSettings.asStateFlow()

    // 提示词模板列表
    private val _promptTemplates = MutableStateFlow<List<PromptTemplate>>(emptyList())
    val promptTemplates: StateFlow<List<PromptTemplate>> = _promptTemplates.asStateFlow()

    private val _contextUsage = MutableStateFlow(ContextUsageUiState())
    val contextUsage: StateFlow<ContextUsageUiState> = _contextUsage.asStateFlow()

    private val _messageModelMap = MutableStateFlow<Map<Long, String>>(emptyMap())
    val messageModelMap: StateFlow<Map<Long, String>> = _messageModelMap.asStateFlow()
    private val runtimeMessageModelMap = java.util.concurrent.ConcurrentHashMap<Long, String>()

    private var conversation: Conversation? = null
    private var apiConfig: ApiConfig? = null
    private var generationJob: Job? = null
    private var systemPromptSaveJob: Job? = null
    private var isMessageSaved = false
    private var isPrivateConversation = false
    private var privateExitHandled = false

    init {
        loadConversation()
        loadPromptTemplates()
        observeUsageStatsForModels()
    }

    private fun observeUsageStatsForModels() {
        viewModelScope.launch {
            repository.getAllUsageStats().collect {
                updateMessageModelMap(_messages.value)
            }
        }
    }

    private fun loadConversation() {
        viewModelScope.launch {
            conversation = repository.getConversationById(conversationId)
            conversation?.let { conv ->
                isPrivateConversation = repository.hasConversationTag(conv, "private")
                apiConfig = repository.getApiConfigById(conv.apiConfigId)
                if (apiConfig == null) {
                    _error.value = "API配置不存在，请重新配置"
                } else {
                    // 加载可用模型列表
                    loadAvailableModels()
                    // 设置当前模型
                    _currentModel.value = conv.modelName
                    // 使用对话级别配置，如果没有则使用API配置默认值
                    _tempSettings.value = TempChatSettings(
                        temperature = conv.temperature ?: apiConfig?.temperature ?: 0.95f,
                        maxTokens = conv.maxTokens ?: apiConfig?.maxTokens ?: 8192,
                        topP = conv.topP ?: apiConfig?.topP ?: 1.0f,
                        enableThinking = conv.enableThinking ?: apiConfig?.enableThinking ?: true,
                        thinkingEffort = conv.thinkingEffort ?: apiConfig?.thinkingEffort ?: "high",
                        enableWebSearch = conv.enableWebSearch ?: apiConfig?.enableWebSearch ?: false
                    )
                    // 如果对话有自定义配置，自动启用临时设置
                    _useTempSettings.value = true
                }

                val roleplayRepo = AiAssistantApp.instance.roleplayRepository
                val rpSession = roleplayRepo.getSessionByConversationId(conversationId)
                val charIds = rpSession?.getEffectiveCharacterIds().orEmpty()
                val rpCharacters = if (charIds.isNotEmpty()) {
                    roleplayRepo.getCharactersByIds(charIds)
                } else {
                    listOfNotNull(rpSession?.characterId?.let { roleplayRepo.getCharacterById(it) })
                }
                val rpCharacter = rpCharacters.firstOrNull()
                val rpScenario = rpSession?.scenarioId?.let { roleplayRepo.getScenarioById(it) }
                val narrativeMode = rpSession?.let { NarrativeMode.fromValue(it.narrativeMode) } ?: NarrativeMode.CHARACTER

                _uiState.update {
                    it.copy(
                        conversationTitle = conv.title,
                        modelName = conv.modelName,
                        systemPrompt = conv.systemPrompt,
                        enableThinking = conversation?.enableThinking ?: false,
                        isRoleplay = rpSession != null,
                        roleplaySession = rpSession,
                        roleplayCharacter = rpCharacter,
                        roleplayCharacters = rpCharacters,
                        roleplayScenario = rpScenario,
                        narrativeMode = narrativeMode
                    )
                }
            }

            repository.getMessages(conversationId).collect { messageList ->
                _messages.value = messageList
                refreshContextUsage()
                updateMessageModelMap(messageList)
            }
        }
    }

    private fun loadAvailableModels() {
        viewModelScope.launch {
            val conv = conversation ?: return@launch
            val currentConfig = apiConfig ?: return@launch
            val fallbackOption = ChatModelOption(
                apiConfigId = currentConfig.id,
                configName = currentConfig.name,
                provider = currentConfig.provider,
                apiType = currentConfig.apiType,
                modelName = conv.modelName.ifBlank { currentConfig.modelName },
                capability = "auto"
            )
            val options = (repository.getAllVisibleChatModelOptions() + fallbackOption)
                .filter { it.modelName.isNotBlank() }
                .distinctBy { "${it.apiConfigId}:${it.modelName}" }

            _availableModelOptions.value = options
            val selected = options.firstOrNull {
                it.apiConfigId == conv.apiConfigId && it.modelName == conv.modelName
            } ?: options.firstOrNull {
                it.modelName == conv.modelName
            } ?: options.firstOrNull()

            selected?.let { applyCurrentModelOption(it, persist = false) }
        }
    }

    private fun loadPromptTemplates() {
        viewModelScope.launch {
            repository.getAllPromptTemplates().collect { templates ->
                _promptTemplates.value = templates
            }
        }
    }

    // 保存提示词模板
    fun savePromptTemplate(name: String, content: String, description: String? = null, category: String = "general") {
        viewModelScope.launch {
            val template = PromptTemplate(
                name = name,
                content = content,
                description = description,
                category = category
            )
            repository.savePromptTemplate(template)
        }
    }

    // 使用模板
    fun usePromptTemplate(template: PromptTemplate) {
        viewModelScope.launch {
            repository.incrementTemplateUseCount(template.id)
        }
    }

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
            provider.contains("MiMo", ignoreCase = true) -> listOf("mimo")
            else -> emptyList()
        }
    }

    // 切换模型和 API 配置（仅当前对话有效）
    fun switchModel(option: ChatModelOption) {
        applyCurrentModelOption(option, persist = true)
    }

    private fun applyCurrentModelOption(option: ChatModelOption, persist: Boolean) {
        _currentModelOption.value = option
        _currentModel.value = option.modelName
        _uiState.update { it.copy(modelName = option.modelName) }
        viewModelScope.launch {
            if (!persist) {
                apiConfig = repository.getApiConfigById(option.apiConfigId)
                if (!_useTempSettings.value) {
                    apiConfig?.let { cfg ->
                        _tempSettings.value = TempChatSettings(
                            temperature = cfg.temperature,
                            maxTokens = cfg.maxTokens,
                            topP = cfg.topP,
                            enableThinking = cfg.enableThinking,
                            thinkingEffort = cfg.thinkingEffort,
                            enableWebSearch = cfg.enableWebSearch
                        )
                    }
                }
                refreshContextUsage()
                return@launch
            }
            conversation?.let { conv ->
                val updated = conv.copy(
                    apiConfigId = option.apiConfigId,
                    modelName = option.modelName,
                    updatedAt = System.currentTimeMillis()
                )
                AiAssistantApp.instance.database.conversationDao().updateConversation(updated)
                conversation = updated
                apiConfig = repository.getApiConfigById(option.apiConfigId)
                if (!_useTempSettings.value) {
                    apiConfig?.let { cfg ->
                        _tempSettings.value = TempChatSettings(
                            temperature = cfg.temperature,
                            maxTokens = cfg.maxTokens,
                            topP = cfg.topP,
                            enableThinking = cfg.enableThinking,
                            thinkingEffort = cfg.thinkingEffort,
                            enableWebSearch = cfg.enableWebSearch
                        )
                    }
                }
            }
            refreshContextUsage()
        }
    }

    // 更新临时设置并保存到对话
    fun updateTempSettings(settings: TempChatSettings) {
        _tempSettings.value = settings
        _useTempSettings.value = true
        // 保存到对话
        saveConversationSettings(settings)
        refreshContextUsage()
    }

    fun updateChatSettings(settings: TempChatSettings, prompt: String?) {
        val normalizedPrompt = normalizeSystemPrompt(prompt)
        _tempSettings.value = settings
        _useTempSettings.value = true
        _uiState.update {
            it.copy(
                systemPrompt = normalizedPrompt,
                enableThinking = settings.enableThinking
            )
        }
        saveConversationSettings(settings, normalizedPrompt)
        refreshContextUsage()
    }

    // 启用/禁用临时设置
    fun toggleTempSettings(enabled: Boolean) {
        _useTempSettings.value = enabled
        if (!enabled) {
            // 清除对话级别配置
            saveConversationSettings(null)
        }
        refreshContextUsage()
    }

    fun refreshContextUsage() {
        viewModelScope.launch {
            val modelName = _currentModel.value ?: conversation?.modelName ?: _uiState.value.modelName
            val maxTokens = (_useTempSettings.value)
                .takeIf { it }
                ?.let { _tempSettings.value.maxTokens }
                ?: conversation?.maxTokens
                ?: apiConfig?.maxTokens
            val usage = repository.getConversationContextUsage(
                conversationId = conversationId,
                modelNameOverride = modelName,
                maxOutputTokens = maxTokens
            )
            _contextUsage.update {
                it.copy(
                    usage = usage,
                    isCompressing = false,
                    statusMessage = null
                )
            }
        }
    }

    fun compressContextNow() {
        if (_contextUsage.value.isCompressing) return
        viewModelScope.launch {
            val shouldCompress = _contextUsage.value.usage?.canCompress == true
            _contextUsage.update { it.copy(isCompressing = true, statusMessage = "正在压缩上下文...") }
            val modelName = _currentModel.value ?: conversation?.modelName ?: _uiState.value.modelName
            val maxTokens = (_useTempSettings.value)
                .takeIf { it }
                ?.let { _tempSettings.value.maxTokens }
                ?: conversation?.maxTokens
                ?: apiConfig?.maxTokens
            repository.compressConversationContext(
                conversationId = conversationId,
                modelNameOverride = modelName,
                maxOutputTokens = maxTokens
            ).fold(
                onSuccess = { usage ->
                    conversation = repository.getConversationById(conversationId) ?: conversation
                    _contextUsage.value = ContextUsageUiState(
                        usage = usage,
                        statusMessage = if (shouldCompress) "已完成本轮压缩" else "当前上下文已是最新压缩状态"
                    )
                },
                onFailure = { error ->
                    _contextUsage.update {
                        it.copy(
                            isCompressing = false,
                            statusMessage = error.message ?: "压缩失败"
                        )
                    }
                }
            )
        }
    }

    // 保存对话级别配置
    private fun saveConversationSettings(
        settings: TempChatSettings?,
        systemPrompt: String? = _uiState.value.systemPrompt
    ) {
        conversation?.let { conv ->
            val updated = conv.copy(
                temperature = settings?.temperature,
                maxTokens = settings?.maxTokens,
                topP = settings?.topP,
                enableThinking = settings?.enableThinking,
                thinkingEffort = settings?.thinkingEffort,
                enableWebSearch = settings?.enableWebSearch,
                systemPrompt = normalizeSystemPrompt(systemPrompt)
            )
            conversation = updated
            systemPromptSaveJob?.cancel()
            systemPromptSaveJob = viewModelScope.launch {
                AiAssistantApp.instance.database.conversationDao().updateConversation(updated)
            }
        }
    }

    fun sendMessage(content: String, attachments: List<Attachment> = emptyList()) {
        sendMessageInternal(content, attachments, saveUserMessage = true)
    }

    fun sendEditedMessage(source: Message, content: String, attachments: List<Attachment> = emptyList()) {
        if (_isGenerating.value) return

        viewModelScope.launch {
            val allMessages = repository.getMessagesList(conversationId)
            val sourceIndex = allMessages.indexOfFirst { it.id == source.id }
            if (sourceIndex < 0) {
                sendMessage(content, attachments)
                return@launch
            }

            val turnKey = source.variantGroupId
                ?.substringBeforeLast("_user")
                ?: "turn_${source.id}"
            val userGroupId = "${turnKey}_user"
            val assistantGroupId = "${turnKey}_assistant"

            if (source.variantGroupId == null) {
                AiAssistantApp.instance.database.messageDao().updateMessage(
                    source.copy(variantGroupId = userGroupId, variantIndex = 1)
                )
            }

            val nextAssistant = allMessages
                .drop(sourceIndex + 1)
                .takeWhile { it.role != "user" }
                .firstOrNull { it.role == "assistant" }
            if (nextAssistant != null && nextAssistant.variantGroupId == null) {
                AiAssistantApp.instance.database.messageDao().updateMessage(
                    nextAssistant.copy(variantGroupId = assistantGroupId, variantIndex = 1)
                )
            }

            val nextIndex = (allMessages
                .filter { it.variantGroupId == userGroupId }
                .maxOfOrNull { it.variantIndex } ?: 1) + 1

            sendMessageInternal(
                content = content,
                attachments = attachments,
                saveUserMessage = true,
                userVariantGroupId = userGroupId,
                userVariantIndex = nextIndex,
                assistantVariantGroupId = assistantGroupId,
                assistantVariantIndex = nextIndex
            )
        }
    }

    private fun sendMessageInternal(
        content: String,
        attachments: List<Attachment> = emptyList(),
        saveUserMessage: Boolean,
        userVariantGroupId: String? = null,
        userVariantIndex: Int = 1,
        assistantVariantGroupId: String? = null,
        assistantVariantIndex: Int = 1
    ) {
        if ((content.isBlank() && attachments.isEmpty()) || _isGenerating.value) return

        val selectedOption = _currentModelOption.value ?: conversation?.let { conv ->
            val cfg = apiConfig
            if (cfg != null) {
                ChatModelOption(
                    apiConfigId = cfg.id,
                    configName = cfg.name,
                    provider = cfg.provider,
                    apiType = cfg.apiType,
                    modelName = conv.modelName.ifBlank { cfg.modelName },
                    capability = "auto"
                )
            } else null
        }

        if (selectedOption == null) {
            _error.value = "API配置不存在，请在设置中配置API"
            return
        }

        // 保存用户消息
        val attachmentsJson = if (attachments.isNotEmpty()) {
            gson.toJson(attachments)
        } else null

        isMessageSaved = false

        // 获取当前选择的模型和设置
        val settings = if (_useTempSettings.value) _tempSettings.value else null
        val currentSystemPrompt = normalizeSystemPrompt(_uiState.value.systemPrompt)

        generationJob = AiAssistantApp.instance.applicationScope.launch {
            _isGenerating.value = true
            _currentResponse.value = ""
            _currentThinking.value = ""
            _error.value = null
            val currentCallingModel = selectedOption.modelName
            val requestStartTime = System.currentTimeMillis()
            runtimeMessageModelMap[requestStartTime] = currentCallingModel

            try {
                if (saveUserMessage) {
                    val userMessage = Message(
                        conversationId = conversationId,
                        role = "user",
                        content = content,
                        attachments = attachmentsJson,
                        variantGroupId = userVariantGroupId,
                        variantIndex = userVariantIndex
                    )
                    repository.saveMessage(userMessage)
                }

                val selectedConfig = repository.getDecryptedConfig(selectedOption.apiConfigId)
                    ?: throw Exception("API配置不存在，请重新配置")
                systemPromptSaveJob?.join()
                val effectiveConfig = selectedConfig.copy(modelName = selectedOption.modelName)

                val roleplayRepo = AiAssistantApp.instance.roleplayRepository
                val currentRoleplaySession = _uiState.value.roleplaySession
                val effectiveSystemPrompt = if (currentRoleplaySession != null) {
                    roleplayRepo.assembleRoleplayContext(
                        sessionId = currentRoleplaySession.id,
                        globalSystemPrompt = currentSystemPrompt,
                        userMessage = content
                    )
                } else {
                    currentSystemPrompt
                }

                val requestOptions = ChatRequestOptions(
                    temperature = settings?.temperature,
                    maxTokens = settings?.maxTokens,
                    topP = settings?.topP,
                    enableThinking = settings?.enableThinking,
                    thinkingEffort = settings?.thinkingEffort,
                    enableWebSearch = settings?.enableWebSearch,
                    overrideSystemPrompt = true,
                    systemPromptOverride = effectiveSystemPrompt
                )

                // 直接在主线程调用，通过withContext切换到IO线程
                withContext(Dispatchers.IO) {
                    repository.sendChatMessageWithConfig(
                        config = effectiveConfig,
                        conversationId = conversationId,
                        userMessage = content,
                        attachments = attachments,
                        options = requestOptions,
                        assistantVariantGroupId = assistantVariantGroupId,
                        assistantVariantIndex = assistantVariantIndex,
                        onToken = { token ->
                            // 使用update确保线程安全
                            _currentResponse.update { it + token }
                        },
                        onThinkingToken = { token ->
                            _currentThinking.update { it + token }
                        },
                        onComplete = { _, _, _ ->
                            isMessageSaved = true
                            _isGenerating.value = false
                            _currentResponse.value = ""
                            _currentThinking.value = ""
                            autoNameIfNeeded()
                        },
                        onError = { errorMsg ->
                            isMessageSaved = true
                            _isGenerating.value = false
                            _error.value = errorMsg
                            saveErrorReply(errorMsg)
                            _currentResponse.value = ""
                            _currentThinking.value = ""
                        }
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    _isGenerating.value = false
                    return@launch
                }
                _isGenerating.value = false
                if (!isMessageSaved) {
                    isMessageSaved = true
                    val errorMsg = e.message ?: "未知错误"
                    _error.value = errorMsg
                    saveErrorReply(errorMsg)
                    _currentResponse.value = ""
                    _currentThinking.value = ""
                }
            }
        }
    }

    private fun saveErrorReply(errorMsg: String) {
        val partialResponse = _currentResponse.value.trim()
        val partialThinking = _currentThinking.value.trim().ifEmpty { null }
        AiAssistantApp.instance.applicationScope.launch {
            val content = if (partialResponse.isNotBlank()) {
                "$partialResponse\n\n[输出已被中断: $errorMsg]"
            } else {
                buildString {
                    append("请求失败\n\n")
                    append(errorMsg.trim().ifBlank { "未知错误" })
                    append("\n\n可以检查 API 地址、密钥、模型名称或网络状态后重试。")
                }
            }
            val message = Message(
                conversationId = conversationId,
                role = "assistant",
                content = content,
                thinkingContent = partialThinking
            )
            repository.saveMessage(message)
        }
    }

    fun stopGeneration() {
        repository.cancelActiveRequest(conversationId)
        generationJob?.cancel(CancellationException("用户暂停生成"))
        _isGenerating.value = false

        val responseToSave = _currentResponse.value
        val thinkingToSave = _currentThinking.value.ifBlank { null }

        if (!isMessageSaved && (responseToSave.isNotBlank() || thinkingToSave != null)) {
            isMessageSaved = true
            val finalContent = if (responseToSave.isNotBlank()) {
                "$responseToSave\n\n[输出已由用户暂停]"
            } else {
                "[思考已停止]"
            }
            AiAssistantApp.instance.applicationScope.launch {
                val message = Message(
                    conversationId = conversationId,
                    role = "assistant",
                    content = finalContent,
                    thinkingContent = thinkingToSave
                )
                repository.saveMessage(message)
            }
        }
        _currentResponse.value = ""
        _currentThinking.value = ""
    }

    fun clearError() {
        _error.value = null
    }

    // 重新生成最后一条AI消息
    fun regenerateLastMessage() {
        if (_isGenerating.value) return

        viewModelScope.launch {
            val messages = repository.getMessagesList(conversationId)
            if (messages.isEmpty()) return@launch

            // 找到最后一条AI消息和它之前的用户消息
            val lastAssistantIndex = messages.indexOfLast { it.role == "assistant" }
            if (lastAssistantIndex < 0) return@launch

            val lastAssistantMessage = messages[lastAssistantIndex]
            val lastUserMessage = messages.lastOrNull { it.role == "user" && it.createdAt < lastAssistantMessage.createdAt }
            if (lastUserMessage != null) {
                val groupId = lastAssistantMessage.variantGroupId ?: "reply_${lastAssistantMessage.id}"
                if (lastAssistantMessage.variantGroupId == null) {
                    AiAssistantApp.instance.database.messageDao().updateMessage(
                        lastAssistantMessage.copy(variantGroupId = groupId, variantIndex = 1)
                    )
                }
                val nextIndex = (messages
                    .filter { it.variantGroupId == groupId }
                    .maxOfOrNull { it.variantIndex } ?: 1) + 1

                sendMessageInternal(
                    content = lastUserMessage.content,
                    saveUserMessage = false,
                    assistantVariantGroupId = groupId,
                    assistantVariantIndex = nextIndex
                )
            }
        }
    }

    // 删除单条消息
    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            repository.deleteMessage(message)
        }
    }

    fun deleteMessagesFrom(message: Message) {
        viewModelScope.launch {
            repository.deleteMessagesFrom(conversationId, message.createdAt)
        }
    }

    // 重命名对话标题
    fun renameConversation(newTitle: String) {
        viewModelScope.launch {
            conversation?.let { conv ->
                val updated = conv.copy(title = newTitle)
                AiAssistantApp.instance.database.conversationDao().updateConversation(updated)
                conversation = updated
                _uiState.update { it.copy(conversationTitle = newTitle) }
            }
        }
    }

    // 自动生成标题（根据对话内容）
    fun generateAutoTitle() {
        viewModelScope.launch {
            val messages = repository.getMessagesList(conversationId)
            if (messages.isEmpty()) return@launch

            // 取第一条用户消息作为标题依据
            val firstUserMessage = messages.firstOrNull { it.role == "user" }
            if (firstUserMessage != null) {
                val title = generateTitleFromContent(firstUserMessage.content)
                renameConversation(title)
            }
        }
    }

    private fun generateTitleFromContent(content: String): String {
        // 简单的标题生成逻辑
        val cleanContent = content.trim()
        return when {
            cleanContent.length <= 20 -> cleanContent
            cleanContent.contains("\n") -> cleanContent.substringBefore("\n").take(20) + "..."
            else -> cleanContent.take(20) + "..."
        }
    }

    private fun autoNameIfNeeded() {
        AiAssistantApp.instance.applicationScope.launch {
            val latestConversation = repository.getConversationById(conversationId) ?: return@launch
            if (latestConversation.title != "新对话" && latestConversation.title.isNotBlank()) {
                return@launch
            }

            val generatedTitle = repository.generateConversationTitle(conversationId)
            val fallbackTitle = repository.getMessagesList(conversationId)
                .firstOrNull { it.role == "user" }
                ?.content
                ?.let { generateTitleFromContent(it) }
            val title = generatedTitle ?: fallbackTitle ?: return@launch
            val updated = latestConversation.copy(title = title)
            AiAssistantApp.instance.database.conversationDao().updateConversation(updated)
            conversation = updated
            _uiState.update { it.copy(conversationTitle = title) }
        }
    }

    fun updateSystemPrompt(prompt: String?) {
        val normalizedPrompt = normalizeSystemPrompt(prompt)
        conversation?.let { conv ->
            val updated = conv.copy(systemPrompt = normalizedPrompt)
            conversation = updated
            _uiState.update { it.copy(systemPrompt = normalizedPrompt) }
            systemPromptSaveJob?.cancel()
            systemPromptSaveJob = viewModelScope.launch {
                AiAssistantApp.instance.database.conversationDao().updateConversation(updated)
            }
        }
    }

    private fun normalizeSystemPrompt(prompt: String?): String? {
        return prompt?.trim()?.ifBlank { null }
    }

    // 创建会话分支
    fun createBranch(messageId: Long, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val originalConversation = conversation ?: return@launch

            // 创建新的对话
            val newConversationId = repository.createConversation(
                title = "${originalConversation.title} (分支)",
                apiConfigId = originalConversation.apiConfigId,
                modelName = originalConversation.modelName,
                folderId = originalConversation.folderId,
                systemPrompt = originalConversation.systemPrompt
            )

            // 复制分支点之前的所有消息
            val messages = repository.getMessagesList(conversationId)
            val branchIndex = messages.indexOfFirst { it.id == messageId }
            if (branchIndex >= 0) {
                val messagesToCopy = messages.subList(0, branchIndex + 1)
                messagesToCopy.forEach { msg ->
                    repository.saveMessage(msg.copy(id = 0, conversationId = newConversationId))
                }
            }

            // 创建分支记录
            repository.createBranch(conversationId, messageId, newConversationId)

            onComplete(newConversationId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isPrivateConversation) {
            destroyPrivateConversation()
        } else {
            // generationJob运行在Application级作用域中，退出页面后会继续完成并保存回复。
            autoNameIfNeeded()
        }
    }

    fun leaveConversation(onComplete: () -> Unit) {
        if (isPrivateConversation) {
            destroyPrivateConversation()
        } else {
            autoNameIfNeeded()
        }
        onComplete()
    }

    private fun destroyPrivateConversation() {
        if (privateExitHandled) return
        privateExitHandled = true
        repository.cancelActiveRequest(conversationId)
        generationJob?.cancel(CancellationException("隐私对话退出"))
        AiAssistantApp.instance.applicationScope.launch {
            repository.destroyPrivateConversation(conversationId)
        }
    }

    fun sendPlotAction(action: PlotAction, customInstruction: String? = null) {
        val session = _uiState.value.roleplaySession ?: return
        viewModelScope.launch {
            val instruction = AiAssistantApp.instance.roleplayRepository.processPlotAction(session.id, action, customInstruction)
            sendMessage(instruction)
        }
    }

    fun updateNarrativeMode(mode: NarrativeMode) {
        val session = _uiState.value.roleplaySession ?: return
        viewModelScope.launch {
            AiAssistantApp.instance.roleplayRepository.updateSession(session.copy(narrativeMode = mode.value))
            _uiState.update { it.copy(narrativeMode = mode) }
        }
    }

    fun appendAndMergeStoryBundle(
        newCharacters: List<CharacterProfile>,
        newScenario: RoleplayScenario?,
        resolutionMap: Map<String, com.aiassistant.ui.screens.roleplay.ConflictAction>,
        onComplete: (String) -> Unit
    ) {
        val session = _uiState.value.roleplaySession ?: return
        viewModelScope.launch {
            try {
                val roleplayRepo = AiAssistantApp.instance.roleplayRepository
                val existingAllChars = roleplayRepo.getAllCharacters().first()
                val currentIds = session.getEffectiveCharacterIds().toMutableList()

                newCharacters.forEach { incoming ->
                    val existing = existingAllChars.firstOrNull { it.name.trim() == incoming.name.trim() }
                    val action = resolutionMap[incoming.name.trim()] ?: com.aiassistant.ui.screens.roleplay.ConflictAction.MERGE

                    if (existing != null) {
                        when (action) {
                            com.aiassistant.ui.screens.roleplay.ConflictAction.CREATE_COPY -> {
                                val copyChar = incoming.copy(name = "${incoming.name} (副本)")
                                val newId = roleplayRepo.insertCharacter(copyChar)
                                if (newId !in currentIds) currentIds.add(newId)
                            }
                            com.aiassistant.ui.screens.roleplay.ConflictAction.OVERWRITE -> {
                                val updated = incoming.copy(id = existing.id, isFavorite = existing.isFavorite)
                                roleplayRepo.updateCharacter(updated)
                                if (existing.id !in currentIds) currentIds.add(existing.id)
                            }
                            com.aiassistant.ui.screens.roleplay.ConflictAction.MERGE -> {
                                val merged = com.aiassistant.utils.RoleplaySmartAnalyzer.mergeCharacters(existing, incoming)
                                roleplayRepo.updateCharacter(merged)
                                if (existing.id !in currentIds) currentIds.add(existing.id)
                            }
                        }
                    } else {
                        val newId = roleplayRepo.insertCharacter(incoming)
                        if (newId !in currentIds) currentIds.add(newId)
                    }
                }

                var finalScenarioId = session.scenarioId
                if (newScenario != null) {
                    val currentSc = session.scenarioId?.let { roleplayRepo.getScenarioById(it) }
                    if (currentSc != null) {
                        val mergedSc = com.aiassistant.utils.RoleplaySmartAnalyzer.mergeScenarios(currentSc, newScenario)
                        roleplayRepo.updateScenario(mergedSc)
                    } else {
                        finalScenarioId = roleplayRepo.insertScenario(newScenario)
                    }
                }

                val updatedCharIdsJson = com.google.gson.Gson().toJson(currentIds)
                val updatedSession = session.copy(
                    characterId = currentIds.firstOrNull(),
                    characterIds = updatedCharIdsJson,
                    scenarioId = finalScenarioId
                )
                roleplayRepo.updateSession(updatedSession)

                val updatedChars = roleplayRepo.getCharactersByIds(currentIds)
                val updatedSc = finalScenarioId?.let { roleplayRepo.getScenarioById(it) }

                _uiState.update {
                    it.copy(
                        roleplaySession = updatedSession,
                        roleplayCharacter = updatedChars.firstOrNull(),
                        roleplayCharacters = updatedChars,
                        roleplayScenario = updatedSc
                    )
                }

                onComplete("已成功追加/融合 ${newCharacters.size} 位角色" + (if (newScenario != null) "与世界观设定" else ""))
            } catch (e: Exception) {
                onComplete("追加融合失败: ${e.message}")
            }
        }
    }

    fun updateStorySessionContext(
        characterIds: List<Long>,
        scenarioId: Long?,
        narrativeMode: NarrativeMode,
        plotSummary: String
    ) {
        viewModelScope.launch {
            val roleplayRepo = AiAssistantApp.instance.roleplayRepository
            val currentSession = _uiState.value.roleplaySession ?: roleplayRepo.getSessionByConversationId(conversationId) ?: return@launch
            val charIdsJson = com.google.gson.Gson().toJson(characterIds)
            val updated = currentSession.copy(
                characterIds = charIdsJson,
                characterId = characterIds.firstOrNull(),
                scenarioId = scenarioId,
                narrativeMode = narrativeMode.value,
                currentPlotSummary = plotSummary,
                updatedAt = System.currentTimeMillis()
            )
            roleplayRepo.updateSession(updated)

            val rpCharacters = roleplayRepo.getEffectiveCharactersForSession(updated)
            val rpScenario = roleplayRepo.getEffectiveScenarioForSession(updated)
            _uiState.update {
                it.copy(
                    roleplaySession = updated,
                    roleplayCharacter = rpCharacters.firstOrNull(),
                    roleplayCharacters = rpCharacters,
                    roleplayScenario = rpScenario,
                    narrativeMode = narrativeMode
                )
            }
        }
    }

    fun saveLocalCharacterOverride(editedCharacter: CharacterProfile) {
        viewModelScope.launch {
            val roleplayRepo = AiAssistantApp.instance.roleplayRepository
            val currentSession = _uiState.value.roleplaySession ?: roleplayRepo.getSessionByConversationId(conversationId) ?: return@launch
            val baseCharacters = _uiState.value.roleplayCharacters
            val currentCustomized = currentSession.getCustomizedCharacters(baseCharacters).toMutableList()
            val existingIndex = currentCustomized.indexOfFirst { it.id == editedCharacter.id && it.id > 0 || it.name == editedCharacter.name }
            if (existingIndex >= 0) {
                currentCustomized[existingIndex] = editedCharacter
            } else {
                currentCustomized.add(editedCharacter)
            }
            val jsonStr = com.google.gson.Gson().toJson(currentCustomized)
            val updatedSession = currentSession.copy(
                customCharacterData = jsonStr,
                updatedAt = System.currentTimeMillis()
            )
            roleplayRepo.updateSession(updatedSession)
            val effectiveChars = roleplayRepo.getEffectiveCharactersForSession(updatedSession)
            _uiState.update {
                it.copy(
                    roleplaySession = updatedSession,
                    roleplayCharacter = effectiveChars.firstOrNull(),
                    roleplayCharacters = effectiveChars
                )
            }
        }
    }

    fun saveLocalScenarioOverride(editedScenario: RoleplayScenario) {
        viewModelScope.launch {
            val roleplayRepo = AiAssistantApp.instance.roleplayRepository
            val currentSession = _uiState.value.roleplaySession ?: roleplayRepo.getSessionByConversationId(conversationId) ?: return@launch
            val jsonStr = com.google.gson.Gson().toJson(editedScenario)
            val updatedSession = currentSession.copy(
                customScenarioData = jsonStr,
                updatedAt = System.currentTimeMillis()
            )
            roleplayRepo.updateSession(updatedSession)
            val effectiveScenario = roleplayRepo.getEffectiveScenarioForSession(updatedSession)
            _uiState.update {
                it.copy(
                    roleplaySession = updatedSession,
                    roleplayScenario = effectiveScenario
                )
            }
        }
    }

    fun analyzeAndProposeSettingFromInput(
        text: String,
        onProgress: (String) -> Unit = {},
        onNoProposal: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val currentConfig = apiConfig ?: repository.getDefaultApiConfig() ?: return@launch
                val modelToUse = _currentModel.value ?: currentConfig.modelName
                withContext(Dispatchers.Main) { onProgress("正在结合故事已有设定进行精准分析...") }
                val currentChars = _uiState.value.roleplayCharacters
                val currentScenario = _uiState.value.roleplayScenario
                val proposal = com.aiassistant.utils.RoleplaySmartAnalyzer.analyzeStoryInputForProposal(
                    context = AiAssistantApp.instance,
                    rawText = text,
                    existingCharacters = currentChars,
                    existingScenario = currentScenario,
                    repository = repository,
                    preferredConfig = currentConfig,
                    selectedModel = modelToUse,
                    onProgress = { msg ->
                        viewModelScope.launch(Dispatchers.Main) {
                            onProgress(msg)
                        }
                    }
                )
                if (proposal.hasAnyUpdates) {
                    _uiState.update {
                        it.copy(
                            suggestedProposal = ProposedSettingBundle(
                                updatedCharacters = proposal.updatedCharacters,
                                newCharacters = proposal.newCharacters,
                                scenarioUpdate = proposal.scenarioUpdate,
                                summary = proposal.summaryReport
                            )
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) { onNoProposal() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "识别失败") }
            }
        }
    }

    fun dismissProposedSetting() {
        _uiState.update { it.copy(suggestedProposal = null) }
    }

    fun applyProposedSetting(characters: List<CharacterProfile>, scenario: RoleplayScenario?) {
        viewModelScope.launch {
            val roleplayRepo = AiAssistantApp.instance.roleplayRepository
            val currentSession = _uiState.value.roleplaySession ?: roleplayRepo.getSessionByConversationId(conversationId) ?: return@launch

            // 1. 如果有新增/更新角色，存入局部角色列表
            var updatedSession = currentSession
            if (characters.isNotEmpty()) {
                val baseChars = _uiState.value.roleplayCharacters
                val currentCustomized = currentSession.getCustomizedCharacters(baseChars).toMutableList()
                characters.forEach { newChar ->
                    val idx = currentCustomized.indexOfFirst { (it.id > 0 && it.id == newChar.id) || it.name == newChar.name }
                    if (idx >= 0) {
                        currentCustomized[idx] = newChar
                    } else {
                        currentCustomized.add(newChar)
                    }
                }
                val jsonStr = com.google.gson.Gson().toJson(currentCustomized)
                updatedSession = updatedSession.copy(
                    customCharacterData = jsonStr,
                    updatedAt = System.currentTimeMillis()
                )
            }

            // 2. 如果有更新世界观，存入局部世界观
            if (scenario != null) {
                val jsonStr = com.google.gson.Gson().toJson(scenario)
                updatedSession = updatedSession.copy(
                    customScenarioData = jsonStr,
                    updatedAt = System.currentTimeMillis()
                )
            }

            roleplayRepo.updateSession(updatedSession)
            val effectiveChars = roleplayRepo.getEffectiveCharactersForSession(updatedSession)
            val effectiveScenario = roleplayRepo.getEffectiveScenarioForSession(updatedSession)
            _uiState.update {
                it.copy(
                    roleplaySession = updatedSession,
                    roleplayCharacter = effectiveChars.firstOrNull(),
                    roleplayCharacters = effectiveChars,
                    roleplayScenario = effectiveScenario,
                    suggestedProposal = null
                )
            }
        }
    }

    fun addNewLocalCharacter(newChar: CharacterProfile) {
        viewModelScope.launch {
            val roleplayRepo = AiAssistantApp.instance.roleplayRepository
            val currentSession = _uiState.value.roleplaySession ?: roleplayRepo.getSessionByConversationId(conversationId) ?: return@launch

            val baseChars = _uiState.value.roleplayCharacters
            val currentCustomized = currentSession.getCustomizedCharacters(baseChars).toMutableList()
            val idx = currentCustomized.indexOfFirst { (it.id > 0 && it.id == newChar.id) || it.name == newChar.name }
            if (idx >= 0) {
                currentCustomized[idx] = newChar
            } else {
                currentCustomized.add(newChar)
            }
            val jsonStr = com.google.gson.Gson().toJson(currentCustomized)
            val updatedSession = currentSession.copy(
                customCharacterData = jsonStr,
                updatedAt = System.currentTimeMillis()
            )
            roleplayRepo.updateSession(updatedSession)
            val effectiveChars = roleplayRepo.getEffectiveCharactersForSession(updatedSession)
            _uiState.update {
                it.copy(
                    roleplaySession = updatedSession,
                    roleplayCharacter = effectiveChars.firstOrNull(),
                    roleplayCharacters = effectiveChars
                )
            }
        }
    }

    fun updateLocalCharacter(updatedChar: CharacterProfile) {
        addNewLocalCharacter(updatedChar)
    }

    fun deleteLocalCharacter(character: CharacterProfile) {
        viewModelScope.launch {
            val roleplayRepo = AiAssistantApp.instance.roleplayRepository
            val currentSession = _uiState.value.roleplaySession ?: roleplayRepo.getSessionByConversationId(conversationId) ?: return@launch

            val charIds = currentSession.getEffectiveCharacterIds().filterNot { it == character.id }
            val charIdsJson = com.google.gson.Gson().toJson(charIds)

            val baseChars = _uiState.value.roleplayCharacters
            val currentCustomized = currentSession.getCustomizedCharacters(baseChars).filterNot {
                (it.id > 0 && it.id == character.id) || it.name == character.name
            }
            val customJson = if (currentCustomized.isNotEmpty()) {
                com.google.gson.Gson().toJson(currentCustomized)
            } else null

            val updatedSession = currentSession.copy(
                characterIds = charIdsJson,
                customCharacterData = customJson,
                updatedAt = System.currentTimeMillis()
            )
            roleplayRepo.updateSession(updatedSession)
            val effectiveChars = roleplayRepo.getEffectiveCharactersForSession(updatedSession)
            _uiState.update {
                it.copy(
                    roleplaySession = updatedSession,
                    roleplayCharacter = effectiveChars.firstOrNull(),
                    roleplayCharacters = effectiveChars
                )
            }
        }
    }

    fun saveLocalScenario(scenario: RoleplayScenario) {
        viewModelScope.launch {
            val roleplayRepo = AiAssistantApp.instance.roleplayRepository
            val currentSession = _uiState.value.roleplaySession ?: roleplayRepo.getSessionByConversationId(conversationId) ?: return@launch

            val jsonStr = com.google.gson.Gson().toJson(scenario)
            val updatedSession = currentSession.copy(
                scenarioId = if (scenario.id > 0) scenario.id else currentSession.scenarioId,
                customScenarioData = jsonStr,
                updatedAt = System.currentTimeMillis()
            )
            roleplayRepo.updateSession(updatedSession)
            val effectiveScenario = roleplayRepo.getEffectiveScenarioForSession(updatedSession)
            _uiState.update {
                it.copy(
                    roleplaySession = updatedSession,
                    roleplayScenario = effectiveScenario
                )
            }
        }
    }

    fun deleteLocalScenario() {
        viewModelScope.launch {
            val roleplayRepo = AiAssistantApp.instance.roleplayRepository
            val currentSession = _uiState.value.roleplaySession ?: roleplayRepo.getSessionByConversationId(conversationId) ?: return@launch

            val updatedSession = currentSession.copy(
                scenarioId = null,
                customScenarioData = null,
                updatedAt = System.currentTimeMillis()
            )
            roleplayRepo.updateSession(updatedSession)
            _uiState.update {
                it.copy(
                    roleplaySession = updatedSession,
                    roleplayScenario = null
                )
            }
        }
    }

    fun syncCharacterToDatabase(character: CharacterProfile, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val roleplayRepo = AiAssistantApp.instance.roleplayRepository
                val savedChar = roleplayRepo.syncCharacterToDatabase(character)
                withContext(Dispatchers.Main) {
                    onDone(true, "已将【${savedChar.name}】同步保存至角色库")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onDone(false, "同步失败: ${e.message}")
                }
            }
        }
    }

    fun syncScenarioToDatabase(scenario: RoleplayScenario, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val roleplayRepo = AiAssistantApp.instance.roleplayRepository
                val savedSc = roleplayRepo.syncScenarioToDatabase(scenario)
                withContext(Dispatchers.Main) {
                    onDone(true, "已将【${savedSc.name}】同步保存至世界观库")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onDone(false, "同步失败: ${e.message}")
                }
            }
        }
    }

    fun syncCharacterFromDatabase(characterId: Long, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val roleplayRepo = AiAssistantApp.instance.roleplayRepository
                val currentSession = _uiState.value.roleplaySession ?: roleplayRepo.getSessionByConversationId(conversationId) ?: return@launch
                val updatedSession = roleplayRepo.syncCharacterFromDatabase(currentSession, characterId)
                val effectiveChars = roleplayRepo.getEffectiveCharactersForSession(updatedSession)
                _uiState.update {
                    it.copy(
                        roleplaySession = updatedSession,
                        roleplayCharacter = effectiveChars.firstOrNull(),
                        roleplayCharacters = effectiveChars
                    )
                }
                withContext(Dispatchers.Main) {
                    onDone(true, "已从数据库重新加载角色最新设定")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onDone(false, "同步失败: ${e.message}")
                }
            }
        }
    }

    fun syncScenarioFromDatabase(scenarioId: Long, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val roleplayRepo = AiAssistantApp.instance.roleplayRepository
                val currentSession = _uiState.value.roleplaySession ?: roleplayRepo.getSessionByConversationId(conversationId) ?: return@launch
                val effectiveSc = roleplayRepo.syncScenarioFromDatabase(currentSession, scenarioId)
                _uiState.update {
                    it.copy(
                        roleplaySession = currentSession.copy(scenarioId = scenarioId, customScenarioData = null),
                        roleplayScenario = effectiveSc
                    )
                }
                withContext(Dispatchers.Main) {
                    onDone(true, "已从数据库重新加载世界观最新设定")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onDone(false, "同步失败: ${e.message}")
                }
            }
        }
    }

    fun summarizeAndExtractMemories(onSuccess: (String) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val roleplaySession = _uiState.value.roleplaySession ?: AiAssistantApp.instance.roleplayRepository.getSessionByConversationId(conversationId)
                if (roleplaySession == null) {
                    withContext(Dispatchers.Main) { onError("当前不是故事会话") }
                    return@launch
                }
                val allMessages = repository.getMessagesList(conversationId)
                if (allMessages.isEmpty()) {
                    withContext(Dispatchers.Main) { onError("暂无剧情记录可提炼") }
                    return@launch
                }
                val transcript = allMessages.takeLast(16).joinToString("\n") { m ->
                    val role = if (m.role == "user") "【导演/用户】" else "【模型剧情】"
                    "$role: ${m.content.take(300)}"
                }
                val prompt = """
                    请分析以下故事剧本对白与情节发展：
                    $transcript

                    请完成两项任务：
                    1. 生成一段简明扼要、连贯的中文【当前剧情摘要】（不超过 200 字）；
                    2. 提取 2~4 条不可违背的【关键事实或既定设定】（每条一句话）。

                    请严格输出合法 JSON，格式如下：
                    {
                      "plotSummary": "剧情摘要文本...",
                      "extractedFacts": [
                        "事实1...",
                        "事实2..."
                      ]
                    }
                """.trimIndent()

                val config = apiConfig ?: repository.getDefaultApiConfig()
                if (config == null) {
                    withContext(Dispatchers.Main) { onError("未找到可用的 API 配置") }
                    return@launch
                }
                val responseText = withContext(Dispatchers.IO) {
                    repository.executeQuickCompletion(config, prompt, maxTokens = 1024).orEmpty()
                }
                val jsonStart = responseText.indexOf('{')
                val jsonEnd = responseText.lastIndexOf('}')
                val jsonStr = if (jsonStart >= 0 && jsonEnd > jsonStart) responseText.substring(jsonStart, jsonEnd + 1) else null

                if (jsonStr != null) {
                    val parsed = com.google.gson.JsonParser.parseString(jsonStr).asJsonObject
                    val newSummary = parsed.get("plotSummary")?.asString.orEmpty()
                    val factsArray = parsed.get("extractedFacts")?.asJsonArray

                    if (newSummary.isNotBlank()) {
                        AiAssistantApp.instance.roleplayRepository.savePlotSummary(roleplaySession.id, newSummary)
                    }
                    factsArray?.forEach { el ->
                        val factStr = el.asString.trim()
                        if (factStr.isNotBlank()) {
                            AiAssistantApp.instance.roleplayRepository.addPinnedFact(roleplaySession.id, factStr)
                        }
                    }
                    val updatedSession = AiAssistantApp.instance.roleplayRepository.getSessionById(roleplaySession.id)
                    _uiState.update { it.copy(roleplaySession = updatedSession) }
                    withContext(Dispatchers.Main) {
                        onSuccess("已成功提炼剧情摘要与 ${factsArray?.size() ?: 0} 条关键事实！")
                    }
                } else {
                    AiAssistantApp.instance.roleplayRepository.savePlotSummary(roleplaySession.id, responseText.take(200))
                    val updatedSession = AiAssistantApp.instance.roleplayRepository.getSessionById(roleplaySession.id)
                    _uiState.update { it.copy(roleplaySession = updatedSession) }
                    withContext(Dispatchers.Main) {
                        onSuccess("已保存剧情摘要")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "提炼失败")
                }
            }
        }
    }

    private fun updateMessageModelMap(messageList: List<Message>) {
        viewModelScope.launch {
            val stats = repository.getUsageStatsListByTimeRange(0, System.currentTimeMillis())
            val defaultModel = conversation?.modelName.orEmpty()
            val currentMap = _messageModelMap.value.toMutableMap()
            var changed = false

            messageList.filter { it.role == "assistant" }.forEach { msg ->
                if (!currentMap.containsKey(msg.id)) {
                    val matchingStat = stats.filter {
                        Math.abs(it.timestamp - msg.createdAt) < 15000L ||
                        (it.responseTime > 0 && it.responseTime == msg.responseTime)
                    }.minByOrNull { Math.abs(it.timestamp - msg.createdAt) }

                    val model = matchingStat?.modelName?.ifBlank { null }
                        ?: runtimeMessageModelMap[msg.createdAt]
                        ?: defaultModel.ifBlank { null }

                    if (!model.isNullOrBlank()) {
                        currentMap[msg.id] = model
                        currentMap[msg.createdAt] = model
                        changed = true
                    }
                }
            }
            if (changed) {
                _messageModelMap.value = currentMap
            }
        }
    }

    companion object {
        private val conversationDrafts = java.util.concurrent.ConcurrentHashMap<Long, String>()
        fun getDraft(conversationId: Long): String = conversationDrafts[conversationId].orEmpty()
        fun saveDraft(conversationId: Long, text: String) {
            if (text.isBlank()) conversationDrafts.remove(conversationId)
            else conversationDrafts[conversationId] = text
        }

        fun factory(conversationId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatViewModel(conversationId) as T
                }
            }
        }
    }
}

data class ProposedSettingBundle(
    val updatedCharacters: List<com.aiassistant.utils.ProposedCharacterUpdate> = emptyList(),
    val newCharacters: List<com.aiassistant.utils.ProposedCharacterUpdate> = emptyList(),
    val scenarioUpdate: com.aiassistant.utils.ProposedScenarioUpdate? = null,
    val summary: String = ""
) {
    val allCharacters: List<CharacterProfile>
        get() = updatedCharacters.map { it.character } + newCharacters.map { it.character }

    val scenario: RoleplayScenario?
        get() = scenarioUpdate?.scenario
}

data class ChatUiState(
    val conversationTitle: String = "新对话",
    val modelName: String = "",
    val systemPrompt: String? = null,
    val enableThinking: Boolean = false,
    val isLoading: Boolean = false,
    val isRoleplay: Boolean = false,
    val roleplaySession: RoleplaySession? = null,
    val roleplayCharacter: CharacterProfile? = null,
    val roleplayCharacters: List<CharacterProfile> = emptyList(),
    val roleplayScenario: RoleplayScenario? = null,
    val narrativeMode: NarrativeMode = NarrativeMode.CHARACTER,
    val suggestedProposal: ProposedSettingBundle? = null
)

data class ContextUsageUiState(
    val usage: ConversationContextUsage? = null,
    val isCompressing: Boolean = false,
    val statusMessage: String? = null
)

// 临时聊天设置（仅当前对话有效）
data class TempChatSettings(
    val temperature: Float = 0.95f,
    val maxTokens: Int = 8192,
    val topP: Float = 1.0f,
    val enableThinking: Boolean = true,
    val thinkingEffort: String = "high",
    val enableWebSearch: Boolean = false
)
