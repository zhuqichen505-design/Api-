package com.aiassistant.utils

import android.content.Context

data class PersonalizationSettings(
    val globalSystemPrompt: String = "",
    val enabled: Boolean = true,
    val aboutUser: String = "",
    val responseStyle: String = "",
    val preferences: String = "",
    val avoid: String = "",
    val autoMemoryEnabled: Boolean = true
)

class PersonalizationManager(private val context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "personalization_settings",
        Context.MODE_PRIVATE
    )
    private val legacyPrefs = context.applicationContext.getSharedPreferences(
        "app_settings",
        Context.MODE_PRIVATE
    )

    fun getSettings(): PersonalizationSettings {
        val legacyGlobalPrompt = legacyPrefs.getString("global_system_prompt", "").orEmpty()
        val globalPrompt = prefs.getString(KEY_GLOBAL_PROMPT, null) ?: legacyGlobalPrompt

        return PersonalizationSettings(
            globalSystemPrompt = globalPrompt,
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            aboutUser = prefs.getString(KEY_ABOUT_USER, "").orEmpty(),
            responseStyle = prefs.getString(KEY_RESPONSE_STYLE, "").orEmpty(),
            preferences = prefs.getString(KEY_PREFERENCES, "").orEmpty(),
            avoid = prefs.getString(KEY_AVOID, "").orEmpty(),
            autoMemoryEnabled = prefs.getBoolean(KEY_AUTO_MEMORY_ENABLED, true)
        )
    }

    fun saveSettings(settings: PersonalizationSettings): Boolean {
        legacyPrefs.edit().putString("global_system_prompt", settings.globalSystemPrompt).apply()
        return prefs.edit()
            .putString(KEY_GLOBAL_PROMPT, settings.globalSystemPrompt)
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putString(KEY_ABOUT_USER, settings.aboutUser)
            .putString(KEY_RESPONSE_STYLE, settings.responseStyle)
            .putString(KEY_PREFERENCES, settings.preferences)
            .putString(KEY_AVOID, settings.avoid)
            .putBoolean(KEY_AUTO_MEMORY_ENABLED, settings.autoMemoryEnabled)
            .commit()
    }

    fun buildPrompt(): String? {
        val settings = getSettings()
        if (!settings.enabled) return null

        val sections = listOfNotNull(
            settings.aboutUser.trim().takeIf { it.isNotBlank() }?.let {
                "用户自定义偏好：\n$it"
            },
            settings.responseStyle.trim().takeIf { it.isNotBlank() }?.let {
                "回答风格偏好：\n$it"
            },
            settings.preferences.trim().takeIf { it.isNotBlank() }?.let {
                "长期偏好和习惯：\n$it"
            },
            settings.avoid.trim().takeIf { it.isNotBlank() }?.let {
                "尽量避免：\n$it"
            }
        )

        if (sections.isEmpty()) return null

        return """
            以下是用户在应用内设置的个性化偏好。它们是长期偏好，不是本轮对话的新任务；当它们与用户当前消息或当前对话系统提示词冲突时，优先遵守更具体、更新的要求。

            ${sections.joinToString("\n\n")}
        """.trimIndent()
    }

    private companion object {
        const val KEY_GLOBAL_PROMPT = "global_system_prompt"
        const val KEY_ENABLED = "enabled"
        const val KEY_ABOUT_USER = "about_user"
        const val KEY_RESPONSE_STYLE = "response_style"
        const val KEY_PREFERENCES = "preferences"
        const val KEY_AVOID = "avoid"
        const val KEY_AUTO_MEMORY_ENABLED = "auto_memory_enabled"
    }
}
