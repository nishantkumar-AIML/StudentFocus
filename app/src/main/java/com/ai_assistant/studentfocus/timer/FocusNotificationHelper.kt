package com.ai_assistant.studentfocus.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ai_assistant.studentfocus.MainActivity
import com.ai_assistant.studentfocus.R
import com.ai_assistant.studentfocus.models.ActiveFocusSession

object FocusNotificationHelper {

    const val CHANNEL_ONGOING_ID = "focus_ongoing_channel"
    const val CHANNEL_COMPLETE_ID = "focus_complete_channel"
    const val CHANNEL_DAILY_REMINDER_ID = "daily_task_reminder_channel"
    const val ONGOING_NOTIFICATION_ID = 1001
    const val COMPLETE_NOTIFICATION_ID = 1002
    const val DAILY_REMINDER_NOTIFICATION_ID = 1005

    const val ACTION_PAUSE = "com.ai_assistant.studentfocus.ACTION_PAUSE"
    const val ACTION_RESUME = "com.ai_assistant.studentfocus.ACTION_RESUME"
    const val ACTION_RESET = "com.ai_assistant.studentfocus.ACTION_RESET"
    const val ACTION_COMPLETE = "com.ai_assistant.studentfocus.ACTION_COMPLETE"

    private const val PREFS_NAME = "app_settings"
    const val KEY_NOTIF_ENABLED = "notifications_enabled"
    const val KEY_NOTIF_ONGOING_TIMER = "notif_ongoing_timer"
    const val KEY_NOTIF_SESSION_COMPLETE = "notif_session_complete"
    const val KEY_NOTIF_DAILY_REMINDER = "notif_daily_reminder"
    const val KEY_NOTIF_TIMETABLE_REMINDER = "notif_timetable_reminder"
    const val KEY_SOUND_ENABLED = "notification_sound_enabled"

    fun areNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIF_ENABLED, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply()
        if (!enabled) {
            cancelAllNotifications(context)
            DailyReminderScheduler.cancelDailyReminders(context)
            TimeTableReminderScheduler.cancelAllTimeTableReminders(context)
            try {
                val serviceIntent = Intent(context, FocusTimerService::class.java)
                context.stopService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            if (areDailyRemindersEnabled(context)) {
                DailyReminderScheduler.scheduleDailyReminders(context)
            }
            if (areTimeTableRemindersEnabled(context)) {
                TimeTableReminderScheduler.scheduleAllTimeTableReminders(context)
            }
        }
    }

