package com.aiassistant.utils

import com.google.gson.JsonParser
import java.util.UUID
import java.util.regex.Pattern

/**
 * 时间线与设定条目分类
 */
enum class TimelineCategory(val displayName: String, val emoji: String, val tagColorHex: String) {
    PLOT_EVENT("剧情推进", "📖", "#2196F3"),
    RULE_CONSTRAINT("规则约束", "⚖️", "#FF9800"),
    CHARACTER_SETTING("角色设定", "🎭", "#9C27B0"),
    WORLD_SETTING("剧情设定", "🌍", "#4CAF50"),
    ATEMPORAL_SETTING("固有设定", "💡", "#E91E63");

    companion object {
        fun fromKey(key: String?): TimelineCategory {
            if (key.isNullOrBlank()) return PLOT_EVENT
            val lower = key.lowercase()
            return when {
                lower.contains("atemporal") || lower.contains("固有") || lower.contains("常驻") || lower.contains("固有设定") -> ATEMPORAL_SETTING
                lower.contains("rule") || lower.contains("constraint") || lower.contains("规则") || lower.contains("约束") || lower.contains("禁止") -> RULE_CONSTRAINT
                lower.contains("char") || lower.contains("role") || lower.contains("角色") || lower.contains("人物") || lower.contains("关系") -> CHARACTER_SETTING
                lower.contains("world") || lower.contains("scene") || lower.contains("世界") || lower.contains("设定") || lower.contains("状态") -> WORLD_SETTING
                else -> PLOT_EVENT
            }
        }
    }
}

/**
 * 时间线单条事件或时间锚定设定模型
 */
data class TimelineEventItem(
    val id: String = UUID.randomUUID().toString(),
    var timeTag: String = "",
    var content: String = "",
    var category: TimelineCategory = TimelineCategory.PLOT_EVENT
)

/**
 * 与具体时间无关的全局角色/世界设定模型（用于确认加入记忆）
 */
data class AtemporalSettingItem(
    val id: String = UUID.randomUUID().toString(),
    var category: String = "角色设定", // 角色特质 / 习惯偏好 / 世界规则 / 生理禁忌 / 人际羁绊
    var content: String = "",
    var isSelected: Boolean = true,
    var targetScope: String = "session" // "session" (会话专属记忆) 或 "global" (长期记忆)
) {
    fun nextCategory(): String = when (category) {
        "角色设定", "角色特质" -> "习惯偏好"
        "习惯偏好" -> "生理禁忌"
        "生理禁忌" -> "世界规则"
        "世界规则" -> "人际羁绊"
        else -> "角色特质"
    }
}

/**
 * 时间线全量校对提炼结果模型
 */
data class TimelineReconcileResult(
    var currentStoryTime: String = "",
    val events: MutableList<TimelineEventItem> = mutableListOf(),
    val atemporalSettings: MutableList<AtemporalSettingItem> = mutableListOf(),
    var extractionSource: String = "AI_MODEL", // "AI_MODEL" 或 "LOCAL_FALLBACK"
    var modelUsed: String = "",
    var extractionErrorMessage: String? = null
)

/**
 * 时间线记忆辅助类
 */
object TimelineMemoryHelper {

    // 匹配如 [第3天·傍晚]、[第3天]、[DAY 2]、[10月5日·上午]、[周三·晚上] 等时间标签
    private val TIME_TAG_PATTERN = Pattern.compile("""^[\s\[【](?:第\s*(\d+)\s*天(?:[·\s\-]([^\]】]+))?|DAY\s*(\d+)|([^\]】]+))[\]】]\s*(.*)$""", Pattern.CASE_INSENSITIVE)
    private val DAY_NUMBER_PATTERN = Pattern.compile("""(?:第\s*(\d+)\s*天|DAY\s*(\d+))""", Pattern.CASE_INSENSITIVE)
    private val CATEGORY_TAG_PATTERN = Pattern.compile("""^\[(剧情推进|规则约束|角色设定|剧情设定|固有设定)\]\s*(.*)$""")

