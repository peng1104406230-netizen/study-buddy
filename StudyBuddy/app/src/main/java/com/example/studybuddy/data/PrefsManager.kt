package com.example.studybuddy.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * 本地存储管理
 */
object PrefsManager {

    private const val PREFS_NAME = "study_buddy_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_SETUP_COMPLETE = "setup_complete"
    private const val KEY_REMINDER_HOUR = "reminder_hour"
    private const val KEY_REMINDER_MINUTE = "reminder_minute"
    private const val KEY_IS_STUDYING = "is_studying"
    private const val KEY_STUDY_START_TIME = "study_start_time"
    private const val KEY_RESET_TIME = "reset_time"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getUserId(): String {
        var userId = prefs.getString(KEY_USER_ID, null)
        if (userId == null) {
            userId = "user_${UUID.randomUUID().toString().take(8)}"
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }
        return userId
    }

    /** 是否已完成首次设置 */
    fun isSetupComplete(): Boolean {
        return prefs.getBoolean(KEY_SETUP_COMPLETE, false)
    }

    /** 标记首次设置完成 */
    fun markSetupComplete() {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
    }

    /** 保存提醒时间 */
    fun saveReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
    }

    fun getReminderHour(): Int = prefs.getInt(KEY_REMINDER_HOUR, 21)
    fun getReminderMinute(): Int = prefs.getInt(KEY_REMINDER_MINUTE, 30)

    /** 开始学习：记录开始时间 */
    fun startStudying() {
        prefs.edit()
            .putBoolean(KEY_IS_STUDYING, true)
            .putLong(KEY_STUDY_START_TIME, System.currentTimeMillis())
            .apply()
    }

    /** 结束学习：返回学习时长（秒），并清除状态 */
    fun stopStudying(): Long {
        val startTime = prefs.getLong(KEY_STUDY_START_TIME, 0L)
        val duration = if (startTime > 0) (System.currentTimeMillis() - startTime) / 1000 else 0
        prefs.edit()
            .putBoolean(KEY_IS_STUDYING, false)
            .putLong(KEY_STUDY_START_TIME, 0L)
            .apply()
        return duration
    }

    /** 是否正在学习中 */
    fun isStudying(): Boolean = prefs.getBoolean(KEY_IS_STUDYING, false)

    /** 获取学习开始时间 */
    fun getStudyStartTime(): Long = prefs.getLong(KEY_STUDY_START_TIME, 0L)

    /** 清除所有数据（保留 user_id，记录重置时间） */
    fun clearAll() {
        val userId = getUserId()
        prefs.edit().clear().apply()
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_RESET_TIME, java.time.LocalDateTime.now().toString())
            .apply()
    }

    /** 获取重置时间（用于过滤旧历史记录） */
    fun getResetTime(): String? = prefs.getString(KEY_RESET_TIME, null)
}
