package com.aiassistant.utils

import com.aiassistant.domain.model.PendingMemoryCandidate
import java.util.Locale

object SmartMemoryExtractor {

    private val NEGATIVE_MARKERS = listOf(
        "不要记住", "别记住", "不用记", "不要保存", "别保存", "do not remember", "don't remember", "forget"
    )

    private val QUESTION_MARKERS = listOf(
        "吗", "？", "?", "怎么", "什么", "为什么", "如何", "是不是", "有没有", "能否", "可以吗", "记不记得"
    )

    // 短暂瞬态日常动作过滤（非长期事实）
    private val EPHEMERAL_MARKERS = listOf(
        "刚刚", "现在去", "准备去", "正在吃", "去睡觉了", "晚安", "早安", "你好", "在吗", "哈哈", "谢谢"
    )

    fun extractCandidate(
        content: String,
        conversationId: Long = 0L,
        messageId: Long? = null
    ): PendingMemoryCandidate? {
        val trimmed = content.trim()
        if (trimmed.length < 5 || trimmed.length > 300) return null

        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. 过滤否定词与疑问句
        if (NEGATIVE_MARKERS.any { lower.contains(it) }) return null
        if (QUESTION_MARKERS.any { trimmed.endsWith(it) || trimmed.contains(it) }) return null
        if (EPHEMERAL_MARKERS.any { lower.startsWith(it) || lower == it }) return null

        // 2. 判断作用域：会话专属 vs 全局长期
        val isConversationScoped = isConversationScoped(lower)
        val defaultScope = if (isConversationScoped) "conversation" else "user"

        // 3. 模式匹配提炼
        // A. 语言偏好
        if (lower.contains("中文") && (lower.contains("回答") || lower.contains("思考") || lower.contains("交流") || lower.contains("输出"))) {
            if (lower.contains("永远") || lower.contains("始终") || lower.contains("一直") || lower.contains("默认") || lower.contains("请用") || lower.contains("以后") || lower.contains("都用") || lower.contains("务必")) {
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

        // B. 身份、姓名与职业
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

        val careerRegex = Regex("""(?:我是(?:一名|一个)?)\s*([A-Za-z0-9\u4e00-\u9fa5\s]{2,25}?(?:工程师|程序员|开发者|学生|老师|设计师|产品经理|医生|律师|作家|学者|研究员|架构师))""")
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

        // C. 回答风格与格式偏好
        if (lower.contains("简短") || lower.contains("简洁") || lower.contains("不要长篇大论") || lower.contains("直接给结论") || lower.contains("精简")) {
            return PendingMemoryCandidate(
                distilledContent = "用户偏好：回答需简短精炼，直奔主题，避免冗长说明",
                originalSnippet = trimmed.take(80),
                suggestedScope = defaultScope,
                conversationId = conversationId,
                sourceMessageId = messageId,
                category = "PREFERENCE"
            )
        }

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

        // D. 会话项目事实
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

        // E. 显式指令："记住：..."、"请记住..."
        val explicitRememberRegex = Regex("""^(?:请?记住[：:]?\s*)(.{5,80})$""")
        explicitRememberRegex.find(trimmed)?.let { match ->
            val fact = match.groupValues[1].trim()
            return PendingMemoryCandidate(
                distilledContent = "用户指定事实：$fact",
                originalSnippet = trimmed.take(80),
                suggestedScope = defaultScope,
                conversationId = conversationId,
                sourceMessageId = messageId,
                category = "FACT"
            )
        }

        return null
    }

    private fun isConversationScoped(lower: String): Boolean {
        return listOf("这个项目", "当前项目", "本项目", "这个对话", "当前会话", "本会话", "this project", "this conversation")
            .any { lower.contains(it) }
    }
}
