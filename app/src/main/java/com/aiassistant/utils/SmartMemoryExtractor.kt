package com.aiassistant.utils

import com.aiassistant.domain.model.PendingMemoryCandidate
import java.util.Locale

object SmartMemoryExtractor {

    private val NEGATIVE_MARKERS = listOf(
        "不要记住", "别记住", "不用记", "不用记住", "不要保存", "别保存",
        "do not remember", "don't remember", "forget"
    )

    private val QUESTION_MARKERS = listOf(
        "吗", "？", "?", "怎么", "什么", "为什么", "如何", "是不是", "有没有",
        "能否", "可以吗", "记不记得", "哪样", "哪位", "几点", "多少", "哪些"
    )

    // 短暂瞬态日常寒暄/动作过滤（非长期事实）
    private val EPHEMERAL_MARKERS = listOf(
        "刚刚", "现在去", "准备去", "正在吃", "去睡觉了", "晚安", "早安", "你好",
        "在吗", "哈哈", "谢谢", "好的", "收到", "收到谢谢", "拜拜", "再见", "ok", "yes", "no"
    )

    // 单次任务动作词：如果包含这些词且没有显式“记住：”或持久指示，说明是普通单次请求，绝不提取为记忆
    private val SINGLE_TURN_TASK_VERBS = listOf(
        "帮我", "请写", "写一个", "写一段", "写篇", "解释一下", "分析一下", "翻译一下",
        "总结一下", "修改一下", "优化一下", "重构一下", "查找", "查一下",
        "看看", "算一下", "画一个", "推荐几个", "介绍一下", "讲讲", "怎么做", "列举一下",
        "排查一下", "修复一下"
    )

