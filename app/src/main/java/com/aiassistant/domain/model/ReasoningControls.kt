package com.aiassistant.domain.model

/** Shared by both editors and wire serialization; labels are parameter values, never marketing aliases. */
object ReasoningControls {
    data class Option(val value: String, val label: String, val enabled: Boolean = true)
    fun options(model: String, apiType: String = "openai"): List<Option> {
        val policy = ModelVendorProfiles.policyFor(model)
        val nativeClaude = apiType == "anthropic"
        if (policy.vendor == ModelVendor.CLAUDE && !nativeClaude) return listOf(Option("default", "服务端默认"))
        if (nativeClaude && policy.thinkingGears.isEmpty()) return listOf(
            Option("disabled", "thinking.type=disabled", false),
            Option("budget:1024", "budget_tokens=1024"), Option("budget:4096", "budget_tokens=4096"),
            Option("budget:8192", "budget_tokens=8192"), Option("budget:16384", "budget_tokens=16384")
        )
        if (policy.thinkingGears.isEmpty()) return listOf(Option("default", "服务端默认"))
        val gears = policy.thinkingGears.map { Option(it, it, it != "none") }
        return if (policy.alwaysThinking || "none" in policy.thinkingGears) gears
        else listOf(Option("disabled", "thinking.type=disabled", false)) + gears
    }

    fun selected(model: String, apiType: String, enabled: Boolean, raw: String?): Option {
        val options = options(model, apiType)
        if (!enabled) options.firstOrNull { !it.enabled }?.let { return it }
        options.firstOrNull { it.value == raw }?.let { return it }
        if (apiType == "anthropic" && options.any { it.value.startsWith("budget:") }) {
            val budget = when(raw) { "low", "fast" -> 1024; "high", "deep" -> 8192; "max", "ultra" -> 16384; else -> 4096 }
            return options.first { it.value == "budget:$budget" }
        }
        val mapped = ModelVendorProfiles.mapThinkingGear(raw, model)
        return options.firstOrNull { it.value == mapped } ?: options.first { it.enabled }
    }
}
