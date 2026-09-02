package com.ai_assistant.studentfocus.timer

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object GeneralTimerManager {

    private const val PREFS_NAME = "student_general_timer_prefs"
    private const val KEY_TIMER_MODE = "timer_mode"
    private const val KEY_IS_RUNNING = "is_running"
    private const val KEY_STOPWATCH_ACC = "stopwatch_accumulated"
    private const val KEY_STOPWATCH_LAST_RESUME = "stopwatch_last_resume"
    private const val KEY_POMODORO_TARGET = "pomodoro_target"
    private const val KEY_POMODORO_ACC_ELAPSED = "pomodoro_accumulated_elapsed"
    private const val KEY_POMODORO_LAST_RESUME = "pomodoro_last_resume"

    // Mode: 0 for Stopwatch, 1 for Pomodoro
    private val _timerMode = MutableStateFlow(0)
    val timerMode: StateFlow<Int> = _timerMode.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // Stopwatch state
    private var stopwatchAccumulatedSeconds = 0
    private var stopwatchLastResumeTimestamp = 0L
    private val _stopwatchElapsedSeconds = MutableStateFlow(0)
    val stopwatchElapsedSeconds: StateFlow<Int> = _stopwatchElapsedSeconds.asStateFlow()

    // Pomodoro state
    private val _pomodoroTargetSeconds = MutableStateFlow(1500) // 25 min default
    val pomodoroTargetSeconds: StateFlow<Int> = _pomodoroTargetSeconds.asStateFlow()

    private var pomodoroAccumulatedElapsed = 0
    private var pomodoroLastResumeTimestamp = 0L
    private val _pomodoroRemainingSeconds = MutableStateFlow(1500)
    val pomodoroRemainingSeconds: StateFlow<Int> = _pomodoroRemainingSeconds.asStateFlow()

    var onPomodoroComplete: (() -> Unit)? = null

    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null

    private var isInitialized = false

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedMode = prefs.getInt(KEY_TIMER_MODE, 0)
            val savedRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
            val savedStopwatchAcc = prefs.getInt(KEY_STOPWATCH_ACC, 0)
            val savedStopwatchLastResume = prefs.getLong(KEY_STOPWATCH_LAST_RESUME, 0L)
            val savedPomoTarget = prefs.getInt(KEY_POMODORO_TARGET, 1500)
            val savedPomoAccElapsed = prefs.getInt(KEY_POMODORO_ACC_ELAPSED, 0)
            val savedPomoLastResume = prefs.getLong(KEY_POMODORO_LAST_RESUME, 0L)

            _timerMode.value = savedMode
            _pomodoroTargetSeconds.value = savedPomoTarget

            val now = System.currentTimeMillis()
            if (savedRunning) {
                if (savedMode == 0) {
                    val runSecs = if (savedStopwatchLastResume > 0L) maxOf(0, ((now - savedStopwatchLastResume) / 1000).toInt()) else 0
                    val totalSecs = minOf(86400, savedStopwatchAcc + runSecs)
                    stopwatchAccumulatedSeconds = totalSecs
                    stopwatchLastResumeTimestamp = now
                    _stopwatchElapsedSeconds.value = totalSecs
                    _isRunning.value = true
                    startTicker(context)
                } else {
                    val runSecs = if (savedPomoLastResume > 0L) maxOf(0, ((now - savedPomoLastResume) / 1000).toInt()) else 0
                    val totalElapsed = savedPomoAccElapsed + runSecs
                    if (totalElapsed >= savedPomoTarget) {
                        pomodoroAccumulatedElapsed = savedPomoTarget
                        _pomodoroRemainingSeconds.value = 0
                        _isRunning.value = false
                        onPomodoroComplete?.invoke()
                    } else {
                        pomodoroAccumulatedElapsed = totalElapsed
                        pomodoroLastResumeTimestamp = now
                        _pomodoroRemainingSeconds.value = savedPomoTarget - totalElapsed
                        _isRunning.value = true
                        startTicker(context)
                    }
                }
            } else {
                _isRunning.value = false
                stopwatchAccumulatedSeconds = savedStopwatchAcc
                _stopwatchElapsedSeconds.value = savedStopwatchAcc
                pomodoroAccumulatedElapsed = savedPomoAccElapsed
                _pomodoroRemainingSeconds.value = maxOf(0, savedPomoTarget - savedPomoAccElapsed)
            }

            isInitialized = true
            saveToPrefs(context)
            if (_isRunning.value) {
                updateServiceNotification(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isInitialized = true
        }
    }

    private fun saveToPrefs(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(KEY_TIMER_MODE, _timerMode.value)
                .putBoolean(KEY_IS_RUNNING, _isRunning.value)
                .putInt(KEY_STOPWATCH_ACC, stopwatchAccumulatedSeconds)
                .putLong(KEY_STOPWATCH_LAST_RESUME, stopwatchLastResumeTimestamp)
                .putInt(KEY_POMODORO_TARGET, _pomodoroTargetSeconds.value)
                .putInt(KEY_POMODORO_ACC_ELAPSED, pomodoroAccumulatedElapsed)
                .putLong(KEY_POMODORO_LAST_RESUME, pomodoroLastResumeTimestamp)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hasActiveSession(): Boolean {
        return _isRunning.value || (_timerMode.value == 0 && _stopwatchElapsedSeconds.value > 0) || (_timerMode.value == 1 && _pomodoroRemainingSeconds.value < _pomodoroTargetSeconds.value)
    }

    fun setTimerMode(mode: Int, context: Context? = null) {
        if (!isInitialized && context != null) init(context)
        if (_timerMode.value != mode) {
            context?.let { pauseTimer(it) } ?: run { _isRunning.value = false; tickerJob?.cancel() }
            _timerMode.value = mode
            if (mode == 1) {
                pomodoroAccumulatedElapsed = 0
                _pomodoroRemainingSeconds.value = _pomodoroTargetSeconds.value
            } else {
                stopwatchAccumulatedSeconds = 0
                _stopwatchElapsedSeconds.value = 0
            }
            context?.let { saveToPrefs(it) }
        }
    }

    fun setPomodoroTarget(minutes: Int, context: Context? = null) {
        if (!isInitialized && context != null) init(context)
        context?.let { pauseTimer(it) } ?: run { _isRunning.value = false; tickerJob?.cancel() }
        val clampedMins = minutes.coerceIn(1, 1440)
        _pomodoroTargetSeconds.value = clampedMins * 60
        pomodoroAccumulatedElapsed = 0
        _pomodoroRemainingSeconds.value = clampedMins * 60
        context?.let { saveToPrefs(it) }
    }

    fun startTimer(context: Context) {
        if (!isInitialized) init(context)
        if (_isRunning.value) return
        _isRunning.value = true

        val now = System.currentTimeMillis()
        if (_timerMode.value == 0) {
            stopwatchLastResumeTimestamp = now
        } else {
            pomodoroLastResumeTimestamp = now
        }
        saveToPrefs(context)

        if (FocusNotificationHelper.areNotificationsEnabled(context)) {
            try {
                val serviceIntent = Intent(context, FocusTimerService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        startTicker(context)
        updateServiceNotification(context)
    }

    fun pauseTimer(context: Context) {
        if (!isInitialized) init(context)
        if (!_isRunning.value) return
        _isRunning.value = false
        tickerJob?.cancel()

        val now = System.currentTimeMillis()
        if (_timerMode.value == 0) {
            val runSecs = maxOf(0, ((now - stopwatchLastResumeTimestamp) / 1000).toInt())
            stopwatchAccumulatedSeconds = minOf(86400, stopwatchAccumulatedSeconds + runSecs)
            _stopwatchElapsedSeconds.value = stopwatchAccumulatedSeconds
        } else {
            val runSecs = maxOf(0, ((now - pomodoroLastResumeTimestamp) / 1000).toInt())
            pomodoroAccumulatedElapsed = minOf(_pomodoroTargetSeconds.value, pomodoroAccumulatedElapsed + runSecs)
            _pomodoroRemainingSeconds.value = maxOf(0, _pomodoroTargetSeconds.value - pomodoroAccumulatedElapsed)
        }
        saveToPrefs(context)
        updateServiceNotification(context)
    }

    fun resetTimer(context: Context) {
        if (!isInitialized) init(context)
        _isRunning.value = false
        tickerJob?.cancel()

        if (_timerMode.value == 0) {
            stopwatchAccumulatedSeconds = 0
            _stopwatchElapsedSeconds.value = 0
        } else {
            pomodoroAccumulatedElapsed = 0
            _pomodoroRemainingSeconds.value = _pomodoroTargetSeconds.value
        }
        saveToPrefs(context)

        // If no other task is running, stop service
        if (FocusSessionManager.activeSession.value == null) {
            try {
                val serviceIntent = Intent(context, FocusTimerService::class.java)
                context.stopService(serviceIntent)
                FocusNotificationHelper.cancelOngoingNotification(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            FocusSessionManager.updateServiceNotification(context)
        }
    }

    private fun startTicker(context: Context) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                if (!_isRunning.value) break

                val now = System.currentTimeMillis()
                if (_timerMode.value == 0) {
                    val runSecs = maxOf(0, ((now - stopwatchLastResumeTimestamp) / 1000).toInt())
                    val totalSecs = minOf(86400, stopwatchAccumulatedSeconds + runSecs)
                    _stopwatchElapsedSeconds.value = totalSecs

                    if (totalSecs >= 86400) {
                        pauseTimer(context)
                        break
                    }
                    if (totalSecs % 60 == 0) {
                        updateServiceNotification(context)
                    }
                } else {
                    val runSecs = maxOf(0, ((now - pomodoroLastResumeTimestamp) / 1000).toInt())
                    val totalElapsed = pomodoroAccumulatedElapsed + runSecs
                    val remaining = maxOf(0, _pomodoroTargetSeconds.value - totalElapsed)
                    _pomodoroRemainingSeconds.value = remaining

                    if (remaining <= 0) {
                        pomodoroAccumulatedElapsed = _pomodoroTargetSeconds.value
                        _isRunning.value = false
                        saveToPrefs(context)
                        FocusNotificationHelper.showPomodoroCompletionNotification(
                            context = context,
                            targetMinutes = _pomodoroTargetSeconds.value / 60
                        )
                        onPomodoroComplete?.invoke()
                        updateServiceNotification(context)
                        break
                    }
                    if (remaining % 60 == 0) {
                        updateServiceNotification(context)
                    }
                }
            }
        }
    }

    fun updateServiceNotification(context: Context) {
        if (!FocusNotificationHelper.areNotificationsEnabled(context) ||
            !FocusNotificationHelper.areOngoingTimerNotificationsEnabled(context)) {
            FocusNotificationHelper.cancelOngoingNotification(context)
            return
        }
        if (!hasActiveSession() && FocusSessionManager.activeSession.value == null) return
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val notification = if (hasActiveSession()) {
                FocusNotificationHelper.buildGeneralTimerNotification(
                    context = context,
                    isStopwatch = _timerMode.value == 0,
                    elapsedSeconds = _stopwatchElapsedSeconds.value,
                    remainingSeconds = _pomodoroRemainingSeconds.value,
                    targetSeconds = _pomodoroTargetSeconds.value,
                    isRunning = _isRunning.value
                )
            } else {
                val taskSession = FocusSessionManager.activeSession.value ?: return
                FocusNotificationHelper.buildOngoingNotification(context, taskSession)
            }

            manager.notify(FocusNotificationHelper.ONGOING_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

