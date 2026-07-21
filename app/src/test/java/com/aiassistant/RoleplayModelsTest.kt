package com.aiassistant

import com.aiassistant.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class RoleplayModelsTest {

    @Test
    fun `CharacterProfile default values are correct`() {
        val character = CharacterProfile(name = "测试角色")

        assertEquals("测试角色", character.name)
        assertEquals("", character.identity)
        assertEquals("", character.personality)
        assertEquals("", character.background)
        assertEquals("", character.speakingStyle)
        assertEquals("", character.goals)
        assertEquals("", character.relationships)
        assertEquals("", character.knowledge)
        assertEquals("", character.constraints)
        assertEquals("", character.behaviorRules)
        assertEquals("", character.greeting)
        assertEquals("", character.exampleDialogue)
        assertNull(character.tags)
        assertFalse(character.isFavorite)
        assertFalse(character.isDefault)
        assertTrue(character.createdAt > 0)
        assertTrue(character.updatedAt > 0)
    }

    @Test
    fun `CharacterProfile with all fields`() {
        val character = CharacterProfile(
            id = 1,
            name = "艾莉丝",
            avatarUri = "content://avatar/1",
            identity = "魔法师",
            personality = "温柔、坚强",
            background = "来自古老的魔法家族",
            speakingStyle = "文雅、带有古语",
            goals = "寻找失落的魔法",
            relationships = "与主角是师徒关系",
            knowledge = "精通元素魔法",
            constraints = "不能使用黑暗魔法",
            behaviorRules = "保护弱者",
            greeting = "你好，年轻的冒险者。",
            exampleDialogue = "用户：你是谁？\n艾莉丝：我是艾莉丝，一名元素魔法师。",
            tags = "奇幻,主角,女性",
            isFavorite = true,
            isDefault = true
        )

        assertEquals(1, character.id)
        assertEquals("艾莉丝", character.name)
        assertEquals("魔法师", character.identity)
        assertEquals("温柔、坚强", character.personality)
        assertEquals("来自古老的魔法家族", character.background)
        assertEquals("文雅、带有古语", character.speakingStyle)
        assertEquals("寻找失落的魔法", character.goals)
        assertEquals("与主角是师徒关系", character.relationships)
        assertEquals("精通元素魔法", character.knowledge)
        assertEquals("不能使用黑暗魔法", character.constraints)
        assertEquals("保护弱者", character.behaviorRules)
        assertEquals("你好，年轻的冒险者。", character.greeting)
        assertEquals("奇幻,主角,女性", character.tags)
        assertTrue(character.isFavorite)
        assertTrue(character.isDefault)
    }

    @Test
    fun `RoleplayScenario default values are correct`() {
        val scenario = RoleplayScenario(name = "测试场景")

        assertEquals("测试场景", scenario.name)
        assertEquals("", scenario.worldview)
        assertEquals("", scenario.time)
        assertEquals("", scenario.location)
        assertEquals("", scenario.environment)
        assertEquals("", scenario.premise)
        assertEquals("", scenario.rules)
        assertEquals("", scenario.relationshipState)
        assertEquals("", scenario.conflict)
        assertEquals("", scenario.plotGoal)
        assertEquals("", scenario.atmosphere)
        assertEquals("", scenario.narrativePerspective)
        assertEquals("", scenario.outputFormat)
        assertEquals("", scenario.contentRestrictions)
        assertEquals("", scenario.openingPrompt)
        assertNull(scenario.tags)
        assertFalse(scenario.isFavorite)
        assertTrue(scenario.createdAt > 0)
        assertTrue(scenario.updatedAt > 0)
    }

    @Test
    fun `RoleplayScenario with all fields`() {
        val scenario = RoleplayScenario(
            id = 1,
            name = "魔法学院",
            worldview = "一个充满魔法的世界",
            time = "中世纪",
            location = "艾尔文魔法学院",
            environment = "古老的城堡，周围环绕着魔法森林",
            premise = "新生入学的第一天",
            rules = "魔法分为元素、治愈、召唤三大系",
            relationshipState = "同学关系",
            conflict = "学院中出现了神秘的魔法事件",
            plotGoal = "查明真相，保护学院",
            atmosphere = "神秘、紧张",
            narrativePerspective = "第三人称有限",
            outputFormat = "包含对话和动作描写",
            contentRestrictions = "无暴力血腥内容",
            openingPrompt = "你站在魔法学院的大门前...",
            tags = "奇幻,校园,冒险",
            isFavorite = true
        )

        assertEquals(1, scenario.id)
        assertEquals("魔法学院", scenario.name)
        assertEquals("一个充满魔法的世界", scenario.worldview)
        assertEquals("中世纪", scenario.time)
        assertEquals("艾尔文魔法学院", scenario.location)
        assertEquals("奇幻,校园,冒险", scenario.tags)
        assertTrue(scenario.isFavorite)
    }

    @Test
    fun `RoleplaySession default values are correct`() {
        val session = RoleplaySession(conversationId = 1)

        assertEquals(0, session.id)
        assertNull(session.characterId)
        assertNull(session.scenarioId)
        assertEquals(1, session.conversationId)
        assertEquals("character", session.narrativeMode)
        assertEquals("", session.currentPlotSummary)
        assertNull(session.pinnedFacts)
        assertEquals(1, session.lastVersionIndex)
        assertTrue(session.createdAt > 0)
        assertTrue(session.updatedAt > 0)
    }

    @Test
    fun `RoleplaySession with all fields`() {
        val session = RoleplaySession(
            id = 1,
            characterId = 10,
            scenarioId = 20,
            conversationId = 30,
            narrativeMode = "author",
            currentPlotSummary = "故事发展到一半",
            pinnedFacts = "[\"角色A是好人\",\"角色B是坏人\"]",
            lastVersionIndex = 3
        )

        assertEquals(1, session.id)
        assertEquals(10, session.characterId)
        assertEquals(20, session.scenarioId)
        assertEquals(30, session.conversationId)
        assertEquals("author", session.narrativeMode)
        assertEquals("故事发展到一半", session.currentPlotSummary)
        assertEquals("[\"角色A是好人\",\"角色B是坏人\"]", session.pinnedFacts)
        assertEquals(3, session.lastVersionIndex)
    }

    @Test
    fun `RoleplayMemory default values are correct`() {
        val memory = RoleplayMemory(
            sessionId = 1,
            content = "测试记忆"
        )

        assertEquals(0, memory.id)
        assertEquals(1, memory.sessionId)
        assertEquals("fact", memory.memoryType)
        assertEquals("测试记忆", memory.content)
        assertNull(memory.sourceMessageId)
        assertFalse(memory.isPinned)
        assertTrue(memory.createdAt > 0)
        assertTrue(memory.updatedAt > 0)
    }

    @Test
    fun `RoleplayMemory with all fields`() {
        val memory = RoleplayMemory(
            id = 1,
            sessionId = 10,
            memoryType = "summary",
            content = "这是一段摘要",
            sourceMessageId = 100,
            isPinned = true
        )

        assertEquals(1, memory.id)
        assertEquals(10, memory.sessionId)
        assertEquals("summary", memory.memoryType)
        assertEquals("这是一段摘要", memory.content)
        assertEquals(100, memory.sourceMessageId)
        assertTrue(memory.isPinned)
    }

    @Test
    fun `CharacterTag default values are correct`() {
        val tag = CharacterTag(name = "奇幻")

        assertEquals(0, tag.id)
        assertEquals("奇幻", tag.name)
        assertTrue(tag.createdAt > 0)
    }

    @Test
    fun `CharacterTagCrossRef creation`() {
        val crossRef = CharacterTagCrossRef(
            characterId = 1,
            tagId = 2
        )

        assertEquals(1, crossRef.characterId)
        assertEquals(2, crossRef.tagId)
    }

    @Test
    fun `NarrativeMode from value`() {
        assertEquals(NarrativeMode.CHARACTER, NarrativeMode.fromValue("character"))
        assertEquals(NarrativeMode.AUTHOR, NarrativeMode.fromValue("author"))
        assertEquals(NarrativeMode.NARRATOR, NarrativeMode.fromValue("narrator"))
        assertEquals(NarrativeMode.MULTI, NarrativeMode.fromValue("multi"))
        assertEquals(NarrativeMode.CHARACTER, NarrativeMode.fromValue("unknown"))
    }

    @Test
    fun `NarrativeMode display names`() {
        assertEquals("角色内指令", NarrativeMode.CHARACTER.displayName)
        assertEquals("作者/导演指令", NarrativeMode.AUTHOR.displayName)
        assertEquals("旁白模式", NarrativeMode.NARRATOR.displayName)
        assertEquals("多角色模式", NarrativeMode.MULTI.displayName)
    }

    @Test
    fun `PlotAction from value`() {
        assertEquals(PlotAction.CONTINUE, PlotAction.fromValue("continue"))
        assertEquals(PlotAction.REGENERATE, PlotAction.fromValue("regenerate"))
        assertEquals(PlotAction.REWRITE, PlotAction.fromValue("rewrite"))
        assertEquals(PlotAction.EXTEND, PlotAction.fromValue("extend"))
        assertEquals(PlotAction.SHORTEN, PlotAction.fromValue("shorten"))
        assertEquals(PlotAction.CHANGE_PERSPECTIVE, PlotAction.fromValue("change_perspective"))
        assertEquals(PlotAction.CHANGE_TONE, PlotAction.fromValue("change_tone"))
        assertEquals(PlotAction.BRANCH, PlotAction.fromValue("branch"))
        assertEquals(PlotAction.ROLLBACK, PlotAction.fromValue("rollback"))
        assertEquals(PlotAction.SUMMARY, PlotAction.fromValue("summary"))
        assertEquals(PlotAction.DIALOGUE_ONLY, PlotAction.fromValue("dialogue_only"))
        assertEquals(PlotAction.NARRATION_ONLY, PlotAction.fromValue("narration_only"))
        assertEquals(PlotAction.DIALOGUE_ACTION, PlotAction.fromValue("dialogue_action"))
        assertEquals(PlotAction.CUSTOM, PlotAction.fromValue("custom"))
        assertEquals(PlotAction.CONTINUE, PlotAction.fromValue("unknown"))
    }

    @Test
    fun `PlotAction display names`() {
        assertEquals("继续剧情", PlotAction.CONTINUE.displayName)
        assertEquals("重生成", PlotAction.REGENERATE.displayName)
        assertEquals("改写上一段", PlotAction.REWRITE.displayName)
        assertEquals("延长内容", PlotAction.EXTEND.displayName)
        assertEquals("缩短内容", PlotAction.SHORTEN.displayName)
        assertEquals("改变叙事视角", PlotAction.CHANGE_PERSPECTIVE.displayName)
        assertEquals("改变语气", PlotAction.CHANGE_TONE.displayName)
        assertEquals("创建剧情分支", PlotAction.BRANCH.displayName)
        assertEquals("回到上一个版本", PlotAction.ROLLBACK.displayName)
        assertEquals("生成剧情摘要", PlotAction.SUMMARY.displayName)
        assertEquals("只生成角色对白", PlotAction.DIALOGUE_ONLY.displayName)
        assertEquals("只生成旁白", PlotAction.NARRATION_ONLY.displayName)
        assertEquals("生成对白加动作", PlotAction.DIALOGUE_ACTION.displayName)
        assertEquals("自定义指令", PlotAction.CUSTOM.displayName)
    }

    @Test
    fun `PlotAction descriptions`() {
        assertEquals("继续当前剧情发展", PlotAction.CONTINUE.description)
        assertEquals("重新生成上一段内容", PlotAction.REGENERATE.description)
        assertEquals("改写上一段内容", PlotAction.REWRITE.description)
        assertEquals("延长当前内容", PlotAction.EXTEND.description)
        assertEquals("缩短当前内容", PlotAction.SHORTEN.description)
    }

    @Test
    fun `CharacterProfile copy preserves id and createdAt`() {
        val original = CharacterProfile(
            id = 1,
            name = "原始角色",
            createdAt = 1000L
        )

        val copied = original.copy(
            name = "修改后的角色",
            updatedAt = 2000L
        )

        assertEquals(1, copied.id)
        assertEquals("修改后的角色", copied.name)
        assertEquals(1000L, copied.createdAt)
        assertEquals(2000L, copied.updatedAt)
    }

    @Test
    fun `RoleplayScenario copy preserves id and createdAt`() {
        val original = RoleplayScenario(
            id = 1,
            name = "原始场景",
            createdAt = 1000L
        )

        val copied = original.copy(
            name = "修改后的场景",
            updatedAt = 2000L
        )

        assertEquals(1, copied.id)
        assertEquals("修改后的场景", copied.name)
        assertEquals(1000L, copied.createdAt)
        assertEquals(2000L, copied.updatedAt)
    }

    @Test
    fun `RoleplaySession copy preserves id and createdAt`() {
        val original = RoleplaySession(
            id = 1,
            conversationId = 10,
            createdAt = 1000L
        )

        val copied = original.copy(
            narrativeMode = "author",
            updatedAt = 2000L
        )

        assertEquals(1, copied.id)
        assertEquals(10, copied.conversationId)
        assertEquals("author", copied.narrativeMode)
        assertEquals(1000L, copied.createdAt)
        assertEquals(2000L, copied.updatedAt)
    }
}
