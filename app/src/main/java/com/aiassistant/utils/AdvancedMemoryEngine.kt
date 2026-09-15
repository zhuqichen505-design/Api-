package com.aiassistant.utils

import com.aiassistant.domain.model.MemoryItem
import com.aiassistant.domain.model.Message
import java.util.Locale
import kotlin.math.exp

/**
 * 高级记忆提取、冲突消解与分层上下文压缩引擎 (AdvancedMemoryEngine)
 * 借鉴 Mem0, Generative Agents, LangChain Memory 等开源前沿实践：
 * 1. 原子事实分类体系与重要度权重 (1~5)
 * 2. 事实冲突消解与自适应更替 (Conflict Resolution & Upsert)
 * 3. 三维混合检索评分算法 (Relevance + Importance + Recency + Entity Boosting)
 * 4. 结构化多维分层上下文压缩模型 (Core Constraints + Milestones + Open Items)
 * 5. 智能信息密度提纯 (Loss-Aware Pre-pruning)
 * 6. 高保真本地抽取式多维结构化摘要兜底
 */
object AdvancedMemoryEngine {

    enum class MemoryCategory(val displayName: String, val baseImportance: Int) {
        CONSTRAINT("不可违背约束", 5),
        PREFERENCE("偏好习惯", 4),
        TIMELINE("时空经历", 4),
        FACT("客观事实", 3),
        WORLD_STATE("状态设定", 3)
    }

    data class AtomicFact(
        val content: String,
        val category: MemoryCategory,
        val importance: Int,
        val entities: List<String> = emptyList()
    )

    data class StructuredStateSummary(
        val coreConstraints: List<String> = emptyList(),
        val milestones: List<String> = emptyList(),
        val openItems: List<String> = emptyList()
    ) {
        fun toPromptBlock(): String {
            val sb = StringBuilder()
            if (coreConstraints.isNotEmpty()) {
                sb.append("【核心背景与用户固定约束】：\n")
                coreConstraints.forEach { sb.append("- $it\n") }
                sb.append("\n")
            }
            if (milestones.isNotEmpty()) {
                sb.append("【历史关键里程碑与决策推进】：\n")
                milestones.forEachIndexed { idx, m -> sb.append("${idx + 1}. $m\n") }
                sb.append("\n")
            }
            if (openItems.isNotEmpty()) {
                sb.append("【当前未决议题与待办上下文】：\n")
                openItems.forEach { sb.append("• $it\n") }
            }
            return sb.toString().trim()
        }

        fun isEmpty(): Boolean = coreConstraints.isEmpty() && milestones.isEmpty() && openItems.isEmpty()
    }

    // ==========================================
    // 1. 原子事实类别与重要度推断
    // ==========================================

