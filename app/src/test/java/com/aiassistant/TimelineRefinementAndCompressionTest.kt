package com.aiassistant

import com.aiassistant.domain.model.Message
import com.aiassistant.utils.*
import org.junit.Assert.*
import org.junit.Test

class TimelineRefinementAndCompressionTest {

    // 1. 日内时段细分状态机测试：验证 DayPhase 推断与防突兀跨越时段
    @Test
    fun testDayPhaseDetectionAndDescriptions() {
        val morningText = "晨光微熹，两人一起坐在餐桌前吃着热气腾腾的早餐。"
        val phaseMorning = DayPhase.inferFromText(morningText)
        assertNotNull("吃早餐、晨光应当推断为早晨时段", phaseMorning)
        assertEquals(DayPhase.EARLY_MORNING, phaseMorning)
        assertEquals("清晨/早晨", phaseMorning?.displayName)

        val afternoonText = "午后的阳光透过窗棂洒在书桌上，两人喝着红茶聊起接下来的打算。"
        val phaseAfternoon = DayPhase.inferFromText(afternoonText)
        assertEquals(DayPhase.AFTERNOON, phaseAfternoon)
        assertEquals("下午", phaseAfternoon?.displayName)

        val duskText = "夕阳西下，天边的晚霞被染成金红色，两人漫步在海边。"
        val phaseDusk = DayPhase.inferFromText(duskText)
        assertEquals(DayPhase.DUSK, phaseDusk)
        assertEquals("傍晚/黄昏", phaseDusk?.displayName)

        val nightText = "夜幕降临，街灯初上，两人互道晚安准备入睡。"
        val phaseNight = DayPhase.inferFromText(nightText)
        assertEquals(DayPhase.NIGHT, phaseNight)
        assertEquals("入夜/晚间", phaseNight?.displayName)

        val lateNightText = "时钟指向凌晨两点，窗外一片寂静。"
        val phaseLateNight = DayPhase.inferFromText(lateNightText)
        assertEquals(DayPhase.LATE_NIGHT, phaseLateNight)
        assertEquals("深夜/拂晓", phaseLateNight?.displayName)
    }

    // 2. 时空看板与时序守护上下文构建测试：验证绝对跨度、防“昨天”漂移与日序铁律
    @Test
    fun testBuildTimelinePromptContextWithMilestoneAndPhaseGuard() {
        val memoryContents = listOf(
            "[第1天] 男女主正式确立恋爱关系",
            "[第3天 下午] 两人共同参加了大学同学聚会",
            "[第5天 清晨] 两人吃完早餐商量周末旅行计划"
        )

        val currentStoryTime = "第5天 清晨"
        val contextPrompt = TimelineMemoryHelper.buildTimelinePromptContext(currentStoryTime, memoryContents)

        // 验证置顶看板
        assertTrue("必须包含故事当前时间节点与时空看板", contextPrompt.contains("【故事当前时间节点与时空看板】"))
        assertTrue("必须标明当前故事时间为第5天 清晨", contextPrompt.contains("第5天 清晨"))

        // 验证关键里程碑防漂移看板（第1天确立关系，当前第5天，跨度为4天，绝非昨天）
        assertTrue("必须包含关键里程碑置顶防漂移看板", contextPrompt.contains("核心关系与重大里程碑锚点（绝对禁止混淆时序！）："))
        assertTrue("确立关系里程碑必须计算并提示距今跨度", contextPrompt.contains("距今已过去 4 天") && contextPrompt.contains("绝非昨天！"))

        // 验证日内时序守护铁律
        assertTrue("必须包含时空连贯性与日内时序守护铁律", contextPrompt.contains("【时空连贯性与日内时序守护铁律"))
        assertTrue("当前为清晨时必须明确指示日内生理与时序连贯要求", contextPrompt.contains("当前故事时段停留在【清晨/早晨】"))
        assertTrue("严禁无过渡突兀跳跃至天黑入睡", contextPrompt.contains("严格禁止在未描写数小时时间自然流逝"))
    }