    /**
     * 判断某文本是否为纯用户导演/作者剧情指导（而非故事发生的客观事实）
     */
    fun isPureDirectorInstruction(text: String): Boolean {
        val trimmed = text.trim()
        val isEnclosed = (trimmed.startsWith("[") && trimmed.endsWith("]")) ||
                         (trimmed.startsWith("【") && trimmed.endsWith("】")) ||
                         (trimmed.startsWith("(") && trimmed.endsWith(")")) ||
                         (trimmed.startsWith("（") && trimmed.endsWith("）"))
        if (!isEnclosed) return false
        val inside = trimmed.substring(1, trimmed.length - 1).trim()
        val directorVerbs = listOf(
            "让", "请继续", "继续写", "重写", "改写", "描写", "从配角视角", "视角切换",
            "增加", "冲突升级", "不要解决", "接下来让", "推进", "剧情推进", "展开", "写一段",
            "安排", "设定为", "要求", "注意", "提示", "下文", "接上文", "继续", "剧情发展"
        )
        return directorVerbs.any { inside.startsWith(it) || inside.contains(it) }
    }

    private fun getPhaseOrder(subPhase: String): Int {
        return when {
            subPhase.contains("早") || subPhase.contains("晨") || subPhase.contains("上午") -> 1
            subPhase.contains("中") || subPhase.contains("午") -> 2
            subPhase.contains("傍晚") || subPhase.contains("黄昏") -> 3
            subPhase.contains("晚") || subPhase.contains("夜") || subPhase.contains("宿") -> 4
            else -> 0
        }
    }

    /**
     * 判断并估算叙事时间跨度跃迁天数（例如：两周后、一个月后、数日后）
     * 返回跃迁的天数（> 0），若非时间跨度则返回 0
     */
    fun estimateTimeSpanJumpDays(tag: String): Int {
        return when {
            tag.contains("三年") || tag.contains("3年") -> 1095
            tag.contains("两年") || tag.contains("2年") -> 730
            tag.contains("一年") || tag.contains("1年") -> 365
            tag.contains("半年") -> 180
            tag.contains("三个月") || tag.contains("3个月") -> 90
            tag.contains("两个月") || tag.contains("2个月") -> 60
            tag.contains("一个月") || tag.contains("1个月") || tag.contains("一月后") || tag.contains("次月") -> 30
            tag.contains("半个月") -> 15
            tag.contains("三周") || tag.contains("3周") -> 21
            tag.contains("两周") || tag.contains("2周") -> 14
            tag.contains("一周") || tag.contains("1周") || tag.contains("一星期") || tag.contains("七天") -> 7
            tag.contains("数日") || tag.contains("几天") || tag.contains("三天") || tag.contains("3天") -> 3
            tag.contains("两天") || tag.contains("2天") -> 2
            else -> 0
        }
    }

