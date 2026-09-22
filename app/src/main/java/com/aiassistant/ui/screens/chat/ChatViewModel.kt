package com.aiassistant.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aiassistant.AiAssistantApp
import com.aiassistant.domain.model.*
import com.aiassistant.utils.TimelineMemoryHelper
import com.aiassistant.utils.TimelineReconcileResult
import com.aiassistant.utils.TimelineEventItem
import com.aiassistant.utils.TimelineCategory
import com.aiassistant.utils.AtemporalSettingItem
import com.aiassistant.utils.TimelineDraftManager
import com.aiassistant.utils.TimelineReconcileDraft
import com.aiassistant.utils.TimelineReconcileCheckpoint
import com.aiassistant.data.repository.AutoTimelineUpdateResult
import com.google.gson.Gson
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(private val conversationId: Long) : ViewModel() {
    private val repository = AiAssistantApp.instance.repository
    private val personalizationManager = AiAssistantApp.instance.personalizationManager
    private val gson = Gson()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // 正在回复时的输入排队系统（需求 6）
    private val _messageQueue = MutableStateFlow<List<QueuedMessage>>(emptyList())
    val messageQueue: StateFlow<List<QueuedMessage>> = _messageQueue.asStateFlow()

    private val _isQueuePaused = MutableStateFlow(false)
    val isQueuePaused: StateFlow<Boolean> = _isQueuePaused.asStateFlow()

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

    // 智能记忆提取待确认候选
    private val _pendingMemoryCandidate = MutableStateFlow<com.aiassistant.domain.model.PendingMemoryCandidate?>(null)
    val pendingMemoryCandidate: StateFlow<com.aiassistant.domain.model.PendingMemoryCandidate?> = _pendingMemoryCandidate.asStateFlow()

    // 思考链翻译状态
    private val _translatingMessageIds = MutableStateFlow<Set<Long>>(emptySet())
    val translatingMessageIds: StateFlow<Set<Long>> = _translatingMessageIds.asStateFlow()

    // 本会话专属记忆列表（仅存放纯净设定与规则）
    val sessionMemories: StateFlow<List<MemoryItem>> = repository.getConversationMemories(conversationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 本会话独立时间线节点流（单一独立存放时间与关键事件）
    val timelineNodes: StateFlow<List<TimelineNode>> = repository.getTimelineNodesFlow(conversationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 全量历史时间轴梳理与校对状态
    private val _isReconcilingTimeline = MutableStateFlow(false)
    val isReconcilingTimeline: StateFlow<Boolean> = _isReconcilingTimeline.asStateFlow()

    private val _timelineReconcileResult = MutableStateFlow<TimelineReconcileResult?>(null)
    val timelineReconcileResult: StateFlow<TimelineReconcileResult?> = _timelineReconcileResult.asStateFlow()

    private val _showTimelineReconcileDialog = MutableStateFlow(false)
    val showTimelineReconcileDialog: StateFlow<Boolean> = _showTimelineReconcileDialog.asStateFlow()

    private var timelineReconcileJob: Job? = null
    private val _timelineReconcileProgress = MutableStateFlow<String?>(null)
    val timelineReconcileProgress: StateFlow<String?> = _timelineReconcileProgress.asStateFlow()

    private val _timelineUpdateNotice = MutableStateFlow<String?>(null)
    val timelineUpdateNotice: StateFlow<String?> = _timelineUpdateNotice.asStateFlow()

    // 时间线变动待确认提案（需求 1：自动识别需用户确认）
    private val _pendingTimelineProposal = MutableStateFlow<AutoTimelineUpdateResult?>(null)
    val pendingTimelineProposal: StateFlow<AutoTimelineUpdateResult?> = _pendingTimelineProposal.asStateFlow()

    // 实时梳理草稿（需求 4：实时可见与断点续梳）
    private val _liveReconcileDraft = MutableStateFlow<TimelineReconcileDraft?>(
        TimelineDraftManager.getDraft(AiAssistantApp.instance, conversationId)
    )
    val liveReconcileDraft: StateFlow<TimelineReconcileDraft?> = _liveReconcileDraft.asStateFlow()

    // 时间线梳理断点检查点（需求 1：记录上次梳理到的对话节点）
    private val _timelineCheckpoint = MutableStateFlow<TimelineReconcileCheckpoint?>(
        TimelineDraftManager.getCheckpoint(AiAssistantApp.instance, conversationId)
    )
    val timelineCheckpoint: StateFlow<TimelineReconcileCheckpoint?> = _timelineCheckpoint.asStateFlow()

    private val _showDraftDialog = MutableStateFlow(false)
    val showDraftDialog: StateFlow<Boolean> = _showDraftDialog.asStateFlow()

    fun dismissTimelineUpdateNotice() {
        _timelineUpdateNotice.value = null
    }

    fun dismissTimelineProposal() {
        _pendingTimelineProposal.value = null
    }

    fun openDraftDialog() {
        _showDraftDialog.value = true
    }

    fun closeDraftDialog() {
        _showDraftDialog.value = false
    }

    fun cancelTimelineReconciliation() {
        timelineReconcileJob?.cancel()
        timelineReconcileJob = null
        _isReconcilingTimeline.value = false
        _timelineReconcileProgress.value = null
    }

    private var activeAssistantVariantGroupId: String? = null
    private var activeAssistantVariantIndex: Int = 1

    private var conversation: Conversation? = null
    private var apiConfig: ApiConfig? = null
    private var generationJob: Job? = null
    private var systemPromptSaveJob: Job? = null
    private var isMessageSaved = false
    @Volatile private var isUserStopping = false
    private val currentKeyAttemptErrors = mutableListOf<String>()
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

    // 当前模型是否所属配置失效
    private val _isModelConfigInvalid = MutableStateFlow(false)
    val isModelConfigInvalid: StateFlow<Boolean> = _isModelConfigInvalid.asStateFlow()

    // 模型连接与重连状态
    private val _reconnectStatus = MutableStateFlow<String?>(null)
    val reconnectStatus: StateFlow<String?> = _reconnectStatus.asStateFlow()

    private fun loadConversation() {
        viewModelScope.launch {
            conversation = repository.getConversationById(conversationId)
            _timelineCheckpoint.value = TimelineDraftManager.getCheckpoint(AiAssistantApp.instance, conversationId)
            conversation?.let { conv ->
                isPrivateConversation = repository.hasConversationTag(conv, "private")
                apiConfig = repository.getApiConfigById(conv.apiConfigId)
                if (apiConfig == null) {
                    _isModelConfigInvalid.value = true
                    val fallback = repository.getDefaultApiConfig()
                        ?: repository.getAllApiConfigs().first().firstOrNull()
                    if (fallback != null) {
                        apiConfig = fallback
                    }
                    _error.value = "当前会话绑定的 API 配置已失效或被删除，请重新选择可用模型"
                } else {
                    _isModelConfigInvalid.value = false
                }

                // 无论原配置是否存在，均加载可用模型列表，保证用户能正常切换模型
                loadAvailableModels()
                // 设置当前模型
                _currentModel.value = conv.modelName
                // 使用对话级别配置，如果没有则使用API配置默认值
                val rpRepoForInit = AiAssistantApp.instance.roleplayRepository
                val rpSessionForInit = rpRepoForInit.getSessionByConversationId(conversationId)
                _tempSettings.value = TempChatSettings(
                    temperature = conv.temperature ?: apiConfig?.temperature ?: 0.95f,
                    maxTokens = conv.maxTokens ?: apiConfig?.maxTokens ?: 8192,
                    topP = conv.topP ?: apiConfig?.topP ?: 1.0f,
                    enableThinking = conv.enableThinking ?: true,
                    thinkingEffort = conv.thinkingEffort ?: apiConfig?.thinkingEffort ?: "high",
                    enableWebSearch = conv.enableWebSearch ?: false,
                    enableSessionMemory = conv.enableSessionMemory ?: false,
                    enableExternalMemory = conv.enableExternalMemory ?: rpSessionForInit?.enableExternalMemory ?: false,
                    enableWorldBook = conv.enableWorldBook ?: rpSessionForInit?.enableWorldBook ?: false,
                    activeWorldBookIds = conv.activeWorldBookIds ?: rpSessionForInit?.activeWorldBookIds
                )
                // 如果对话有自定义配置，自动启用临时设置
                _useTempSettings.value = true

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
                        enableThinking = conversation?.enableThinking ?: true,
                        modelAvatarUri = conv.modelAvatarUri,
                        isRoleplay = rpSession != null,
                        roleplaySession = rpSession,
                        roleplayCharacter = rpCharacter,
                        roleplayCharacters = rpCharacters,
                        roleplayScenario = rpScenario,
                        narrativeMode = narrativeMode,
                        currentStoryTime = conv.currentStoryTime
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
            var currentConfig = apiConfig
            if (currentConfig == null) {
                currentConfig = repository.getDefaultApiConfig()?.takeIf { it.isEnabled }
                    ?: repository.getAllApiConfigs().first().firstOrNull { it.isEnabled }
                if (currentConfig != null) {
                    apiConfig = currentConfig
                }
            }
            val fallbackOption = currentConfig?.takeIf { it.isEnabled }?.let { cfg ->
                ChatModelOption(
                    apiConfigId = cfg.id,
                    configName = cfg.name,
                    provider = cfg.provider,
                    apiType = cfg.apiType,
                    modelName = conv.modelName.ifBlank { cfg.modelName },
                    capability = "auto"
                )
            }
            val visibleOptions = repository.getAllVisibleChatModelOptions()
            val options = (visibleOptions + listOfNotNull(fallbackOption))
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
                        val current = _tempSettings.value
                        _tempSettings.value = TempChatSettings(
                            temperature = cfg.temperature,
                            maxTokens = cfg.maxTokens,
                            topP = cfg.topP,
                            enableThinking = cfg.enableThinking,
                            thinkingEffort = cfg.thinkingEffort,
                            enableWebSearch = cfg.enableWebSearch,
                            enableSessionMemory = current.enableSessionMemory,
                            enableExternalMemory = current.enableExternalMemory,
                            enableWorldBook = current.enableWorldBook,
                            activeWorldBookIds = current.activeWorldBookIds
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
                _isModelConfigInvalid.value = apiConfig == null
                if (apiConfig != null) {
                    _error.value = null
                }
                if (!_useTempSettings.value) {
                    apiConfig?.let { cfg ->
                        val current = _tempSettings.value
                        _tempSettings.value = TempChatSettings(
                            temperature = cfg.temperature,
                            maxTokens = cfg.maxTokens,
                            topP = cfg.topP,
                            enableThinking = cfg.enableThinking,
                            thinkingEffort = cfg.thinkingEffort,
                            enableWebSearch = cfg.enableWebSearch,
                            enableSessionMemory = current.enableSessionMemory,
                            enableExternalMemory = current.enableExternalMemory,
                            enableWorldBook = current.enableWorldBook,
                            activeWorldBookIds = current.activeWorldBookIds
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

    fun compressContextNow(isAuto: Boolean = false) {
        if (_contextUsage.value.isCompressing) return
        viewModelScope.launch {
            val shouldCompress = _contextUsage.value.usage?.canCompress == true
            val initialPercent = (_contextUsage.value.usage?.usagePercent ?: 0f) * 100
            val startMsg = if (isAuto) "🔄 正在自动压缩历史上下文，精简早期对话..." else "🔄 正在压缩上下文，精简历史消息..."
            _contextUsage.update { it.copy(isCompressing = true, statusMessage = startMsg) }
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
                    val newPercent = usage.usagePercent * 100
                    val freed = (initialPercent - newPercent).coerceAtLeast(0f)
                    val finishMsg = if (shouldCompress) {
                        if (freed > 1f) {
                            "✅ 上下文已成功压缩，释放约 ${freed.toInt()}% 空间，当前占用 ${newPercent.toInt()}%"
                        } else {
                            "✅ 上下文已完成压缩，保留核心摘要与最新对话"
                        }
                    } else {
                        "当前上下文已处于最优压缩状态"
                    }
                    _contextUsage.update {
                        it.copy(
                            usage = usage,
                            isCompressing = false,
                            statusMessage = finishMsg
                        )
                    }
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

    fun clearContextStatusMessage() {
        _contextUsage.update { it.copy(statusMessage = null) }
    }

    fun generateRollingSummaryNow() {
        if (_contextUsage.value.isGeneratingSummary) return
        viewModelScope.launch {
            _contextUsage.update { it.copy(isGeneratingSummary = true, statusMessage = "🔄 正在提炼滚动摘要...") }
            val modelName = _currentModel.value ?: conversation?.modelName ?: _uiState.value.modelName
            repository.generateRollingSummaryNow(
                conversationId = conversationId,
                modelNameOverride = modelName
            ).fold(
                onSuccess = { usage ->
                    conversation = repository.getConversationById(conversationId) ?: conversation
                    val summaryTokenCount = usage.summaryTokens
                    val msg = "✅ 滚动摘要提炼成功（当前摘要约 $summaryTokenCount tokens），已自动融入上下文"
                    _contextUsage.update {
                        it.copy(
                            usage = usage,
                            isGeneratingSummary = false,
                            statusMessage = msg
                        )
                    }
                },
                onFailure = { error ->
                    _contextUsage.update {
                        it.copy(
                            isGeneratingSummary = false,
                            statusMessage = error.message ?: "生成滚动摘要失败"
                        )
                    }
                }
            )
        }
    }

    fun updateRollingSummary(newSummary: String) {
        viewModelScope.launch {
            repository.updateRollingSummary(conversationId, newSummary)
            conversation = repository.getConversationById(conversationId) ?: conversation
            refreshContextUsage()
            _contextUsage.update { it.copy(statusMessage = "✅ 滚动摘要已手动更新并保存") }
        }
    }

    fun clearRollingSummary() {
        viewModelScope.launch {
            repository.clearRollingSummary(conversationId)
            conversation = repository.getConversationById(conversationId) ?: conversation
            refreshContextUsage()
            _contextUsage.update { it.copy(statusMessage = "✅ 滚动摘要已清除") }
        }
    }

    fun getCurrentRollingSummary(): String = conversation?.rollingSummary.orEmpty()

    private fun evaluateAutoCompression() {
        viewModelScope.launch {
            try {
                val usage = _contextUsage.value.usage ?: return@launch
                val percent = usage.usagePercent
                if (_contextUsage.value.isCompressing) return@launch

                // 预留用户反应时间缓冲阶段（60% ~ 75%）：给出提前预警提示
                if (usage.canCompress && percent in 0.60f..0.75f) {
                    val warning = "⚠️ 上下文占用已达 ${(percent * 100).toInt()}%，接近自动压缩阈值（75%）。将在达到阈值后自动精简早期对话，您也可手动提前压缩。"
                    if (_contextUsage.value.statusMessage == null) {
                        _contextUsage.update { it.copy(statusMessage = warning) }
                    }
                } else if (usage.canCompress && percent > 0.75f) {
                    // 超过 75% 触发自动压缩，带有开始与结束提示
                    compressContextNow(isAuto = true)
                }
            } catch (_: Exception) {}
        }
    }

    fun updateConversationModelAvatar(avatarUri: String?) {
        viewModelScope.launch {
            conversation?.let { conv ->
                val updated = conv.copy(modelAvatarUri = avatarUri)
                conversation = updated
                repository.updateConversationModelAvatar(conv.id, avatarUri)
                _uiState.update { it.copy(modelAvatarUri = avatarUri) }
            }
        }
    }

    fun editAssistantMessage(messageId: Long, newContent: String) {
        viewModelScope.launch {
            val target = _messages.value.firstOrNull { it.id == messageId } ?: return@launch
            val updated = target.copy(content = newContent)
            repository.updateMessage(updated)
            _messages.update { list ->
                list.map { if (it.id == messageId) updated else it }
            }
            refreshContextUsage()
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
                enableSessionMemory = settings?.enableSessionMemory ?: conv.enableSessionMemory ?: false,
                enableExternalMemory = settings?.enableExternalMemory ?: conv.enableExternalMemory ?: false,
                enableWorldBook = settings?.enableWorldBook ?: conv.enableWorldBook ?: false,
                activeWorldBookIds = settings?.activeWorldBookIds ?: conv.activeWorldBookIds,
                systemPrompt = normalizeSystemPrompt(systemPrompt)
            )
            conversation = updated
            systemPromptSaveJob?.cancel()
            systemPromptSaveJob = viewModelScope.launch {
                AiAssistantApp.instance.database.conversationDao().updateConversation(updated)
                // 若当前会话为角色扮演会话，同步保存至角色扮演会话实体
                val rpRepo = AiAssistantApp.instance.roleplayRepository
                val rpSession = _uiState.value.roleplaySession ?: rpRepo.getSessionByConversationId(conversationId)
                if (rpSession != null && settings != null) {
                    val updatedRp = rpSession.copy(
                        enableExternalMemory = settings.enableExternalMemory,
                        enableWorldBook = settings.enableWorldBook,
                        activeWorldBookIds = settings.activeWorldBookIds,
                        updatedAt = System.currentTimeMillis()
                    )
                    rpRepo.updateSession(updatedRp)
                    _uiState.update { it.copy(roleplaySession = updatedRp) }
                }
            }
        }
    }

    fun sendMessage(content: String, attachments: List<Attachment> = emptyList()) {
        if (_isGenerating.value) {
            enqueueMessage(content, attachments)
            return
        }
        sendMessageInternal(content, attachments, saveUserMessage = true)
    }

    fun enqueueMessage(content: String, attachments: List<Attachment> = emptyList()) {
        val trimmed = content.trim()
        if (trimmed.isBlank() && attachments.isEmpty()) return
        _messageQueue.update { it + QueuedMessage(content = trimmed, attachments = attachments) }
    }

    fun toggleQueuePause() {
        _isQueuePaused.update { !it }
        if (!_isQueuePaused.value && !_isGenerating.value && _messageQueue.value.isNotEmpty()) {
            checkAndDispatchQueue()
        }
    }

    fun removeQueuedMessage(id: String) {
        _messageQueue.update { list -> list.filter { it.id != id } }
    }

    fun recallQueuedMessage(id: String): QueuedMessage? {
        val target = _messageQueue.value.firstOrNull { it.id == id }
        if (target != null) {
            _messageQueue.update { list -> list.filter { it.id != id } }
        }
        return target
    }

    fun editQueuedMessage(id: String, newContent: String) {
        val trimmed = newContent.trim()
        if (trimmed.isBlank()) return
        _messageQueue.update { list ->
            list.map { if (it.id == id) it.copy(content = trimmed) else it }
        }
    }

    fun moveQueuedMessage(fromIndex: Int, toIndex: Int) {
        _messageQueue.update { list ->
            if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return@update list
            val mutable = list.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            mutable
        }
    }

    fun checkAndDispatchQueue() {
        if (_isQueuePaused.value || _isGenerating.value) return
        val next = _messageQueue.value.firstOrNull() ?: return
        _messageQueue.update { it.drop(1) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(200L)
            sendMessage(next.content, next.attachments)
        }
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
        isUserStopping = false
        activeAssistantVariantGroupId = assistantVariantGroupId
        activeAssistantVariantIndex = assistantVariantIndex

        // 获取当前选择的模型和设置
        val settings = if (_useTempSettings.value) _tempSettings.value else null
        val currentSystemPrompt = normalizeSystemPrompt(_uiState.value.systemPrompt)

        generationJob = AiAssistantApp.instance.applicationScope.launch {
            _isGenerating.value = true
            _currentResponse.value = ""
            _currentThinking.value = ""
            _error.value = null
            currentKeyAttemptErrors.clear()
            val currentCallingModel = selectedOption.modelName
            val requestStartTime = System.currentTimeMillis()
            runtimeMessageModelMap[requestStartTime] = currentCallingModel

            val slowTimeoutJob = launch {
                kotlinx.coroutines.delay(120_000L)
                if (_isGenerating.value && _reconnectStatus.value == null) {
                    _reconnectStatus.value = "响应耗时较长（已持续 120s+），若为长篇生成或深度推理请耐心稍候，也可随时点击停止..."
                }
            }

            try {
                var currentUserMsgId: Long? = null
                if (saveUserMessage) {
                    val userMessage = Message(
                        conversationId = conversationId,
                        role = "user",
                        content = content,
                        attachments = attachmentsJson,
                        variantGroupId = userVariantGroupId,
                        variantIndex = userVariantIndex
                    )
                    val savedMsgId = repository.saveMessage(userMessage)
                    currentUserMsgId = savedMsgId
                }

                val selectedConfig = repository.getDecryptedConfig(selectedOption.apiConfigId)
                    ?: throw Exception("API配置不存在，请重新配置")
                systemPromptSaveJob?.join()
                val effectiveConfig = selectedConfig.copy(modelName = selectedOption.modelName)

                val roleplayRepo = AiAssistantApp.instance.roleplayRepository
                val currentRoleplaySession = _uiState.value.roleplaySession
                val effectiveSystemPrompt = if (currentRoleplaySession != null) {
                    val globalRpPrompt = AiAssistantApp.instance.personalizationManager.getSettings().globalRoleplayPrompt
                    roleplayRepo.assembleRoleplayContext(
                        sessionId = currentRoleplaySession.id,
                        globalSystemPrompt = currentSystemPrompt,
                        userMessage = null, // 设定与上下文纯净解耦，用户消息由 user 角色独立发送
                        globalRoleplayPrompt = globalRpPrompt,
                        queryText = content // 用于动态匹配世界书设定与外置记忆库相关条目
                    )
                } else {
                    currentSystemPrompt
                }

                val requestOptions = ChatRequestOptions(
                    temperature = settings?.temperature,
                    maxTokens = settings?.maxTokens,
                    topP = settings?.topP,
                    enableThinking = settings?.enableThinking ?: conversation?.enableThinking ?: effectiveConfig.enableThinking,
                    thinkingEffort = settings?.thinkingEffort ?: conversation?.thinkingEffort ?: effectiveConfig.thinkingEffort,
                    enableWebSearch = settings?.enableWebSearch,
                    enableSessionMemory = settings?.enableSessionMemory ?: conversation?.enableSessionMemory ?: true,
                    enableExternalMemory = settings?.enableExternalMemory ?: conversation?.enableExternalMemory ?: false,
                    enableWorldBook = settings?.enableWorldBook ?: conversation?.enableWorldBook ?: false,
                    activeWorldBookIds = settings?.activeWorldBookIds ?: conversation?.activeWorldBookIds,
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
                        onStatusUpdate = { status ->
                            _reconnectStatus.value = status
                        },
                        onKeyAttemptError = { keyIndex, keyMasked, errorMsg ->
                            currentKeyAttemptErrors.add("Key #$keyIndex ($keyMasked)：$errorMsg")
                        },
                        onResetBuffer = {
                            _currentResponse.value = ""
                            _currentThinking.value = ""
                        },
                        onComplete = { replyContent, _, _ ->
                            slowTimeoutJob.cancel()
                            isMessageSaved = true
                            _isGenerating.value = false
                            activeAssistantVariantGroupId = null
                            activeAssistantVariantIndex = 1
                            val savedReply = replyContent.ifBlank { _currentResponse.value }
                            _currentResponse.value = ""
                            _currentThinking.value = ""
                            _reconnectStatus.value = null
                            autoNameIfNeeded()
                            refreshContextUsage()
                            evaluateAutoCompression()
                            evaluateAutoTimelineUpdate(content, savedReply)
                            evaluateMemoryCandidate(content, currentUserMsgId, selectedOption)
                            checkAndDispatchQueue()
                        },
                        onError = { errorMsg ->
                            slowTimeoutJob.cancel()
                            _reconnectStatus.value = null
                            if (isUserStopping || errorMsg.contains("Socket closed", ignoreCase = true) || errorMsg.contains("Canceled", ignoreCase = true)) {
                                _isGenerating.value = false
                                _currentResponse.value = ""
                                _currentThinking.value = ""
                            } else if (!isMessageSaved) {
                                isMessageSaved = true
                                _isGenerating.value = false
                                _error.value = errorMsg
                                saveErrorReply(errorMsg)
                                _currentResponse.value = ""
                                _currentThinking.value = ""
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                slowTimeoutJob.cancel()
                _reconnectStatus.value = null
                if (isUserStopping || e is CancellationException || e.message?.contains("Socket closed", ignoreCase = true) == true || e.message?.contains("Canceled", ignoreCase = true) == true) {
                    _isGenerating.value = false
                    _currentResponse.value = ""
                    _currentThinking.value = ""
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
        val variantGroupId = activeAssistantVariantGroupId
        val variantIndex = activeAssistantVariantIndex
        activeAssistantVariantGroupId = null
        activeAssistantVariantIndex = 1
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
                thinkingContent = partialThinking,
                variantGroupId = variantGroupId,
                variantIndex = variantIndex
            )
            repository.saveMessage(message)
        }
    }

    fun stopGeneration() {
        isUserStopping = true
        repository.cancelActiveRequest(conversationId)
        generationJob?.cancel(CancellationException("用户暂停生成"))
        _isGenerating.value = false

        val responseToSave = _currentResponse.value.trim()
        val thinkingToSave = _currentThinking.value.trim().ifEmpty { null }
        val variantGroupId = activeAssistantVariantGroupId
        val variantIndex = activeAssistantVariantIndex
        activeAssistantVariantGroupId = null
        activeAssistantVariantIndex = 1

        val connStatus = _reconnectStatus.value?.takeIf { it.isNotBlank() }
        val activeError = _error.value?.takeIf { it.isNotBlank() }
        val hasErrors = currentKeyAttemptErrors.isNotEmpty() || connStatus != null || activeError != null

        if (!isMessageSaved) {
            isMessageSaved = true
            val finalContent = buildString {
                when {
                    responseToSave.isNotBlank() -> {
                        append(responseToSave)
                        append("\n\n*(回复已被暂停)*")
                    }
                    thinkingToSave != null -> {
                        append("*(思考已停止，回复已暂停)*")
                    }
                    else -> {
                        append("回复已停止 (用户已暂停)")
                    }
                }

                if (hasErrors) {
                    append("\n\n【连接异常信息记录】：")
                    if (connStatus != null) {
                        append("\n• 当前状态: $connStatus")
                    }
                    if (activeError != null && activeError != connStatus) {
                        append("\n• 报错详情: $activeError")
                    }
                    if (currentKeyAttemptErrors.isNotEmpty()) {
                        append("\n• 尝试的 Key 报错记录:")
                        currentKeyAttemptErrors.forEach { append("\n  - $it") }
                    }
                }
            }.trim()
            AiAssistantApp.instance.applicationScope.launch {
                val message = Message(
                    conversationId = conversationId,
                    role = "assistant",
                    content = finalContent,
                    thinkingContent = thinkingToSave,
                    variantGroupId = variantGroupId,
                    variantIndex = variantIndex
                )
                repository.saveMessage(message)
            }
        }
        _currentResponse.value = ""
        _currentThinking.value = ""
    }

    fun translateMessageThinking(message: Message) {
        val thinking = message.thinkingContent
        if (thinking.isNullOrBlank()) return
        if (_translatingMessageIds.value.contains(message.id)) return

        viewModelScope.launch {
            _translatingMessageIds.update { it + message.id }
            try {
                val settings = AiAssistantApp.instance.personalizationManager.getSettings()
                val targetConfigId = if (settings.thinkingTranslationApiConfigId > 0L) {
                    settings.thinkingTranslationApiConfigId
                } else {
                    _currentModelOption.value?.apiConfigId ?: conversation?.apiConfigId ?: 0L
                }
                val targetModel = if (settings.thinkingTranslationModel.isNotBlank()) {
                    settings.thinkingTranslationModel
                } else {
                    _currentModel.value ?: conversation?.modelName.orEmpty()
                }

                val result = repository.translateThinkingContent(
                    thinkingText = thinking,
                    targetApiConfigId = targetConfigId,
                    targetModelName = targetModel
                )
                result.onSuccess { translated ->
                    repository.updateTranslatedThinking(message.id, translated)
                }.onFailure { e ->
                    _error.value = "思考链翻译失败: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "思考链翻译异常: ${e.message}"
            } finally {
                _translatingMessageIds.update { it - message.id }
            }
        }
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

    // 自动生成标题（优先使用配置的独立自动命名模型）
    fun generateAutoTitle() {
        viewModelScope.launch {
            val generated = repository.generateConversationTitle(conversationId)
            if (generated != null) {
                renameConversation(generated)
            } else {
                val messages = repository.getMessagesList(conversationId)
                val firstUserMessage = messages.firstOrNull { it.role == "user" }
                if (firstUserMessage != null) {
                    val title = generateTitleFromContent(firstUserMessage.content)
                    renameConversation(title)
                }
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

    // 创建会话分支：基于 Room 事务批量落库、继承活跃模型与全部参数、深度克隆角色扮演与记忆设定
    fun createBranch(
        messageId: Long,
        sourceMessages: List<Message>? = null,
        onComplete: (Long, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val effectiveOption = _currentModelOption.value
                val rawMessages = if (!sourceMessages.isNullOrEmpty()) {
                    sourceMessages
                } else {
                    repository.getMessagesList(conversationId)
                }

                val (newConversationId, branchTitle) = repository.createBranchConversation(
                    parentId = conversationId,
                    branchMessageId = messageId,
                    sourceMessages = rawMessages,
                    activeApiConfigId = effectiveOption?.apiConfigId,
                    activeModelName = effectiveOption?.modelName
                )

                withContext(Dispatchers.Main) {
                    onComplete(newConversationId, branchTitle)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "创建分支对话失败", e)
                withContext(Dispatchers.Main) {
                    _error.value = "创建分支失败: ${e.message}"
                }
            }
        }
    }

    fun createBranch(
        messageId: Long,
        sourceMessages: List<Message>? = null,
        onCompleteLegacy: (Long) -> Unit
    ) {
        createBranch(messageId, sourceMessages) { newId, _ -> onCompleteLegacy(newId) }
    }

    fun acceptPendingMemory(scope: String) {
        val candidate = _pendingMemoryCandidate.value ?: return
        viewModelScope.launch {
            val targetScope = if (scope == "session" || scope == "conversation") "conversation" else "user"
            repository.saveConfirmedMemory(
                content = candidate.distilledContent,
                scope = targetScope,
                conversationId = if (targetScope == "conversation") candidate.conversationId else null,
                sourceMessageId = candidate.sourceMessageId
            )
            _pendingMemoryCandidate.value = null
        }
    }

    fun dismissPendingMemory() {
        _pendingMemoryCandidate.value = null
    }

    fun addSessionMemory(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.addConversationMemory(conversationId, trimmed)
            loadConversation()
        }
    }

    fun updateSessionMemory(memory: MemoryItem) {
        viewModelScope.launch {
            repository.updateMemory(memory.copy(updatedAt = System.currentTimeMillis()))
            loadConversation()
        }
    }

    fun toggleSessionMemory(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.setMemoryEnabled(id, isEnabled)
            loadConversation()
        }
    }

    fun deleteSessionMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
            loadConversation()
        }
    }

    fun clearSessionMemories() {
        viewModelScope.launch {
            repository.clearConversationMemories(conversationId)
            loadConversation()
        }
    }

    fun startTimelineReconciliation(startFromDraft: Boolean = false, fromCheckpoint: Boolean = false) {
        if (_isReconcilingTimeline.value) return
        _isReconcilingTimeline.value = true
        _timelineReconcileProgress.value = when {
            startFromDraft -> "正在读取先前进度继续梳理..."
            fromCheckpoint -> "正在结合原有时间线，梳理后续新对话..."
            else -> "准备分析对话历史..."
        }
        timelineReconcileJob?.cancel()
        timelineReconcileJob = viewModelScope.launch {
            try {
                val activeCfgId = apiConfig?.id ?: _currentModelOption.value?.apiConfigId
                val activeModel = _currentModel.value?.ifBlank { null } ?: _currentModelOption.value?.modelName.orEmpty()
                val draft = if (startFromDraft) {
                    TimelineDraftManager.getDraft(AiAssistantApp.instance, conversationId)
                } else null
                val checkpoint = if (fromCheckpoint) {
                    _timelineCheckpoint.value ?: TimelineDraftManager.getCheckpoint(AiAssistantApp.instance, conversationId)
                } else null
                val existingNodes = if (fromCheckpoint) timelineNodes.value else null
                val result = repository.reconcileConversationTimeline(
                    conversationId = conversationId,
                    activeConfigId = activeCfgId,
                    activeModelName = activeModel,
                    startFromDraft = draft,
                    reconcileCheckpoint = checkpoint,
                    existingTimelineNodes = existingNodes,
                    onProgress = { step, total, detail ->
                        _timelineReconcileProgress.value = if (total > 1) "[$step/$total] $detail" else detail
                    },
                    onIntermediateResult = { intermediateDraft ->
                        TimelineDraftManager.saveDraft(AiAssistantApp.instance, intermediateDraft)
                        _liveReconcileDraft.value = intermediateDraft
                    }
                )
                _timelineReconcileResult.value = result
                _showTimelineReconcileDialog.value = true
            } catch (e: CancellationException) {
                // 用户主动取消，当前进度已实时留存在本地草稿文件
                Log.d("ChatViewModel", "时间线梳理已暂停/取消，中间结果已保存草稿")
            } catch (e: Exception) {
                _timelineReconcileResult.value = TimelineReconcileResult(
                    currentStoryTime = "未确定",
                    events = mutableListOf(),
                    extractionSource = "LOCAL_FALLBACK",
                    extractionErrorMessage = e.message ?: "提炼请求异常"
                )
                _showTimelineReconcileDialog.value = true
            } finally {
                _isReconcilingTimeline.value = false
                _timelineReconcileProgress.value = null
                timelineReconcileJob = null
            }
        }
    }

    private fun evaluateAutoTimelineUpdate(userMsg: String, assistantReply: String) {
        viewModelScope.launch {
            try {
                val activeCfgId = apiConfig?.id ?: _currentModelOption.value?.apiConfigId
                val activeModel = _currentModel.value?.ifBlank { null } ?: _currentModelOption.value?.modelName.orEmpty()
                val result = repository.evaluateAndAutoUpdateTimeline(
                    conversationId = conversationId,
                    userMessage = userMsg,
                    assistantReply = assistantReply,
                    activeConfigId = activeCfgId,
                    activeModelName = activeModel
                )
                if (result != null) {
                    // 需求 1：自动识别到的时间和事件放入待确认提案，由用户在界面交互确认后再应用
                    _pendingTimelineProposal.value = result
                }
            } catch (e: Exception) {
                Log.w("ChatViewModel", "自动更新时间线后台任务异常: ${e.message}")
            }
        }
    }

    private fun evaluateMemoryCandidate(
        content: String,
        messageId: Long?,
        selectedOption: ChatModelOption
    ) {
        val isMemoryEnabled = if (_useTempSettings.value) {
            _tempSettings.value.enableSessionMemory
        } else {
            conversation?.enableSessionMemory ?: true
        }
        if (_uiState.value.roleplaySession == null && content.isNotBlank() && isMemoryEnabled) {
            AiAssistantApp.instance.applicationScope.launch(Dispatchers.IO) {
                try {
                    val candidate = repository.extractMemoryCandidate(
                        content = content,
                        conversationId = conversationId,
                        messageId = messageId,
                        activeConfigId = selectedOption.apiConfigId,
                        activeModelName = selectedOption.modelName
                    )
                    if (candidate != null) {
                        _pendingMemoryCandidate.value = candidate
                    }
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "后台提取记忆候选异常: ${e.message}")
                }
            }
        }
    }

    fun applyTimelineProposal(proposal: AutoTimelineUpdateResult) {
        viewModelScope.launch {
            try {
                // 1. 故事时间推进
                if (!proposal.updatedStoryTime.isNullOrBlank()) {
                    repository.updateStoryTime(conversationId, proposal.updatedStoryTime)
                }

                // 2. 事件操作 (UPDATE 或 APPEND)
                val ev = proposal.newEvent
                if (ev != null && ev.content.isNotBlank()) {
                    val currentNodes = timelineNodes.value
                    if (proposal.action == "UPDATE" && proposal.targetNodeId != null && proposal.targetNodeId > 0L) {
                        val existingNode = currentNodes.firstOrNull { it.id == proposal.targetNodeId }
                        if (existingNode != null) {
                            repository.updateTimelineNode(
                                existingNode.copy(
                                    timeTag = ev.timeTag.ifBlank { existingNode.timeTag },
                                    event = ev.content,
                                    category = ev.category.key,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        } else {
                            val nextOrder = (currentNodes.maxOfOrNull { it.orderIndex } ?: 0) + 1
                            repository.addTimelineNode(
                                TimelineNode(
                                    conversationId = conversationId,
                                    timeTag = ev.timeTag.ifBlank { proposal.updatedStoryTime ?: "未确定" },
                                    event = ev.content,
                                    category = ev.category.key,
                                    orderIndex = nextOrder,
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    } else {
                        val nextOrder = (currentNodes.maxOfOrNull { it.orderIndex } ?: 0) + 1
                        repository.addTimelineNode(
                            TimelineNode(
                                conversationId = conversationId,
                                timeTag = ev.timeTag.ifBlank { proposal.updatedStoryTime ?: "未确定" },
                                event = ev.content,
                                category = ev.category.key,
                                orderIndex = nextOrder,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                _pendingTimelineProposal.value = null
                _timelineUpdateNotice.value = "已将时间线变动应用到记录"
                loadConversation()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "应用时间线提案失败: ${e.message}", e)
            }
        }
    }

    fun dismissTimelineReconcileDialog() {
        _showTimelineReconcileDialog.value = false
        _timelineReconcileResult.value = null
    }

    fun getSavedTimelineDraft(): TimelineReconcileDraft? {
        return TimelineDraftManager.getDraft(AiAssistantApp.instance, conversationId)
    }

    fun hasTimelineDraft(): Boolean {
        return TimelineDraftManager.hasDraft(AiAssistantApp.instance, conversationId)
    }

    fun clearTimelineDraft() {
        TimelineDraftManager.clearDraft(AiAssistantApp.instance, conversationId)
        _liveReconcileDraft.value = null
    }

    fun openSavedDraftForReview() {
        val draft = getSavedTimelineDraft() ?: _liveReconcileDraft.value ?: return
        _timelineReconcileResult.value = TimelineReconcileResult(
            currentStoryTime = draft.currentStoryTime,
            events = draft.events.toMutableList(),
            atemporalSettings = draft.atemporalSettings.toMutableList(),
            extractionSource = "SAVED_DRAFT"
        )
        _showTimelineReconcileDialog.value = true
    }

    fun openLiveDraftForReview() {
        val draft = _liveReconcileDraft.value ?: getSavedTimelineDraft() ?: return
        _timelineReconcileResult.value = TimelineReconcileResult(
            currentStoryTime = draft.currentStoryTime,
            events = draft.events.toMutableList(),
            atemporalSettings = draft.atemporalSettings.toMutableList(),
            extractionSource = "LIVE_PROGRESS"
        )
        _showTimelineReconcileDialog.value = true
    }

    fun applySavedDraft() {
        val draft = getSavedTimelineDraft() ?: return
        applyReconciledTimeline(
            currentStoryTime = draft.currentStoryTime,
            events = draft.events,
            confirmedSettings = draft.atemporalSettings
        )
    }

    fun applyReconciledTimeline(
        currentStoryTime: String,
        events: List<TimelineEventItem>,
        confirmedSettings: List<AtemporalSettingItem> = emptyList()
    ) {
        viewModelScope.launch {
            // 1. 故事时间独立存入 Conversation 表
            val cleanStoryTime = currentStoryTime.trim().takeIf { it.isNotBlank() && it != "未确定" }
            repository.updateStoryTime(conversationId, cleanStoryTime)

            // 2. 时间与事件独立存入 timeline_nodes 表，保留明确时序与分类
            val nodesToSave = events.mapIndexed { idx, ev ->
                TimelineNode(
                    conversationId = conversationId,
                    timeTag = ev.timeTag.trim(),
                    event = ev.content.trim(),
                    category = ev.category.key,
                    orderIndex = idx,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            repository.replaceTimelineNodes(conversationId, nodesToSave)

            // 3. 清理之前误存放在 memory_items 中的时间线碎片，但完整保留用户自定义的纯净设定与规则
            try {
                val oldMemories = repository.getConversationMemoriesList(conversationId)
                for (oldMem in oldMemories) {
                    val trimmed = oldMem.content.trim()
                    if (trimmed.startsWith("【当前故事时间】：") || trimmed.startsWith("当前故事时间：") || TimelineMemoryHelper.isExplicitTimelineEvent(trimmed)) {
                        repository.deleteMemory(oldMem.id)
                    }
                }
            } catch (e: Exception) {
                Log.w("ChatViewModel", "清理旧时间线记忆缓存失败: ${e.message}")
            }

            // 4. 需求 8：展示的世界观与固有设定 100% 真正保存进记忆中（在会话专属记忆与角色扮演记忆中立即可见并生效）
            val roleplayRepo = AiAssistantApp.instance.roleplayRepository
            val currentRpSession = _uiState.value.roleplaySession
            for (setting in confirmedSettings) {
                if (setting.isSelected && setting.content.isNotBlank()) {
                    val formatted = "【${setting.category}】${setting.content.trim()}"
                    // 无论 targetScope 是 session 还是 global，都向当前会话专属记忆存入一条（带 conversationId），确保在当前会话的“会话专属记忆与规则”列表中 100% 立即可见！
                    repository.addConversationMemory(conversationId, formatted)
                    // 若用户明确选择 global，额外存入一条 user 级全局记忆，让跨会话生效
                    if (setting.targetScope == "global") {
                        repository.addUserMemory(formatted)
                    }
                    // 若当前处于角色扮演/剧情创作会话，同时写入角色扮演专属记忆表 (RoleplayMemory) 并标记为已固定，确保在角色扮演上下文组装中立即生效
                    if (currentRpSession != null) {
                        try {
                            roleplayRepo.insertMemory(
                                RoleplayMemory(
                                    sessionId = currentRpSession.id,
                                    memoryType = "fact",
                                    content = formatted,
                                    isPinned = true
                                )
                            )
                        } catch (e: Exception) {
                            Log.w("ChatViewModel", "保存角色扮演专属记忆失败: ${e.message}")
                        }
                    }
                }
            }

            // 确保当前会话的会话记忆功能处于开启状态，使刚存入的设定立即可用
            conversation?.let { conv ->
                if (conv.enableSessionMemory != true) {
                    repository.updateConversation(conv.copy(enableSessionMemory = true))
                }
            }

            // 记录时间线水线检查点 (Checkpoint)
            try {
                val allMsgs = _messages.value
                    .filter { !it.isExcluded && it.role != "system" && it.content.isNotBlank() }
                    .sortedBy { it.createdAt }
                val lastProcessedId = _timelineReconcileResult.value?.lastProcessedMessageId
                    ?: _liveReconcileDraft.value?.lastProcessedMessageId
                    ?: allMsgs.lastOrNull()?.id ?: 0L
                val lastIdx = allMsgs.indexOfFirst { it.id == lastProcessedId }.takeIf { it >= 0 }?.let { it + 1 } ?: allMsgs.size
                val checkpoint = TimelineReconcileCheckpoint(
                    conversationId = conversationId,
                    lastReconciledMessageId = lastProcessedId,
                    lastReconciledMessageIndex = lastIdx,
                    totalMessageCountAtReconciliation = allMsgs.size,
                    storyTimeAtReconciliation = cleanStoryTime,
                    nodeCountAtReconciliation = nodesToSave.size,
                    timestamp = System.currentTimeMillis()
                )
                TimelineDraftManager.saveCheckpoint(AiAssistantApp.instance, checkpoint)
                _timelineCheckpoint.value = checkpoint
            } catch (e: Exception) {
                Log.w("ChatViewModel", "记录时间线水线检查点失败: ${e.message}")
            }

            // 清理已应用的草稿
            TimelineDraftManager.clearDraft(AiAssistantApp.instance, conversationId)
            _liveReconcileDraft.value = null

            dismissTimelineReconcileDialog()
            loadConversation()
        }
    }

    // -------------------------------------------------------------
    // 用户对独立时间线的编辑与管理操作接口（满足需求 1）
    // -------------------------------------------------------------
    fun updateCurrentStoryTime(newTime: String?) {
        viewModelScope.launch {
            repository.updateStoryTime(conversationId, newTime?.trim()?.takeIf { it.isNotBlank() && it != "未确定" })
            loadConversation()
        }
    }

    fun addTimelineNode(timeTag: String, event: String, category: String = TimelineCategory.PLOT_EVENT.key) {
        viewModelScope.launch {
            val currentNodes = timelineNodes.value
            val nextOrder = (currentNodes.maxOfOrNull { it.orderIndex } ?: 0) + 1
            repository.addTimelineNode(
                TimelineNode(
                    conversationId = conversationId,
                    timeTag = timeTag.trim(),
                    event = event.trim(),
                    category = category,
                    orderIndex = nextOrder,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateTimelineNode(node: TimelineNode) {
        viewModelScope.launch {
            repository.updateTimelineNode(node.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTimelineNode(nodeId: Long) {
        viewModelScope.launch {
            repository.deleteTimelineNodeById(nodeId)
        }
    }

    fun clearTimeline() {
        viewModelScope.launch {
            repository.clearTimeline(conversationId)
            repository.updateStoryTime(conversationId, null)
            TimelineDraftManager.clearDraft(AiAssistantApp.instance, conversationId)
            TimelineDraftManager.clearCheckpoint(AiAssistantApp.instance, conversationId)
            _liveReconcileDraft.value = null
            _timelineCheckpoint.value = null
            loadConversation()
        }
    }

    fun clearTimelineCheckpoint() {
        TimelineDraftManager.clearCheckpoint(AiAssistantApp.instance, conversationId)
        _timelineCheckpoint.value = null
    }

    fun getNewMessagesCountSinceCheckpoint(): Int {
        val checkpoint = _timelineCheckpoint.value ?: return 0
        val allMsgs = _messages.value.filter { !it.isExcluded && it.role != "system" && it.content.isNotBlank() }
        return allMsgs.count { it.id > checkpoint.lastReconciledMessageId }
    }

    fun convertToRoleplay(
        charName: String? = null,
        charIdentity: String? = null,
        charPersonality: String? = null,
        scenarioName: String? = null,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val db = AiAssistantApp.instance.database
            val converter = com.aiassistant.utils.ConversationConverter(
                conversationDao = db.conversationDao(),
                messageDao = db.messageDao(),
                roleplaySessionDao = db.roleplaySessionDao(),
                characterProfileDao = db.characterProfileDao(),
                roleplayScenarioDao = db.roleplayScenarioDao()
            )
            val sessionId = converter.convertToRoleplay(
                conversationId = conversationId,
                charName = charName,
                charIdentity = charIdentity,
                charPersonality = charPersonality,
                scenarioName = scenarioName
            )
            loadConversation()
            onSuccess(sessionId)
        }
    }

    fun convertToNormal(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val db = AiAssistantApp.instance.database
            val converter = com.aiassistant.utils.ConversationConverter(
                conversationDao = db.conversationDao(),
                messageDao = db.messageDao(),
                roleplaySessionDao = db.roleplaySessionDao(),
                characterProfileDao = db.characterProfileDao(),
                roleplayScenarioDao = db.roleplayScenarioDao()
            )
            val success = converter.convertToNormal(conversationId)
            if (success) {
                loadConversation()
                onSuccess()
            }
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
        when (action) {
            PlotAction.REGENERATE -> {
                regenerateLastMessage()
            }
            PlotAction.ROLLBACK -> {
                viewModelScope.launch {
                    val messages = repository.getMessagesList(conversationId)
                    val lastAssistant = messages.lastOrNull { it.role == "assistant" }
                    if (lastAssistant != null) {
                        repository.deleteMessage(lastAssistant)
                    }
                }
            }
            else -> {
                viewModelScope.launch {
                    val instruction = AiAssistantApp.instance.roleplayRepository.processPlotAction(session.id, action, customInstruction)
                    sendMessage(instruction)
                }
            }
        }
    }

    fun triggerCharacterOpening() {
        val session = _uiState.value.roleplaySession ?: return
        val character = _uiState.value.roleplayCharacter
        val scenario = _uiState.value.roleplayScenario
        viewModelScope.launch {
            val greeting = character?.greeting?.trim()
            if (!greeting.isNullOrBlank()) {
                val assistantMsg = Message(
                    conversationId = conversationId,
                    role = "assistant",
                    content = greeting,
                    tokenCount = com.aiassistant.data.repository.AiRepository.estimateTokenCount(greeting)
                )
                repository.saveMessage(assistantMsg)
            } else {
                val charName = character?.name ?: "角色"
                val scenarioDesc = scenario?.let { "当前场景为【${it.name}】（${it.location.ifBlank { "" }} ${it.environment.ifBlank { "" }}）。" }.orEmpty()
                val openingPrompt = "【导演开场指令】${scenarioDesc}请以【$charName】的身份，根据角色设定与当前场景世界观，开启故事的第一幕，展现角色当前的动作、神态与首句对话，为故事奠定氛围并留出互动切入点。"
                sendMessage(openingPrompt)
            }
        }
    }

    fun togglePinMessage(message: Message) {
        viewModelScope.launch {
            val updated = message.copy(isPinned = !message.isPinned)
            repository.updateMessage(updated)
        }
    }

    fun toggleExcludeMessage(message: Message) {
        viewModelScope.launch {
            val updated = message.copy(isExcluded = !message.isExcluded)
            repository.updateMessage(updated)
            refreshContextUsage()
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

            // 如果是全新创建的角色 (id <= 0)，先同步持久化进全局 Room 数据库以分配真实主键 ID
            val finalCharacter = if (editedCharacter.id <= 0L) {
                val newId = roleplayRepo.insertCharacter(editedCharacter)
                editedCharacter.copy(id = newId)
            } else {
                editedCharacter
            }

            val baseCharacters = _uiState.value.roleplayCharacters
            val currentCustomized = currentSession.getCustomizedCharacters(baseCharacters).toMutableList()
            val existingIndex = currentCustomized.indexOfFirst { (it.id == finalCharacter.id && it.id > 0) || it.name == finalCharacter.name }
            if (existingIndex >= 0) {
                currentCustomized[existingIndex] = finalCharacter
            } else {
                currentCustomized.add(finalCharacter)
            }

            // 确保角色 ID 加入到本故事会话的登场角色列表中
            val sessionCharIds = currentSession.getEffectiveCharacterIds().toMutableList()
            if (!sessionCharIds.contains(finalCharacter.id)) {
                sessionCharIds.add(finalCharacter.id)
            }

            val jsonStr = com.google.gson.Gson().toJson(currentCustomized)
            val updatedSession = currentSession.copy(
                characterIds = com.google.gson.Gson().toJson(sessionCharIds),
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

            // 如果是全新创建的世界观 (id <= 0)，先同步持久化进全局 Room 数据库以分配真实主键 ID
            val finalScenario = if (editedScenario.id <= 0L) {
                val newId = roleplayRepo.insertScenario(editedScenario)
                editedScenario.copy(id = newId)
            } else {
                editedScenario
            }

            val jsonStr = com.google.gson.Gson().toJson(finalScenario)
            val updatedSession = currentSession.copy(
                scenarioId = finalScenario.id,
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
                val transcript = allMessages.takeLast(30).joinToString("\n") { m ->
                    val role = if (m.role == "user") "【导演/用户】" else "【模型剧情】"
                    "$role: ${m.content}"
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
    val enableThinking: Boolean = true,
    val isLoading: Boolean = false,
    val modelAvatarUri: String? = null,
    val isRoleplay: Boolean = false,
    val roleplaySession: RoleplaySession? = null,
    val roleplayCharacter: CharacterProfile? = null,
    val roleplayCharacters: List<CharacterProfile> = emptyList(),
    val roleplayScenario: RoleplayScenario? = null,
    val narrativeMode: NarrativeMode = NarrativeMode.CHARACTER,
    val suggestedProposal: ProposedSettingBundle? = null,
    val currentStoryTime: String? = null
)

data class ContextUsageUiState(
    val usage: ConversationContextUsage? = null,
    val isCompressing: Boolean = false,
    val isGeneratingSummary: Boolean = false,
    val statusMessage: String? = null
)

// 临时聊天设置（仅当前对话有效）
data class TempChatSettings(
    val temperature: Float = 0.95f,
    val maxTokens: Int = 50000,
    val topP: Float = 1.0f,
    val enableThinking: Boolean = true,
    val thinkingEffort: String = "high",
    val enableWebSearch: Boolean = false,
    val enableSessionMemory: Boolean = false,
    val enableExternalMemory: Boolean = false,
    val enableWorldBook: Boolean = false,
    val activeWorldBookIds: String? = null
)
