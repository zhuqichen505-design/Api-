package com.aiassistant

import android.app.Application
import com.aiassistant.domain.model.*
import com.aiassistant.utils.PersonalizationManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, manifest = Config.NONE)
class RetryPreferencesTest {
    @Test fun rulesSurviveManagerRecreationAndUnrelatedSettingsSave() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("personalization_settings", 0)
        prefs.edit().clear().commit()
        val manager = PersonalizationManager(context)
        assertEquals(RetryRule(true, 3), manager.getRetryPolicy().rule(RetryErrorType.TIMEOUT))
        assertFalse(manager.getRetryPolicy().rule(RetryErrorType.AUTHENTICATION).enabled)
        manager.saveRetryRule(RetryErrorType.TIMEOUT, RetryRule(false, 7))
        manager.saveRetryRule(RetryErrorType.RATE_LIMIT, RetryRule(true, 4))
        assertEquals(RetryRule(false, 7), manager.getRetryPolicy().rule(RetryErrorType.TIMEOUT))
        assertEquals(RetryRule(true, 4), manager.getRetryPolicy().rule(RetryErrorType.RATE_LIMIT))
        manager.setBackupKeyFallbackEnabled(false)
        manager.saveSettings(manager.getSettings().copy(autoNameEnabled = false))
        val recreated = PersonalizationManager(context)
        assertEquals(RetryRule(false, 7), recreated.getRetryPolicy().rule(RetryErrorType.TIMEOUT))
        assertEquals(RetryRule(true, 4), recreated.getRetryPolicy().rule(RetryErrorType.RATE_LIMIT))
        assertFalse(recreated.isBackupKeyFallbackEnabled())
        assertFalse(recreated.getSettings().autoNameEnabled)
    }

    @Test fun retryCountsClampWithoutChangingOtherCategories() {
        val manager = PersonalizationManager(RuntimeEnvironment.getApplication())
        manager.saveRetryRule(RetryErrorType.SERVER, RetryRule(true, 500))
        manager.saveRetryRule(RetryErrorType.BAD_REQUEST, RetryRule(true, -1))
        assertEquals(20, manager.getRetryPolicy().rule(RetryErrorType.SERVER).maxRetries)
        assertEquals(0, manager.getRetryPolicy().rule(RetryErrorType.BAD_REQUEST).maxRetries)
        assertFalse(manager.getRetryPolicy().canRetry(Exception("HTTP 400: invalid"), 0))
    }
}
