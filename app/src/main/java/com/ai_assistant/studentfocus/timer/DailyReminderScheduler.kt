package com.ai_assistant.studentfocus.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object DailyReminderScheduler {

    private const val REQUEST_CODE_MORNING = 301
    private const val REQUEST_CODE_EVENING = 302

    fun scheduleDailyReminders(context: Context) {
        // If user disabled notifications or daily reminders in settings, cancel all alarms and return
        if (!FocusNotificationHelper.areNotificationsEnabled(context) || !FocusNotificationHelper.areDailyRemindersEnabled(context)) {
            cancelDailyReminders(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Morning Study Reminder: 09:00 AM
        scheduleAlarm(context, alarmManager, REQUEST_CODE_MORNING, 9, 0)

        // 2. Evening Task Wrap-up Reminder: 07:00 PM (19:00)
        scheduleAlarm(context, alarmManager, REQUEST_CODE_EVENING, 19, 0)
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        hour: Int,
        minute: Int
    ) {
        val intent = Intent(context, DailyTaskReminderReceiver::class.java).apply {
            action = "com.ai_assistant.studentfocus.DAILY_TASK_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Safe fallback if exact alarms are restricted
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelDailyReminders(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, DailyTaskReminderReceiver::class.java)
            val p1 = PendingIntent.getBroadcast(context, REQUEST_CODE_MORNING, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val p2 = PendingIntent.getBroadcast(context, REQUEST_CODE_EVENING, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            alarmManager.cancel(p1)
            alarmManager.cancel(p2)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
