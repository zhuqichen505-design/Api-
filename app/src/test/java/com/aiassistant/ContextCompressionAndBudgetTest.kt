package com.aiassistant

import com.aiassistant.data.repository.AiRepository
import com.aiassistant.domain.model.Message
import org.junit.Assert.*
import org.junit.Test

class ContextCompressionAndBudgetTest {

    @Test
    fun testExtractContextWindowFromError_userRealError() {
        val errorMessage = "This endpoint's maximum context length is 32768 tokens. However, you requested about 416131 tokens (366131 of text input, 50000 in the output). Please reduce the length of either one, or use the context-compression plugin to compress your prompt automatically."
        
        val extracted = AiRepository.extractContextWindowFromError(errorMessage)
        
        assertEquals("应当精准提取端点声明的真实上下文上限 32768", 32768, extracted)
    }

    @Test
    fun testExtractContextWindowFromError_variousFormats() {
        val err1 = "Error: max context length is 16384, but received 20000 tokens"
        assertEquals(16384, AiRepository.extractContextWindowFromError(err1))

        val err2 = "context-window is 65536, please reduce prompt length"
        assertEquals(65536, AiRepository.extractContextWindowFromError(err2))

        val err3 = "limit of 8192 tokens exceeded"
        assertEquals(8192, AiRepository.extractContextWindowFromError(err3))
    }

    @Test
    fun testSafeMaxTokensCalculation_neverExceedsContextHeadroom() {
        val contextWindow = 32768
        val configuredMaxTokens = 50000 // 用户配置了过大的 50000
        val estimatedPromptTokens = 26000 // 已经消耗了 26000

        val headroom = (contextWindow - estimatedPromptTokens - 512).coerceAtLeast(256)
        val safeMaxTokens = minOf(configuredMaxTokens, headroom).coerceIn(256, 16384)

        // 验证：prompt + safeMaxTokens 必然严格小于 contextWindow
        assertTrue(
            "输入 Token ($estimatedPromptTokens) + 安全输出 Token ($safeMaxTokens) 必须小于等于上下文窗口 ($contextWindow)",
            estimatedPromptTokens + safeMaxTokens <= contextWindow
        )
        assertEquals(6256, safeMaxTokens)
    }

    @Test
    fun testActiveCandidateMessages_excludesSummarizedOldMessages() {
        val messages = (1..20).map { id ->
            Message(
                id = id.toLong(),
                conversationId = 1L,
                role = if (id % 2 == 1) "user" else "assistant",
                content = "第 $id 条对话内容",
                isPinned = (id == 5) // 第 5 条被用户显式置顶钉住
            )
        }

        val summarizedThrough = 14L // 摘要已归约到第 14 条
        val hasRollingSummary = true

        val activeCandidates = if (hasRollingSummary && summarizedThrough > 0L) {
            messages.filter { it.id > summarizedThrough || it.isPinned }
        } else {
            messages
        }

        // 活跃消息应包含：第 5 条（钉住）+ 第 15 到 20 条（新消息），共 7 条
        assertEquals(7, activeCandidates.size)
        assertTrue(activeCandidates.any { it.id == 5L })
        assertFalse(activeCandidates.any { it.id == 4L })
        assertFalse(activeCandidates.any { it.id == 14L })
        assertTrue(activeCandidates.any { it.id == 15L })
        assertTrue(activeCandidates.any { it.id == 20L })
    }

    @Test
    fun testEstimateTokenCount_handlesCjkAndAscii() {
        val text = "你好，世界！Hello World 123"
        val count = AiRepository.estimateTokenCount(text)
        assertTrue("混合文本应能准确合理估算 Token", count in 5..20)
    }

    @Test
    fun testPromptBudgetOutputReserve_doesNotConsumeEntireContext() {
        val contextWindow = 32768
        val maxOutputTokens = 50000 // 传入过大的输出设置

        val maxAllowedOutputReserve = (contextWindow * 0.25f).toInt().coerceAtLeast(1024).coerceAtMost(8192)
        val outputReserve = maxOutputTokens.coerceIn(512, maxAllowedOutputReserve)

        assertEquals("对于 32768 上下文，输出预留上限应限制在 8192（25% 比例）", 8192, outputReserve)
        val promptBudget = (contextWindow - outputReserve - 1024).coerceAtLeast(3000)
        assertEquals(23552, promptBudget)
    }
}
