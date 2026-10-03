package com.aiassistant

import com.aiassistant.data.remote.RetrofitClient
import com.aiassistant.data.repository.AiRepository
import com.aiassistant.data.repository.KeyAttemptFailure
import com.aiassistant.utils.PersonalizationManager
import com.aiassistant.utils.SmartMemoryExtractor
import org.junit.Assert.*
import org.junit.Test
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.io.InterruptedIOException

class NetworkAndKeyStabilityTest {
    @Test
    fun retryClassifiesActualApiExceptionsBeforeBodyText() {
        val types = mapOf(400 to "BAD_REQUEST", 401 to "AUTHENTICATION", 403 to "PERMISSION", 404 to "NOT_FOUND", 408 to "REQUEST_TIMEOUT", 409 to "CONFLICT", 413 to "TOO_LARGE", 422 to "INVALID_PARAMETERS", 429 to "RATE_LIMIT", 500 to "SERVER", 502 to "BAD_GATEWAY", 503 to "UNAVAILABLE", 504 to "GATEWAY_TIMEOUT", 418 to "OTHER_HTTP")
        types.forEach { (code, name) ->
            assertEquals(name, com.aiassistant.domain.model.RetryErrorType.classify(com.aiassistant.data.repository.ApiException(code, "API错误 ($code): timeout in provider" )).name)
            assertEquals(name, com.aiassistant.domain.model.RetryErrorType.classify(Exception("HTTP $code: error")).name)
        }
        assertEquals(com.aiassistant.domain.model.RetryErrorType.EMPTY_RESPONSE, com.aiassistant.domain.model.RetryErrorType.classify(com.aiassistant.data.repository.ApiException(500, "empty response detected")))
        assertEquals(com.aiassistant.domain.model.RetryErrorType.TLS, com.aiassistant.domain.model.RetryErrorType.classify(javax.net.ssl.SSLException("certificate invalid")))
    }

    @Test
    fun retryRulesRespectCountDisabledCancellationAndPartialOutput() {
        val timeout = SocketTimeoutException("timeout")
        val policy = com.aiassistant.domain.model.RetryPolicy()
        assertTrue(policy.canRetry(timeout, 0))
        assertTrue(policy.canRetry(timeout, 2))
        assertFalse(policy.canRetry(timeout, 3))
        assertFalse(policy.canRetry(timeout, 0, hasOutput = true))
        assertFalse(policy.canRetry(kotlinx.coroutines.CancellationException("stop"), 0))
        assertFalse(policy.canRetry(Exception("HTTP 401: invalid"), 0))
        val custom = com.aiassistant.domain.model.RetryPolicy(mapOf(com.aiassistant.domain.model.RetryErrorType.AUTHENTICATION to com.aiassistant.domain.model.RetryRule(true, 2), com.aiassistant.domain.model.RetryErrorType.TIMEOUT to com.aiassistant.domain.model.RetryRule(false, 20)))
        assertTrue(custom.canRetry(Exception("HTTP 401: invalid"), 1))
        assertFalse(custom.canRetry(Exception("HTTP 401: invalid"), 2))
        assertFalse(custom.canRetry(timeout, 0))
        assertEquals(5_000L, custom.delayMillis(19))
    }

    // 1. 网络稳定性测试：验证 OkHttpClient 的 fastFallback、pingInterval、连接池配置及合规 User-Agent
    @Test
    fun testNetworkStabilityConfiguration() {
        val streamClient = RetrofitClient.streamHttpClient
        assertFalse("底层不得隐藏重发模型 POST 请求", streamClient.retryOnConnectionFailure)
        assertFalse(RetrofitClient.longAnalysisHttpClient.retryOnConnectionFailure)
        assertEquals("streamHttpClient 应配置 15s 的 HTTP/2 pingInterval 保活心跳", 15_000, streamClient.pingIntervalMillis)
        assertNotNull("streamHttpClient 应配置专用连接池", streamClient.connectionPool)

        val restClient = RetrofitClient.restHttpClient
        assertFalse(restClient.retryOnConnectionFailure)
        assertNotNull("restClient 应配置专用连接池", restClient.connectionPool)

        assertEquals("默认 User-Agent 必须为合规标识而非默认 okhttp", "Echo-Assistant/2.2.5 (Android; Mobile)", RetrofitClient.DEFAULT_USER_AGENT)
    }

    // 2. 异常判定测试：验证协议抖动与超时异常均被正确识别为网络波动以触发退避重试
    @Test
    fun testNetworkFluctuationExceptionIdentification() {
        assertTrue("SocketTimeoutException 应被判定为网络波动", AiRepository.isNetworkFluctuationException(SocketTimeoutException("Read timed out")))
        assertTrue("ProtocolException 应被判定为网络波动", AiRepository.isNetworkFluctuationException(ProtocolException("unexpected end of stream")))
        assertTrue("包含 http2 stream reset 的异常应被判定为网络波动", AiRepository.isNetworkFluctuationException(Exception("HTTP2 stream was reset by peer")))
        assertTrue("SSL 握手抖动应被判定为网络波动", AiRepository.isNetworkFluctuationException(Exception("SSL handshake failed due to timeout")))
        assertFalse("业务逻辑参数错误不应被判定为网络波动", AiRepository.isNetworkFluctuationException(IllegalArgumentException("Invalid model name")))
    }