    /**
     * 单调递增时序状态机：
     * 1. 解决“第二天”剧情发生后，后续再次出现的“第二天/次日/第二天早上”被机械识别为第2天的严重时序倒流错误，
     *    将重复或相对次日单调递增累进为绝对故事天数（第3天、第4天...）。
     * 2. 拥抱自然文学叙事与阶段锚点：并非所有事件都以具体“第X天”为单位，全面兼容“两周过后”、“暑假开始”、“一年后·春”等
     *    自然时间跨度与阶段性事件，合理维护内部递增推进的同时，完整保留真实文学叙事时间标签。
     */
    fun normalizeMonotonicTimeline(events: List<TimelineEventItem>): List<TimelineEventItem> {
        var currentDay = 1
        var lastPhaseOrder = 0
        var hasSeenDayInCurrentEpoch = false
        val normalized = mutableListOf<TimelineEventItem>()

        for (item in events) {
            val tag = item.timeTag.trim()
            val matcher = DAY_NUMBER_PATTERN.matcher(tag)
            val isRelativeNextDay = tag.contains("第二天") || tag.contains("次日") || tag.contains("翌日") || tag.contains("隔天") || tag.contains("又过了一天")
            val spanJumpDays = estimateTimeSpanJumpDays(tag)

            val updatedTag = if (matcher.find()) {
                val parsedDay = (matcher.group(1) ?: matcher.group(2))?.toIntOrNull() ?: 1
                val subPhase = tag.substringAfter("·", "").ifBlank {
                    tag.substringAfter("天", "").trim('-', ' ', '·')
                }
                val phaseOrder = getPhaseOrder(subPhase)

                if (parsedDay > currentDay) {
                    currentDay = parsedDay
                    lastPhaseOrder = phaseOrder
                    hasSeenDayInCurrentEpoch = true
                } else if (parsedDay == currentDay) {
                    // 若在同一天内，但后文出现的时段比前文更早（如傍晚之后出现早上），说明已经跨过了夜晚进入了次日！
                    // 或者如果包含“第二天/次日”标记且前序已有事件，也是次日！
                    if (isRelativeNextDay && hasSeenDayInCurrentEpoch) {
                        currentDay++
                        lastPhaseOrder = phaseOrder
                    } else if (lastPhaseOrder > 0 && phaseOrder > 0 && phaseOrder <= lastPhaseOrder) {
                        currentDay++
                        lastPhaseOrder = phaseOrder
                    } else {
                        if (phaseOrder > lastPhaseOrder) lastPhaseOrder = phaseOrder
                        hasSeenDayInCurrentEpoch = true
                    }
                } else {
                    // parsedDay < currentDay
                    currentDay++
                    lastPhaseOrder = phaseOrder
                }
                
                if (subPhase.isNotBlank()) "第 $currentDay 天·$subPhase" else "第 $currentDay 天"
            } else if (isRelativeNextDay) {
                currentDay++
                val subPhase = when {
                    tag.contains("早") || tag.contains("晨") || tag.contains("上午") -> "早晨"
                    tag.contains("午") -> "中午"
                    tag.contains("傍晚") || tag.contains("黄昏") -> "傍晚"
                    tag.contains("夜") || tag.contains("晚") -> "夜间"
                    else -> "白天"
                }
                lastPhaseOrder = getPhaseOrder(subPhase)
                hasSeenDayInCurrentEpoch = true
                "第 $currentDay 天·$subPhase"
            } else if (spanJumpDays > 0) {
                // 遇到“两周过后”、“一个月后”等跨度跳跃词，在内部天数上向前推进，同时完整保留自然描述标签
                currentDay += spanJumpDays
                lastPhaseOrder = 0
                hasSeenDayInCurrentEpoch = false
                tag
            } else {
                // 阶段性叙事节点（如“暑假开始”、“开学第一天”、“深秋·初雪”），完整保留叙事标签
                val subPhase = tag.substringAfter("·", "")
                if (subPhase.isNotBlank()) {
                    lastPhaseOrder = getPhaseOrder(subPhase)
                }
                tag
            }

            normalized.add(item.copy(timeTag = updatedTag))
        }

        return normalized
    }

