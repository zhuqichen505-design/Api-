package com.aiassistant.tools

import android.content.Context
import com.aiassistant.domain.model.ChatRequestOptions
import com.aiassistant.tools.cloud.JinaReaderEngine
import com.aiassistant.tools.cloud.OpenMeteoWeatherEngine
import com.aiassistant.tools.device.DeviceHardwareManager
import com.aiassistant.tools.device.HealthDataManager
import com.aiassistant.tools.device.LocationAddressManager
import com.aiassistant.tools.device.TimeCalendarManager
import com.aiassistant.tools.search.ExaSearchEngine
import com.aiassistant.tools.search.SearchEngineType
import com.aiassistant.utils.CryptoManager
import com.aiassistant.utils.TavilySearchManager
import com.aiassistant.utils.WebSearchBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class EchoToolHub(
    private val context: Context,
    private val cryptoManager: CryptoManager,
    val tavilySearchManager: TavilySearchManager
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val timeCalendarManager = TimeCalendarManager(context)
    val locationAddressManager = LocationAddressManager(context)
    val healthDataManager = HealthDataManager(context)
    val deviceHardwareManager = DeviceHardwareManager(context)
    val openMeteoWeatherEngine = OpenMeteoWeatherEngine()
    val jinaReaderEngine = JinaReaderEngine(getJinaApiKey())
    val exaSearchEngine = ExaSearchEngine(getExaApiKey())

    fun getSearchEngine(): SearchEngineType {
        val name = prefs.getString(KEY_SEARCH_ENGINE, SearchEngineType.EXA.name) ?: SearchEngineType.EXA.name
        return SearchEngineType.fromValue(name)
    }

    fun setSearchEngine(type: SearchEngineType) {
        prefs.edit().putString(KEY_SEARCH_ENGINE, type.name).apply()
    }

    fun getExaApiKey(): String {
        val encrypted = prefs.getString(KEY_EXA_KEY, "").orEmpty()
        return cryptoManager.decrypt(encrypted)
    }

    fun setExaApiKey(key: String) {
        val clean = key.trim()
        exaSearchEngine.updateApiKey(clean)
        prefs.edit().putString(KEY_EXA_KEY, cryptoManager.encrypt(clean)).apply()
    }

    fun getJinaApiKey(): String {
        val encrypted = prefs.getString(KEY_JINA_KEY, "").orEmpty()
        return cryptoManager.decrypt(encrypted)
    }

    fun setJinaApiKey(key: String) {
        val clean = key.trim()
        jinaReaderEngine.updateApiKey(clean)
        prefs.edit().putString(KEY_JINA_KEY, cryptoManager.encrypt(clean)).apply()
    }

    fun isDeviceToolsEnabled(): Boolean = prefs.getBoolean(KEY_DEVICE_TOOLS_ENABLED, true)

    fun setDeviceToolsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEVICE_TOOLS_ENABLED, enabled).apply()
    }

    suspend fun performWebSearch(query: String): Result<WebSearchBundle> = withContext(Dispatchers.IO) {
        when (getSearchEngine()) {
            SearchEngineType.EXA -> exaSearchEngine.search(query)
            SearchEngineType.TAVILY -> tavilySearchManager.search(query)
        }
    }

    suspend fun enrichUserPrompt(
        userMessage: String,
        options: ChatRequestOptions
    ): String = withContext(Dispatchers.IO) {
        val blocks = mutableListOf<String>()
        val trimmed = userMessage.trim()

        // 1. 联网搜索处理
        if (options.enableWebSearch == true) {
            val engine = getSearchEngine()
            if (engine == SearchEngineType.TAVILY && !tavilySearchManager.isReady()) {
                blocks.add("[联网搜索状态]\n用户开启了 Tavily 搜索，但未配置有效的 Tavily API Key。建议在设置中切换为「Exa (免Key即用)」搜索引擎。")
            } else {
                val searchResult = performWebSearch(trimmed)
                searchResult.fold(
                    onSuccess = { bundle ->
                        blocks.add(bundle.toPromptBlock())
                    },
                    onFailure = { err ->
                        blocks.add("[联网搜索状态]\n搜索调度未能成功获取结果: \n请明确说明本轮未能成功联网。")
                    }
                )
            }
        }

        // 若未启用智能设备工具箱，则直接返回基础处理结果
        if (!isDeviceToolsEnabled()) {
            return@withContext formatEnrichedMessage(userMessage, blocks)
        }

        // 2. 网页 URL 抓取意图（Jina Reader）
        val urlMatch = extractUrl(trimmed)
        if (urlMatch != null && shouldFetchWebpage(trimmed)) {
            val readResult = jinaReaderEngine.readUrl(urlMatch)
            readResult.fold(
                onSuccess = { page -> blocks.add(page.toPromptBlock()) },
                onFailure = { err -> blocks.add("【网页提取状态】\n抓取该网页时出现异常: ") }
            )
        }

        // 3. 天气意图（Open-Meteo）
        if (isWeatherIntent(trimmed)) {
            val city = extractCityForWeather(trimmed)
            val loc = locationAddressManager.getCurrentLocation()
            val weatherResult = openMeteoWeatherEngine.getWeather(
                cityName = city,
                defaultLat = loc.latitude,
                defaultLon = loc.longitude
            )
            weatherResult.fold(
                onSuccess = { report -> blocks.add(report.toPromptBlock()) },
                onFailure = { err -> blocks.add("【天气服务状态】\n实时气象查询未成功: ") }
            )
        }

        // 4. 时间与日历意图
        if (isTimeOrCalendarIntent(trimmed)) {
            blocks.add(timeCalendarManager.getTodayScheduleSummary())
        }

        // 5. 手机健康与步数意图
        if (isHealthIntent(trimmed)) {
            blocks.add(healthDataManager.getHealthDataSummary().toPromptBlock())
        }

        // 6. 手机设备与硬件状态意图
        if (isDeviceStatusIntent(trimmed)) {
            blocks.add(deviceHardwareManager.getDeviceStatus().toPromptBlock())
        }

        // 7. 定位与所在位置意图
        if (isLocationIntent(trimmed)) {
            blocks.add(locationAddressManager.getCurrentLocation().toPromptBlock())
        }

        formatEnrichedMessage(userMessage, blocks)
    }

    private fun formatEnrichedMessage(original: String, blocks: List<String>): String {
        if (blocks.isEmpty()) return original
        return buildString {
            blocks.forEach { block ->
                append(block)
                append("\n\n")
            }
            append("【用户输入的问题/指令】\n")
            append(original)
        }
    }

    companion object {
        private const val PREFS_NAME = "echo_tool_hub_prefs"
        private const val KEY_SEARCH_ENGINE = "tool_search_engine"
        private const val KEY_EXA_KEY = "tool_exa_api_key"
        private const val KEY_JINA_KEY = "tool_jina_api_key"
        private const val KEY_DEVICE_TOOLS_ENABLED = "tool_device_tools_enabled"

        private val URL_REGEX = Pattern.compile("https?://[\\w\\d:#@%/;~_?\\+-=\\\\\\.&]+")

        fun extractUrl(text: String): String? {
            val matcher = URL_REGEX.matcher(text)
            return if (matcher.find()) matcher.group(0) else null
        }

        fun shouldFetchWebpage(text: String): Boolean {
            val keywords = listOf("总结", "读一下", "看下", "提取", "网页", "文章", "链接", "分析这个", "什么内容")
            if (keywords.any { text.contains(it) }) return true
            // 如果文本主要是 URL
            val pure = text.trim()
            return pure.startsWith("http://") || pure.startsWith("https://")
        }

        fun isWeatherIntent(text: String): Boolean {
            val keywords = listOf("天气", "气温", "下雨", "降雨", "温度", "穿什么", "冷不冷", "热不热", "刮风", "气候", "有雨吗", "预报")
            return keywords.any { text.contains(it) }
        }

        fun extractCityForWeather(text: String): String? {
            // 先清理常见的发问引导词
            var cleaned = text
            val leadings = listOf("请问", "查一下", "查询", "帮我查", "帮我看一下", "帮我看看", "看一下", "看下", "查下", "告诉我", "想知道", "问一下", "我想知道")
            for (lead in leadings) {
                if (cleaned.startsWith(lead)) {
                    cleaned = cleaned.removePrefix(lead)
                }
            }

            // 匹配 如 "深圳天气", "上海的天气", "北京市气温", "杭州的气温"
            val regex = Regex("([\\u4e00-\\u9fa5]{2,6})(?:的)?(?:天气|气温|温度|预报)")
            val match = regex.find(cleaned)
            var candidate = match?.groupValues?.getOrNull(1) ?: return null

            for (lead in leadings) {
                if (candidate.startsWith(lead)) {
                    candidate = candidate.removePrefix(lead)
                }
            }

            candidate = candidate.removeSuffix("的")
            val timeWords = listOf("今天", "明天", "后天", "大后天", "这几天", "近几天", "实时", "现在", "目前", "本地", "当前")
            for (tw in timeWords) {
                if (candidate.endsWith(tw)) {
                    candidate = candidate.removeSuffix(tw)
                }
                if (candidate.startsWith(tw)) {
                    candidate = candidate.removePrefix(tw)
                }
            }
            candidate = candidate.removeSuffix("的")

            // 过滤掉非地名词汇与空值
            val exclusions = listOf("今天", "明天", "后天", "现在", "目前", "本地", "当前", "查一下", "看看", "天气", "气温", "预报")
            return if (candidate.length in 2..10 && candidate !in exclusions) candidate else null
        }

        fun isTimeOrCalendarIntent(text: String): Boolean {
            val keywords = listOf("几点", "几月几号", "今天几号", "今天星期几", "周几", "现在时间", "当前时间", "日程", "日历", "待办", "有什么安排")
            return keywords.any { text.contains(it) }
        }

        fun isHealthIntent(text: String): Boolean {
            val keywords = listOf("步数", "走了多少步", "运动步数", "今日步数", "心率", "心跳", "睡眠", "睡得怎么样", "健康数据", "深睡")
            return keywords.any { text.contains(it) }
        }

        fun isDeviceStatusIntent(text: String): Boolean {
            val keywords = listOf("电量", "电池", "充电", "还剩多少电", "手机型号", "系统版本", "内存剩余", "存储空间", "网络状态")
            return keywords.any { text.contains(it) }
        }

        fun isLocationIntent(text: String): Boolean {
            val keywords = listOf("我在哪", "我的位置", "当前位置", "所在城市", "哪个城市", "什么城市", "当前城市", "所在地", "经纬度", "哪个区", "什么地方", "在什么地方", "定位")
            return keywords.any { text.contains(it) }
        }
    }
}
