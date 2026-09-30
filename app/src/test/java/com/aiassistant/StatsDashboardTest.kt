package com.aiassistant

import com.aiassistant.ui.screens.stats.StatsPeriod
import com.aiassistant.ui.screens.stats.UsageRow
import com.aiassistant.ui.screens.stats.UsageSummary
import com.aiassistant.ui.screens.stats.buildBuckets
import com.aiassistant.ui.screens.stats.buildDonutSlices
import com.aiassistant.ui.screens.stats.computeDeltaPct
import com.aiassistant.ui.screens.stats.donutSweepDegrees
import com.aiassistant.ui.screens.stats.formatDeltaPct
import com.aiassistant.ui.screens.stats.formatMillis
import com.aiassistant.ui.screens.stats.formatNumber
import com.aiassistant.ui.screens.stats.toFailureSlices
import com.aiassistant.ui.screens.stats.toHourSlices
import com.aiassistant.ui.screens.stats.toProviderShares
import com.aiassistant.ui.screens.stats.toSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * v2.6.1 使用统计看板：纯统计逻辑回归
 * （概览聚合、时段分布、供应商占比、失败归纳、环比、环形图角度与格式化）
 */
class StatsDashboardTest {

    private fun row(
        timestamp: Long,
        provider: String = "openai",
        modelName: String = "gpt-test",
        inputTokens: Int = 100,
        outputTokens: Int = 200,
        thinkingTokens: Int = 0,
        cachedTokens: Int = 0,
        responseTime: Long = 500L,
        success: Boolean = true,
        errorMessage: String? = null
    ) = UsageRow(
        provider = provider,
        modelName = modelName,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        thinkingTokens = thinkingTokens,
        otherTokens = 0,
        totalTokens = inputTokens + outputTokens + thinkingTokens,
        cachedTokens = cachedTokens,
        responseTime = responseTime,
        success = success,
        timestamp = timestamp,
        errorMessage = errorMessage
    )

    // ==================== 概览聚合 ====================

    @Test
    fun testToSummary_extendedMetrics() {
        val rows = listOf(
            row(timestamp = 1L, inputTokens = 100, outputTokens = 200, cachedTokens = 50, responseTime = 400L, success = true),
            row(timestamp = 2L, inputTokens = 100, outputTokens = 200, cachedTokens = 0, responseTime = 800L, success = false, errorMessage = "HTTP 429"),
            row(timestamp = 3L, inputTokens = 100, outputTokens = 200, cachedTokens = 50, responseTime = 600L, success = true)
        )

        val summary = rows.toSummary()

        assertEquals("总请求数应为 3", 3, summary.requestCount)
        assertEquals("失败次数应为 1", 1, summary.failedCount)
        assertEquals("成功率应为 2/3", 2f / 3f, summary.successRate, 0.0001f)
        assertEquals("总 Token 应为 900", 900, summary.totalTokens)
        assertEquals("缓存命中应为 100/300", 100f / 300f, summary.cacheHitRate, 0.0001f)
        assertEquals("平均响应应为 600ms", 600L, summary.avgResponseTime)
    }

    @Test
    fun testToSummary_emptyRows() {
        val summary = emptyList<UsageRow>().toSummary()
        assertEquals(0, summary.requestCount)
        assertEquals(0, summary.failedCount)
        assertEquals(0f, summary.successRate, 0.0001f)
        assertEquals(0L, summary.avgResponseTime)
        assertEquals(0f, summary.cacheHitRate, 0.0001f)
    }

    // ==================== 24 小时分布 ====================

    @Test
    fun testToHourSlices_bucketsRequestsByLocalHour() {
        fun hourTimestamp(hour: Int, dayOffset: Int = 0): Long {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, dayOffset)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val slices = listOf(
            row(timestamp = hourTimestamp(8), inputTokens = 10, outputTokens = 20),
            row(timestamp = hourTimestamp(8, dayOffset = -1), inputTokens = 1, outputTokens = 2),
            row(timestamp = hourTimestamp(23), inputTokens = 5, outputTokens = 5)
        ).toHourSlices()

        assertEquals("必须固定返回 24 个切片", 24, slices.size)
        assertEquals("8 时应聚合 2 次调用", 2, slices[8].requestCount)
        assertEquals("8 时应聚合 33 Token", 33, slices[8].totalTokens)
        assertEquals("23 时应聚合 1 次调用", 1, slices[23].requestCount)
        assertEquals("0 时应无调用", 0, slices[0].requestCount)
        assertEquals("总调用次数应守恒", 3, slices.sumOf { it.requestCount })
    }

    // ==================== 供应商占比 ====================

    @Test
    fun testToProviderShares_sortedAndNormalized() {
        val shares = listOf(
            row(timestamp = 1L, provider = "openai", inputTokens = 300, outputTokens = 0),
            row(timestamp = 2L, provider = "anthropic", inputTokens = 100, outputTokens = 0),
            row(timestamp = 3L, provider = "openai", inputTokens = 100, outputTokens = 0)
        ).toProviderShares()

        assertEquals("去重后应只有两个供应商", 2, shares.size)
        assertEquals("占比应按消耗降序", "openai", shares[0].provider)
        assertEquals("openai 占比应为 80%", 0.8f, shares[0].share, 0.0001f)
        assertEquals("anthropic 占比应为 20%", 0.2f, shares[1].share, 0.0001f)
        assertEquals("openai 调用次数应为 2", 2, shares[0].requestCount)
    }