    /**
     * 将原始记忆内容解析为结构化事件
     */
    fun parseContentToEvent(rawContent: String): TimelineEventItem {
        val trimmed = rawContent.trim()
        val matcher = TIME_TAG_PATTERN.matcher(trimmed)
        if (matcher.find()) {
            val contentGroup = matcher.group(5).orEmpty()
            val fullTag = trimmed.substring(matcher.start(), matcher.end() - contentGroup.length)
                .trim('[', ']', '【', '】', ' ')
            var eventContent = contentGroup.trim()
            var cat = TimelineCategory.PLOT_EVENT

            val catMatcher = CATEGORY_TAG_PATTERN.matcher(eventContent)
            if (catMatcher.find()) {
                val catName = catMatcher.group(1)
                cat = TimelineCategory.fromKey(catName)
                eventContent = catMatcher.group(2)?.trim().orEmpty()
            } else if (eventContent.startsWith("【规则") || eventContent.startsWith("[规则")) {
                cat = TimelineCategory.RULE_CONSTRAINT
                eventContent = eventContent.replace(Regex("""^[\[【][^\]】]+[\]】]\s*"""), "").trim()
            } else if (eventContent.startsWith("【角色") || eventContent.startsWith("[角色")) {
                cat = TimelineCategory.CHARACTER_SETTING
                eventContent = eventContent.replace(Regex("""^[\[【][^\]】]+[\]】]\s*"""), "").trim()
            } else if (eventContent.startsWith("【设定") || eventContent.startsWith("[设定") || eventContent.startsWith("【世界")) {
                cat = TimelineCategory.WORLD_SETTING
                eventContent = eventContent.replace(Regex("""^[\[【][^\]】]+[\]】]\s*"""), "").trim()
            } else if (eventContent.startsWith("【固有") || eventContent.startsWith("[固有") || eventContent.startsWith("【常驻") || eventContent.startsWith("[常驻")) {
                cat = TimelineCategory.ATEMPORAL_SETTING
                eventContent = eventContent.replace(Regex("""^[\[【][^\]】]+[\]】]\s*"""), "").trim()
            }

            if (eventContent.isNotBlank()) {
                return TimelineEventItem(
                    timeTag = fullTag,
                    content = eventContent,
                    category = cat
                )
            }
        }
        return TimelineEventItem(
            timeTag = "",
            content = trimmed,
            category = TimelineCategory.PLOT_EVENT
        )
    }

    /**
     * 将时间标签、类别与事件内容格式化为标准化存储字符串
     */
    fun formatEventContent(timeTag: String, content: String, category: TimelineCategory = TimelineCategory.PLOT_EVENT): String {
        val cleanTag = timeTag.trim().trim('[', ']', '【', '】')
        val cleanContent = content.trim()
        val catPrefix = if (category != TimelineCategory.PLOT_EVENT) "[${category.displayName}] " else ""
        return if (cleanTag.isNotBlank()) {
            "[$cleanTag] $catPrefix$cleanContent"
        } else {
            "$catPrefix$cleanContent"
        }
    }

