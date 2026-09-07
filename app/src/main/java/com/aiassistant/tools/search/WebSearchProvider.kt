package com.aiassistant.tools.search

import com.aiassistant.utils.WebSearchBundle

enum class SearchEngineType(val displayName: String) {
    EXA("Exa 搜索 (免Key即用)"),
    TAVILY("Tavily 搜索 (需API Key)");

    companion object {
        fun fromValue(value: String): SearchEngineType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXA
        }
    }
}

interface WebSearchProvider {
    val engineType: SearchEngineType
    fun isReady(): Boolean
    fun search(query: String, maxResults: Int = 5): Result<WebSearchBundle>
}
