package com.aiassistant

import com.aiassistant.data.repository.helpers.ChatContextAssemblyHelper
import com.aiassistant.data.repository.helpers.ContentPruningHelper
import com.aiassistant.domain.model.CompressionTier
import com.aiassistant.domain.model.CompressionTierPolicy
import com.aiassistant.domain.model.Message
import org.junit.Assert.*
import org.junit.Test

class CompressionTierPolicyTest {

    @Test
    fun testEvaluateAutoUpgrade_whenDisabled_returnsNull() {
        val res = CompressionTierPolicy.evaluateAutoUpgrade(
            currentTier = CompressionTier.L0,
            usagePercent = 0.99f,
            autoEnabled = false
        )
        assertNull("自动升档关闭时严禁自动升档", res)
    }

    @Test
    fun testEvaluateAutoUpgrade_whenBelowThreshold_returnsNull() {
        val res = CompressionTierPolicy.evaluateAutoUpgrade(
            currentTier = CompressionTier.L0,
            usagePercent = 0.70f,
            autoEnabled = true,
            thresholdL2 = 0.75f
        )
        assertNull("占用率低于 75% 阈值时不建议升档", res)
    }

    @Test
    fun testEvaluateAutoUpgrade_progressiveTiers() {
        // 75% -> 触发 L2
        val resL2 = CompressionTierPolicy.evaluateAutoUpgrade(
            currentTier = CompressionTier.L0,
            usagePercent = 0.76f,
            autoEnabled = true
        )
        assertEquals(CompressionTier.L2, resL2)

        // 85% -> 触发 L3
        val resL3 = CompressionTierPolicy.evaluateAutoUpgrade(
            currentTier = CompressionTier.L2,
            usagePercent = 0.86f,
            autoEnabled = true
        )
        assertEquals(CompressionTier.L3, resL3)

        // 95% -> 触发 L4
        val resL4 = CompressionTierPolicy.evaluateAutoUpgrade(
            currentTier = CompressionTier.L3,
            usagePercent = 0.96f,
            autoEnabled = true
        )
        assertEquals(CompressionTier.L4, resL4)

        // 若当前已是 L4，不重复升档
        val resL4Max = CompressionTierPolicy.evaluateAutoUpgrade(
            currentTier = CompressionTier.L4,
            usagePercent = 0.99f,
            autoEnabled = true
        )
        assertNull(resL4Max)

        // 若当前已是 L3，占用率仅在 76%（低于 L3 阈值但高于 L2），不反向降级
        val noDowngrade = CompressionTierPolicy.evaluateAutoUpgrade(
            currentTier = CompressionTier.L3,
            usagePercent = 0.76f,
            autoEnabled = true
        )
        assertNull("自动升档判定绝不向低档位反向降级", noDowngrade)
    }

    @Test
    fun testFallbackOnContextOverflow() {
        // 未超上限
        assertNull(CompressionTierPolicy.fallbackOnContextOverflow(CompressionTier.L0, 1000, 2000))

        // 超过上限依次降档
        assertEquals(CompressionTier.L1, CompressionTierPolicy.fallbackOnContextOverflow(CompressionTier.L0, 2500, 2000))
        assertEquals(CompressionTier.L2, CompressionTierPolicy.fallbackOnContextOverflow(CompressionTier.L1, 2500, 2000))
        assertEquals(CompressionTier.L3, CompressionTierPolicy.fallbackOnContextOverflow(CompressionTier.L2, 2500, 2000))
        assertEquals(CompressionTier.L4, CompressionTierPolicy.fallbackOnContextOverflow(CompressionTier.L3, 2500, 2000))
        assertNull(CompressionTierPolicy.fallbackOnContextOverflow(CompressionTier.L4, 2500, 2000))
    }

