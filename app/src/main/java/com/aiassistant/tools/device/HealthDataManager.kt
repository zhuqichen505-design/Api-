package com.aiassistant.tools.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
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

    fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasStepSensor(): Boolean = stepSensor != null

    fun getSensorStatusText(): String {
        return when {
            stepSensor == null -> "设备无硬件计步传感器"
            !hasActivityRecognitionPermission() -> "未授权活动识别权限"
            isListening -> "硬件计步传感器运行中"
            else -> "传感器待命就绪"
        }
    }

    fun startListening() {
        if (!hasActivityRecognitionPermission()) return
        if (!isListening && stepSensor != null && sensorManager != null) {
            isListening = sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    sensorManager.flush(this)
                }
            } catch (_: Exception) {}
        }
    }

    fun stopListening() {
        if (isListening && sensorManager != null) {
            sensorManager.unregisterListener(this)
            isListening = false
        }
    }

    fun forceRefreshHardwareSteps(): HealthDataSummary {
        stopListening()
        startListening()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                sensorManager?.flush(this)
            }
        } catch (_: Exception) {}
        prefs.edit().putLong(KEY_HEALTH_UPDATE_TIME, System.currentTimeMillis()).apply()
        return getHealthDataSummary()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values.getOrNull(0)?.toInt() ?: return
            handleStepUpdate(totalSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Volatile
    private var lastHardwareTotal: Int = 0

    @Synchronized
    private fun handleStepUpdate(currentTotal: Int) {
        lastHardwareTotal = currentTotal
        val today = getTodayDateKey()
        val savedDate = prefs.getString(KEY_STEP_DATE, "").orEmpty()
        var baseline = prefs.getInt(KEY_STEP_BASELINE, -1)

        if (savedDate != today) {
            // 新的一天
            baseline = currentTotal
            prefs.edit()
                .putString(KEY_STEP_DATE, today)
                .putInt(KEY_STEP_BASELINE, baseline)
                .putInt(KEY_TODAY_STEPS, 0)
                .putInt(KEY_LAST_HARDWARE_TOTAL, currentTotal)
                .apply()
        } else {
            if (baseline < 0) {
                baseline = currentTotal
                prefs.edit().putInt(KEY_STEP_BASELINE, baseline).apply()
            }
            if (currentTotal < baseline) {
                baseline = 0
                prefs.edit().putInt(KEY_STEP_BASELINE, 0).apply()
            }
            val todaySteps = (currentTotal - baseline).coerceAtLeast(0)
            prefs.edit()
                .putInt(KEY_TODAY_STEPS, todaySteps)
                .putInt(KEY_LAST_HARDWARE_TOTAL, currentTotal)
                .apply()
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
        calibrateTodaySteps(steps)
    }

    /**
     * 用户手动校准华为运动健康步数：
     * 将今日步数与硬件传感器基准线对齐，后续步数实时自增
     */
    fun calibrateTodaySteps(steps: Int) {
        val today = getTodayDateKey()
        val validSteps = steps.coerceAtLeast(0)
        val currentHw = if (lastHardwareTotal > 0) lastHardwareTotal else prefs.getInt(KEY_LAST_HARDWARE_TOTAL, validSteps)
        val newBaseline = (currentHw - validSteps).coerceAtLeast(0)

        prefs.edit()
            .putString(KEY_STEP_DATE, today)
            .putInt(KEY_STEP_BASELINE, newBaseline)
            .putInt(KEY_TODAY_STEPS, validSteps)
            .putInt(KEY_LAST_HARDWARE_TOTAL, currentHw)
            .putLong(KEY_HEALTH_UPDATE_TIME, System.currentTimeMillis())
            .apply()
    }

    fun syncHuaweiHealthData(
        steps: Int,
        heartRate: Int,
        sleepHours: Int,
        sleepMinutes: Int,
        deepSleepMinutes: Int,
        sleepScore: Int
    ) {
        val totalSleepMins = sleepHours * 60 + sleepMinutes
        calibrateTodaySteps(steps)
        setHeartRate(heartRate)
        saveSleepRecord(totalSleepMins, deepSleepMinutes, sleepScore)
    }

    fun syncHuaweiHealthData(
        steps: Int,
        heartRate: Int,
        totalSleepMinutes: Int,
        deepSleepMinutes: Int,
        sleepScore: Int
    ) {
        calibrateTodaySteps(steps)
        setHeartRate(heartRate)
        saveSleepRecord(totalSleepMinutes, deepSleepMinutes, sleepScore)
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

    fun getLastUpdateTime(): Long = prefs.getLong(KEY_HEALTH_UPDATE_TIME, 0L)

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

            val estimatedKm = String.format(Locale.US, "%.2f", todaySteps * 0.0007)
            val estimatedKcal = (todaySteps * 0.035).toInt()

            return buildString {
                append("【用户手机健康与运动数据 (华为运动健康/硬件传感器)】")
                append("\n今日累计步数：").append(todaySteps).append(" 步")
                append(" (约 ").append(estimatedKm).append(" 公里, 消耗约 ").append(estimatedKcal).append(" 千卡)")
                if (todaySteps >= 10000) {
                    append(" - 已达成万步运动目标 🎉")
                } else {
                    append(" - 距一万步还差 ").append(10000 - todaySteps).append(" 步")
                }
                append("\n当前/最近心率：").append(heartRate).append(" bpm")
                append("\n昨晚睡眠时长：").append(sleepHours).append("小时").append(sleepMins).append("分钟")
                if (deepSleepMinutes > 0) {
                    append(" (其中深睡 ").append(deepHours).append("小时").append(deepMins).append("分)")
                }
                append("\n睡眠质量评分：").append(sleepScore).append(" 分")
                append("\n数据来源：华为运动健康与手机硬件步数传感器")
            }
        }
    }

    companion object {
        private const val KEY_STEP_DATE = "health_step_date"
        private const val KEY_STEP_BASELINE = "health_step_baseline"
        private const val KEY_LAST_HARDWARE_TOTAL = "health_last_hardware_total"
        private const val KEY_TODAY_STEPS = "health_today_steps"
        private const val KEY_HEART_RATE = "health_heart_rate"
        private const val KEY_SLEEP_MINUTES = "health_sleep_minutes"
        private const val KEY_DEEP_SLEEP_MINUTES = "health_deep_sleep_minutes"
        private const val KEY_SLEEP_SCORE = "health_sleep_score"
        private const val KEY_HEALTH_UPDATE_TIME = "health_update_time"
    }
}