    fun inferFactCategoryAndImportance(text: String): Pair<MemoryCategory, Int> {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            // 约束类：行为规则、输出禁止、格式要求
            listOf("必须", "绝对不能", "不要输出", "严禁", "只能使用", "始终使用", "输出格式", "约束", "规则").any { lower.contains(it) } ->
                Pair(MemoryCategory.CONSTRAINT, 5)

            // 偏好类：个人习惯、喜好、风格
            listOf("喜欢", "偏好", "习惯", "擅长", "倾向于", "喜好", "常用").any { lower.contains(it) } ->
                Pair(MemoryCategory.PREFERENCE, 4)

            // 时空经历类：时间、日期、在某处发生了什么
            listOf("第", "天", "年", "月", "日", "昨天", "去了", "吃了", "见过", "相遇").any { lower.contains(it) } &&
                    (lower.startsWith("[") || lower.startsWith("【") || lower.contains("·")) ->
                Pair(MemoryCategory.TIMELINE, 4)

            // 状态设定类：角色健康、关系、世界观参数
            listOf("等级", "状态", "处于", "关系", "好感", "生命值", "血量", "心情").any { lower.contains(it) } ->
                Pair(MemoryCategory.WORLD_STATE, 3)

            // 普通客观事实
            else -> Pair(MemoryCategory.FACT, 3)
        }
    }

    // ==========================================
    // 2. 记忆冲突检测与消解更替 (Conflict Resolution)
    // ==========================================

    /**
     * 判定新记忆是否与已有旧记忆发生排他性属性冲突（需要覆盖更新旧记忆）
     * 例如：“我改名叫李四” 冲突并覆盖 “我叫张三”
     *      “搬到了深圳” 冲突并覆盖 “住在北京”
     *      “偏好改为使用Python” 冲突并覆盖 “偏好使用Java”
     */
    fun detectConflict(newContent: String, existingItem: MemoryItem): Boolean {
        val n = newContent.trim()
        val o = existingItem.content.trim()
        if (n == o) return false // 完全相同走常规强化流程，不是冲突覆盖

        // 居住地冲突
        val locationVerbs = listOf("住在", "搬到", "位于", "常住", "搬家到")
        if (locationVerbs.any { n.contains(it) } && locationVerbs.any { o.contains(it) }) {
            return true
        }

        // 姓名/称呼冲突
        val namePatterns = listOf("叫我", "我叫", "名字是", "称呼我")
        if (namePatterns.any { n.contains(it) } && namePatterns.any { o.contains(it) }) {
            return true
        }

        // 偏好技术栈/工具更替
        val prefPatterns = listOf("偏好使用", "改为使用", "换用", "主力语言是")
        if (prefPatterns.any { n.contains(it) } && prefPatterns.any { o.contains(it) }) {
            return true
        }

        // 相同前缀声明冲突（如两者均以相同属性开头）
        val colonPrefixNew = n.substringBefore("：").substringBefore(":")
        val colonPrefixOld = o.substringBefore("：").substringBefore(":")
        if (colonPrefixNew.length in 2..8 && colonPrefixNew == colonPrefixOld) {
            val attrKey = colonPrefixNew.removePrefix("用户").removePrefix("会话")
            if (attrKey in listOf("身份", "职业", "称谓", "年龄", "城市", "时区", "语言偏好", "当前目标")) {
                return true
            }
        }

        return false
    }

    // ==========================================
    // 3. 三维混合检索评分算法 (Tri-Factor Hybrid Scoring)
    // ==========================================

    /**
     * 综合 Relevance (40%) + Importance (25%) + Recency (20%) + Entity Boosting (15%) + ScopeBoost
     */
    fun calculateHybridScore(
        memory: MemoryItem,
        queryTerms: Set<String>,
        queryEntities: Set<String>,
        conversationId: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Float {
        // 1. Relevance: 字面重合度 (0.0 ~ 1.0)
        val memoryTerms = memory.content.lowercase(Locale.ROOT).split(Regex("""[\s,，.。!！?？:：;；、]+"""))
            .filter { it.length >= 2 }
            .toSet()
        val overlapCount = queryTerms.count { it in memoryTerms }
        val relevanceScore = (overlapCount * 0.35f).coerceIn(0f, 1f)

        // 2. Importance: 静态重要度 (0.2 ~ 1.0)
        val (_, importance) = inferFactCategoryAndImportance(memory.content)
        val importanceScore = (importance / 5f).coerceIn(0.2f, 1f)

        // 3. Recency: 时间衰减 (0.1 ~ 1.0)
        val ageDays = ((currentTimeMs - memory.updatedAt) / (1000.0 * 60 * 60 * 24)).coerceAtLeast(0.0)
        val halfLifeDays = 7.0
        val recencyScore = exp(-0.693 * (ageDays / halfLifeDays)).toFloat().coerceIn(0.1f, 1f)

        // 4. Entity Boosting: 实体精准命中加成 (0.0 或 1.0)
        val entityHit = queryEntities.any { it.isNotBlank() && memory.content.contains(it, ignoreCase = true) }
        val entityScore = if (entityHit) 1f else 0f

        // 5. Scope Boost: 当前会话记忆加成
        val scopeBoost = if (memory.scope == "conversation" && memory.conversationId == conversationId) 0.18f else 0f

        return (0.40f * relevanceScore) +
                (0.25f * importanceScore) +
                (0.20f * recencyScore) +
                (0.15f * entityScore) +
                scopeBoost
    }

    // ==========================================
    // 4. 智能信息密度提纯 (Loss-Aware Pre-pruning)
    // ==========================================

    private val PURE_NOISE_SNIPPETS = setOf(
        "好的", "收到", "谢谢", "好的谢谢", "收到谢谢", "嗯", "嗯嗯", "行", "可以", "没问题",
        "哈哈", "是的", "对", "ok", "yes", "thanks", "thank you", "got it"
    )

    /**
     * 过滤短纯寒暄或无实质信息的废话轮次，提升送入摘要器的信息信噪比
     */
    fun pruneLowInformationTurns(messages: List<Message>): List<Message> {
        return messages.filter { msg ->
            val trimmed = msg.content.trim().lowercase(Locale.ROOT)
            val isShortNoise = trimmed.length <= 6 && PURE_NOISE_SNIPPETS.contains(trimmed)
            !isShortNoise
        }
    }

    // ==========================================
    // 5. 结构化多维摘要模型解析与本地抽取式兜底
    // ==========================================

    fun parseStructuredSummary(rawText: String): StructuredStateSummary {
        val text = rawText.trim()
        val constraints = mutableListOf<String>()
        val milestones = mutableListOf<String>()
        val openItems = mutableListOf<String>()

        var currentSection = 0 // 0: none, 1: constraints, 2: milestones, 3: openItems

        text.lineSequence().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.contains("核心背景") || trimmed.contains("固定约束") || trimmed.contains("约束") ->
                    currentSection = 1
                trimmed.contains("里程碑") || trimmed.contains("事件推进") || trimmed.contains("决策") ->
                    currentSection = 2
                trimmed.contains("未决") || trimmed.contains("待办") || trimmed.contains("下一步") ->
                    currentSection = 3
                trimmed.isNotBlank() -> {
                    val content = trimmed.removePrefix("-").removePrefix("•").removePrefix("*")
                        .replace(Regex("""^\d+[\.、]\s*"""), "").trim()
                    if (content.isNotBlank()) {
                        when (currentSection) {
                            1 -> constraints.add(content)
                            2 -> milestones.add(content)
                            3 -> openItems.add(content)
                            else -> {
                                if (milestones.size < 5) milestones.add(content)
                            }
                        }
                    }
                }
            }
        }

        return StructuredStateSummary(
            coreConstraints = constraints,
            milestones = milestones,
            openItems = openItems
        )
    }

    /**
     * 本地高保真抽取式结构化多维摘要兜底
     * 当外部模型调用不可用或超时时，基于启发式算法抽取核心约束、时序事件和待办项
     */
    fun generateExtractiveStructuredSummary(
        messages: List<Message>,
        maxTokens: Int = 1000
    ): StructuredStateSummary {
        val pruned = pruneLowInformationTurns(messages)
        val constraints = mutableListOf<String>()
        val milestones = mutableListOf<String>()
        val openItems = mutableListOf<String>()

        for (msg in pruned) {
            val content = msg.content.trim()
            val lower = content.lowercase(Locale.ROOT)

            // 1. 抽取用户固定约束与核心要求
            if (msg.role == "user") {
                if (listOf("请记住", "要求", "设定", "必须", "不要", "始终", "偏好").any { lower.contains(it) }) {
                    val clean = content.lines().firstOrNull { l ->
                        listOf("要求", "设定", "必须", "不要", "始终", "偏好", "记住").any { l.contains(it) }
                    }?.take(100) ?: content.take(80)
                    if (constraints.none { it == clean }) constraints.add(clean)
                }
            }

            // 2. 抽取关键里程碑与决策
            if (content.startsWith("[") || content.startsWith("【") || listOf("决定", "完成了", "达成", "发现", "商定", "推进至").any { lower.contains(it) }) {
                val clean = content.take(120)
                if (milestones.none { it == clean }) milestones.add(clean)
            }

            // 3. 抽取末尾未决议题与待办事项
            if (listOf("下一步", "还需", "待办", "待确认", "待解决", "稍后", "接下来需要").any { lower.contains(it) }) {
                val clean = content.take(100)
                if (openItems.none { it == clean }) openItems.add(clean)
            }
        }

        // 如果未命中明确里程碑，以近期关键轮次对话作为里程碑兜底
        if (milestones.isEmpty() && pruned.isNotEmpty()) {
            pruned.takeLast(6).forEach { msg ->
                val role = if (msg.role == "user") "用户" else "助手"
                milestones.add("$role: ${msg.content.take(70)}")
            }
        }

        return StructuredStateSummary(
            coreConstraints = constraints.take(4),
            milestones = milestones.take(8),
            openItems = openItems.take(4)
        )
    }

    /**
     * 生成引导模型输出结构化三层摘要的标准系统提示词
     */
    fun buildStructuredSummaryPrompt(
        existingSummary: String?,
        transcript: String,
        tokenBudget: Int
    ): String {
        return """
            请把下面的历史对话压缩提炼为高质量、结构化的多维滚动上下文状态机。
            严格按以下三部分输出，保证高信噪比，杜绝废话流式记录：

            【核心背景与用户固定约束】
            - 列出用户长期不变的目标、底线规则、输出格式约束、关键角色设定。

            【历史关键里程碑与决策推进】
            - 按时序提炼剧情/项目经历的核心事件、关键决定、已解决的问题（编号 1, 2, 3...）。

            【当前未决议题与待办上下文】
            - 提取当前对话停顿处正在进行、尚未完成的事项或下一步待办。

            要求：
            1. 控制在 $tokenBudget token 以内，使用简明中文。
            2. 不要输出任何开场白或“好的，以下是摘要”等客套。

            已有摘要：
            ${existingSummary ?: "无"}

            新增对话历史：
            $transcript
        """.trimIndent()
    }
}
