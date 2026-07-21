package com.aiassistant.ui.screens.roleplay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.data.local.AppDatabase
import com.aiassistant.data.repository.RoleplayRepository
import com.aiassistant.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RoleplayViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = RoleplayRepository(
        characterProfileDao = database.characterProfileDao(),
        roleplayScenarioDao = database.roleplayScenarioDao(),
        roleplaySessionDao = database.roleplaySessionDao(),
        roleplayMemoryDao = database.roleplayMemoryDao(),
        characterTagDao = database.characterTagDao()
    )

    // ============ 角色卡相关状态 ============
    private val _characters = MutableStateFlow<List<CharacterProfile>>(emptyList())
    val characters: StateFlow<List<CharacterProfile>> = _characters.asStateFlow()

    private val _selectedCharacter = MutableStateFlow<CharacterProfile?>(null)
    val selectedCharacter: StateFlow<CharacterProfile?> = _selectedCharacter.asStateFlow()

    private val _characterSearchQuery = MutableStateFlow("")
    val characterSearchQuery: StateFlow<String> = _characterSearchQuery.asStateFlow()

    private val _characterTags = MutableStateFlow<List<CharacterTag>>(emptyList())
    val characterTags: StateFlow<List<CharacterTag>> = _characterTags.asStateFlow()

    // ============ 场景卡相关状态 ============
    private val _scenarios = MutableStateFlow<List<RoleplayScenario>>(emptyList())
    val scenarios: StateFlow<List<RoleplayScenario>> = _scenarios.asStateFlow()

    private val _selectedScenario = MutableStateFlow<RoleplayScenario?>(null)
    val selectedScenario: StateFlow<RoleplayScenario?> = _selectedScenario.asStateFlow()

    private val _scenarioSearchQuery = MutableStateFlow("")
    val scenarioSearchQuery: StateFlow<String> = _scenarioSearchQuery.asStateFlow()

    // ============ 会话相关状态 ============
    private val _sessions = MutableStateFlow<List<RoleplaySession>>(emptyList())
    val sessions: StateFlow<List<RoleplaySession>> = _sessions.asStateFlow()

    private val _currentSession = MutableStateFlow<RoleplaySession?>(null)
    val currentSession: StateFlow<RoleplaySession?> = _currentSession.asStateFlow()

    // ============ 记忆相关状态 ============
    private val _memories = MutableStateFlow<List<RoleplayMemory>>(emptyList())
    val memories: StateFlow<List<RoleplayMemory>> = _memories.asStateFlow()

    private val _pinnedFacts = MutableStateFlow<List<RoleplayMemory>>(emptyList())
    val pinnedFacts: StateFlow<List<RoleplayMemory>> = _pinnedFacts.asStateFlow()

    private val _plotSummary = MutableStateFlow("")
    val plotSummary: StateFlow<String> = _plotSummary.asStateFlow()

    // ============ UI状态 ============
    private val _uiState = MutableStateFlow<RoleplayUiState>(RoleplayUiState.Idle)
    val uiState: StateFlow<RoleplayUiState> = _uiState.asStateFlow()

    private val _narrativeMode = MutableStateFlow(NarrativeMode.CHARACTER)
    val narrativeMode: StateFlow<NarrativeMode> = _narrativeMode.asStateFlow()

    init {
        loadCharacters()
        loadScenarios()
        loadSessions()
    }

    // ============ 角色卡操作 ============

    private fun loadCharacters() {
        viewModelScope.launch {
            repository.getAllCharacters().collect { list ->
                _characters.value = list
            }
        }
    }

    fun searchCharacters(query: String) {
        _characterSearchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                repository.getAllCharacters().collect { list ->
                    _characters.value = list
                }
            } else {
                repository.searchCharacters(query).collect { list ->
                    _characters.value = list
                }
            }
        }
    }

    fun selectCharacter(character: CharacterProfile) {
        _selectedCharacter.value = character
        loadCharacterTags(character.id)
    }

    fun clearSelectedCharacter() {
        _selectedCharacter.value = null
        _characterTags.value = emptyList()
    }

    fun saveCharacter(character: CharacterProfile) {
        viewModelScope.launch {
            try {
                _uiState.value = RoleplayUiState.Saving
                if (character.id == 0L) {
                    repository.insertCharacter(character)
                } else {
                    repository.updateCharacter(character)
                }
                _uiState.value = RoleplayUiState.SaveSuccess("角色保存成功")
            } catch (e: Exception) {
                _uiState.value = RoleplayUiState.Error("保存角色失败: ${e.message}")
            }
        }
    }

    fun deleteCharacter(character: CharacterProfile) {
        viewModelScope.launch {
            try {
                repository.deleteCharacter(character)
                if (_selectedCharacter.value?.id == character.id) {
                    _selectedCharacter.value = null
                }
                _uiState.value = RoleplayUiState.SaveSuccess("角色已删除")
            } catch (e: Exception) {
                _uiState.value = RoleplayUiState.Error("删除角色失败: ${e.message}")
            }
        }
    }

    fun setCharacterFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.setCharacterFavorite(id, isFavorite)
        }
    }

    fun setDefaultCharacter(id: Long) {
        viewModelScope.launch {
            repository.setDefaultCharacter(id)
        }
    }

    // ============ 标签操作 ============

    private fun loadCharacterTags(characterId: Long) {
        viewModelScope.launch {
            repository.getTagsForCharacter(characterId).collect { tags ->
                _characterTags.value = tags
            }
        }
    }

    fun addTagToCharacter(characterId: Long, tagName: String) {
        viewModelScope.launch {
            repository.addTagToCharacter(characterId, tagName)
            loadCharacterTags(characterId)
        }
    }

    fun removeTagFromCharacter(characterId: Long, tagId: Long) {
        viewModelScope.launch {
            repository.removeTagFromCharacter(characterId, tagId)
            loadCharacterTags(characterId)
        }
    }

    fun setCharacterTags(characterId: Long, tagNames: List<String>) {
        viewModelScope.launch {
            repository.setCharacterTags(characterId, tagNames)
            loadCharacterTags(characterId)
        }
    }

    // ============ 场景卡操作 ============

    private fun loadScenarios() {
        viewModelScope.launch {
            repository.getAllScenarios().collect { list ->
                _scenarios.value = list
            }
        }
    }

    fun searchScenarios(query: String) {
        _scenarioSearchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                repository.getAllScenarios().collect { list ->
                    _scenarios.value = list
                }
            } else {
                repository.searchScenarios(query).collect { list ->
                    _scenarios.value = list
                }
            }
        }
    }

    fun selectScenario(scenario: RoleplayScenario) {
        _selectedScenario.value = scenario
    }

    fun clearSelectedScenario() {
        _selectedScenario.value = null
    }

    fun saveScenario(scenario: RoleplayScenario) {
        viewModelScope.launch {
            try {
                _uiState.value = RoleplayUiState.Saving
                if (scenario.id == 0L) {
                    repository.insertScenario(scenario)
                } else {
                    repository.updateScenario(scenario)
                }
                _uiState.value = RoleplayUiState.SaveSuccess("场景保存成功")
            } catch (e: Exception) {
                _uiState.value = RoleplayUiState.Error("保存场景失败: ${e.message}")
            }
        }
    }

    fun deleteScenario(scenario: RoleplayScenario) {
        viewModelScope.launch {
            try {
                repository.deleteScenario(scenario)
                if (_selectedScenario.value?.id == scenario.id) {
                    _selectedScenario.value = null
                }
                _uiState.value = RoleplayUiState.SaveSuccess("场景已删除")
            } catch (e: Exception) {
                _uiState.value = RoleplayUiState.Error("删除场景失败: ${e.message}")
            }
        }
    }

    fun setScenarioFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.setScenarioFavorite(id, isFavorite)
        }
    }

    // ============ 会话操作 ============

    private fun loadSessions() {
        viewModelScope.launch {
            repository.getAllSessions().collect { list ->
                _sessions.value = list
            }
        }
    }

    fun createSession(
        characterId: Long?,
        scenarioId: Long?,
        conversationId: Long,
        narrativeMode: NarrativeMode = NarrativeMode.CHARACTER
    ) {
        viewModelScope.launch {
            try {
                val session = RoleplaySession(
                    characterId = characterId,
                    scenarioId = scenarioId,
                    conversationId = conversationId,
                    narrativeMode = narrativeMode.value
                )
                val sessionId = repository.insertSession(session)
                _currentSession.value = repository.getSessionById(sessionId)
                _narrativeMode.value = narrativeMode
                loadMemories(sessionId)
            } catch (e: Exception) {
                _uiState.value = RoleplayUiState.Error("创建会话失败: ${e.message}")
            }
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            _currentSession.value = session
            if (session != null) {
                _narrativeMode.value = NarrativeMode.fromValue(session.narrativeMode)
                _plotSummary.value = session.currentPlotSummary
                loadMemories(sessionId)
                loadPinnedFacts(sessionId)
            }
        }
    }

    fun loadSessionByConversationId(conversationId: Long) {
        viewModelScope.launch {
            val session = repository.getSessionByConversationId(conversationId)
            _currentSession.value = session
            if (session != null) {
                _narrativeMode.value = NarrativeMode.fromValue(session.narrativeMode)
                _plotSummary.value = session.currentPlotSummary
                loadMemories(session.id)
                loadPinnedFacts(session.id)
                // 加载关联的角色和场景
                session.characterId?.let { characterId ->
                    repository.getCharacterById(characterId)?.let { _selectedCharacter.value = it }
                }
                session.scenarioId?.let { scenarioId ->
                    repository.getScenarioById(scenarioId)?.let { _selectedScenario.value = it }
                }
            }
        }
    }

    fun deleteSession(session: RoleplaySession) {
        viewModelScope.launch {
            try {
                repository.deleteSession(session)
                if (_currentSession.value?.id == session.id) {
                    _currentSession.value = null
                }
                _uiState.value = RoleplayUiState.SaveSuccess("会话已删除")
            } catch (e: Exception) {
                _uiState.value = RoleplayUiState.Error("删除会话失败: ${e.message}")
            }
        }
    }

    fun setNarrativeMode(mode: NarrativeMode) {
        _narrativeMode.value = mode
        _currentSession.value?.let { session ->
            viewModelScope.launch {
                repository.updateSession(session.copy(narrativeMode = mode.value))
            }
        }
    }

    // ============ 记忆操作 ============

    private fun loadMemories(sessionId: Long) {
        viewModelScope.launch {
            repository.getMemoriesBySession(sessionId).collect { list ->
                _memories.value = list
            }
        }
    }

    private fun loadPinnedFacts(sessionId: Long) {
        viewModelScope.launch {
            _pinnedFacts.value = repository.getPinnedFacts(sessionId)
        }
    }

    fun addMemory(sessionId: Long, type: String, content: String, sourceMessageId: Long? = null) {
        viewModelScope.launch {
            repository.insertMemory(
                RoleplayMemory(
                    sessionId = sessionId,
                    memoryType = type,
                    content = content,
                    sourceMessageId = sourceMessageId
                )
            )
            loadMemories(sessionId)
        }
    }

    fun updateMemory(memory: RoleplayMemory) {
        viewModelScope.launch {
            repository.updateMemory(memory)
            loadMemories(memory.sessionId)
        }
    }

    fun deleteMemory(memory: RoleplayMemory) {
        viewModelScope.launch {
            repository.deleteMemory(memory)
            loadMemories(memory.sessionId)
        }
    }

    fun setMemoryPinned(id: Long, isPinned: Boolean) {
        viewModelScope.launch {
            repository.setMemoryPinned(id, isPinned)
            _currentSession.value?.let { session ->
                loadMemories(session.id)
                loadPinnedFacts(session.id)
            }
        }
    }

    fun addPinnedFact(sessionId: Long, fact: String) {
        viewModelScope.launch {
            repository.addPinnedFact(sessionId, fact)
            loadPinnedFacts(sessionId)
        }
    }

    fun clearSessionMemories(sessionId: Long) {
        viewModelScope.launch {
            repository.clearSessionMemories(sessionId)
            _memories.value = emptyList()
            _pinnedFacts.value = emptyList()
            _plotSummary.value = ""
        }
    }

    // ============ 剧情操作 ============

    fun processPlotAction(action: PlotAction, customInstruction: String? = null) {
        val sessionId = _currentSession.value?.id ?: return
        viewModelScope.launch {
            val instruction = repository.processPlotAction(sessionId, action, customInstruction)
            _uiState.value = RoleplayUiState.PlotInstruction(instruction)
        }
    }

    fun savePlotSummary(summary: String) {
        val sessionId = _currentSession.value?.id ?: return
        viewModelScope.launch {
            repository.savePlotSummary(sessionId, summary)
            _plotSummary.value = summary
        }
    }

    // ============ 上下文组装 ============

    suspend fun assembleContext(
        userMessage: String?,
        globalSystemPrompt: String?,
        includeHistory: Boolean = true,
        maxHistoryMessages: Int = 50
    ): String {
        val sessionId = _currentSession.value?.id ?: return userMessage ?: ""
        return repository.assembleRoleplayContext(
            sessionId = sessionId,
            globalSystemPrompt = globalSystemPrompt,
            userMessage = userMessage,
            includeHistory = includeHistory,
            maxHistoryMessages = maxHistoryMessages
        )
    }

    fun clearUiState() {
        _uiState.value = RoleplayUiState.Idle
    }
}

sealed class RoleplayUiState {
    object Idle : RoleplayUiState()
    object Saving : RoleplayUiState()
    data class SaveSuccess(val message: String) : RoleplayUiState()
    data class Error(val message: String) : RoleplayUiState()
    data class PlotInstruction(val instruction: String) : RoleplayUiState()
}
