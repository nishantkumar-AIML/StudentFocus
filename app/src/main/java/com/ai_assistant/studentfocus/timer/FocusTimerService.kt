package com.ai_assistant.studentfocus.timer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.ai_assistant.studentfocus.MainActivity

class FocusTimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        FocusSessionManager.init(this)
        GeneralTimerManager.init(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hasGeneralSession = GeneralTimerManager.hasActiveSession()
        val taskSession = FocusSessionManager.activeSession.value

        val notifEnabled = FocusNotificationHelper.areNotificationsEnabled(this)

        if (hasGeneralSession) {
            val notification = FocusNotificationHelper.buildGeneralTimerNotification(
                context = this,
                isStopwatch = GeneralTimerManager.timerMode.value == 0,
                elapsedSeconds = GeneralTimerManager.stopwatchElapsedSeconds.value,
                remainingSeconds = GeneralTimerManager.pomodoroRemainingSeconds.value,
                targetSeconds = GeneralTimerManager.pomodoroTargetSeconds.value,
                isRunning = GeneralTimerManager.isRunning.value
            )
            startForeground(FocusNotificationHelper.ONGOING_NOTIFICATION_ID, notification)
            if (!notifEnabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                FocusNotificationHelper.cancelAllNotifications(this)
            }
        } else if (taskSession != null) {
            val notification = FocusNotificationHelper.buildOngoingNotification(this, taskSession)
            startForeground(FocusNotificationHelper.ONGOING_NOTIFICATION_ID, notification)
            if (!notifEnabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                FocusNotificationHelper.cancelAllNotifications(this)
            }
        } else {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            FocusNotificationHelper.ACTION_PAUSE -> {
                if (GeneralTimerManager.isRunning.value) {
                    GeneralTimerManager.pauseTimer(this)
                } else if (FocusSessionManager.activeSession.value?.isRunning == true) {
                    FocusSessionManager.pauseSession(this)
                }
            }
            FocusNotificationHelper.ACTION_RESUME -> {
                if (!GeneralTimerManager.isRunning.value && (GeneralTimerManager.stopwatchElapsedSeconds.value > 0 || GeneralTimerManager.timerMode.value == 1)) {
                    GeneralTimerManager.startTimer(this)
                } else if (FocusSessionManager.activeSession.value?.isRunning == false) {
                    FocusSessionManager.resumeSession(this)
                }
            }
            FocusNotificationHelper.ACTION_RESET -> {
                GeneralTimerManager.resetTimer(this)
            }
            FocusNotificationHelper.ACTION_COMPLETE -> {
                // Open app so user can save notes or confirm
                val appIntent = Intent(this, MainActivity::class.java).apply {
                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(appIntent)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        FocusNotificationHelper.cancelOngoingNotification(this)
    }
}