    fun areOngoingTimerNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIF_ONGOING_TIMER, true)
    }

    fun setOngoingTimerNotificationsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIF_ONGOING_TIMER, enabled).apply()
        if (!enabled) {
            cancelOngoingNotification(context)
        }
    }

    fun areCompletionNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIF_SESSION_COMPLETE, true)
    }

    fun setCompletionNotificationsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIF_SESSION_COMPLETE, enabled).apply()
    }

    fun areDailyRemindersEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIF_DAILY_REMINDER, true)
    }

    fun setDailyRemindersEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIF_DAILY_REMINDER, enabled).apply()
        if (enabled && areNotificationsEnabled(context)) {
            DailyReminderScheduler.scheduleDailyReminders(context)
        } else {
            DailyReminderScheduler.cancelDailyReminders(context)
        }
    }

    fun areTimeTableRemindersEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIF_TIMETABLE_REMINDER, true)
    }

    fun setTimeTableRemindersEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIF_TIMETABLE_REMINDER, enabled).apply()
        if (enabled && areNotificationsEnabled(context)) {
            TimeTableReminderScheduler.scheduleAllTimeTableReminders(context)
        } else {
            TimeTableReminderScheduler.cancelAllTimeTableReminders(context)
        }
    }

    fun areSoundAlertsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundAlertsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun cancelAllNotifications(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancelAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Ongoing Focus Timer Channel (Low importance so it doesn't beep every second)
            val ongoingChannel = NotificationChannel(
                CHANNEL_ONGOING_ID,
                "Active Study Focus Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live remaining study time and stopwatch/pomodoro duration"
                setShowBadge(false)
            }
            manager.createNotificationChannel(ongoingChannel)

            // 2. Focus Completion Alert Channel (High importance with sound & vibration)
            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "Study Target Completed Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a study target time or pomodoro interval finishes"
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(completeChannel)

            // 3. Daily Task & Timetable Reminder Channel
            val dailyReminderChannel = NotificationChannel(
                CHANNEL_DAILY_REMINDER_ID,
                "Timetable & Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timetable class reminders (5 mins before) and daily task reminder alerts"
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(dailyReminderChannel)
        }
    }

    fun buildOngoingNotification(
        context: Context,
        session: ActiveFocusSession
    ): android.app.Notification {
        createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingSecs = session.getLiveRemainingSeconds()
        val mins = remainingSecs / 60
        val secs = remainingSecs % 60
        val timeFormatted = "%02d:%02d".format(mins, secs)

        val totalDurationMins = session.targetDurationMinutes
        val statusText = if (session.isRunning) {
            "Target: ${totalDurationMins}m (Focus Mode)"
        } else {
            "⏸️ Paused at $timeFormatted (Target: ${totalDurationMins}m)"
        }

        // Action: Pause / Resume
        val toggleActionIntent = Intent(context, FocusTimerService::class.java).apply {
            action = if (session.isRunning) ACTION_PAUSE else ACTION_RESUME
        }
        val togglePendingIntent = PendingIntent.getService(
            context,
            1,
            toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleActionTitle = if (session.isRunning) "⏸️ Pause" else "▶️ Resume"

        // Action: Complete
        val completeActionIntent = Intent(context, FocusTimerService::class.java).apply {
            action = ACTION_COMPLETE
        }
        val completePendingIntent = PendingIntent.getService(
            context,
            2,
            completeActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isMasterEnabled = areNotificationsEnabled(context)
        val isOngoingEnabled = areOngoingTimerNotificationsEnabled(context)
        val isEnabled = isMasterEnabled && isOngoingEnabled

        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle("⏳ Studying: ${session.taskTitle}")
            .setContentText(statusText)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        if (session.isRunning) {
            builder.setUsesChronometer(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setChronometerCountDown(true)
            }
            builder.setWhen(System.currentTimeMillis() + remainingSecs * 1000L)
        } else {
            builder.setUsesChronometer(false)
        }

        if (isEnabled) {
            builder.addAction(0, toggleActionTitle, togglePendingIntent)
                .addAction(0, "✅ Complete", completePendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        }

        return builder.build()
    }

    fun buildGeneralTimerNotification(
        context: Context,
        isStopwatch: Boolean,
        elapsedSeconds: Int,
        remainingSeconds: Int,
        targetSeconds: Int,
        isRunning: Boolean
    ): android.app.Notification {
        createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val statusText: String

        if (isStopwatch) {
            val h = elapsedSeconds / 3600
            val m = (elapsedSeconds % 3600) / 60
            val s = elapsedSeconds % 60
            val formatted = String.format("%02d:%02d:%02d", h, m, s)
            title = "⏱️ Focus Study Stopwatch"
            statusText = if (isRunning) "Active Learning Stopwatch" else "⏸️ Paused at $formatted"
        } else {
            val m = remainingSeconds / 60
            val s = remainingSeconds % 60
            val formatted = String.format("%02d:%02d", m, s)
            val targetMins = targetSeconds / 60
            title = "🍅 Pomodoro Focus Timer"
            statusText = if (isRunning) "Target: ${targetMins}m (Pomodoro Focus)" else "⏸️ Paused at $formatted (Target: ${targetMins}m)"
        }

        // Action: Pause / Resume
        val toggleActionIntent = Intent(context, FocusTimerService::class.java).apply {
            action = if (isRunning) ACTION_PAUSE else ACTION_RESUME
        }
        val togglePendingIntent = PendingIntent.getService(
            context,
            10,
            toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleActionTitle = if (isRunning) "⏸️ Pause" else "▶️ Resume"

        // Action: Reset
        val resetActionIntent = Intent(context, FocusTimerService::class.java).apply {
            action = ACTION_RESET
        }
        val resetPendingIntent = PendingIntent.getService(
            context,
            11,
            resetActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isMasterEnabled = areNotificationsEnabled(context)
        val isOngoingEnabled = areOngoingTimerNotificationsEnabled(context)
        val isEnabled = isMasterEnabled && isOngoingEnabled

        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(statusText)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        if (isRunning) {
            builder.setUsesChronometer(true)
            if (isStopwatch) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    builder.setChronometerCountDown(false)
                }
                builder.setWhen(System.currentTimeMillis() - elapsedSeconds * 1000L)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    builder.setChronometerCountDown(true)
                }
                builder.setWhen(System.currentTimeMillis() + remainingSeconds * 1000L)
            }
        } else {
            builder.setUsesChronometer(false)
        }

        if (isEnabled) {
            builder.addAction(0, toggleActionTitle, togglePendingIntent)
                .addAction(0, "⏹️ Reset", resetPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        }

        return builder.build()
    }

    fun showCompletionNotification(context: Context, taskTitle: String, minutesStudied: Int) {
        if (!areNotificationsEnabled(context) || !areCompletionNotificationsEnabled(context)) return

        createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundEnabled = areSoundAlertsEnabled(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle("🎉 Focus Session Finished!")
            .setContentText("Target completed for '$taskTitle' ($minutesStudied mins). Tap to Save or Extend.")
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(if (soundEnabled) NotificationCompat.DEFAULT_ALL else 0)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(COMPLETE_NOTIFICATION_ID, notification)
    }

    fun showPomodoroCompletionNotification(context: Context, targetMinutes: Int) {
        if (!areNotificationsEnabled(context) || !areCompletionNotificationsEnabled(context)) return

        createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundEnabled = areSoundAlertsEnabled(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle("🍅 Pomodoro Interval Finished!")
            .setContentText("Great job on completing your $targetMinutes min focus interval! Take a short break.")
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(if (soundEnabled) NotificationCompat.DEFAULT_ALL else 0)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(COMPLETE_NOTIFICATION_ID, notification)
    }

    fun cancelOngoingNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(ONGOING_NOTIFICATION_ID)
    }
}
