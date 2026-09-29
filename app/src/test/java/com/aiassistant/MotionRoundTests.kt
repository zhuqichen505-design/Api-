package com.aiassistant

import com.aiassistant.ui.components.MarkdownStableSplit
import com.aiassistant.ui.components.computeStableSplit
import com.aiassistant.ui.screens.chat.GenerationUiState
import com.aiassistant.ui.screens.chat.GenerationUiStateRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 动效轮（UI-微交互与动效深化方案.md）核心红线测试：
 * - R-2：Markdown 增量解析稳定点单调性与围栏/数学块安全
 * - P0-4：GenerationUiState 状态机派生行为等价
 */
class MotionRoundTests {

    // ---------- R-2：稳定点切分 ----------

    @Test
    fun testStableSplit_emptyContent() {
        val split = computeStableSplit("")
        assertEquals("", split.stable)
        assertEquals("", split.tail)
        assertFalse(split.tailInFence)
    }

    @Test
    fun testStableSplit_monotonicGrowth_overTokenAppends() {
        // 模拟流式：逐 token 追加，稳定前缀必须单调不减（R-2 核心）
        val full = buildString {
            append("# 标题\n\n")
            append("第一段落内容，包含 **加粗** 与 *斜体* 片段。\n\n")
            append("- 列表项一\n- 列表项二\n\n")
            append("第二段落收尾文字。")
        }
        var lastStable = ""
        var step = 8
        while (step <= full.length) {
            val partial = full.substring(0, step)
            val split = computeStableSplit(partial)
            assertTrue(
                "稳定前缀在 step=$step 回退：'$lastStable' -> '${split.stable}'",
                split.stable.startsWith(lastStable)
            )
            assertTrue("稳定+尾必须还原原文", (split.stable + split.tail) == partial)
            lastStable = split.stable
            step += 8
        }
        val finalSplit = computeStableSplit(full)
        assertTrue(finalSplit.stable.startsWith(lastStable))
    }

    @Test
    fun testStableSplit_unclosedFence_keepsTailInFence() {
        // 围栏未闭合：不得把围栏内的空行当稳定点
        val content = "代码开始：\n\n```kotlin\nfun main() {\n\n    val x = 1\n"
        val split = computeStableSplit(content)
        assertTrue("未闭合围栏内不应产生稳定前缀中的围栏内容", !split.stable.contains("fun main()"))
        assertTrue(split.tailInFence)
        // 尾部含围栏开启行，渲染侧负责剥离开启行后以等宽体预览
        assertEquals("```kotlin\nfun main() {\n\n    val x = 1\n", split.tail)
    }

    @Test
    fun testStableSplit_closedFence_becomesStable() {
        val content = "段落一。\n\n```python\nprint('hi')\n```\n\n后续段落。"
        val split = computeStableSplit(content)
        assertTrue("闭合围栏整体应进入稳定前缀", split.stable.contains("print('hi')"))
        assertFalse(split.tailInFence)
        assertTrue(split.stable + split.tail == content)
    }

    @Test
    fun testStableSplit_unclosedMathBlock_notSplit() {
        // $$ 块未闭合期间不得在块内空行处切分（公式跨块撕裂防护）
        val content = "前置段落。\n\n$$\nE = mc^2\n\n\\int_0^1 x dx\n"
        val split = computeStableSplit(content)
        assertTrue(split.stable.contains("前置段落。"))
        assertFalse("公式内容不得进入稳定前缀", split.stable.contains("E = mc^2"))
        assertTrue(split.tail.contains("E = mc^2"))
    }

    @Test
    fun testStableSplit_trailingBlankLine_keepsTailNonEmpty() {
        val content = "段落一。\n\n"
        val split = computeStableSplit(content)
        // 内容以空行结尾：空行边界进入稳定段，尾部为空（该帧光标隐藏，属预期）
        assertEquals("段落一。\n\n", split.stable)
        assertEquals("", split.tail)
        assertFalse(split.tailInFence)
    }

    @Test
    fun testStableSplit_tableBlock_staysIntact() {
        val content = "| A | B |\n|---|---|\n| 1 | 2 |\n\n下一段。"
        val split = computeStableSplit(content)
        // 表格行之间无空行 → 表格要么整体在尾部，要么整体在稳定区，不得拦腰切断
        val tableInStable = split.stable.contains("| A | B |")
        val tableInTail = split.tail.contains("| A | B |")
        assertTrue("表格必须完整位于一侧", tableInStable != tableInTail)
    }

    // ---------- P0-4：状态机派生 ----------

    @Test
    fun testGenerationState_idle() {
        assertEquals(
            GenerationUiState.Idle,
            GenerationUiStateRules.derive(false, "已有内容", true, null)
        )
    }

    @Test
    fun testGenerationState_connecting() {
        assertEquals(
            GenerationUiState.Connecting,
            GenerationUiStateRules.derive(true, "", false, null)
        )
    }

    @Test
    fun testGenerationState_reconnecting() {
        assertEquals(
            GenerationUiState.Reconnecting,
            GenerationUiStateRules.derive(true, "", false, "网络波动，正在重连…")
        )
    }

    @Test
    fun testGenerationState_thinking() {
        assertEquals(
            GenerationUiState.Thinking,
            GenerationUiStateRules.derive(true, "", true, null)
        )
    }

    @Test
    fun testGenerationState_streaming() {
        assertEquals(
            GenerationUiState.Streaming,
            GenerationUiStateRules.derive(true, "部分正文", true, null)
        )
        assertEquals(
            GenerationUiState.Streaming,
            GenerationUiStateRules.derive(true, "正文", false, null)
        )
    }

    @Test
    fun testGenerationState_failed_viaReconnectErrorText() {
        // 行为等价：reconnect 文案命中错误词 → Failed（原 capsule contains 规则）
        assertEquals(
            GenerationUiState.Failed,
            GenerationUiStateRules.derive(true, "", false, "连接异常，HTTP 500")
        )
        assertEquals(
            GenerationUiState.Failed,
            GenerationUiStateRules.derive(true, "", false, "请求 Error: timeout")
        )
    }

    @Test
    fun testGenerationState_failed_viaContentError() {
        // v2.5.7 错误报告全包裹：内容命中 isErrorMessage 语义 → Failed 优先
        assertEquals(
            GenerationUiState.Failed,
            GenerationUiStateRules.derive(true, "请求失败：所有 API Key 均不可用", false, null, contentIsError = true)
        )
    }

    @Test
    fun testErrorText_rulesMatchOriginalCapsuleKeywords() {
        assertTrue(GenerationUiStateRules.isErrorText("包含异常"))
        assertTrue(GenerationUiStateRules.isErrorText("包含报错"))
        assertTrue(GenerationUiStateRules.isErrorText("任务失败"))
        assertTrue(GenerationUiStateRules.isErrorText("出现错误"))
        assertTrue(GenerationUiStateRules.isErrorText("an Error occurred"))
        assertTrue(GenerationUiStateRules.isErrorText("HTTP 403"))
        assertFalse(GenerationUiStateRules.isErrorText("正常重连中，请稍候"))
    }
}
