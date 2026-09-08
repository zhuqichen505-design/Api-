package com.aiassistant

import com.aiassistant.domain.model.ApiConfig
import com.aiassistant.domain.model.ChatRequestOptions
import com.aiassistant.ui.screens.chat.ChatUiState
import com.aiassistant.ui.screens.chat.TempChatSettings
import com.aiassistant.ui.screens.chat.displayModelShortName
import com.aiassistant.ui.screens.settings.CurrentVersionUserUpdates
import org.junit.Assert.*
import org.junit.Test

class V1924FeaturesTest {

    @Test
    fun testDisplayModelShortName() {
        // Test vendor prefix stripping
        assertEquals("glm-5.2", "z-ai/glm-5.2".displayModelShortName())
        assertEquals("gpt-4o", "openai/gpt-4o".displayModelShortName())
        assertEquals("claude-3-5-sonnet", "anthropic/claude-3-5-sonnet".displayModelShortName())
        assertEquals("deepseek-r1", "deepseek-ai/deepseek-r1".displayModelShortName())
        assertEquals("final-model", "a/b/c/final-model".displayModelShortName())
        
        // Test without slash
        assertEquals("gpt-4o-mini", "gpt-4o-mini".displayModelShortName())
        assertEquals("custom-local-model", "custom-local-model".displayModelShortName())
        assertEquals("", "".displayModelShortName())
    }

    @Test
    fun testDefaultsMaxTokensAndThinking() {
        // ApiConfig defaults
        val apiConfig = ApiConfig(
            name = "Test",
            provider = "openai",
            baseUrl = "https://api.openai.com",
            apiKey = "sk-test",
            modelName = "gpt-4o"
        )
        assertEquals(50000, apiConfig.maxTokens)
        assertTrue(apiConfig.enableThinking)

        // TempChatSettings defaults
        val tempSettings = TempChatSettings()
        assertEquals(50000, tempSettings.maxTokens)
        assertTrue(tempSettings.enableThinking)
        assertTrue(tempSettings.enableSessionMemory)

        // ChatUiState defaults
        val uiState = ChatUiState()
        assertTrue(uiState.enableThinking)
    }

    @Test
    fun testChatRequestOptionsEnableSessionMemory() {
        val defaultOptions = ChatRequestOptions()
        assertNull(defaultOptions.enableSessionMemory)

        val disabledOptions = ChatRequestOptions(enableSessionMemory = false)
        assertEquals(false, disabledOptions.enableSessionMemory)

        val enabledOptions = ChatRequestOptions(enableSessionMemory = true)
        assertEquals(true, enabledOptions.enableSessionMemory)
    }

    @Test
    fun testCurrentVersionUserUpdatesCompleteness() {
        assertFalse("本次更新日志列表不得为空", CurrentVersionUserUpdates.isEmpty())
        assertTrue("必须包含 v1.9.24 更新项", CurrentVersionUserUpdates.any { it.contains("v1.9.24") })
        assertTrue("必须包含全屏状态栏阴影修复说明", CurrentVersionUserUpdates.any { it.contains("全屏状态栏阴影") || it.contains("EchoGlassDialog") })
        assertTrue("必须包含顶部悬浮胶囊毛玻璃说明", CurrentVersionUserUpdates.any { it.contains("顶部悬浮胶囊") || it.contains("悬浮栏") })
        assertTrue("必须包含思考强度全链路即时双向同步说明", CurrentVersionUserUpdates.any { it.contains("思考强度全链路即时双向同步") || it.contains("双向即时绑定") })
        assertTrue("必须包含思考档位配色与真实参数说明", CurrentVersionUserUpdates.any { it.contains("思考档位配色") || it.contains("真实参数说明") })
        assertTrue("必须包含全屏点击收起思考弹窗说明", CurrentVersionUserUpdates.any { it.contains("全屏点击收起思考弹窗") || it.contains("点击拦截器") })
        assertTrue("必须包含平滑页面转场过渡动画说明", CurrentVersionUserUpdates.any { it.contains("平滑页面转场过渡动画") || it.contains("NavHost") })
        assertTrue("必须包含会话内记忆专属控制总开关说明", CurrentVersionUserUpdates.any { it.contains("会话内记忆专属控制总开关") || it.contains("专属记忆") })
        assertTrue("必须包含进入对话直达底部与极速起止跳转说明", CurrentVersionUserUpdates.any { it.contains("进入对话直达底部") || it.contains("极速起止跳转") })
        assertTrue("必须包含右侧全局滚动条加粗跟手优化说明", CurrentVersionUserUpdates.any { it.contains("滚动条加粗跟手优化") || it.contains("滚动条") })
        assertTrue("必须包含默认开启思考与50000超长最大生成Token说明", CurrentVersionUserUpdates.any { it.contains("50,000") || it.contains("默认开启思考") })
        assertTrue("必须包含思考胶囊展示模型名称与翻译按钮内嵌说明", CurrentVersionUserUpdates.any { it.contains("展示模型名称") || it.contains("翻译按钮") })
        assertTrue("必须包含模型列表智能精简显示说明", CurrentVersionUserUpdates.any { it.contains("模型列表智能精简显示") || it.contains("智能精简") })
        assertTrue("必须包含模型回复分割线微距美化说明", CurrentVersionUserUpdates.any { it.contains("模型回复分割线微距美化") || it.contains("分割线") })
    }
}
