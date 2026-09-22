package com.aiassistant.domain.model

import java.util.Locale

data class ModelCapabilityInfo(
    val contextWindowTokens: Int,
    val contextWindowLabel: String,
    val isMultimodal: Boolean,
    val supportsToolCalling: Boolean,
    val supportsThinking: Boolean,
    val supportedThinkingGears: List<String> = emptyList(),
    val defaultThinkingBudget: Int = 1024,
    val reasoningProviderType: String = "none" // "openai", "anthropic", "deepseek_fixed", "none"
) {
    val contextWindowDisplay: String get() = contextWindowLabel
    val supportsVision: Boolean get() = isMultimodal
    val supportsTools: Boolean get() = supportsToolCalling
    val supportsReasoning: Boolean get() = supportsThinking
    val isContextWindowRecognized: Boolean get() = contextWindowLabel.isNotBlank()
}

object ModelCapabilityEngine {

    const val DEFAULT_CONTEXT_TOKENS = 256_000 // 默认上下文设定为 256k

    fun evaluateModel(
        modelName: String,
        provider: String = "",
        baseUrl: String = ""
    ): ModelCapabilityInfo = resolveCapabilities(modelName, provider, baseUrl)

    fun resolveCapabilities(
        modelName: String,
        provider: String = "",
        baseUrl: String = ""
    ): ModelCapabilityInfo {
        val name = modelName.trim().lowercase(Locale.ROOT)
        val fullIdentity = "$provider $baseUrl $name".lowercase(Locale.ROOT)

        // 1. 上下文窗口识别（未识别成功时不带标签，默认 256k）
        val (contextTokens, contextLabel) = resolveContextWindow(name)

        // 2. 多模态视觉识别 (Vision / Image)
        val isMultimodal = resolveIsMultimodal(name, fullIdentity)

        // 3. 工具调用 (Function Calling / Tools)
        val supportsToolCalling = resolveSupportsToolCalling(name)

        // 4. 思考模式 (Reasoning / Thinking) & 档位
        val (supportsThinking, gears, defaultBudget, providerType) = resolveThinkingCapabilities(name, fullIdentity)

        return ModelCapabilityInfo(
            contextWindowTokens = contextTokens,
            contextWindowLabel = contextLabel,
            isMultimodal = isMultimodal,
            supportsToolCalling = supportsToolCalling,
            supportsThinking = supportsThinking,
            supportedThinkingGears = gears,
            defaultThinkingBudget = defaultBudget,
            reasoningProviderType = providerType
        )
    }

