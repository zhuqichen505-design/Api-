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
    fun testThinkingCapsuleTextFormatting() {
        fun formatCapsule(isThinkingActive: Boolean, modelName: String, timeMs: Long, thinkingTokens: Int, totalTokens: Int): String {
            return when {
                isThinkingActive -> "模型正在思考中"
                thinkingTokens > 0 -> "$modelName 已深度思考 (${timeMs / 1000.0}s, $thinkingTokens tokens)"
                else -> modelName
            }
        }

        assertEquals("模型正在思考中", formatCapsule(true, "DeepSeek", 0L, 0, 0))
        assertEquals("DeepSeek 已深度思考 (2.5s, 350 tokens)", formatCapsule(false, "DeepSeek", 2500L, 350, 500))
        assertEquals("GPT-4o", formatCapsule(false, "GPT-4o", 1200L, 0, 150))
    }

    @Test
    fun testHtmlEntityDecoding() {
        fun decodeEntities(input: String): String {
            return input
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
        }

        val htmlText = "&lt;div class=&quot;box&quot;&gt;Hello &amp; Welcome&lt;/div&gt;"
        assertEquals("<div class=\"box\">Hello & Welcome</div>", decodeEntities(htmlText))
    }

    @Test
    fun testMarkdownHeadingLevels() {
        fun parseHeadingLevel(line: String): Int {
            return when {
                line.startsWith("###### ") -> 6
                line.startsWith("##### ") -> 5
                line.startsWith("#### ") -> 4
                line.startsWith("### ") -> 3
                line.startsWith("## ") -> 2
                line.startsWith("# ") -> 1
                else -> 0
            }
        }

        assertEquals(1, parseHeadingLevel("# 一级标题"))
        assertEquals(5, parseHeadingLevel("##### 五级标题"))
        assertEquals(6, parseHeadingLevel("###### 六级标题"))
        assertEquals(0, parseHeadingLevel("普通文本"))
    }
}