    @Test
    fun testContentPruningHelper() {
        // 1. OCR 文本修剪
        val ocrRaw = "用户问题：\n[图片OCR识别：document.png]\n这里是非常长非常长的一大堆识别出来的印刷体文字，包含很多页的扫描文本。\n\n请帮我翻译第一句话。"
        val prunedOcr = ContentPruningHelper.pruneMessageContent(ocrRaw)
        assertTrue(prunedOcr.contains("[附件OCR内容已省略]"))
        assertTrue(prunedOcr.contains("请帮我翻译第一句话。"))
        assertFalse(prunedOcr.contains("一大堆识别出来的印刷体文字"))

        // 2. 联网与工具结果修剪
        val toolRaw = "【实时联网参考信息】\n1. 网页标题：最新科技快讯\n摘要：今日发布重要大模型更新...\n【用户输入的问题/指令】\n请总结今日要闻"
        val prunedTool = ContentPruningHelper.pruneMessageContent(toolRaw)
        assertTrue(prunedTool.contains("[联网与工具调用信息已省略]"))
        assertTrue(prunedTool.contains("请总结今日要闻"))
        assertFalse(prunedTool.contains("今日发布重要大模型更新"))

        // 3. 长代码块折叠（>8 行）
        val longCodeRaw = """
            以下是实现逻辑：
            ```kotlin
            fun line1() {}
            fun line2() {}
            fun line3() {}
            fun line4() {}
            fun line5() {}
            fun line6() {}
            fun line7() {}
            fun line8() {}
            fun line9() {}
            fun line10() {}
            ```
            请检查代码是否有问题。
        """.trimIndent()
        val prunedCode = ContentPruningHelper.pruneMessageContent(longCodeRaw)
        assertTrue(prunedCode.contains("[长代码块已精简折叠: 共 10 行]"))
        assertTrue(prunedCode.contains("请检查代码是否有问题。"))
        assertFalse(prunedCode.contains("fun line5()"))
    }

    @Test
    fun testAssembleTieredContextMessages_safetyBoundaries() {
        val messages = mutableListOf<Message>()
        for (i in 1..40) {
            messages.add(
                Message(
                    id = i.toLong(),
                    conversationId = 1L,
                    role = if (i % 2 == 1) "user" else "assistant",
                    content = if (i == 5) "【固定事实】我必须在任何情况下被保留。" else "对话消息序号 $i",
                    isPinned = (i == 5) // 第 5 条置顶固定
                )
            )
        }

        // L4 极限压缩档位测试：
        // 应该保留：isPinned (id=5) + 最近 8 轮 (最后 16 条，id 25..40)
        val l4Result = ChatContextAssemblyHelper.assembleTieredContextMessages(
            tier = CompressionTier.L4,
            usableMessages = messages,
            recentBudget = 100_000
        )
        val l4Ids = l4Result.activeMessages.map { it.id }.toSet()
        assertTrue("L4 极限压缩必须无条件保留 isPinned 消息", l4Ids.contains(5L))
        assertTrue("L4 极限压缩必须保留最近 1 轮消息 (id=40)", l4Ids.contains(40L))
        assertTrue("L4 极限压缩必须保留最近 1 轮消息 (id=39)", l4Ids.contains(39L))
        assertTrue("L4 极限压缩必须保留最近 8 轮窗口内的消息 (id=25)", l4Ids.contains(25L))
        assertFalse("L4 极限压缩应安全退休未置顶的较早消息 (id=10)", l4Ids.contains(10L))

        // L2 滚动摘要档位测试 (保留 8 轮，16条)
        val l2Result = ChatContextAssemblyHelper.assembleTieredContextMessages(
            tier = CompressionTier.L2,
            usableMessages = messages,
            recentBudget = 100_000,
            l2RecentRounds = 8,
            existingRollingSummary = "之前关于项目技术选型的讨论摘要"
        )
        assertEquals("之前关于项目技术选型的讨论摘要", l2Result.injectedSummary)
        val l2Ids = l2Result.activeMessages.map { it.id }.toSet()
        assertTrue("L2 档位必须强制保留 isPinned 消息", l2Ids.contains(5L))
        assertTrue("L2 档位必须保留最近 1 轮", l2Ids.contains(40L))

        // L3 深度压缩档位测试 (保留 16 轮，32条)
        val l3Result = ChatContextAssemblyHelper.assembleTieredContextMessages(
            tier = CompressionTier.L3,
            usableMessages = messages,
            recentBudget = 100_000,
            structuredSummary = "【核心背景与用户固定约束】：\n- 重要约束"
        )
        assertEquals("【核心背景与用户固定约束】：\n- 重要约束", l3Result.injectedSummary)
        val l3Ids = l3Result.activeMessages.map { it.id }.toSet()
        assertTrue("L3 必须保留最近 16 轮内的消息 (id=15)", l3Ids.contains(15L))
        assertTrue("L3 必须强制保留 isPinned 消息", l3Ids.contains(5L))
    }

    @Test
    fun testDatabaseMigration29To30Registered() {
        val migration = com.aiassistant.data.local.AppDatabase.MIGRATION_29_30
        assertNotNull("MIGRATION_29_30 必须存在", migration)
        assertEquals(29, migration.startVersion)
        assertEquals(30, migration.endVersion)
    }
}
