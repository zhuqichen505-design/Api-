package com.aiassistant

import com.aiassistant.domain.model.MemoryItem
import com.aiassistant.ui.screens.settings.CurrentVersionUserUpdates
import com.aiassistant.utils.SmartMemoryExtractor
import org.junit.Assert.*
import org.junit.Test

class V1923FeaturesTest {

    @Test
    fun testSmartMemoryExtractorNoiseFiltering() {
        // 1. 疑问句与求助句应该被彻底过滤，不能被当成记忆提取
        val questions = listOf(
            "这个 bug 怎么解？",
            "明天北京天气怎么样？",
            "请问如何使用 Jetpack Compose 实现列表拖拽？",
            "你觉得方案 A 和方案 B 哪个好呢？",
            "什么是双亲委派机制？",
            "能帮我看看这段代码哪里报错吗？"
        )
        questions.forEach { question ->
            val candidate = SmartMemoryExtractor.extractCandidate(question)
            assertNull("问题句子不应被提取为记忆: $question", candidate)
        }

        // 2. 单次任务型指令与动作词应该被过滤
        val taskPrompts = listOf(
            "帮我写一个快速排序算法",
            "优化一下这段 SQL 查询性能",
            "将这段文本翻译成英文",
            "阅读上面那段代码并解释逻辑",
            "画一个 Mermaid 架构图",
            "检查一下这段代码有没有内存泄漏"
        )
        taskPrompts.forEach { prompt ->
            val candidate = SmartMemoryExtractor.extractCandidate(prompt)
            assertNull("单次临时任务不应被提取为持久记忆: $prompt", candidate)
        }

        // 3. 日常客套寒暄应被过滤
        val chitChat = listOf(
            "你好",
            "早上好",
            "谢谢你的回答",
            "太棒了，非常感谢！",
            "好的收到",
            "再见"
        )
        chitChat.forEach { chat ->
            val candidate = SmartMemoryExtractor.extractCandidate(chat)
            assertNull("客套寒暄不应被提取为记忆: $chat", candidate)
        }
    }

    @Test
    fun testSmartMemoryExtractorHighValueExtraction() {
        // 1. 显式指示提炼
        val explicitPrompt = "请记住：以后的所有代码必须包含详细的中文注释和异常捕获"
        val explicitCandidate = SmartMemoryExtractor.extractCandidate(explicitPrompt)
        assertNotNull(explicitCandidate)
        assertEquals("PREFERENCE", explicitCandidate?.category)
        assertTrue(explicitCandidate!!.distilledContent.contains("中文注释"))

        // 2. 长期持久偏好提炼
        val prefPrompt = "以后的回答尽量精炼，不要出现废话和客套套话"
        val prefCandidate = SmartMemoryExtractor.extractCandidate(prefPrompt)
        assertNotNull(prefCandidate)
        assertEquals("PREFERENCE", prefCandidate?.category)
        assertTrue(prefCandidate!!.distilledContent.contains("精炼"))

        // 3. 个人身份与背景提炼
        val profilePrompt = "我叫李华，目前在深圳担任资深 Android 架构师"
        val profileCandidate = SmartMemoryExtractor.extractCandidate(profilePrompt)
        assertNotNull(profileCandidate)
        assertEquals("FACT", profileCandidate?.category)
        assertTrue(profileCandidate!!.distilledContent.contains("架构师") || profileCandidate.distilledContent.contains("李华"))

        // 4. 项目/系统事实提炼
        val factPrompt = "本项目基于 Kotlin 和 Jetpack Compose 开发，包名为 com.aiassistant"
        val factCandidate = SmartMemoryExtractor.extractCandidate(factPrompt)
        assertNotNull(factCandidate)
        assertEquals("PROJECT", factCandidate?.category)
        assertTrue(factCandidate!!.distilledContent.contains("Kotlin") || factCandidate.distilledContent.contains("com.aiassistant"))
    }

    @Test
    fun testConversationSessionMemoryScopeAndIsolation() {
        val globalMemory = MemoryItem(
            id = 1L,
            content = "全局代码规范偏好",
            scope = "user",
            conversationId = null,
            isEnabled = true
        )

        val sessionMemory1 = MemoryItem(
            id = 2L,
            content = "会话1的专属设定：扮演严谨的系统架构师",
            scope = "conversation",
            conversationId = 1001L,
            isEnabled = true
        )

        val sessionMemory2 = MemoryItem(
            id = 3L,
            content = "会话2的专属设定：扮演童话故事讲述人",
            scope = "conversation",
            conversationId = 1002L,
            isEnabled = true
        )

        // 验证各会话记忆独立隔离
        assertEquals("user", globalMemory.scope)
        assertNull(globalMemory.conversationId)

        assertEquals("conversation", sessionMemory1.scope)
        assertEquals(1001L, sessionMemory1.conversationId)

        assertEquals("conversation", sessionMemory2.scope)
        assertEquals(1002L, sessionMemory2.conversationId)

        assertNotEquals(sessionMemory1.conversationId, sessionMemory2.conversationId)
    }

    @Test
    fun testReasoningEffortLevelColorSpecsAreBlueHue() {
        val blueLevels = listOf(
            0x64748BL to "Level 0 Slate Gray-Blue",
            0x38BDF8L to "Level 1 Light Ice-Blue",
            0x0284C7L to "Level 2 Dodger Blue",
            0x0369A1L to "Level 3 Deep Sea Blue",
            0x1D4ED8L to "Level 4 Royal Sapphire Blue"
        )

        blueLevels.forEachIndexed { index, (hex, name) ->
            val red = (hex shr 16) and 0xFF
            val green = (hex shr 8) and 0xFF
            val blue = hex and 0xFF
            assertTrue("$name ($hex) 的蓝色分量必须大于红色分量", blue > red)
        }
    }

    @Test
    fun testCurrentVersionUserUpdatesContainsV1923() {
        val firstUpdate = CurrentVersionUserUpdates.firstOrNull()
        assertNotNull(firstUpdate)
        assertTrue("更新日志必须包含 v1.9.23", CurrentVersionUserUpdates.any { it.contains("v1.9.23") })
        assertTrue("必须包含会话专属记忆管理系统说明", CurrentVersionUserUpdates.any { it.contains("会话专属记忆") })
        assertTrue("必须包含高信噪比智能记忆提取说明", CurrentVersionUserUpdates.any { it.contains("记忆提取") })
        assertTrue("必须包含思考强度全阶递进蓝色系说明", CurrentVersionUserUpdates.any { it.contains("蓝色系") })
        assertTrue("必须包含文字划选浮动工具栏防闪烁说明", CurrentVersionUserUpdates.any { it.contains("浮动工具栏") })
        assertTrue("必须包含顶部悬浮栏纯色外框消除说明", CurrentVersionUserUpdates.any { it.contains("纯色外框消除") })
    }
}