    /**
     * 解析模型输出（支持标准 JSON、Markdown 包裹的 JSON，以及行列表格式兜底）
     */
    fun parseModelOutput(rawOutput: String): TimelineReconcileResult {
        val trimmed = rawOutput.trim()
        if (trimmed.isBlank()) {
            return TimelineReconcileResult(currentStoryTime = "未确定", events = mutableListOf())
        }

        // 1. 尝试提取 JSON 代码块或 JSON 字符串
        val jsonStr = extractJsonString(trimmed)
        if (jsonStr != null) {
            try {
                val jsonElement = JsonParser.parseString(jsonStr)
                if (jsonElement.isJsonObject) {
                    val obj = jsonElement.asJsonObject
                    val currentTime = obj.get("currentStoryTime")?.asString?.trim().orEmpty()
                    val eventsList = mutableListOf<TimelineEventItem>()
                    val atemporalList = mutableListOf<AtemporalSettingItem>()

                    // 解析时序事件
                    val eventsArray = obj.getAsJsonArray("timelineEvents")
                        ?: obj.getAsJsonArray("events")
                    if (eventsArray != null) {
                        for (item in eventsArray) {
                            if (item.isJsonObject) {
                                val itemObj = item.asJsonObject
                                val tag = itemObj.get("timeTag")?.asString?.trim()
                                    ?: itemObj.get("time")?.asString?.trim()
                                    ?: itemObj.get("day")?.asString?.trim().orEmpty()
                                val text = itemObj.get("content")?.asString?.trim()
                                    ?: itemObj.get("event")?.asString?.trim().orEmpty()
                                val catKey = itemObj.get("category")?.asString?.trim()
                                val cat = TimelineCategory.fromKey(catKey)
                                if (text.isNotBlank()) {
                                    eventsList.add(TimelineEventItem(timeTag = tag, content = text, category = cat))
                                }
                            } else if (item.isJsonPrimitive) {
                                val text = item.asString.trim()
                                if (text.isNotBlank()) {
                                    eventsList.add(parseContentToEvent(text))
                                }
                            }
                        }
                    }

                    // 解析时间无关设定 (atemporalSettings)
                    val atemporalArray = obj.getAsJsonArray("atemporalSettings")
                        ?: obj.getAsJsonArray("settings")
                        ?: obj.getAsJsonArray("globalSettings")
                    if (atemporalArray != null) {
                        for (item in atemporalArray) {
                            if (item.isJsonObject) {
                                val itemObj = item.asJsonObject
                                val cat = itemObj.get("category")?.asString?.trim() ?: "角色设定"
                                val text = itemObj.get("content")?.asString?.trim()
                                    ?: itemObj.get("setting")?.asString?.trim().orEmpty()
                                val scope = itemObj.get("targetScope")?.asString?.trim() ?: "session"
                                if (text.isNotBlank()) {
                                    atemporalList.add(
                                        AtemporalSettingItem(
                                            category = cat,
                                            content = text,
                                            isSelected = true,
                                            targetScope = if (scope == "global") "global" else "session"
                                        )
                                    )
                                }
                            } else if (item.isJsonPrimitive) {
                                val text = item.asString.trim()
                                if (text.isNotBlank()) {
                                    atemporalList.add(
                                        AtemporalSettingItem(
                                            category = "世界/角色设定",
                                            content = text,
                                            isSelected = true
                                        )
                                    )
                                }
                            }
                        }
                    }

                    val normalizedEvents = normalizeMonotonicTimeline(eventsList)
                    return TimelineReconcileResult(
                        currentStoryTime = currentTime.ifBlank { inferCurrentStoryTime(normalizedEvents) },
                        events = normalizedEvents.toMutableList(),
                        atemporalSettings = atemporalList
                    )
                }
            } catch (_: Exception) {
                // JSON 解析失败降级走文本解析
            }
        }

        // 2. 纯文本行列表解析兜底
        return parseTextFallback(trimmed)
    }

