package com.ai_assistant.studentfocus.timer

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

object TimeWasteManager {
    private const val PREFS_NAME = "student_focus_time_waste"
    private const val KEY_TODAY_DATE = "waste_today_date"
    private const val KEY_TODAY_SECONDS = "waste_today_seconds"
    private const val KEY_TOTAL_SECONDS = "waste_total_seconds"
    private const val KEY_LAST_TICK_TS = "waste_last_tick_ts"

    private var prefs: SharedPreferences? = null

    private val _todayWastedSeconds = MutableStateFlow(0L)
    val todayWastedSeconds: StateFlow<Long> = _todayWastedSeconds.asStateFlow()

    private val _totalWastedSeconds = MutableStateFlow(0L)
    val totalWastedSeconds: StateFlow<Long> = _totalWastedSeconds.asStateFlow()

    private val _isWasteTicking = MutableStateFlow(false)
    val isWasteTicking: StateFlow<Boolean> = _isWasteTicking.asStateFlow()

    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null
    private var isInitialized = false

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isInitialized = true

        onAppResumed()
        startTicker()
    }

    private fun getTodayDateStr(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private var timeTableSlots: List<com.ai_assistant.studentfocus.models.TimeTableSlotEntity> = emptyList()

    fun updateTimeTableSlots(slots: List<com.ai_assistant.studentfocus.models.TimeTableSlotEntity>) {
        timeTableSlots = slots
        // Check immediate state transition
        val studyActive = isStudyActive()
        _isWasteTicking.value = !studyActive
    }

    fun isStudyActive(): Boolean {
        val isTaskActive = FocusSessionManager.activeSession.value?.isRunning == true
        val isGeneralTimerActive = GeneralTimerManager.isRunning.value
        val isAutoTimeTableActive = isAutoTimeTableSlotActiveNow()
        return isTaskActive || isGeneralTimerActive || isAutoTimeTableActive
    }

    fun isAutoTimeTableSlotActiveNow(): Boolean {
        if (timeTableSlots.isEmpty()) return false
        val now = Calendar.getInstance()
        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(now.time)
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentMinsOfDay = currentHour * 60 + currentMinute

        return timeTableSlots.any { slot ->
            if (!slot.isTimeWasteAutoManaged) return@any false
            val isDayMatch = slot.dayOfWeek.equals("Daily", ignoreCase = true) ||
                    slot.dayOfWeek.equals(currentDay, ignoreCase = true)
            if (!isDayMatch) return@any false

            try {
                val startParts = slot.startTime.split(":").map { it.toInt() }
                val endParts = slot.endTime.split(":").map { it.toInt() }
                val startMins = startParts[0] * 60 + startParts[1]
                val endMins = endParts[0] * 60 + endParts[1]

                if (endMins >= startMins) {
                    currentMinsOfDay in startMins until endMins
                } else {
                    currentMinsOfDay >= startMins || currentMinsOfDay < endMins
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Called when the app starts or resumes from background.
     * Calculates the exact elapsed time while the app was closed/backgrounded
     * and credits it to wasted time if no study task was active.
     */
    fun onAppResumed() {
        val p = prefs ?: return
        val now = System.currentTimeMillis()
        val todayStr = getTodayDateStr()

        val lastTs = p.getLong(KEY_LAST_TICK_TS, 0L)
        val savedToday = p.getLong(KEY_TODAY_SECONDS, 0L)
        val savedTotal = p.getLong(KEY_TOTAL_SECONDS, 0L)
        val savedDate = p.getString(KEY_TODAY_DATE, todayStr) ?: todayStr

        val todayMidnightCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMidnightTs = todayMidnightCalendar.timeInMillis

        // If this is the very first launch
        if (lastTs == 0L) {
            _todayWastedSeconds.value = savedToday
            _totalWastedSeconds.value = savedTotal
            _isWasteTicking.value = !isStudyActive()
            p.edit()
                .putString(KEY_TODAY_DATE, todayStr)
                .putLong(KEY_TODAY_SECONDS, savedToday)
                .putLong(KEY_TOTAL_SECONDS, savedTotal)
                .putLong(KEY_LAST_TICK_TS, now)
                .apply()
            startTicker()
            return
        }

        // Calculate time elapsed while app was closed / paused
        val elapsedSeconds = if (now > lastTs) (now - lastTs) / 1000L else 0L

        // Check if a study task focus session or general timer was running during that time
        val studyActive = isStudyActive()

        var newToday: Long
        var newTotal: Long

        if (studyActive) {
            // Study was running -> Don't add to waste
            newToday = if (savedDate == todayStr) savedToday else 0L
            newTotal = savedTotal
        } else {
            // No study was running -> Add elapsed idle time to waste counters
            newTotal = savedTotal + elapsedSeconds

            if (savedDate == todayStr && lastTs >= todayMidnightTs) {
                // Closed and reopened on the same day
                newToday = savedToday + elapsedSeconds
            } else {
                // Day rolled over (midnight passed while app was closed)
                val secondsSinceMidnight = (now - todayMidnightTs) / 1000L
                newToday = minOf(86400L, maxOf(0L, secondsSinceMidnight))
            }
        }

        _todayWastedSeconds.value = newToday
        _totalWastedSeconds.value = newTotal
        _isWasteTicking.value = !studyActive

        p.edit()
            .putString(KEY_TODAY_DATE, todayStr)
            .putLong(KEY_TODAY_SECONDS, newToday)
            .putLong(KEY_TOTAL_SECONDS, newTotal)
            .putLong(KEY_LAST_TICK_TS, now)
            .apply()

        // Always restart ticker on resume!
        startTicker()
    }

    /**
     * Called when the app is paused / goes into the background.
     * Records the exact timestamp so elapsed time can be calculated on resume.
     * Stops the foreground ticker so zero CPU is wasted while app is backgrounded.
     */
    fun onAppPaused() {
        tickerJob?.cancel()
        tickerJob = null
        _isWasteTicking.value = false

        val p = prefs ?: return
        val now = System.currentTimeMillis()
        val todayStr = getTodayDateStr()

        p.edit()
            .putString(KEY_TODAY_DATE, todayStr)
            .putLong(KEY_TODAY_SECONDS, _todayWastedSeconds.value)
            .putLong(KEY_TOTAL_SECONDS, _totalWastedSeconds.value)
            .putLong(KEY_LAST_TICK_TS, now)
            .apply()
    }

    fun startTicker() {
        if (tickerJob?.isActive == true) return

        if (!scope.isActive) {
            scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        }

        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                tickOneSecond()
            }
        }
    }

    private fun tickOneSecond() {
        val now = System.currentTimeMillis()
        val todayStr = getTodayDateStr()
        val p = prefs

        // Check if any study task session or general timer is currently running
        val studyActive = isStudyActive()

        if (studyActive) {
            // Task is ON -> Waste counter is PAUSED
            _isWasteTicking.value = false
            return
        }

        // No task is ON -> Waste counter is TICKING (+1s every second in foreground)
        _isWasteTicking.value = true

        val savedDate = p?.getString(KEY_TODAY_DATE, todayStr) ?: todayStr
        if (savedDate != todayStr) {
            // Day rollover at midnight
            _todayWastedSeconds.value = 0L
            p?.edit()?.putString(KEY_TODAY_DATE, todayStr)?.apply()
        }

        val newToday = _todayWastedSeconds.value + 1L
        val newTotal = _totalWastedSeconds.value + 1L

        _todayWastedSeconds.value = newToday
        _totalWastedSeconds.value = newTotal

        // Save periodically every 30 seconds to avoid flash wear
        if (newToday % 30L == 0L) {
            p?.edit()
                ?.putString(KEY_TODAY_DATE, todayStr)
                ?.putLong(KEY_TODAY_SECONDS, newToday)
                ?.putLong(KEY_TOTAL_SECONDS, newTotal)
                ?.putLong(KEY_LAST_TICK_TS, now)
                ?.apply()
        }
    }

    fun resetStats() {
        val now = System.currentTimeMillis()
        val todayStr = getTodayDateStr()
        _todayWastedSeconds.value = 0L
        _totalWastedSeconds.value = 0L
        prefs?.edit()
            ?.putString(KEY_TODAY_DATE, todayStr)
            ?.putLong(KEY_TODAY_SECONDS, 0L)
            ?.putLong(KEY_TOTAL_SECONDS, 0L)
            ?.putLong(KEY_LAST_TICK_TS, now)
            ?.apply()
        startTicker()
    }

    fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%dh %02dm %02ds", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds)
        }
    }
}
