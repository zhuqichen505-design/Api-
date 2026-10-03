package com.aiassistant

import com.aiassistant.domain.model.*
import com.aiassistant.data.repository.helpers.TokenEstimationHelper
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class ReasoningControlsTest {
    @Test fun modelSpecificRangesAndCanonicalValues() {
        val cases = mapOf(
            "gpt-6-astra" to listOf("low", "medium", "high", "xhigh", "max"),
            "gpt-5.2-pro" to listOf("medium", "high", "xhigh"),
            "kimi-k3" to listOf("low", "high", "max"),
            "glm-5.2" to listOf("high", "max"),
            "MiniMax-M3.1-Flash-Preview" to listOf("low", "medium", "high", "xhigh", "max"),
            "gemini-3.1-pro" to listOf("low", "medium", "high")
        )
        cases.forEach { (model, values) ->
            assertEquals(model, values, ReasoningControls.options(model).map { it.value })
            values.forEach { value ->
                assertEquals(value, ReasoningControls.selected(model, "openai", true, value).value)
                assertEquals(value, TokenEstimationHelper.normalizeThinkingEffort(value, "openai", model))
            }
        }
        assertEquals("xhigh", ReasoningControls.selected("gpt-6-astra", "openai", true, "xhigh").label)
        assertEquals("max", ReasoningControls.selected("kimi-k3", "openai", true, "ultra").value)
        assertEquals("high", ReasoningControls.selected("deepseek-v4-pro", "openai", true, "medium").value)
        assertTrue(ReasoningControls.options("kimi-k2").none { it.value == "max" })
    }

    @Test fun cannotDisableAlwaysThinkingAndOmissionIsNotOff() {
        assertTrue(ReasoningControls.selected("gpt-6-astra", "openai", false, "high").enabled)
        assertEquals("none", ReasoningControls.selected("gpt-5.6", "openai", false, "high").value)
        assertEquals("disabled", ReasoningControls.selected("deepseek-v4-pro", "openai", false, "high").value)
        assertEquals("default", ReasoningControls.selected("unknown-private-model", "openai", true, "max").value)
    }

    @Test fun nativeClaudeBudgetAndEffortAreDifferentWireContracts() {
        val old = ReasoningControls.selected("claude-3-7-sonnet", "anthropic", true, "high")
        assertEquals("budget_tokens=8192", old.label)
        assertEquals(8192, TokenEstimationHelper.thinkingBudgetForEffort(old.value, 1024))
        val selection = ReasoningControls.selected("claude-opus-4-6", "anthropic", true, "max")
        val request = AnthropicRequest("claude-opus-4-6", emptyList(), 16000,
            thinking = AnthropicThinking(type = "adaptive"), output_config = AnthropicOutputConfig(selection.value))
        val json = JsonParser.parseString(Gson().toJson(request)).asJsonObject
        assertEquals("max", json.getAsJsonObject("output_config").get("effort").asString)
        assertFalse(json.getAsJsonObject("thinking").has("budget_tokens"))
        assertEquals("default", ReasoningControls.options("claude-opus-4-6", "openai").single().value)
    }
}