    fun extractCandidate(
        content: String,
        conversationId: Long = 0L,
        messageId: Long? = null
    ): PendingMemoryCandidate? {
        val trimmed = content.trim()
        if (trimmed.length < 4 || trimmed.length > 300) return null

        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. 过滤否定词
        if (NEGATIVE_MARKERS.any { lower.contains(it) }) return null

        // 2. 检查显式记忆指令（最高优先级）：例如 "请记住：..."、"牢记：..."、"记住我喜欢..."
        val explicitRememberRegex = Regex("""^(?:(?:请|麻烦|务必)?(?:记住|牢记|记下|记一下)[：:]?\s*)(.{4,120})$""")
        explicitRememberRegex.find(trimmed)?.let { match ->
            val fact = match.groupValues[1].trim()
            if (fact.isNotBlank()) {
                val isConv = isConversationScoped(fact.lowercase(Locale.ROOT))
                val isPref = listOf("喜欢", "偏好", "习惯", "希望", "要求", "必须", "注释", "语言", "回答", "代码").any { fact.contains(it) }
                return PendingMemoryCandidate(
                    distilledContent = "用户设定：$fact",
                    originalSnippet = trimmed.take(80),
                    suggestedScope = if (isConv) "conversation" else "user",
                    conversationId = conversationId,
                    sourceMessageId = messageId,
                    category = if (isPref) "PREFERENCE" else "FACT"
                )
            }
        }

        // 2.1 显式规则与设定：例如 "设定：在这个会话中始终使用中文"、"规则：不要输出解释"、"要求：代码附带类型标注"
        val explicitRuleRegex = Regex("""^(?:(?:会话|对话|当前)?(?:设定|规则|要求|约束)[：:]\s*)(.{4,120})$""")
        explicitRuleRegex.find(trimmed)?.let { match ->
            val rule = match.groupValues[1].trim()
            if (rule.isNotBlank()) {
                return PendingMemoryCandidate(
                    distilledContent = "会话规则：$rule",
                    originalSnippet = trimmed.take(80),
                    suggestedScope = "conversation",
                    conversationId = conversationId,
                    sourceMessageId = messageId,
                    category = "PREFERENCE"
                )
            }
        }

        // 2.2 用户偏好声明：例如 "我的偏好：优先使用Kotlin"、"习惯：回答简洁"
        val userPrefRegex = Regex("""^(?:(?:我的)?(?:偏好|习惯|喜好)(?:是|[：:])\s*)(.{3,80})$""")
        userPrefRegex.find(trimmed)?.let { match ->
            val pref = match.groupValues[1].trim()
            if (pref.isNotBlank()) {
                return PendingMemoryCandidate(
                    distilledContent = "用户偏好：$pref",
                    originalSnippet = trimmed.take(80),
                    suggestedScope = "user",
                    conversationId = conversationId,
                    sourceMessageId = messageId,
                    category = "PREFERENCE"
                )
            }
        }

        // 3. 过滤疑问句（疑问句绝不作为事实或偏好入库）
        if (QUESTION_MARKERS.any { trimmed.endsWith(it) || trimmed.contains(it) }) return null

        // 4. 过滤瞬态寒暄与日常聊天
        if (EPHEMERAL_MARKERS.any { lower.startsWith(it) || lower == it }) return null

        // 5. 过滤单次任务指令（非持久性任务）
        if (SINGLE_TURN_TASK_VERBS.any { lower.contains(it) }) {
            val hasDurableMarker = listOf("以后", "每次", "始终", "一直", "永远", "默认都", "时请", "请附带", "附带", "必须").any { lower.contains(it) }
            if (!hasDurableMarker) return null
        }

        val isConversationScoped = isConversationScoped(lower)
        val defaultScope = if (isConversationScoped) "conversation" else "user"

        // 6. 会话内项目架构或角色设定
        val projectStackRegex = Regex("""(?:当前项目|这个项目|本项目)的?(?:技术栈|架构|语言|主语言|核心依赖|框架|包名)?(?:是|使用|基于)\s*(.{3,60})""")
        projectStackRegex.find(trimmed)?.let { match ->
            val stack = match.groupValues[1].trim()
            return PendingMemoryCandidate(
                distilledContent = "会话事实：项目技术架构为「$stack」",
                originalSnippet = trimmed.take(80),
                suggestedScope = "conversation",
                conversationId = conversationId,
                sourceMessageId = messageId,
                category = "PROJECT"
            )
        }

        val sessionRoleRegex = Regex("""(?:在这个对话|在当前会话|在本对话)(?:中|里)?(?:，|,)?\s*(?:你是一个?|你的身份是|请扮演|你要作为)\s*(.{3,60})""")
        sessionRoleRegex.find(trimmed)?.let { match ->
            val role = match.groupValues[1].trim()
            return PendingMemoryCandidate(
                distilledContent = "会话设定：模型身份设定为「$role」",
                originalSnippet = trimmed.take(80),
                suggestedScope = "conversation",
                conversationId = conversationId,
                sourceMessageId = messageId,
                category = "PROJECT"
            )
        }

        // 7. 语言偏好
        if (lower.contains("中文") && (lower.contains("回答") || lower.contains("思考") || lower.contains("交流") || lower.contains("输出"))) {
            val hasThinking = lower.contains("思考")
            val distilled = if (hasThinking) {
                "用户偏好：要求模型始终使用中文进行思考与回答"
            } else {
                "用户偏好：要求模型始终使用中文回答问题"
            }
            return PendingMemoryCandidate(
                distilledContent = distilled,
                originalSnippet = trimmed.take(80),
                suggestedScope = defaultScope,
                conversationId = conversationId,
                sourceMessageId = messageId,
                category = "PREFERENCE"
            )
        }

        if ((lower.contains("英文") || lower.contains("english")) && (lower.contains("回答") || lower.contains("answer"))) {
            return PendingMemoryCandidate(
                distilledContent = "用户偏好：要求模型使用英文回答交流",
                originalSnippet = trimmed.take(80),
                suggestedScope = defaultScope,
                conversationId = conversationId,
                sourceMessageId = messageId,
                category = "PREFERENCE"
            )
        }

        // 8. 风格与格式偏好（注释、简短、精炼）
        if (lower.contains("注释") && (lower.contains("代码") || lower.contains("写代码"))) {
            return PendingMemoryCandidate(
                distilledContent = "用户偏好：提供代码实现时需附带详尽的中文注释",
                originalSnippet = trimmed.take(80),
                suggestedScope = defaultScope,
                conversationId = conversationId,
                sourceMessageId = messageId,
                category = "PREFERENCE"
            )
        }

        if (lower.contains("简短") || lower.contains("简洁") || lower.contains("精炼") || lower.contains("不要长篇大论") || lower.contains("直接给结论") || lower.contains("精简")) {
            return PendingMemoryCandidate(
                distilledContent = "用户偏好：回答需简短精炼，直奔主题，避免冗长说明",
                originalSnippet = trimmed.take(80),
                suggestedScope = defaultScope,
                conversationId = conversationId,
                sourceMessageId = messageId,
                category = "PREFERENCE"
            )
        }

        // 9. 真实持久身份、姓名与职业
        val identityRegex = Regex("""(?:我叫|我的名字是|我名字叫)\s*([A-Za-z0-9\u4e00-\u9fa5]{2,10})""")
        identityRegex.find(trimmed)?.let { match ->
            val name = match.groupValues[1]
            return PendingMemoryCandidate(
                distilledContent = "用户事实：用户姓名/称呼为「$name」",
                originalSnippet = trimmed.take(80),
                suggestedScope = "user",
                conversationId = conversationId,
                sourceMessageId = messageId,
                category = "FACT"
            )
        }

        val careerRegex = Regex("""(?:我是(?:一名|一个)?)\s*([A-Za-z0-9\u4e00-\u9fa5\s]{2,25}?(?:工程师|程序员|开发者|架构师|学生|老师|设计师|产品经理|医生|律师|作家|学者|研究员))""")
        careerRegex.find(trimmed)?.let { match ->
            val job = match.groupValues[1].trim()
            return PendingMemoryCandidate(
                distilledContent = "用户事实：用户职业身份为「$job」",
                originalSnippet = trimmed.take(80),
                suggestedScope = "user",
                conversationId = conversationId,
                sourceMessageId = messageId,
                category = "FACT"
            )
        }

        val envRegex = Regex("""(?:我的开发环境|我的操作系统|我的系统|我的电脑系统)(?:是|使用)\s*([A-Za-z0-9\s.]{3,30})""")
        envRegex.find(trimmed)?.let { match ->
            val os = match.groupValues[1].trim()
            return PendingMemoryCandidate(
                distilledContent = "用户环境：开发操作系统为「$os」",
                originalSnippet = trimmed.take(80),
                suggestedScope = "user",
                conversationId = conversationId,
                sourceMessageId = messageId,
                category = "FACT"
            )
        }

        // 10. 会话项目事实兜底
        if (isConversationScoped) {
            val cleanSnippet = trimmed.replace(Regex("""^(?:在|对于)?(?:当前项目|这个项目|本项目|这个对话|当前会话)[，,中里]?\s*"""), "")
            if (cleanSnippet.length in 6..60) {
                return PendingMemoryCandidate(
                    distilledContent = "会话事实：$cleanSnippet",
                    originalSnippet = trimmed.take(80),
                    suggestedScope = "conversation",
                    conversationId = conversationId,
                    sourceMessageId = messageId,
                    category = "PROJECT"
                )
            }
        }

        return null
    }

    private fun isConversationScoped(lower: String): Boolean {
        return listOf("这个项目", "当前项目", "本项目", "这个对话", "当前会话", "本会话", "此会话", "该会话", "这个会话", "当前对话", "此对话", "该对话", "this project", "this conversation")
            .any { lower.contains(it) }
    }
}
