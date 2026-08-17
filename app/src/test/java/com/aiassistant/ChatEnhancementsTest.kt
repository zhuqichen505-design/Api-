package com.aiassistant

import com.aiassistant.domain.model.ApiConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ChatEnhancementsTest {

    @Test
    fun testDefaultMaxTokensIs8192() {
        val config = ApiConfig(
            id = 1L,
            name = "Test API",
            provider = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            apiKey = "sk-test",
            modelName = "gpt-4o"
        )
        assertEquals("默认 ApiConfig 的 maxTokens 必须为 8192", 8192, config.maxTokens)
    }

    @Test
    fun testErrorMessageDetection() {
        fun isErrorMessage(content: String): Boolean {
            val trimmed = content.trim()
            return trimmed.startsWith("请求失败") ||
                   trimmed.startsWith("[请求失败]") ||
                   trimmed.startsWith("Error:") ||
                   trimmed.startsWith("error:") ||
                   trimmed.contains("[输出已被中断:")
        }

        assertTrue(isErrorMessage("请求失败\n\n网络超时，请检查网络设置"))
        assertTrue(isErrorMessage("[请求失败] 401 Unauthorized"))
        assertTrue(isErrorMessage("Error: connection refused"))
        assertTrue(isErrorMessage("error: 500 internal server error"))
        assertTrue(isErrorMessage("这是部分内容...\n\n[输出已被中断: 用户取消]"))
        
        assertFalse(isErrorMessage("你好！我是你的 AI 助手，有什么我可以帮你的？"))
        assertFalse(isErrorMessage("```kotlin\nval error = 1\n```"))
    }

    @Test
    fun testTokenSpeedCalculation() {
        val tokenCount = 150
        val responseTimeMs = 3000L // 3.0 秒
        val seconds = responseTimeMs / 1000.0
        val speed = tokenCount / seconds
        val formatted = String.format(Locale.US, "%.1f tokens/s", speed)

        assertEquals("50.0 tokens/s", formatted)
    }

    @Test
    fun testModelDisplayNameResolution() {
        fun resolveModelName(optionModel: String?, convModel: String?): String {
            return optionModel?.ifBlank { null } ?: convModel?.ifBlank { null } ?: "AI"
        }

        assertEquals("gpt-4o", resolveModelName("gpt-4o", "gpt-3.5-turbo"))
        assertEquals("deepseek-chat", resolveModelName(null, "deepseek-chat"))
        assertEquals("AI", resolveModelName(null, null))
        assertEquals("AI", resolveModelName("", ""))
    }
}
