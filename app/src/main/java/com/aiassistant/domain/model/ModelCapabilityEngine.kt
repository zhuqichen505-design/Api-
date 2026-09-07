package com.aiassistant.domain.model

import java.util.Locale

data class ModelCapabilityInfo(
    val contextWindowTokens: Int,
    val contextWindowLabel: String,
    val isMultimodal: Boolean,
    val supportsToolCalling: Boolean,
    val supportsThinking: Boolean,
    val supportedThinkingGears: List<String> = emptyList(),
    val defaultThinkingBudget: Int = 1024
) {
    val contextWindowDisplay: String get() = contextWindowLabel
    val supportsVision: Boolean get() = isMultimodal
    val supportsTools: Boolean get() = supportsToolCalling
    val supportsReasoning: Boolean get() = supportsThinking
}

object ModelCapabilityEngine {

    fun evaluateModel(modelName: String): ModelCapabilityInfo = resolveCapabilities(modelName)

    fun resolveCapabilities(
        modelName: String,
        provider: String = "",
        baseUrl: String = ""
    ): ModelCapabilityInfo {
        val name = modelName.trim().lowercase(Locale.ROOT)
        val fullIdentity = "$provider $baseUrl $name".lowercase(Locale.ROOT)

        // 1. 上下文窗口识别
        val (contextTokens, contextLabel) = resolveContextWindow(name)

        // 2. 多模态视觉识别 (Vision / Image)
        val isMultimodal = resolveIsMultimodal(name, fullIdentity)

        // 3. 工具调用 (Function Calling / Tools)
        val supportsToolCalling = resolveSupportsToolCalling(name)

        // 4. 思考模式 (Reasoning / Thinking) & 档位
        val (supportsThinking, gears, defaultBudget) = resolveThinkingCapabilities(name, fullIdentity)

        return ModelCapabilityInfo(
            contextWindowTokens = contextTokens,
            contextWindowLabel = contextLabel,
            isMultimodal = isMultimodal,
            supportsToolCalling = supportsToolCalling,
            supportsThinking = supportsThinking,
            supportedThinkingGears = gears,
            defaultThinkingBudget = defaultBudget
        )
    }

    private fun resolveContextWindow(name: String): Pair<Int, String> {
        return when {
            // 2M 上下文
            name.contains("gemini-1.5-pro") || name.contains("gemini-2.0-pro") || name.contains("gemini-pro-1.5") ->
                Pair(2_000_000, "2M")

            // 1M 上下文
            name.contains("gemini") || name.contains("gpt-4.1") || name.contains("qwen-long") || name.contains("qwen-max-long") ->
                Pair(1_000_000, "1M")

            // 200K 上下文
            name.contains("claude-3") || name.contains("claude-3-5") || name.contains("claude-3-7") ->
                Pair(200_000, "200K")

            // 128K 上下文
            name.contains("gpt-4o") || name.contains("o1") || name.contains("o3") || name.contains("o4") ||
            name.contains("deepseek-v3") || name.contains("deepseek-chat") || name.contains("deepseek-reasoner") || name.contains("r1") ||
            name.contains("qwen-2.5") || name.contains("qwen-plus") || name.contains("qwen-max") || name.contains("glm-4") || name.contains("kimi") ->
                Pair(128_000, "128K")

            // 64K 上下文
            name.contains("deepseek") || name.contains("llama-3.1") || name.contains("llama-3.2") || name.contains("llama-3.3") ->
                Pair(64_000, "64K")

            // 32K 上下文
            name.contains("gpt-4-32k") || name.contains("qwen-turbo") || name.contains("baichuan") ->
                Pair(32_000, "32K")

            // 16K 上下文
            name.contains("gpt-3.5-turbo-16k") ->
                Pair(16_000, "16K")

            // 默认兜底
            else -> Pair(32_000, "32K")
        }
    }

    private fun resolveIsMultimodal(name: String, identity: String): Boolean {
        if (name.contains("text-only") || name.contains("embedding") || name.contains("dall-e")) return false
        return name.contains("vision") ||
            name.contains("vl") ||
            name.contains("4o") ||
            name.contains("omni") ||
            name.contains("gemini") ||
            name.contains("claude-3") ||
            name.contains("gpt-4-turbo") ||
            name.contains("glm-4v") ||
            name.contains("internvl") ||
            name.contains("minicpm-v") ||
            identity.contains("multimodal")
    }

    private fun resolveSupportsToolCalling(name: String): Boolean {
        // 大多数现代模型支持工具调用
        if (name.contains("o1-mini") || name.contains("o1-preview")) return false
        return name.contains("gpt-4") ||
            name.contains("gpt-3.5") ||
            name.contains("deepseek") ||
            name.contains("claude") ||
            name.contains("gemini") ||
            name.contains("qwen") ||
            name.contains("glm") ||
            name.contains("mistral")
    }

    private fun resolveThinkingCapabilities(name: String, identity: String): Triple<Boolean, List<String>, Int> {
        val isReasoner = name.contains("reasoner") ||
            name.contains("r1") ||
            name.contains("thinking") ||
            name.contains("qwq") ||
            name.contains("claude-3-7") ||
            Regex("""(^|[-_/])(o[134])""").containsMatchIn(name)

        if (!isReasoner) {
            // 普通模型若为 DeepSeek 或 MiMo 也支持开启思考模式
            val isDeepSeekOrMiMo = identity.contains("deepseek") || identity.contains("mimo") || identity.contains("xiaomi")
            if (isDeepSeekOrMiMo && (name.contains("deepseek") || name.contains("mimo"))) {
                return Triple(true, listOf("low", "medium", "high"), 1024)
            }
            return Triple(false, emptyList(), 0)
        }

        val gears = when {
            name.contains("deepseek-reasoner") || name.contains("r1") -> listOf("high", "max")
            Regex("""(^|[-_/])(o[134])""").containsMatchIn(name) -> listOf("low", "medium", "high")
            name.contains("claude-3-7") -> listOf("low", "medium", "high", "max")
            else -> listOf("low", "medium", "high")
        }

        val defaultBudget = when {
            name.contains("claude-3-7") -> 2048
            name.contains("deepseek-reasoner") -> 4096
            else -> 1024
        }

        return Triple(true, gears, defaultBudget)
    }
}