    // 3. 事件增量去重与智能润色合并测试：多轮同一事件合并，不同事件正常追加
    @Test
    fun testMergeOrAppendEventDeduplication() {
        val existingEvents = listOf(
            TimelineEventItem(
                timeTag = "第1天 傍晚",
                content = "男女主在车站告别",
                category = TimelineCategory.PLOT_EVENT
            )
        )

        // 情况一：同一天傍晚，相同核心语义“在车站告别”，但提供了更丰富的细节描述
        val enrichedEvent = TimelineEventItem(
            timeTag = "第1天 傍晚",
            content = "男女主在车站依依不舍告别，并约定周末再次见面",
            category = TimelineCategory.PLOT_EVENT
        )

        val mergedEvents = TimelineMemoryHelper.mergeOrAppendEvent(existingEvents, enrichedEvent)
        assertEquals("事件总数不应增加，保持为 1", 1, mergedEvents.size)
        assertEquals("事件应更新为细节更丰富的描述", "男女主在车站依依不舍告别，并约定周末再次见面", mergedEvents[0].content)

        // 情况二：不同天或不同事件，应当追加
        val newDayEvent = TimelineEventItem(
            timeTag = "第2天 上午",
            content = "男主收到女主发来的早安问候",
            category = TimelineCategory.PLOT_EVENT
        )

        val appendedEvents = TimelineMemoryHelper.mergeOrAppendEvent(mergedEvents, newDayEvent)
        assertEquals("追加后事件总数应为 2", 2, appendedEvents.size)
        assertEquals("第二条事件时间锚点经标准化为第 2 天·上午", "第 2 天·上午", appendedEvents[1].timeTag)
    }

    // 4. 长对话切片分段与滑动窗口测试：验证分段数量与连续性重叠
    @Test
    fun testChunkMessagesForAnalysisWithOverlap() {
        val dummyMessages = (1..60).map { i ->
            Message(
                id = i.toLong(),
                conversationId = 1L,
                role = if (i % 2 == 1) "user" else "assistant",
                content = "第 $i 轮对话内容，测试时间线推进与剧情发展..."
            )
        }

        val chunks = TimelineMemoryHelper.chunkMessagesForAnalysis(dummyMessages, chunkSize = 25, overlap = 3)
        assertEquals("60 条消息按 25 条一组、3 条重叠应当被切为 3 个片段", 3, chunks.size)

        // 验证各片段大小
        assertEquals("第一段包含前 25 条消息", 25, chunks[0].size)
        assertEquals("第一段起始消息 ID 为 1", 1L, chunks[0].first().id)
        assertEquals("第一段结束消息 ID 为 25", 25L, chunks[0].last().id)

        // 验证滑动窗口重叠：第二段应当从 25 - 3 = 22 开始，即 ID 为 23
        assertEquals("第二段起始消息应当与第一段重叠 3 条（ID 为 23）", 23L, chunks[1].first().id)
        assertEquals("第二段包含 25 条消息", 25, chunks[1].size)
        assertEquals("第二段结束消息 ID 为 47", 47L, chunks[1].last().id)

        // 第三段从 47 - 3 = 44 开始，即 ID 为 45，直到 60
        assertEquals("第三段起始消息应当与第二段重叠 3 条（ID 为 45）", 45L, chunks[2].first().id)
        assertEquals("第三段包含剩余 16 条消息", 16, chunks[2].size)
        assertEquals("第三段最后一条消息 ID 为 60", 60L, chunks[2].last().id)
    }

    // 5. 滚动摘要提示词升级测试：验证包含【时空演变与关键时间节点】与绝对天数强制要求
    @Test
    fun testBuildStructuredSummaryPromptContainsTimeAnchors() {
        val prompt = AdvancedMemoryEngine.buildStructuredSummaryPrompt(
            existingSummary = "第1天 男女主确立关系",
            transcript = "第3天 两人去公园散步",
            tokenBudget = 800
        )

        assertTrue("提示词必须包含【时空演变与关键时间节点】要求", prompt.contains("【时空演变与关键时间节点（极重要，严禁遗漏）】"))
        assertTrue("必须强制要求记录故事起始点与总跨度天数", prompt.contains("起始时间与总跨度"))
        assertTrue("必须严厉禁止将早期事件模糊为昨天", prompt.contains("严禁将早期事件模糊为“昨天”！"))
        assertTrue("必须保留当前故事停顿节点", prompt.contains("当前故事停顿节点"))
        assertTrue("要求中必须强调时间概念准确性", prompt.contains("时间概念必须严密准确，严禁出现前序事件时序倒流或将数天前事件混淆为昨天的错误"))
    }

    // 6. 个性化配置字段测试：验证时间线自动更新开关默认状态
    @Test
    fun testPersonalizationSettingsAutoTimelineDefaults() {
        val defaultSettings = PersonalizationSettings()
        assertTrue("默认应开启每次对话后自动根据当前对话判断更新时间线", defaultSettings.autoTimelineEnabled)
        assertTrue("默认应开启时间线自动更新提醒", defaultSettings.autoTimelineNoticeEnabled)
    }
}
