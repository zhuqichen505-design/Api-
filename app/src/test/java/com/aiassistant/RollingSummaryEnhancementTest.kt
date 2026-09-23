package com.aiassistant

import com.aiassistant.domain.model.Message
import com.aiassistant.utils.AdvancedMemoryEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RollingSummaryEnhancementTest {

    @Test
    fun testExtractCompleteSentence_preservesPrimaryPunctuation() {
        val longText = "我们决定采用 Kotlin 和 Jetpack Compose 作为客户端技术栈。数据库使用 Room 进行本地存储。网络层采用 Retrofit。"
        val extracted = AdvancedMemoryEngine.extractCompleteSentence(longText, 45)

        // 验证：应在第一个完整句子句号处截断，绝不能在字中间截断
        assertTrue("必须保留完整的第一个分句并以标点闭合", extracted.endsWith("。"))
        assertTrue("必须包含技术栈关键词", extracted.contains("技术栈"))
        assertFalse("不应截断包含后面的破碎语句", extracted.contains("存储"))
    }

    @Test
    fun testExtractCompleteSentence_preservesSecondaryPunctuationWithEllipsis() {
        val longTextNoFullStop = "我们讨论了客户端重构架构方案，包括模块解耦，以及数据库迁移升级策略，还有流式输出优化"
        val extracted = AdvancedMemoryEngine.extractCompleteSentence(longTextNoFullStop, 30)

        assertTrue("在逗号处截断并保留省略号", extracted.endsWith("..."))
        assertTrue("提取内容完整", extracted.contains("客户端重构架构方案"))
    }

    @Test
    fun testGenerateExtractiveStructuredSummary_neverViolentlyTruncatesSentences() {
        val messages = listOf(
            Message(
                id = 1,
                conversationId = 1,
                role = "user",
                content = "请帮我制定一个完整的项目优化方案，包含架构升级与网络重构。"
            ),
            Message(
                id = 2,
                conversationId = 1,
                role = "assistant",
                content = "好的，我们决定采用分层架构改造，并且统一接入长超时分析服务确保大模型生成稳定。"
            ),
            Message(
                id = 3,
                conversationId = 1,
                role = "user",
                content = "接下来需要确认滚动摘要与记忆系统的协同去重策略。"
            )
        )

        val summary = AdvancedMemoryEngine.generateExtractiveStructuredSummary(messages)
        val promptBlock = summary.toPromptBlock()

        assertFalse("严禁包含暴力截断的破碎词", promptBlock.contains("截断"))
        assertFalse("严禁包含生硬的上下文状态机术语废话", promptBlock.contains("高保真结构化上下文状态机"))
        assertTrue("应提取出决策关键句", summary.milestones.any { it.contains("分层架构改造") })
        assertTrue("应提取出下一步待办项", summary.openItems.any { it.contains("协同去重策略") })
        // 验证每一个 milestone 都是完整句子或自然句，绝无 70 字符硬切
        summary.milestones.forEach { item ->
            assertTrue("里程碑内容应有一定信息量", item.length >= 10)
        }
    }

    @Test
    fun testExtractiveStructuredSummary_onlyScansRecentWindow() {
        val oldMessages = (1..35).map { idx ->
            Message(
                id = idx.toLong(),
                conversationId = 1,
                role = if (idx % 2 == 1) "user" else "assistant",
                content = if (idx == 1) "【决定】：古代开篇决定在车站见面。" else "第 $idx 轮常规对话进展记录。"
            )
        }
        val recentMessages = listOf(
            Message(
                id = 36,
                conversationId = 1,
                role = "assistant",
                content = "【商定】：两人商定在商业街吃拉面。"
            ),
            Message(
                id = 37,
                conversationId = 1,
                role = "user",
                content = "接下来需要前往拉面馆就餐。"
            )
        )
        val allMessages = oldMessages + recentMessages
        val summary = AdvancedMemoryEngine.generateExtractiveStructuredSummary(allMessages)

        // 验证：37 条消息中，前 7 条（包含第 1 条“古代开篇决定在车站见面”）被排除在最近 30 条窗口之外
        assertFalse("兜底提取严禁包含远古第 1 条开篇事件", summary.milestones.any { it.contains("古代开篇决定在车站见面") })
        assertTrue("兜底提取应正确包含近期商定事件", summary.milestones.any { it.contains("商业街吃拉面") })
        assertTrue("兜底提取应包含最新下一步待办", summary.openItems.any { it.contains("前往拉面馆就餐") })
    }

    @Test
    fun testBuildStructuredSummaryPrompt_containsClarityAndCompletenessDirectives() {
        val prompt = AdvancedMemoryEngine.buildStructuredSummaryPrompt(
            existingSummary = "已有技术选型讨论",
            transcript = "用户: 请总结当前进度。\n助手: 目前已完成底层架构迁移。",
            tokenBudget = 2000
        )

        assertTrue("提示词必须明确要求表述完整与严禁截断", prompt.contains("严禁被暴力截断或半句截断"))
        assertTrue("提示词必须要求言之有物与表述完整", prompt.contains("言之有物、表述完整、逻辑严谨"))
        assertTrue("提示词必须要求每句话有始有终", prompt.contains("每条记录必须是完整、通顺、有始有终的句子"))
        assertTrue("提示词必须包含时空演变与阶段发展", prompt.contains("【时空演变与关键时间节点（极重要，严禁遗漏）】"))
        assertTrue("提示词必须包含起始时间与总跨度", prompt.contains("起始时间与总跨度"))
        assertTrue("提示词必须强调时间概念准确性", prompt.contains("时间概念必须严密准确，严禁出现前序事件时序倒流或将数天前事件混淆为昨天的错误"))
        assertTrue("提示词必须严禁模糊为昨天", prompt.contains("严禁将早期事件模糊为“昨天”！"))
        assertTrue("提示词必须包含当前故事停顿节点", prompt.contains("当前故事停顿节点"))
        assertTrue("提示词必须强调核心脉络与未决议题", prompt.contains("核心脉络与未决议题"))
        assertTrue("提示词必须强调时间线系统紧密协同互补", prompt.contains("时间线系统紧密协同互补"))
        assertTrue("提示词必须强调避免机械复读冗长的时间节点列表", prompt.contains("避免机械复读冗长的时间节点列表"))
        assertTrue("提示词必须强调当前未决议题与待办事项", prompt.contains("当前未决议题与待办事项"))
        assertTrue("提示词必须强调整体的时间线通过记忆读取", prompt.contains("整体的时间线通过记忆读取"))
        assertTrue("提示词必须强调摘要只负责总结最近发生了什么", prompt.contains("摘要只负责总结最近发生了什么"))
        assertTrue("提示词必须强调摘要总结的内容只能和时间线最新的时间节点关联上", prompt.contains("摘要总结的内容只能和时间线最新的时间节点关联上"))
        assertTrue("提示词必须强调严禁跨越中段剧情去错误连接开场相见与最新事件等断层情节", prompt.contains("严禁跨越中段剧情去错误连接开场相见与最新事件等断层情节"))
    }

    @Test
    fun testBuildStructuredSummaryPrompt_withLatestTimelineAnchor_enforcesStrictAnchoring() {
        val prompt = AdvancedMemoryEngine.buildStructuredSummaryPrompt(
            existingSummary = "第1天 两主角在车站初次相见并达成调查约定",
            transcript = "用户: 晚上一块去吃拉面吧。\n助手: 好的，在街角那家店碰面。",
            tokenBudget = 2000,
            latestTimelineAnchor = "[第 3 天·傍晚] 调查告一段落，两人相约商业街"
        )

        assertTrue("提示词必须注入时间线最新节点", prompt.contains("【时间线最新时间节点（时序基准）】："))
        assertTrue("提示词必须包含具体时间节点内容", prompt.contains("[第 3 天·傍晚] 调查告一段落，两人相约商业街"))
        assertTrue("提示词必须包含时空锚定铁律", prompt.contains("整体时间线通过记忆读取，摘要只负责总结最近发生了什么，摘要总结的内容只能和时间线最新的时间节点关联上！"))
        assertTrue("提示词必须严禁将更早开场情节与近期事件跨段因果连接", prompt.contains("严禁将更早开场情节（如初次相见）与近期事件跨段因果连接"))
    }

    @Test
    fun testV238UserUpdatesCompleteness() {
        org.junit.Assert.assertEquals("CurrentVersionUserUpdates 必须对齐为 V238UserUpdates", com.aiassistant.ui.screens.settings.V238UserUpdates, com.aiassistant.ui.screens.settings.CurrentVersionUserUpdates)
        org.junit.Assert.assertEquals("V2.3.8 用户更新日志应有 5 项核心内容", 5, com.aiassistant.ui.screens.settings.V238UserUpdates.size)
        assertTrue("必须包含滚动摘要断层情节拼接彻底根除说明", com.aiassistant.ui.screens.settings.V238UserUpdates.any { it.contains("滚动摘要断层情节拼接彻底根除") })
        assertTrue("必须包含时间线记忆与滚动摘要职责彻底明晰说明", com.aiassistant.ui.screens.settings.V238UserUpdates.any { it.contains("时间线记忆与滚动摘要职责彻底明晰") })
        assertTrue("必须包含摘要总结内容强制锚定最新时间节点说明", com.aiassistant.ui.screens.settings.V238UserUpdates.any { it.contains("摘要总结内容强制锚定最新时间节点") })
        assertTrue("必须包含最新时间基准动态注入提炼引擎说明", com.aiassistant.ui.screens.settings.V238UserUpdates.any { it.contains("最新时间基准动态注入提炼引擎") })
        assertTrue("必须包含本地抽取式兜底严格限制近期轮次说明", com.aiassistant.ui.screens.settings.V238UserUpdates.any { it.contains("本地抽取式兜底严格限制近期轮次") })
    }

    @Test
    fun testV237UserUpdatesCompleteness() {
        org.junit.Assert.assertEquals("V2.3.7 用户更新日志应有 5 项核心内容", 5, com.aiassistant.ui.screens.settings.V237UserUpdates.size)
        assertTrue("必须包含滚动摘要标点断句保护彻底根除暴力截断说明", com.aiassistant.ui.screens.settings.V237UserUpdates.any { it.contains("滚动摘要标点断句保护彻底根除暴力截断") })
        assertTrue("必须包含滚动摘要提示词去机械化与上下文深度提炼说明", com.aiassistant.ui.screens.settings.V237UserUpdates.any { it.contains("滚动摘要提示词去机械化与上下文深度提炼") })
        assertTrue("必须包含长分析服务通道全面升级保障生成韧性说明", com.aiassistant.ui.screens.settings.V237UserUpdates.any { it.contains("长分析服务通道全面升级保障生成韧性") })
    }
}