    @Test
    fun testToProviderShares_blankProviderAndEmptyInput() {
        val blank = listOf(row(timestamp = 1L, provider = "", inputTokens = 10)).toProviderShares()
        assertEquals("空白供应商应归入 unknown", "unknown", blank[0].provider)
        assertTrue("全部消耗在同一供应商时占比应为 1", blank[0].share > 0.99f)
        assertTrue("零消耗时应返回空列表", emptyList<UsageRow>().toProviderShares().isEmpty())
    }

    // ==================== 失败原因归纳 ====================

    @Test
    fun testToFailureSlices_groupsByFirstLineAndCounts() {
        val failures = listOf(
            row(timestamp = 1L, success = false, errorMessage = "HTTP 429: rate limited\n请稍后重试"),
            row(timestamp = 2L, success = false, errorMessage = "HTTP 429: rate limited\n其他提示"),
            row(timestamp = 3L, success = false, errorMessage = "连接超时"),
            row(timestamp = 4L, success = true, errorMessage = null)
        ).toFailureSlices()

        assertEquals("仅统计失败记录，成功不计入", 2, failures.size)
        assertEquals("高频原因应排第一", "HTTP 429: rate limited", failures[0].reason)
        assertEquals("HTTP 429 应计数 2 次", 2, failures[0].count)
        assertEquals("首行后的内容不参与分组", "连接超时", failures[1].reason)
    }

    @Test
    fun testToFailureSlices_emptyAndReasonCappedAtFour() {
        assertTrue("无失败记录时应返回空", emptyList<UsageRow>().toFailureSlices().isEmpty())
        val many = (1..6).map { idx ->
            row(timestamp = idx.toLong(), success = false, errorMessage = "错误 $idx")
        }.toFailureSlices()
        assertEquals("最多返回 4 条归纳", 4, many.size)
        assertTrue("全部等频时按文案稳定排序", many[0].count == 1)
    }

    // ==================== 环比 ====================

    @Test
    fun testComputeDeltaPct() {
        assertEquals("+25% 场景", 25f, computeDeltaPct(125L, 100L)!!, 0.0001f)
        assertEquals("-50% 场景", -50f, computeDeltaPct(50L, 100L)!!, 0.0001f)
        assertEquals("持平为 0", 0f, computeDeltaPct(100L, 100L)!!, 0.0001f)
        assertNull("上一周期无数据时返回 null", computeDeltaPct(100L, 0L))
    }

    @Test
    fun testFormatDeltaPct() {
        assertEquals("+12.3%", formatDeltaPct(12.34f))
        assertEquals("-8.0%", formatDeltaPct(-8.0f))
        assertEquals("+0.0%", formatDeltaPct(0f))
    }

    // ==================== 环形图 ====================

    @Test
    fun testDonutSweepDegrees_proportionalWithGaps() {
        val sweeps = donutSweepDegrees(listOf(50, 25, 25, 0))
        assertEquals("4 个输入对应 4 个扫角", 4, sweeps.size)
        assertEquals("零值切片扫角为 0", 0f, sweeps[3], 0.0001f)
        assertEquals("50% 切片应占一半可用角度", (360f - DonutGapDegreesForTest * 3f) / 2f, sweeps[0], 0.01f)
        val totalSweep = sweeps.sum() + DonutGapDegreesForTest * 3f
        assertEquals("扫角 + 间隙应恰好铺满 360°", 360f, totalSweep, 0.01f)
    }

    @Test
    fun testDonutSweepDegrees_emptyAndSingle() {
        assertTrue("全零输入返回空", donutSweepDegrees(listOf(0, 0, 0, 0)).isEmpty())
        val single = donutSweepDegrees(listOf(0, 100, 0, 0))
        assertEquals("单一非零切片应铺满除间隙外的整圆", 360f - DonutGapDegreesForTest, single[1], 0.0001f)
    }

    @Test
    fun testBuildDonutSlices_otherIsResidual() {
        val summary = UsageSummary(
            totalTokens = 1000,
            inputTokens = 400,
            outputTokens = 300,
            thinkingTokens = 200,
            requestCount = 5,
            cacheHitRate = 0f,
            successRate = 1f
        )
        val slices = buildDonutSlices(summary)
        assertEquals("其他 Token 应为残差 100", 100, slices[3].value)
        assertEquals("四个切片总和应等于总量", 1000, slices.sumOf { it.value })
    }

    // ==================== 分桶与格式化 ====================

    @Test
    fun testBuildBuckets_includesRequestCount() {
        val endTime = System.currentTimeMillis()
        val rows = listOf(
            row(timestamp = endTime - 1000L),
            row(timestamp = endTime - 2000L)
        )
        val buckets = buildBuckets(rows, StatsPeriod.Hour, endTime)
        assertEquals("1 小时周期应有 12 个分桶", 12, buckets.size)
        assertEquals("最近一桶应包含 2 次调用", 2, buckets.last().requestCount)
    }

    @Test
    fun testFormatMillis() {
        assertEquals("850ms", formatMillis(850L))
        assertEquals("1.5s", formatMillis(1500L))
        assertEquals("2.0min", formatMillis(120_000L))
        assertEquals("缺失耗时显示 —", "—", formatMillis(0L))
    }

    @Test
    fun testFormatNumber() {
        assertEquals("999", formatNumber(999))
        assertEquals("1.5K", formatNumber(1_500))
        assertEquals("2.0M", formatNumber(2_000_000))
    }

    private companion object {
        const val DonutGapDegreesForTest = 3f
    }
}