    private fun extractJsonString(text: String): String? {
        val codeBlockRegex = Regex("""```(?:json)?\s*([\s\S]*?)\s*```""", RegexOption.IGNORE_CASE)
        val match = codeBlockRegex.find(text)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1).trim()
        }
        return null
    }

    private fun parseTextFallback(text: String): TimelineReconcileResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        var currentStoryTime = ""
        val events = mutableListOf<TimelineEventItem>()
        val atemporalList = mutableListOf<AtemporalSettingItem>()

        var isParsingAtemporal = false

        for (line in lines) {
            val clean = line.removePrefix("-").removePrefix("*").trim()
            if (clean.contains("当前故事时间") || clean.contains("当前时间") || clean.contains("当前停留在")) {
                currentStoryTime = clean.substringAfter("：").substringAfter(":").trim()
                continue
            }
            if (clean.contains("时间无关") || clean.contains("全局设定") || clean.contains("固定规则") || clean.contains("角色特质")) {
                isParsingAtemporal = true
                continue
            }

            if (isParsingAtemporal) {
                if (clean.isNotBlank()) {
                    atemporalList.add(
                        AtemporalSettingItem(
                            category = if (clean.contains("规则") || clean.contains("禁止")) "世界规则" else "角色设定",
                            content = clean,
                            isSelected = true
                        )
                    )
                }
            } else {
                if (clean.startsWith("[") || clean.startsWith("【") || (clean.contains("第") && clean.contains("天"))) {
                    events.add(parseContentToEvent(clean))
                } else if (clean.length > 5) {
                    events.add(TimelineEventItem(timeTag = "", content = clean))
                }
            }
        }

        val normalizedEvents = normalizeMonotonicTimeline(events)
        return TimelineReconcileResult(
            currentStoryTime = currentStoryTime.ifBlank { inferCurrentStoryTime(normalizedEvents) },
            events = normalizedEvents.toMutableList(),
            atemporalSettings = atemporalList
        )
    }

    /**
     * 根据事件列表推断当前故事时间：
     * 1. 优先取倒序最新发生的事件的时间标签（支持“两周过后”、“暑假开始”、“第 5 天·傍晚”等自然与显式时间）；
     * 2. 若列表无任何有效时间标签，返回“未确定”。
     */
    fun inferCurrentStoryTime(events: List<TimelineEventItem>): String {
        if (events.isEmpty()) return "未确定"
        // 倒序寻找最新发生的事件有效时间标签
        for (item in events.reversed()) {
            val tag = item.timeTag.trim().trim('[', ']', '【', '】')
            if (tag.isNotBlank() && tag != "未确定") {
                return tag
            }
        }
        return "未确定"
    }

    /**
     * 计算事件时间相对于当前时间的相对参照（如：今天 / 昨天 / 2天前 / 约两周前）
     */
    fun calculateRelativeTime(eventTimeTag: String, currentTimeTag: String): String? {
        val cleanEvent = eventTimeTag.trim().trim('[', ']', '【', '】')
        val cleanCurrent = currentTimeTag.trim().trim('[', ']', '【', '】')
        if (cleanEvent.isBlank() || cleanCurrent.isBlank()) return null
        if (cleanEvent == cleanCurrent) return "今天"

        val eventMatcher = DAY_NUMBER_PATTERN.matcher(cleanEvent)
        val currentMatcher = DAY_NUMBER_PATTERN.matcher(cleanCurrent)

        if (eventMatcher.find() && currentMatcher.find()) {
            val eventDay = (eventMatcher.group(1) ?: eventMatcher.group(2))?.toIntOrNull() ?: return null
            val currentDay = (currentMatcher.group(1) ?: currentMatcher.group(2))?.toIntOrNull() ?: return null

            val diff = currentDay - eventDay
            return when {
                diff == 0 -> "今天"
                diff == 1 -> "昨天"
                diff == 2 -> "前天"
                diff > 2 -> "${diff}天前"
                diff == -1 -> "明天"
                diff < -1 -> "${-diff}天后"
                else -> null
            }
        }

        // 自然时间相对语义推导
        if (cleanCurrent.contains("两周") && (cleanEvent.contains("第 1 天") || cleanEvent.contains("第1天") || cleanEvent.contains("初遇") || cleanEvent.contains("初期"))) {
            return "约两周前"
        }
        if (cleanCurrent.contains("暑假") && cleanEvent.contains("学期")) {
            return "放假前"
        }
        if (cleanCurrent.contains("开学") && cleanEvent.contains("暑假")) {
            return "暑假期间"
        }

        return null
    }

    /**
     * 构建注入给大模型的完整时间线参照上下文
     */
    fun buildTimelinePromptContext(currentStoryTime: String?, memoryContents: List<String>): String {
        if (memoryContents.isEmpty()) return ""

        val effectiveCurrent = currentStoryTime?.trim()?.takeIf { it.isNotBlank() } ?: "未指定"
        val sb = StringBuilder()
        sb.append("【故事当前时间节点】：$effectiveCurrent\n")
        sb.append("【会话真实时间线与日常备忘】：\n")

        memoryContents.forEach { raw ->
            val event = parseContentToEvent(raw)
            val catPrefix = if (event.category != TimelineCategory.PLOT_EVENT) "【${event.category.displayName}】" else ""
            if (event.timeTag.isNotBlank()) {
                val rel = calculateRelativeTime(event.timeTag, effectiveCurrent)
                val relPrefix = if (rel != null) "（相对于当前：$rel）" else ""
                sb.append("- [${event.timeTag}]$relPrefix $catPrefix${event.content}\n")
            } else {
                sb.append("- $catPrefix${event.content}\n")
            }
        }

        sb.append("\n【时序交互准则】：请严格保持对上述时间线的认知。当用户使用“昨天”、“上次”、“之前”等相对时间词时，请务必根据时间线参照系准确对齐具体是哪一天发生的事件，严禁将不同日子的事件混淆为同一天。")
        return sb.toString().trim()
    }
}