    private fun resolveContextWindow(name: String): Pair<Int, String> {
        // A. 显式数字与单位后缀优先提取：例如 -128k, -200k, -1m, -2m, -32k, -64k
        val explicitSuffix = Regex("""(?i)(?:^|[-_./])(\d+)([km])(?:$|[-_./])""").find(name)
            ?: Regex("""(?i)(?:^|[-_./])([1-9]\d{0,2})m(?:$|[-_./])""").find(name)
            ?: Regex("""(?i)(?:^|[-_./])(\d{1,4})k(?:$|[-_./])""").find(name)
        if (explicitSuffix != null) {
            val num = explicitSuffix.groupValues[1].toIntOrNull()
            val unit = explicitSuffix.groupValues[2].lowercase(Locale.ROOT)
            if (num != null) {
                if (unit == "m" && num in 1..10) {
                    return Pair(num * 1_000_000, "${num}M")
                } else if (unit == "k" && num in 4..2048) {
                    return Pair(num * 1_000, "${num}K")
                }
            }
        }

        // B. 主流大模型家族全谱系与架构特征识别
        return when {
            // 2M 上下文（Gemini Pro 系列）
            name.contains("gemini-1.5-pro") || name.contains("gemini-2.0-pro") ||
            name.contains("gemini-2.5-pro") || name.contains("gemini-pro-1.5") ->
                Pair(2_000_000, "2M")

            // 1M 上下文（现代 Flash 与长文本架构：如 deepseekv4flash, gemini-flash, qwen-long, glm-4-long 等）
            name.contains("flash") || name.contains("long") || name.contains("gemini") ||
            name.contains("grok-3") || name.contains("gpt-4.1") ->
                Pair(1_000_000, "1M")

            // 200K 上下文（Anthropic Claude 系列与 OpenAI o系列）
            name.contains("claude") ||
            name.contains("o1") || name.contains("o3") || name.contains("o4") ||
            name.contains("yi-34b-200k") || name.contains("yi-large-rag") ->
                Pair(200_000, "200K")

            // 128K 上下文（现代主流基座与推理模型：DeepSeek V3/R1/Chat，GPT-4o，Qwen 2.5，GLM-4，Llama 3.x 等）
            name.contains("gpt-4o") || name.contains("gpt-4.5") || name.contains("gpt-4-turbo") ||
            name.contains("deepseek-v3") || name.contains("deepseek-chat") || name.contains("deepseek-reasoner") ||
            name.contains("r1") || name.contains("deepseek-coder") ||
            (name.contains("deepseek") && !name.contains("deepseek-v2") && !name.contains("deepseek-v1")) ||
            name.contains("qwen-2.5") || name.contains("qwen2.5") || name.contains("qwq") ||
            name.contains("qwen-plus") || name.contains("qwen-max") || name.contains("qwen-turbo") ||
            name.contains("glm-4") || name.contains("glm-3-turbo") || name.contains("kimi") || name.contains("moonshot") ||
            name.contains("llama-3.3") || name.contains("llama-3.2") || name.contains("llama-3.1") ||
            name.contains("mistral-large") || name.contains("mistral-small") || name.contains("codestral") ||
            name.contains("baichuan4") || name.contains("grok-2") || name.contains("abab6") ->
                Pair(128_000, "128K")

            // 64K 上下文（明确标注的早期一代架构）
            name.contains("deepseek-v2") || name.contains("open-mixtral-8x22b") ->
                Pair(64_000, "64K")

            // 32K 上下文
            name.contains("gpt-4-32k") || name.contains("chatglm3") || name.contains("baichuan3") || name.contains("baichuan2") ||
            name.contains("yi-large") || name.contains("yi-medium") || name.contains("open-mixtral-8x7b") || name.contains("mistral-7b") ->
                Pair(32_000, "32K")

            // 16K 上下文（仅针对明确的历史早期固定版本）
            name.contains("gpt-3.5-turbo-0613") || name.contains("gpt-3.5-turbo-0301") ->
                Pair(16_000, "16K")

            // 8K 上下文（仅针对明确的历史早期固定版本）
            name.contains("gpt-4-0613") || name.contains("gpt-4-0314") ->
                Pair(8_000, "8K")

            // 4K 上下文（历史早期模型）
            name.contains("llama-2") ->
                Pair(4_000, "4K")

            // 未被确定识别的模型：内部安全预算默认 256K，严禁虚构伪造标签展示给用户
            else -> Pair(DEFAULT_CONTEXT_TOKENS, "")
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
            name.contains("gpt-4.5") ||
            name.contains("glm-4v") ||
            name.contains("internvl") ||
            name.contains("minicpm-v") ||
            identity.contains("multimodal")
    }

    private fun resolveSupportsToolCalling(name: String): Boolean {
        // 早期特定预览版不支持 Function Calling
        if (name.contains("o1-mini") || name.contains("o1-preview") || name.contains("text-embedding")) return false
        return true
    }

    private fun resolveThinkingCapabilities(name: String, identity: String): Tuple4<Boolean, List<String>, Int, String> {
        val standardGears = listOf("low", "medium", "high", "max")

        // 明确标注的纯非思考非对话模型或历史经典标准非思考模型（gpt-4o, 标准 gpt-4, gpt-3.5 等）
        if (name.contains("text-embedding") || name.contains("dall-e") || name.contains("tts") || name.contains("whisper") ||
            name.contains("gpt-4o") || name == "gpt-4" || name.startsWith("gpt-3.5")
        ) {
            return Tuple4(false, emptyList(), 0, "none")
        }

        // 1. OpenAI o 系列推理模型 (o1, o3, o4)
        if (Regex("""(^|[-_/])(o[1-9])""").containsMatchIn(name) || name.contains("o1") || name.contains("o3") || name.contains("o4")) {
            return Tuple4(true, listOf("low", "medium", "high"), 4096, "openai")
        }

        // 2. Anthropic Claude 系列
        if (identity.contains("anthropic") || name.contains("claude")) {
            return Tuple4(true, standardGears, 4096, "anthropic")
        }

        // 3. DeepSeek 官方深度思考/推理系列 (deepseek-reasoner, r1 等) 与 QwQ
        if (identity.contains("deepseek") || name.contains("deepseek") || name.contains("r1") || name.contains("qwq")) {
            return Tuple4(true, standardGears, 4096, "deepseek_fixed")
        }

        // 4. OpenAI 兼容推理与思考系列 (reasoner, thinking, gpt-5 等)
        if (identity.contains("openai") || name.contains("reasoner") || name.contains("thinking") || name.contains("gpt-5")) {
            return Tuple4(true, standardGears, 4096, "openai")
        }

        // 5. 其余现代主流模型默认全面对齐思考架构（依据用户最高准则：现代主流模型均具备思考能力，全系支持 4 档深度）
        return Tuple4(true, standardGears, 4096, "generic")
    }

    data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
