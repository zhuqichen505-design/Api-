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
    private val _useTempSettings = MutableStateFlow(false)
    val useTempSettings: StateFlow<Boolean> = _useTempSettings.asStateFlow()

    // 提示词模板列表
    private val _promptTemplates = MutableStateFlow<List<PromptTemplate>>(emptyList())
    val promptTemplates: StateFlow<List<PromptTemplate>> = _promptTemplates.asStateFlow()

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
                        maxTokens = conv.maxTokens ?: apiConfig?.maxTokens ?: 4096,
                        topP = conv.topP ?: apiConfig?.topP ?: 1.0f,
                        enableThinking = conv.enableThinking ?: apiConfig?.enableThinking ?: false,
                        thinkingEffort = conv.thinkingEffort ?: apiConfig?.thinkingEffort ?: "medium",
                        enableWebSearch = conv.enableWebSearch ?: apiConfig?.enableWebSearch ?: false
                    )
                    // 如果对话有自定义配置，自动启用临时设置
                    _useTempSettings.value = conv.temperature != null || conv.maxTokens != null ||
                            conv.topP != null || conv.enableThinking != null || conv.enableWebSearch != null
                }
                _uiState.update {
                    it.copy(
                        conversationTitle = conv.title,
                        modelName = conv.modelName,
                        systemPrompt = conv.systemPrompt,
                        enableThinking = conversation?.enableThinking ?: false
                    )
                }
            }

            repository.getMessages(conversationId).collect { messageList ->
                _messages.value = messageList
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
        }
    }

    // 更新临时设置并保存到对话
    fun updateTempSettings(settings: TempChatSettings) {
        _tempSettings.value = settings
        _useTempSettings.value = true
        // 保存到对话
        saveConversationSettings(settings)
    }

    // 启用/禁用临时设置
    fun toggleTempSettings(enabled: Boolean) {
        _useTempSettings.value = enabled
        if (!enabled) {
            // 清除对话级别配置
            saveConversationSettings(null)
        }
    }

    // 保存对话级别配置
    private fun saveConversationSettings(settings: TempChatSettings?) {
        viewModelScope.launch {
            conversation?.let { conv ->
                val updated = conv.copy(
                    temperature = settings?.temperature,
                    maxTokens = settings?.maxTokens,
                    topP = settings?.topP,
                    enableThinking = settings?.enableThinking,
                    thinkingEffort = settings?.thinkingEffort,
                    enableWebSearch = settings?.enableWebSearch
                )
                AiAssistantApp.instance.database.conversationDao().updateConversation(updated)
                conversation = updated
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

        generationJob = AiAssistantApp.instance.applicationScope.launch {
            _isGenerating.value = true
            _currentResponse.value = ""
            _currentThinking.value = ""
            _error.value = null

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
                val requestOptions = settings?.let {
                    ChatRequestOptions(
                        temperature = it.temperature,
                        maxTokens = it.maxTokens,
                        topP = it.topP,
                        enableThinking = it.enableThinking,
                        thinkingEffort = it.thinkingEffort,
                        enableWebSearch = it.enableWebSearch
                    )
                }

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
                        onComplete = { fullContent, thinkingContent, usage ->
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
                val errorMsg = e.message ?: "未知错误"
                _error.value = errorMsg
                saveErrorReply(errorMsg)
                _currentResponse.value = ""
                _currentThinking.value = ""
            }
        }
    }

    private fun saveErrorReply(errorMsg: String) {
        AiAssistantApp.instance.applicationScope.launch {
            val message = Message(
                conversationId = conversationId,
                role = "assistant",
                content = buildString {
                    append("请求失败\n\n")
                    append(errorMsg.trim().ifBlank { "未知错误" })
                    append("\n\n可以检查 API 地址、密钥、模型名称或网络状态后重试。")
                }
            )
            repository.saveMessage(message)
        }
    }

    fun stopGeneration() {
        repository.cancelActiveRequest(conversationId)
        generationJob?.cancel(CancellationException("用户暂停生成"))
        _isGenerating.value = false

        if (!isMessageSaved && _currentResponse.value.isNotBlank()) {
            isMessageSaved = true
            viewModelScope.launch {
                val message = Message(
                    conversationId = conversationId,
                    role = "assistant",
                    content = _currentResponse.value,
                    thinkingContent = _currentThinking.value.ifEmpty { null }
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
        conversation?.let { conv ->
            val updated = conv.copy(systemPrompt = prompt)
            conversation = updated
            _uiState.update { it.copy(systemPrompt = prompt) }
            systemPromptSaveJob = viewModelScope.launch {
                AiAssistantApp.instance.database.conversationDao().updateConversation(updated)
            }
        }
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

    companion object {
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

data class ChatUiState(
    val conversationTitle: String = "新对话",
    val modelName: String = "",
    val systemPrompt: String? = null,
    val enableThinking: Boolean = false,
    val isLoading: Boolean = false
)

// 临时聊天设置（仅当前对话有效）
data class TempChatSettings(
    val temperature: Float = 0.95f,
    val maxTokens: Int = 4096,
    val topP: Float = 1.0f,
    val enableThinking: Boolean = false,
    val thinkingEffort: String = "medium",
    val enableWebSearch: Boolean = false
)
