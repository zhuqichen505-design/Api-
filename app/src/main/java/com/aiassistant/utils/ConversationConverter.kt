package com.aiassistant.utils

import com.aiassistant.data.local.CharacterProfileDao
import com.aiassistant.data.local.ConversationDao
import com.aiassistant.data.local.MessageDao
import com.aiassistant.data.local.RoleplayScenarioDao
import com.aiassistant.data.local.RoleplaySessionDao
import com.aiassistant.domain.model.CharacterProfile
import com.aiassistant.domain.model.Conversation
import com.aiassistant.domain.model.Message
import com.aiassistant.domain.model.NarrativeMode
import com.aiassistant.domain.model.RoleplayScenario
import com.aiassistant.domain.model.RoleplaySession
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ConversationExportBundle(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val conversation: Conversation,
    val messages: List<Message>,
    val isRoleplay: Boolean = false,
    val roleplaySession: RoleplaySession? = null,
    val character: CharacterProfile? = null,
    val scenario: RoleplayScenario? = null
)

class ConversationConverter(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val roleplaySessionDao: RoleplaySessionDao,
    private val characterProfileDao: CharacterProfileDao,
    private val roleplayScenarioDao: RoleplayScenarioDao,
    private val gson: Gson = Gson()
) {

    /**
     * 将普通对话转换为角色扮演/故事创作会话
     */
    suspend fun convertToRoleplay(
        conversationId: Long,
        charName: String? = null,
        charIdentity: String? = null,
        charPersonality: String? = null,
        scenarioName: String? = null,
        scenarioWorld: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val conv = conversationDao.getConversationById(conversationId)
            ?: throw IllegalArgumentException("会话不存在: $conversationId")

        // 若已是角色扮演，直接返回 session id
        val existingSession = roleplaySessionDao.getSessionByConversationId(conversationId)
        if (existingSession != null) return@withContext existingSession.id

        val now = System.currentTimeMillis()

        // 1. 创建关联的角色卡
        val resolvedCharName = charName?.takeIf { it.isNotBlank() } ?: "对话主角 (${conv.title.take(8)})"
        val character = CharacterProfile(
            name = resolvedCharName,
            identity = charIdentity.orEmpty().ifBlank { "故事核心主角" },
            personality = charPersonality.orEmpty().ifBlank { "沉着、智慧、性格鲜明" },
            background = conv.systemPrompt?.take(300).orEmpty().ifBlank { "基于历史对话提炼的角色背景" },
            speakingStyle = "符合角色身份的自然对白",
            createdAt = now,
            updatedAt = now
        )
        val charId = characterProfileDao.insertCharacter(character)

        // 2. 创建关联的世界观/场景卡
        val resolvedScenarioName = scenarioName?.takeIf { it.isNotBlank() } ?: "${conv.title.take(10)} · 世界观"
        val scenario = RoleplayScenario(
            name = resolvedScenarioName,
            worldview = scenarioWorld.orEmpty().ifBlank { "现实与幻想交织的叙事空间" },
            environment = "开阔场景",
            conflict = "双方互动与情节发展",
            atmosphere = "沉浸而引人入胜",
            createdAt = now,
            updatedAt = now
        )
        val scenarioId = roleplayScenarioDao.insertScenario(scenario)

        // 3. 创建 RoleplaySession
        val roleplaySession = RoleplaySession(
            conversationId = conversationId,
            characterId = charId,
            scenarioId = scenarioId,
            characterIds = charId.toString(),
            narrativeMode = NarrativeMode.CHARACTER.value,
            currentPlotSummary = "",
            createdAt = now,
            updatedAt = now
        )
        val sessionId = roleplaySessionDao.insertSession(roleplaySession)

        // 4. 更新对话 tags，加入 "roleplay"
        val existingTags = conv.tags.orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
        existingTags.add("roleplay")
        conversationDao.updateConversation(conv.copy(tags = existingTags.joinToString(","), updatedAt = now))

        return@withContext sessionId
    }

    /**
     * 将角色扮演会话转换回普通对话
     * 关键要求：角色卡人设与世界观完整转译为对话的 systemPrompt，绝不丢失任何设定！
     */
    suspend fun convertToNormal(conversationId: Long): Boolean = withContext(Dispatchers.IO) {
        val conv = conversationDao.getConversationById(conversationId) ?: return@withContext false
        val session = roleplaySessionDao.getSessionByConversationId(conversationId) ?: return@withContext false

        val character = session.characterId?.let { characterProfileDao.getCharacterById(it) }
        val scenario = session.scenarioId?.let { roleplayScenarioDao.getScenarioById(it) }

        // 完整转译合并设定为常规系统提示词
        val mergedPrompt = buildString {
            if (character != null) {
                append("【角色设定】\n")
                append("角色名称：").append(character.name).append("\n")
                if (character.identity.isNotBlank()) append("身份职业：").append(character.identity).append("\n")
                if (character.personality.isNotBlank()) append("性格特点：").append(character.personality).append("\n")
                if (character.background.isNotBlank()) append("背景：").append(character.background).append("\n")
                if (character.speakingStyle.isNotBlank()) append("对白风格：").append(character.speakingStyle).append("\n")
                append("\n")
            }
            if (scenario != null) {
                append("【世界观与场景】\n")
                append("场景名称：").append(scenario.name).append("\n")
                if (scenario.worldview.isNotBlank()) append("世界法则：").append(scenario.worldview).append("\n")
                if (scenario.conflict.isNotBlank()) append("当前冲突：").append(scenario.conflict).append("\n")
                append("\n")
            }
            if (session.currentPlotSummary.isNotBlank()) {
                append("【前序剧情摘要】\n").append(session.currentPlotSummary).append("\n\n")
            }
            conv.systemPrompt?.takeIf { it.isNotBlank() }?.let {
                append("【原补充提示词】\n").append(it)
            }
        }.trim()

        // 移除 "roleplay" 标签
        val existingTags = conv.tags.orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "roleplay" }
            .joinToString(",")

        val now = System.currentTimeMillis()
        conversationDao.updateConversation(
            conv.copy(
                systemPrompt = mergedPrompt,
                tags = existingTags.ifBlank { null },
                updatedAt = now
            )
        )

        // 移除 RoleplaySession 绑定
        roleplaySessionDao.deleteSession(session)
        return@withContext true
    }

    /**
     * 导出完整会话（支持常规与角色创作）
     */
    suspend fun exportBundle(conversationId: Long): String = withContext(Dispatchers.IO) {
        val conv = conversationDao.getConversationById(conversationId)
            ?: throw IllegalArgumentException("会话不存在")
        val messages = messageDao.getMessagesList(conversationId)
        val session = roleplaySessionDao.getSessionByConversationId(conversationId)
        val char = session?.characterId?.let { characterProfileDao.getCharacterById(it) }
        val sc = session?.scenarioId?.let { roleplayScenarioDao.getScenarioById(it) }

        val bundle = ConversationExportBundle(
            conversation = conv,
            messages = messages,
            isRoleplay = session != null,
            roleplaySession = session,
            character = char,
            scenario = sc
        )
        gson.toJson(bundle)
    }
}
