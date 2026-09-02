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

class TimeTableReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // STRICT ZERO-NOTIFICATION CHECK:
        if (!FocusNotificationHelper.areNotificationsEnabled(context) || !FocusNotificationHelper.areTimeTableRemindersEnabled(context)) {
            return
        }

        // On device reboot, reschedule all timetable alarms
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            TimeTableReminderScheduler.scheduleAllTimeTableReminders(context)
            return
        }

        val slotId = intent?.getStringExtra(TimeTableReminderScheduler.EXTRA_SLOT_ID) ?: return
        val slotTitle = intent.getStringExtra(TimeTableReminderScheduler.EXTRA_SLOT_TITLE) ?: "Class / Activity"
        val slotTimeRange = intent.getStringExtra(TimeTableReminderScheduler.EXTRA_SLOT_TIME_RANGE) ?: ""
        val slotLocation = intent.getStringExtra(TimeTableReminderScheduler.EXTRA_SLOT_LOCATION)
        val slotLinkUrl = intent.getStringExtra(TimeTableReminderScheduler.EXTRA_SLOT_LINK_URL)
        val isAutoWaste = intent.getBooleanExtra(TimeTableReminderScheduler.EXTRA_SLOT_AUTO_WASTE, false)
        val slotDuration = intent.getIntExtra(TimeTableReminderScheduler.EXTRA_SLOT_DURATION, 60)

        // TIME WASTE TRACKER MANAGEMENT:
        // When isAutoWaste is true, TimeWasteManager natively recognizes active timetable slot
        // and pauses waste counting during class hours without running fake self-study task sessions.

        FocusNotificationHelper.createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            501,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = if (isAutoWaste) "⏳ Starting in 5 mins: $slotTitle" else "⏰ Starting in 5 mins: $slotTitle"
        val locationPart = if (!slotLocation.isNullOrBlank()) " • Room/Location: $slotLocation" else ""
        val autoFocusPart = if (isAutoWaste) " • Waste Tracker Paused" else ""
        val notificationText = "Scheduled: $slotTimeRange$locationPart$autoFocusPart"

        val soundEnabled = FocusNotificationHelper.areSoundAlertsEnabled(context)

        val builder = NotificationCompat.Builder(context, FocusNotificationHelper.CHANNEL_DAILY_REMINDER_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(if (soundEnabled) NotificationCompat.DEFAULT_ALL else 0)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)

        // Add 1-Click Link Action if linkUrl is present
        if (!slotLinkUrl.isNullOrBlank()) {
            try {
                var url = slotLinkUrl.trim()
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                val linkIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val linkPendingIntent = PendingIntent.getActivity(
                    context,
                    (slotId.hashCode() and 0x7FFFFFFF) % 50000 + 1000,
                    linkIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_menu_view, "🔗 Open Link", linkPendingIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (slotId.hashCode() and 0x7FFFFFFF) % 100000 + 5000
        manager.notify(notificationId, builder.build())

        // Reschedule for next recurrence
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val allSlots = db.taskDao().getAllTimeTableSlotsDirect()
                val currentSlot = allSlots.find { it.id == slotId }
                if (currentSlot != null && currentSlot.isNotificationEnabled) {
                    TimeTableReminderScheduler.scheduleSlotReminder(context, currentSlot)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