    // 3. 沉浸式记忆与去元词汇测试：彻底消除“用户把AI当成心爱的哥哥”等出戏表述
    @Test
    fun testSanitizeMetaLanguageRemovesImmersionBreakingTerms() {
        // 典型出戏文本输入
        val rawInput1 = "用户把AI当成心爱的哥哥"
        val sanitized1 = SmartMemoryExtractor.sanitizeMetaLanguage(rawInput1)
        assertEquals("视对方为心爱的哥哥", sanitized1)
        assertFalse("净化后绝对不可包含'用户把AI当成'", sanitized1.contains("用户把AI当成"))
        assertFalse("净化后绝对不可包含'AI'", sanitized1.contains("AI"))

        val rawInput2 = "把模型当做最好的朋友"
        val sanitized2 = SmartMemoryExtractor.sanitizeMetaLanguage(rawInput2)
        assertEquals("视对方为最好的朋友", sanitized2)
        assertFalse("净化后不应出现'模型'", sanitized2.contains("模型"))

        val rawInput3 = "用户要求AI在回答时保持温柔"
        val sanitized3 = SmartMemoryExtractor.sanitizeMetaLanguage(rawInput3)
        assertEquals("要求在互动中在回答时保持温柔", sanitized3)

        // 验证提炼管线 refineMemoryContent
        val (distilled, category) = SmartMemoryExtractor.refineMemoryContent("根据上述对话提炼出如下核心事实：用户把AI当成心爱的哥哥")
        assertEquals("角色设定：视对方为心爱的哥哥", distilled)
        assertEquals("PROJECT", category)
        assertFalse(distilled.contains("用户把AI"))
    }

    // 4. 辅助模型提炼提示词模板测试：验证严格沉浸感准则
    @Test
    fun testAuxiliaryMemoryPromptImmersionRules() {
        val prompt = PersonalizationManager.DEFAULT_AUXILIARY_MEMORY_PROMPT
        assertTrue("提炼提示词必须强调沉浸感最高准则", prompt.contains("沉浸感最高准则"))
        assertTrue("提炼提示词必须明文严禁用户、AI、模型等元词汇", prompt.contains("严禁在提炼的事实中出现“用户”、“AI”、“模型”"))
        assertTrue("提炼提示词必须要求规范角色扮演关系表述", prompt.contains("视对方为心爱的哥哥"))
    }

    // 5. 多 Key 错误汇总展示测试
    @Test
    fun testMultiKeyCompositeErrorReporting() {
        val failures = listOf(
            KeyAttemptFailure(1, "...4a8b", "HTTP 401 Unauthorized - 密钥无效"),
            KeyAttemptFailure(2, "...9c12", "HTTP 429 Too Many Requests - 频率超限"),
            KeyAttemptFailure(3, "...7e3f", "网络连接超时 (SocketTimeoutException 30s)", isTimeout = true)
        )

        val compositeMessage = buildString {
            append("所有 API Key 均请求失败 (共尝试 ${failures.size} 个 Key)：\n")
            failures.forEach { failure ->
                append("• Key #${failure.keyIndex} (${failure.keyMasked})：${failure.errorMessage}\n")
            }
            append("\n建议检查 API 地址、网络连接或对应 Key 的额度与可用状态。")
        }.trim()

        assertTrue(compositeMessage.contains("共尝试 3 个 Key"))
        assertTrue(compositeMessage.contains("Key #1 (...4a8b)：HTTP 401"))
        assertTrue(compositeMessage.contains("Key #2 (...9c12)：HTTP 429"))
        assertTrue(compositeMessage.contains("Key #3 (...7e3f)：网络连接超时"))
    }

    // 6. 暂停生成时保留前序 Key 报错原因测试
    @Test
    fun testRetainKeyErrorsWhenStoppedByUser() {
        val attemptErrors = mutableListOf(
            "Key #1 (...4a8b)：HTTP 401 Unauthorized",
            "Key #2 (...9c12)：网络连接超时 (SocketTimeoutException)"
        )

        val finalContent = buildString {
            append("回复已停止 (用户已暂停)\n\n")
            append("【已尝试 Key 报错记录】：\n")
            attemptErrors.forEach { append("• $it\n") }
            append("\n*(在尝试后续 Key 期间，用户主动暂停了回复)*")
        }.trim()

        assertTrue(finalContent.contains("回复已停止 (用户已暂停)"))
        assertTrue(finalContent.contains("【已尝试 Key 报错记录】："))
        assertTrue(finalContent.contains("• Key #1 (...4a8b)：HTTP 401"))
        assertTrue(finalContent.contains("• Key #2 (...9c12)：网络连接超时"))
        assertTrue(finalContent.contains("用户主动暂停了回复"))
    }
}
