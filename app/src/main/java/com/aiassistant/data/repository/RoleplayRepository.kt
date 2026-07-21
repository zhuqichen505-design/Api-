package com.aiassistant.data.repository

import com.aiassistant.data.local.*
import com.aiassistant.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 角色扮演仓库 - 处理角色扮演相关的业务逻辑
 */
class RoleplayRepository(
    private val characterProfileDao: CharacterProfileDao,
    private val roleplayScenarioDao: RoleplayScenarioDao,
    private val roleplaySessionDao: RoleplaySessionDao,
    private val roleplayMemoryDao: RoleplayMemoryDao,
    private val characterTagDao: CharacterTagDao
) {
    // ============ 角色卡操作 ============

    fun getAllCharacters(): Flow<List<CharacterProfile>> = characterProfileDao.getAllCharacters()

    fun getFavoriteCharacters(): Flow<List<CharacterProfile>> = characterProfileDao.getFavoriteCharacters()

    fun searchCharacters(query: String): Flow<List<CharacterProfile>> = characterProfileDao.searchCharacters(query)

    suspend fun getCharacterById(id: Long): CharacterProfile? = characterProfileDao.getCharacterById(id)

    suspend fun getDefaultCharacter(): CharacterProfile? = characterProfileDao.getDefaultCharacter()

    suspend fun insertCharacter(character: CharacterProfile): Long = characterProfileDao.insertCharacter(character)

    suspend fun updateCharacter(character: CharacterProfile) = characterProfileDao.updateCharacter(character)

    suspend fun deleteCharacter(character: CharacterProfile) = characterProfileDao.deleteCharacter(character)

    suspend fun deleteCharacterById(id: Long) = characterProfileDao.deleteCharacterById(id)

    suspend fun setCharacterFavorite(id: Long, isFavorite: Boolean) = characterProfileDao.setFavorite(id, isFavorite)

    suspend fun setDefaultCharacter(id: Long) {
        characterProfileDao.clearDefaultCharacters()
        characterProfileDao.setDefaultCharacter(id)
    }

    // ============ 场景卡操作 ============

    fun getAllScenarios(): Flow<List<RoleplayScenario>> = roleplayScenarioDao.getAllScenarios()

    fun getFavoriteScenarios(): Flow<List<RoleplayScenario>> = roleplayScenarioDao.getFavoriteScenarios()

    fun searchScenarios(query: String): Flow<List<RoleplayScenario>> = roleplayScenarioDao.searchScenarios(query)

    suspend fun getScenarioById(id: Long): RoleplayScenario? = roleplayScenarioDao.getScenarioById(id)

    suspend fun insertScenario(scenario: RoleplayScenario): Long = roleplayScenarioDao.insertScenario(scenario)

    suspend fun updateScenario(scenario: RoleplayScenario) = roleplayScenarioDao.updateScenario(scenario)

    suspend fun deleteScenario(scenario: RoleplayScenario) = roleplayScenarioDao.deleteScenario(scenario)

    suspend fun deleteScenarioById(id: Long) = roleplayScenarioDao.deleteScenarioById(id)

    suspend fun setScenarioFavorite(id: Long, isFavorite: Boolean) = roleplayScenarioDao.setFavorite(id, isFavorite)

    // ============ 角色扮演会话操作 ============

    fun getAllSessions(): Flow<List<RoleplaySession>> = roleplaySessionDao.getAllSessions()

    fun getSessionsByCharacter(characterId: Long): Flow<List<RoleplaySession>> =
        roleplaySessionDao.getSessionsByCharacter(characterId)

    fun getSessionsByScenario(scenarioId: Long): Flow<List<RoleplaySession>> =
        roleplaySessionDao.getSessionsByScenario(scenarioId)

    suspend fun getSessionById(id: Long): RoleplaySession? = roleplaySessionDao.getSessionById(id)

    suspend fun getSessionByConversationId(conversationId: Long): RoleplaySession? =
        roleplaySessionDao.getSessionByConversationId(conversationId)

    suspend fun insertSession(session: RoleplaySession): Long = roleplaySessionDao.insertSession(session)

    suspend fun updateSession(session: RoleplaySession) = roleplaySessionDao.updateSession(session)

    suspend fun deleteSession(session: RoleplaySession) = roleplaySessionDao.deleteSession(session)

    suspend fun deleteSessionById(id: Long) = roleplaySessionDao.deleteSessionById(id)

    suspend fun updateSessionMemoryState(id: Long, summary: String, pinnedFacts: String?) =
        roleplaySessionDao.updateMemoryState(id, summary, pinnedFacts)

    // ============ 角色扮演记忆操作 ============

    fun getMemoriesBySession(sessionId: Long): Flow<List<RoleplayMemory>> =
        roleplayMemoryDao.getMemoriesBySession(sessionId)

    fun getMemoriesByType(sessionId: Long, type: String): Flow<List<RoleplayMemory>> =
        roleplayMemoryDao.getMemoriesByType(sessionId, type)

    fun getPinnedMemories(sessionId: Long): Flow<List<RoleplayMemory>> =
        roleplayMemoryDao.getPinnedMemories(sessionId)

    suspend fun getMemoryById(id: Long): RoleplayMemory? = roleplayMemoryDao.getMemoryById(id)

    suspend fun insertMemory(memory: RoleplayMemory): Long = roleplayMemoryDao.insertMemory(memory)

    suspend fun updateMemory(memory: RoleplayMemory) = roleplayMemoryDao.updateMemory(memory)

    suspend fun deleteMemory(memory: RoleplayMemory) = roleplayMemoryDao.deleteMemory(memory)

    suspend fun deleteMemoryById(id: Long) = roleplayMemoryDao.deleteMemoryById(id)

    suspend fun deleteAllMemories(sessionId: Long) = roleplayMemoryDao.deleteAllMemories(sessionId)

    suspend fun setMemoryPinned(id: Long, isPinned: Boolean) = roleplayMemoryDao.setPinned(id, isPinned)

    suspend fun getPinnedFacts(sessionId: Long, limit: Int = 20): List<RoleplayMemory> =
        roleplayMemoryDao.getPinnedFacts(sessionId, limit)

    suspend fun getLatestSummary(sessionId: Long): RoleplayMemory? =
        roleplayMemoryDao.getLatestSummary(sessionId)

    // ============ 标签操作 ============

    fun getAllTags(): Flow<List<CharacterTag>> = characterTagDao.getAllTags()

    fun getTagsForCharacter(characterId: Long): Flow<List<CharacterTag>> =
        characterTagDao.getTagsForCharacter(characterId)

    fun getCharactersForTag(tagId: Long): Flow<List<CharacterProfile>> =
        characterTagDao.getCharactersForTag(tagId)

    suspend fun addTagToCharacter(characterId: Long, tagName: String) {
        val tag = characterTagDao.getTagByName(tagName) ?: run {
            val tagId = characterTagDao.insertTag(CharacterTag(name = tagName))
            characterTagDao.getTagById(tagId)!!
        }
        characterTagDao.insertCrossRef(CharacterTagCrossRef(characterId, tag.id))
    }

    suspend fun removeTagFromCharacter(characterId: Long, tagId: Long) {
        characterTagDao.deleteCrossRef(CharacterTagCrossRef(characterId, tagId))
    }

    suspend fun setCharacterTags(characterId: Long, tagNames: List<String>) {
        characterTagDao.deleteAllCrossRefsForCharacter(characterId)
        tagNames.forEach { tagName ->
            addTagToCharacter(characterId, tagName)
        }
    }

    // ============ 上下文组装 ============

    /**
     * 组装角色扮演上下文
     * 按照指定顺序组装：
     * 1. 全局系统约束
     * 2. 角色卡
     * 3. 场景卡
     * 4. 长期记忆
     * 5. 当前剧情摘要
     * 6. 已固定的重要事实
     * 7. 会话历史
     * 8. 用户最新消息或剧情提示
     */
    suspend fun assembleRoleplayContext(
        sessionId: Long,
        globalSystemPrompt: String?,
        userMessage: String?,
        includeHistory: Boolean = true,
        maxHistoryMessages: Int = 50
    ): String {
        val session = roleplaySessionDao.getSessionById(sessionId)
            ?: return userMessage ?: ""

        val parts = mutableListOf<String>()

        // 1. 全局系统约束
        if (!globalSystemPrompt.isNullOrBlank()) {
            parts.add("【全局系统约束】\n$globalSystemPrompt")
        }

        // 2. 角色卡
        session.characterId?.let { characterId ->
            val character = characterProfileDao.getCharacterById(characterId)
            if (character != null) {
                parts.add(buildCharacterCardPrompt(character))
            }
        }

        // 3. 场景卡
        session.scenarioId?.let { scenarioId ->
            val scenario = roleplayScenarioDao.getScenarioById(scenarioId)
            if (scenario != null) {
                parts.add(buildScenarioCardPrompt(scenario))
            }
        }

        // 4. 长期记忆
        val pinnedFacts = roleplayMemoryDao.getPinnedFacts(sessionId)
        if (pinnedFacts.isNotEmpty()) {
            val factsText = pinnedFacts.joinToString("\n") { "- ${it.content}" }
            parts.add("【重要事实】\n$factsText")
        }

        // 5. 当前剧情摘要
        if (session.currentPlotSummary.isNotBlank()) {
            parts.add("【当前剧情摘要】\n${session.currentPlotSummary}")
        }

        // 6. 叙事模式说明
        parts.add(buildNarrativeModePrompt(session.narrativeMode))

        // 7. 用户最新消息或剧情提示
        if (!userMessage.isNullOrBlank()) {
            parts.add("【用户输入】\n$userMessage")
        }

        return parts.joinToString("\n\n")
    }

    /**
     * 构建角色卡提示词
     */
    private fun buildCharacterCardPrompt(character: CharacterProfile): String {
        val parts = mutableListOf<String>()
        parts.add("【角色设定】")
        parts.add("角色名称：${character.name}")

        if (character.identity.isNotBlank()) parts.add("身份/职业：${character.identity}")
        if (character.personality.isNotBlank()) parts.add("性格特征：${character.personality}")
        if (character.background.isNotBlank()) parts.add("背景故事：${character.background}")
        if (character.speakingStyle.isNotBlank()) parts.add("说话方式：${character.speakingStyle}")
        if (character.goals.isNotBlank()) parts.add("目标动机：${character.goals}")
        if (character.relationships.isNotBlank()) parts.add("关系设定：${character.relationships}")
        if (character.knowledge.isNotBlank()) parts.add("知识边界：${character.knowledge}")
        if (character.constraints.isNotBlank()) parts.add("禁止违背：${character.constraints}")
        if (character.behaviorRules.isNotBlank()) parts.add("行为约束：${character.behaviorRules}")
        if (character.greeting.isNotBlank()) parts.add("初始问候：${character.greeting}")
        if (character.exampleDialogue.isNotBlank()) parts.add("示例对话：${character.exampleDialogue}")

        return parts.joinToString("\n")
    }

    /**
     * 构建场景卡提示词
     */
    private fun buildScenarioCardPrompt(scenario: RoleplayScenario): String {
        val parts = mutableListOf<String>()
        parts.add("【场景设定】")
        parts.add("场景名称：${scenario.name}")

        if (scenario.worldview.isNotBlank()) parts.add("世界观：${scenario.worldview}")
        if (scenario.time.isNotBlank()) parts.add("时间：${scenario.time}")
        if (scenario.location.isNotBlank()) parts.add("地点：${scenario.location}")
        if (scenario.environment.isNotBlank()) parts.add("环境描述：${scenario.environment}")
        if (scenario.premise.isNotBlank()) parts.add("剧情前提：${scenario.premise}")
        if (scenario.rules.isNotBlank()) parts.add("世界规则：${scenario.rules}")
        if (scenario.relationshipState.isNotBlank()) parts.add("关系状态：${scenario.relationshipState}")
        if (scenario.conflict.isNotBlank()) parts.add("当前冲突：${scenario.conflict}")
        if (scenario.plotGoal.isNotBlank()) parts.add("剧情目标：${scenario.plotGoal}")
        if (scenario.atmosphere.isNotBlank()) parts.add("叙事氛围：${scenario.atmosphere}")
        if (scenario.narrativePerspective.isNotBlank()) parts.add("叙事视角：${scenario.narrativePerspective}")
        if (scenario.outputFormat.isNotBlank()) parts.add("输出格式：${scenario.outputFormat}")
        if (scenario.contentRestrictions.isNotBlank()) parts.add("内容限制：${scenario.contentRestrictions}")
        if (scenario.openingPrompt.isNotBlank()) parts.add("开场提示：${scenario.openingPrompt}")

        return parts.joinToString("\n")
    }

    /**
     * 构建叙事模式提示词
     */
    private fun buildNarrativeModePrompt(mode: String): String {
        return when (NarrativeMode.fromValue(mode)) {
            NarrativeMode.CHARACTER -> {
                "【叙事模式：角色内指令】\n" +
                "你将以角色身份回应，完全沉浸在角色设定中，使用角色的说话方式和语言风格。"
            }
            NarrativeMode.AUTHOR -> {
                "【叙事模式：作者/导演指令】\n" +
                "用户是故事的导演，你负责根据用户的指示推进剧情发展，可以控制所有角色的行为和对话。"
            }
            NarrativeMode.NARRATOR -> {
                "【叙事模式：旁白模式】\n" +
                "你只负责叙事和环境描写，不代替用户做决定，不代替用户角色说话。"
            }
            NarrativeMode.MULTI -> {
                "【叙事模式：多角色模式】\n" +
                "你将同时扮演多个角色，每个角色都有独立的性格和说话方式，注意区分不同角色的对话。"
            }
        }
    }

    /**
     * 处理剧情提示
     */
    suspend fun processPlotAction(
        sessionId: Long,
        action: PlotAction,
        customInstruction: String?
    ): String {
        val session = roleplaySessionDao.getSessionById(sessionId)
            ?: return ""

        return when (action) {
            PlotAction.CONTINUE -> "请继续发展剧情。"
            PlotAction.REGENERATE -> "请重新生成上一段内容，保持角色设定和场景一致性。"
            PlotAction.REWRITE -> "请改写上一段内容，可以调整细节但保持剧情走向。"
            PlotAction.EXTEND -> "请延长当前内容，增加更多细节描写和对话。"
            PlotAction.SHORTEN -> "请缩短当前内容，保留核心情节和关键对话。"
            PlotAction.CHANGE_PERSPECTIVE -> "请从不同的叙事视角重写当前内容。"
            PlotAction.CHANGE_TONE -> "请改变当前内容的语气和氛围。"
            PlotAction.BRANCH -> {
                // 创建分支会话的逻辑
                "请从当前点开始一个新的剧情分支。"
            }
            PlotAction.ROLLBACK -> {
                // 回滚到上一个版本的逻辑
                "请回到上一个版本重新开始。"
            }
            PlotAction.SUMMARY -> "请生成当前剧情的摘要，包括主要事件、角色关系变化和当前状态。"
            PlotAction.DIALOGUE_ONLY -> "请只生成角色的对话，不要添加旁白和动作描写。"
            PlotAction.NARRATION_ONLY -> "请只生成旁白和环境描写，不要生成角色对话。"
            PlotAction.DIALOGUE_ACTION -> "请生成角色对话和动作描写，不要添加旁白。"
            PlotAction.CUSTOM -> customInstruction ?: "请继续。"
        }
    }

    /**
     * 保存剧情摘要
     */
    suspend fun savePlotSummary(sessionId: Long, summary: String) {
        val session = roleplaySessionDao.getSessionById(sessionId) ?: return
        val existingSummary = roleplayMemoryDao.getLatestSummary(sessionId)

        if (existingSummary != null) {
            roleplayMemoryDao.updateMemory(
                existingSummary.copy(
                    content = summary,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            roleplayMemoryDao.insertMemory(
                RoleplayMemory(
                    sessionId = sessionId,
                    memoryType = "summary",
                    content = summary
                )
            )
        }

        roleplaySessionDao.updateMemoryState(
            id = sessionId,
            summary = summary,
            pinnedFacts = session.pinnedFacts
        )
    }

    /**
     * 添加固定事实
     */
    suspend fun addPinnedFact(sessionId: Long, fact: String) {
        roleplayMemoryDao.insertMemory(
            RoleplayMemory(
                sessionId = sessionId,
                memoryType = "fact",
                content = fact,
                isPinned = true
            )
        )
    }

    /**
     * 清除会话记忆
     */
    suspend fun clearSessionMemories(sessionId: Long) {
        roleplayMemoryDao.deleteAllMemories(sessionId)
        roleplaySessionDao.updateMemoryState(
            id = sessionId,
            summary = "",
            pinnedFacts = null
        )
    }

    /**
     * 检查是否为角色扮演会话
     */
    suspend fun isRoleplaySession(conversationId: Long): Boolean {
        return roleplaySessionDao.getSessionByConversationId(conversationId) != null
    }

    /**
     * 获取会话的角色和场景信息
     */
    suspend fun getSessionInfo(conversationId: Long): Triple<RoleplaySession?, CharacterProfile?, RoleplayScenario?> {
        val session = roleplaySessionDao.getSessionByConversationId(conversationId)
        val character = session?.characterId?.let { characterProfileDao.getCharacterById(it) }
        val scenario = session?.scenarioId?.let { roleplayScenarioDao.getScenarioById(it) }
        return Triple(session, character, scenario)
    }
}
