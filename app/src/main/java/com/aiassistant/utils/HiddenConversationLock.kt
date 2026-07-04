package com.aiassistant.utils

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

class HiddenConversationLock(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "hidden_conversation_lock",
        Context.MODE_PRIVATE
    )

    fun hasPassword(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)
    }

    fun setPassword(pin: String): Boolean {
        if (!pin.matches(PIN_REGEX)) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltText = Base64.encodeToString(salt, Base64.NO_WRAP)
        prefs.edit()
            .putString(KEY_PIN_SALT, saltText)
            .putString(KEY_PIN_HASH, hashPin(pin, saltText))
            .apply()
        return true
    }

    fun verify(pin: String): Boolean {
        if (!pin.matches(PIN_REGEX)) return false
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val expected = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val actual = hashPin(pin, salt)
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            actual.toByteArray(Charsets.UTF_8)
        )
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        private val PIN_REGEX = Regex("\\d{6}")
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
    }
}
