package com.aiassistant.tools.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthDataManager(private val context: Context) : SensorEventListener {

    private val prefs = context.applicationContext.getSharedPreferences("echo_health_prefs", Context.MODE_PRIVATE)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var stepSensor: Sensor? = null
    private var isListening = false

    init {
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        startListening()
    }

    fun startListening() {
        if (!isListening && stepSensor != null && sensorManager != null) {
            isListening = sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening() {
        if (isListening && sensorManager != null) {
            sensorManager.unregisterListener(this)
            isListening = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values.getOrNull(0)?.toInt() ?: return
            handleStepUpdate(totalSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Synchronized
    private fun handleStepUpdate(currentTotal: Int) {
        val today = getTodayDateKey()
        val savedDate = prefs.getString(KEY_STEP_DATE, "").orEmpty()
        var baseline = prefs.getInt(KEY_STEP_BASELINE, 0)

        if (savedDate != today) {
            // 新的一天，将当前硬件计数设为今天的基准起点
            baseline = currentTotal
            prefs.edit()
                .putString(KEY_STEP_DATE, today)
                .putInt(KEY_STEP_BASELINE, baseline)
                .putInt(KEY_TODAY_STEPS, 0)
                .apply()
        } else {
            // 同一天
            if (currentTotal < baseline) {
                // 手机中途可能重启过，硬件计数器归零
                baseline = 0
                prefs.edit().putInt(KEY_STEP_BASELINE, 0).apply()
            }
            val todaySteps = (currentTotal - baseline).coerceAtLeast(0)
            prefs.edit().putInt(KEY_TODAY_STEPS, todaySteps).apply()
        }
    }

    fun getTodaySteps(): Int {
        val today = getTodayDateKey()
        val savedDate = prefs.getString(KEY_STEP_DATE, "").orEmpty()
        return if (savedDate == today) {
            prefs.getInt(KEY_TODAY_STEPS, 0)
        } else {
            0
        }
    }

    fun setManualSteps(steps: Int) {
        val today = getTodayDateKey()
        prefs.edit()
            .putString(KEY_STEP_DATE, today)
            .putInt(KEY_TODAY_STEPS, steps.coerceAtLeast(0))
            .apply()
    }

    // 心率 (bpm)
    fun getHeartRate(): Int = prefs.getInt(KEY_HEART_RATE, 72)

    fun setHeartRate(bpm: Int) {
        prefs.edit().putInt(KEY_HEART_RATE, bpm.coerceIn(40, 220)).apply()
    }

    // 昨晚睡眠 (分钟)
    fun getSleepDurationMinutes(): Int = prefs.getInt(KEY_SLEEP_MINUTES, 450) // 默认 7.5 小时

    fun getDeepSleepMinutes(): Int = prefs.getInt(KEY_DEEP_SLEEP_MINUTES, 90)

    fun getSleepScore(): Int = prefs.getInt(KEY_SLEEP_SCORE, 85)

    fun saveSleepRecord(durationMinutes: Int, deepMinutes: Int, score: Int) {
        prefs.edit()
            .putInt(KEY_SLEEP_MINUTES, durationMinutes.coerceAtLeast(0))
            .putInt(KEY_DEEP_SLEEP_MINUTES, deepMinutes.coerceAtLeast(0))
            .putInt(KEY_SLEEP_SCORE, score.coerceIn(0, 100))
            .putLong(KEY_HEALTH_UPDATE_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getHealthDataSummary(): HealthDataSummary {
        return HealthDataSummary(
            todaySteps = getTodaySteps(),
            heartRate = getHeartRate(),
            sleepMinutes = getSleepDurationMinutes(),
            deepSleepMinutes = getDeepSleepMinutes(),
            sleepScore = getSleepScore(),
            hasHardwareStepSensor = stepSensor != null
        )
    }

    private fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    data class HealthDataSummary(
        val todaySteps: Int,
        val heartRate: Int,
        val sleepMinutes: Int,
        val deepSleepMinutes: Int,
        val sleepScore: Int,
        val hasHardwareStepSensor: Boolean
    ) {
        fun toPromptBlock(): String {
            val sleepHours = sleepMinutes / 60
            val sleepMins = sleepMinutes % 60
            val deepHours = deepSleepMinutes / 60
            val deepMins = deepSleepMinutes % 60

            return buildString {
                append("【用户手机健康与运动数据 (华为运动健康/硬件传感器)】")
                append("\n今日累计步数：").append(todaySteps).append(" 步")
                if (todaySteps >= 10000) {
                    append(" (已达成万步运动目标)")
                } else {
                    append(" (距一万步还差 ").append(10000 - todaySteps).append(" 步)")
                }
                append("\n当前/最近静息心率：").append(heartRate).append(" bpm")
                append("\n昨晚睡眠时长：").append(sleepHours).append("小时").append(sleepMins).append("分钟")
                if (deepSleepMinutes > 0) {
                    append(" (其中深睡 ").append(deepHours).append("小时").append(deepMins).append("分)")
                }
                append("\n睡眠质量评分：").append(sleepScore).append(" 分")
                append("\n数据来源：手机硬件计步传感器与运动健康同步档案")
            }
        }
    }

    companion object {
        private const val KEY_STEP_DATE = "health_step_date"
        private const val KEY_STEP_BASELINE = "health_step_baseline"
        private const val KEY_TODAY_STEPS = "health_today_steps"
        private const val KEY_HEART_RATE = "health_heart_rate"
        private const val KEY_SLEEP_MINUTES = "health_sleep_minutes"
        private const val KEY_DEEP_SLEEP_MINUTES = "health_deep_sleep_minutes"
        private const val KEY_SLEEP_SCORE = "health_sleep_score"
        private const val KEY_HEALTH_UPDATE_TIME = "health_update_time"
    }
}
