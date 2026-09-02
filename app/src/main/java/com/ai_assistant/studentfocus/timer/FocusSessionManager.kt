package com.ai_assistant.studentfocus.timer

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ai_assistant.studentfocus.models.ActiveFocusSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object FocusSessionManager {

    private const val PREFS_NAME = "student_focus_sessions_prefs"
    private const val KEY_SESSIONS = "active_sessions_data"
    private const val KEY_ACTIVE_TASK_ID = "active_task_id"

    // Map of all active/paused task focus sessions: taskId -> ActiveFocusSession
    private val _taskSessions = MutableStateFlow<Map<String, ActiveFocusSession>>(emptyMap())
    val taskSessions: StateFlow<Map<String, ActiveFocusSession>> = _taskSessions.asStateFlow()

    // The currently ticking/selected active session
    private val _activeSession = MutableStateFlow<ActiveFocusSession?>(null)
    val activeSession: StateFlow<ActiveFocusSession?> = _activeSession.asStateFlow()

    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null

    // Callback when session finishes naturally
    var onSessionExpired: ((ActiveFocusSession) -> Unit)? = null

    private var isInitialized = false

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_SESSIONS, null)
            val activeId = prefs.getString(KEY_ACTIVE_TASK_ID, null)

            if (jsonString.isNullOrBlank()) {
                isInitialized = true
                return
            }

            val jsonArray = JSONArray(jsonString)
            val now = System.currentTimeMillis()
            val restoredMap = mutableMapOf<String, ActiveFocusSession>()
            var expiredToTrigger: ActiveFocusSession? = null
            var hasRunning = false

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val taskId = obj.getString("taskId")
                val taskTitle = obj.getString("taskTitle")
                val targetMins = obj.getInt("targetDurationMinutes")
                val startTs = obj.optLong("startTimestamp", now)
                val accSecs = obj.optInt("accumulatedSeconds", 0)
                val lastResume = obj.optLong("lastResumeTimestamp", now)
                val isRunning = obj.optBoolean("isRunning", false)
                val isExpired = obj.optBoolean("isPausedByExpiry", false)

                if (isRunning) {
                    val runSecs = maxOf(0, ((now - lastResume) / 1000).toInt())
                    val totalElapsed = accSecs + runSecs
                    val targetSecs = targetMins * 60

                    if (totalElapsed >= targetSecs) {
                        // Reached target duration while user was away! Capped right at target time.
                        val expiredSession = ActiveFocusSession(
                            taskId = taskId,
                            taskTitle = taskTitle,
                            targetDurationMinutes = targetMins,
                            startTimestamp = startTs,
                            accumulatedSeconds = targetSecs,
                            lastResumeTimestamp = lastResume,
                            isRunning = false,
                            isPausedByExpiry = true,
                            currentTick = now
                        )
                        restoredMap[taskId] = expiredSession
                        if (taskId == activeId || expiredToTrigger == null) {
                            expiredToTrigger = expiredSession
                        }
                    } else {
                        // Still in progress, calculate exact elapsed time
                        val runningSession = ActiveFocusSession(
                            taskId = taskId,
                            taskTitle = taskTitle,
                            targetDurationMinutes = targetMins,
                            startTimestamp = startTs,
                            accumulatedSeconds = totalElapsed,
                            lastResumeTimestamp = now,
                            isRunning = true,
                            isPausedByExpiry = false,
                            currentTick = now
                        )
                        restoredMap[taskId] = runningSession
                        hasRunning = true
                    }
                } else {
                    val pausedSession = ActiveFocusSession(
                        taskId = taskId,
                        taskTitle = taskTitle,
                        targetDurationMinutes = targetMins,
                        startTimestamp = startTs,
                        accumulatedSeconds = accSecs,
                        lastResumeTimestamp = lastResume,
                        isRunning = false,
                        isPausedByExpiry = isExpired,
                        currentTick = now
                    )
                    restoredMap[taskId] = pausedSession
                    if (isExpired && (taskId == activeId || expiredToTrigger == null)) {
                        expiredToTrigger = pausedSession
                    }
                }
            }

            _taskSessions.value = restoredMap
            _activeSession.value = if (activeId != null && restoredMap.containsKey(activeId)) {
                restoredMap[activeId]
            } else {
                restoredMap.values.firstOrNull()
            }

            isInitialized = true
            saveToPrefs(context)

            if (expiredToTrigger != null) {
                onSessionExpired?.invoke(expiredToTrigger)
            }

            if (hasRunning) {
                startTicker(context)
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
            val jsonArray = JSONArray()
            for ((_, session) in _taskSessions.value) {
                val obj = JSONObject().apply {
                    put("taskId", session.taskId)
                    put("taskTitle", session.taskTitle)
                    put("targetDurationMinutes", session.targetDurationMinutes)
                    put("startTimestamp", session.startTimestamp)
                    put("accumulatedSeconds", session.accumulatedSeconds)
                    put("lastResumeTimestamp", session.lastResumeTimestamp)
                    put("isRunning", session.isRunning)
                    put("isPausedByExpiry", session.isPausedByExpiry)
                }
                jsonArray.put(obj)
            }
            prefs.edit()
                .putString(KEY_SESSIONS, jsonArray.toString())
                .putString(KEY_ACTIVE_TASK_ID, _activeSession.value?.taskId)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startFocusSession(
        context: Context,
        taskId: String,
        taskTitle: String,
        targetDurationMinutes: Int
    ) {
        if (!isInitialized) init(context)
        val now = System.currentTimeMillis()
        val currentMap = _taskSessions.value.toMutableMap()

        // 1. If another task is currently active and running, PAUSE it (do not delete or reset it)
        val currentRunning = _activeSession.value
        if (currentRunning != null && currentRunning.taskId != taskId && currentRunning.isRunning) {
            val elapsed = currentRunning.getLiveElapsedSeconds(now)
            val paused = currentRunning.copy(
                accumulatedSeconds = elapsed,
                lastResumeTimestamp = now,
                isRunning = false,
                currentTick = now
            )
            currentMap[currentRunning.taskId] = paused
        }

        // 2. If this task already had a paused session, resume it with the target duration
        val existingSession = currentMap[taskId]
        val sessionToRun = if (existingSession != null && !existingSession.isPausedByExpiry) {
            existingSession.copy(
                lastResumeTimestamp = now,
                isRunning = true,
                isPausedByExpiry = false,
                currentTick = now
            )
        } else {
            ActiveFocusSession(
                taskId = taskId,
                taskTitle = taskTitle,
                targetDurationMinutes = targetDurationMinutes,
                startTimestamp = now,
                accumulatedSeconds = 0,
                lastResumeTimestamp = now,
                isRunning = true,
                isPausedByExpiry = false,
                currentTick = now
            )
        }

        currentMap[taskId] = sessionToRun
        _taskSessions.value = currentMap
        _activeSession.value = sessionToRun
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

    fun pauseSession(context: Context, taskId: String? = null) {
        if (!isInitialized) init(context)
        val targetId = taskId ?: _activeSession.value?.taskId ?: return
        val currentMap = _taskSessions.value.toMutableMap()
        val session = currentMap[targetId] ?: return
        if (!session.isRunning) return

        val now = System.currentTimeMillis()
        val elapsed = session.getLiveElapsedSeconds(now)
        val updated = session.copy(
            accumulatedSeconds = elapsed,
            lastResumeTimestamp = now,
            isRunning = false,
            currentTick = now
        )
        currentMap[targetId] = updated
        _taskSessions.value = currentMap

        if (_activeSession.value?.taskId == targetId) {
            _activeSession.value = updated
        }
        saveToPrefs(context)
        updateServiceNotification(context)
    }

    fun resumeSession(context: Context, taskId: String? = null) {
        if (!isInitialized) init(context)
        val targetId = taskId ?: _activeSession.value?.taskId ?: return
        val now = System.currentTimeMillis()
        val currentMap = _taskSessions.value.toMutableMap()

        // Pause any other currently running task
        val currentRunning = _activeSession.value
        if (currentRunning != null && currentRunning.taskId != targetId && currentRunning.isRunning) {
            val elapsed = currentRunning.getLiveElapsedSeconds(now)
            val paused = currentRunning.copy(
                accumulatedSeconds = elapsed,
                lastResumeTimestamp = now,
                isRunning = false,
                currentTick = now
            )
            currentMap[currentRunning.taskId] = paused
        }

        val session = currentMap[targetId] ?: return
        val updated = session.copy(
            lastResumeTimestamp = now,
            isRunning = true,
            isPausedByExpiry = false,
            currentTick = now
        )
        currentMap[targetId] = updated
        _taskSessions.value = currentMap
        _activeSession.value = updated
        saveToPrefs(context)

        startTicker(context)
        updateServiceNotification(context)
    }

    fun extendSession(context: Context, extraMinutes: Int, taskId: String? = null) {
        if (!isInitialized) init(context)
        val targetId = taskId ?: _activeSession.value?.taskId ?: return
        val currentMap = _taskSessions.value.toMutableMap()
        val session = currentMap[targetId] ?: return

        val newTarget = session.targetDurationMinutes + extraMinutes
        val now = System.currentTimeMillis()
        val updated = session.copy(
            targetDurationMinutes = newTarget,
            lastResumeTimestamp = now,
            isRunning = true,
            isPausedByExpiry = false,
            currentTick = now
        )
        currentMap[targetId] = updated
        _taskSessions.value = currentMap
        _activeSession.value = updated
        saveToPrefs(context)

        startTicker(context)
        updateServiceNotification(context)
    }

    fun stopSession(context: Context, taskId: String? = null) {
        if (!isInitialized) init(context)
        val targetId = taskId ?: _activeSession.value?.taskId
        val currentMap = _taskSessions.value.toMutableMap()

        if (targetId != null) {
            currentMap.remove(targetId)
            _taskSessions.value = currentMap
        }

        if (_activeSession.value?.taskId == targetId || targetId == null) {
            // If another task is paused, switch active to it or null
            val nextSession = currentMap.values.firstOrNull()
            _activeSession.value = nextSession
        }
        saveToPrefs(context)

        if (_taskSessions.value.isEmpty()) {
            tickerJob?.cancel()
            try {
                val serviceIntent = Intent(context, FocusTimerService::class.java)
                context.stopService(serviceIntent)
                FocusNotificationHelper.cancelOngoingNotification(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            updateServiceNotification(context)
        }
    }

    private fun startTicker(context: Context) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val current = _activeSession.value ?: break
                if (current.isRunning) {
                    val now = System.currentTimeMillis()
                    if (current.isTargetReached(now)) {
                        val totalElapsed = current.targetDurationMinutes * 60
                        val expiredSession = current.copy(
                            accumulatedSeconds = totalElapsed,
                            isRunning = false,
                            isPausedByExpiry = true,
                            currentTick = now
                        )
                        val map = _taskSessions.value.toMutableMap()
                        map[current.taskId] = expiredSession
                        _taskSessions.value = map
                        _activeSession.value = expiredSession
                        saveToPrefs(context)

                        FocusNotificationHelper.showCompletionNotification(
                            context = context,
                            taskTitle = expiredSession.taskTitle,
                            minutesStudied = expiredSession.targetDurationMinutes
                        )

                        onSessionExpired?.invoke(expiredSession)
                        updateServiceNotification(context)
                        break
                    } else {
                        // Update currentTick with new timestamp to guarantee StateFlow emission
                        val tickedSession = current.copy(currentTick = now)
                        val map = _taskSessions.value.toMutableMap()
                        map[current.taskId] = tickedSession
                        _taskSessions.value = map
                        _activeSession.value = tickedSession

                        // Chronometer in notification handles live countdown natively.
                        // Refresh notification periodically every 60s as a fallback sync.
                        if (current.getLiveRemainingSeconds(now) % 60 == 0) {
                            updateServiceNotification(context)
                        }
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
        val session = _activeSession.value ?: return
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(
                FocusNotificationHelper.ONGOING_NOTIFICATION_ID,
                FocusNotificationHelper.buildOngoingNotification(context, session)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
