package com.ai_assistant.studentfocus.timer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ai_assistant.studentfocus.MainActivity
import com.ai_assistant.studentfocus.R
import com.ai_assistant.studentfocus.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DailyTaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // STRICT ZERO-NOTIFICATION CHECK:
        // If user disabled notifications or daily reminders in settings, DO NOT send or post anything!
        if (!FocusNotificationHelper.areNotificationsEnabled(context) || !FocusNotificationHelper.areDailyRemindersEnabled(context)) {
            return
        }

        // On device reboot, reschedule alarms
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyReminderScheduler.scheduleDailyReminders(context)
            return
        }

        val pendingResult = goAsync()
        // Query database on IO thread for pending tasks
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = sdf.format(Date())

                val allTasks = db.taskDao().getAllTasksList()
                // STRICTLY filter only tasks due TODAY that are not completed
                val pendingToday = allTasks.filter {
                    !it.isCompleted && it.dueDate == todayStr
                }

                // If no tasks are pending for today, do not send any reminder notification!
                if (pendingToday.isEmpty()) {
                    return@launch
                }

                // Verify setting once more before posting notification
                if (FocusNotificationHelper.areNotificationsEnabled(context) && FocusNotificationHelper.areDailyRemindersEnabled(context)) {
                    postReminderNotification(context, pendingToday.size, pendingToday.firstOrNull()?.title)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Ensure the next day's alarm is scheduled if enabled
                if (FocusNotificationHelper.areNotificationsEnabled(context) && FocusNotificationHelper.areDailyRemindersEnabled(context)) {
                    DailyReminderScheduler.scheduleDailyReminders(context)
                }
                pendingResult.finish()
            }
        }
    }

    private fun postReminderNotification(context: Context, pendingCount: Int, firstTaskTitle: String?) {
        if (!FocusNotificationHelper.areNotificationsEnabled(context)) {
            return
        }

        FocusNotificationHelper.createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            201,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🎯 $pendingCount Pending ${if (pendingCount == 1) "Task" else "Tasks"} Today"

        val message = if (pendingCount == 1 && !firstTaskTitle.isNullOrBlank()) {
            "Today's pending task: '$firstTaskTitle'. Complete it to hit today's study target!"
        } else {
            "You have $pendingCount tasks scheduled for today. Complete them before the day ends!"
        }

        val notification = NotificationCompat.Builder(context, FocusNotificationHelper.CHANNEL_DAILY_REMINDER_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(FocusNotificationHelper.DAILY_REMINDER_NOTIFICATION_ID, notification)
    }
}
