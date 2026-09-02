package com.ai_assistant.studentfocus.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ai_assistant.studentfocus.database.AppDatabase
import com.ai_assistant.studentfocus.models.TimeTableSlotEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

object TimeTableReminderScheduler {

    const val ACTION_TIMETABLE_REMINDER = "com.ai_assistant.studentfocus.TIMETABLE_REMINDER"
    const val EXTRA_SLOT_ID = "extra_slot_id"
    const val EXTRA_SLOT_TITLE = "extra_slot_title"
    const val EXTRA_SLOT_TIME_RANGE = "extra_slot_time_range"
    const val EXTRA_SLOT_LOCATION = "extra_slot_location"
    const val EXTRA_SLOT_LINK_URL = "extra_slot_link_url"
    const val EXTRA_SLOT_AUTO_WASTE = "extra_slot_auto_waste"
    const val EXTRA_SLOT_DURATION = "extra_slot_duration"

    fun scheduleAllTimeTableReminders(context: Context) {
        if (!FocusNotificationHelper.areNotificationsEnabled(context) || !FocusNotificationHelper.areTimeTableRemindersEnabled(context)) {
            cancelAllTimeTableReminders(context)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val allSlots = db.taskDao().getAllTimeTableSlotsDirect()
                for (slot in allSlots) {
                    if (slot.isNotificationEnabled) {
                        scheduleSlotReminder(context, slot)
                    } else {
                        cancelSlotReminder(context, slot.id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun scheduleSlotReminder(context: Context, slot: TimeTableSlotEntity) {
        if (!FocusNotificationHelper.areNotificationsEnabled(context) || !FocusNotificationHelper.areTimeTableRemindersEnabled(context) || !slot.isNotificationEnabled) {
            cancelSlotReminder(context, slot.id)
            return
        }

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val requestCode = getRequestCode(slot.id)

            val intent = Intent(context, TimeTableReminderReceiver::class.java).apply {
                action = ACTION_TIMETABLE_REMINDER
                putExtra(EXTRA_SLOT_ID, slot.id)
                putExtra(EXTRA_SLOT_TITLE, slot.title)
                putExtra(EXTRA_SLOT_TIME_RANGE, slot.getFormattedTimeRange())
                putExtra(EXTRA_SLOT_LOCATION, slot.roomOrLocation)
                putExtra(EXTRA_SLOT_LINK_URL, slot.linkUrl)
                putExtra(EXTRA_SLOT_AUTO_WASTE, slot.isTimeWasteAutoManaged)
                putExtra(EXTRA_SLOT_DURATION, slot.getDurationMinutes())
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val targetTimeMillis = calculateNextTriggerMillis(slot.dayOfWeek, slot.startTime)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                targetTimeMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                targetTimeMillis,
                                pendingIntent
                            )
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            targetTimeMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        targetTimeMillis,
                        pendingIntent
                    )
                }
            } catch (se: SecurityException) {
                // Fallback for OEMs with strict AppOps restrictions
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelSlotReminder(context: Context, slotId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val requestCode = getRequestCode(slotId)
            val intent = Intent(context, TimeTableReminderReceiver::class.java).apply {
                action = ACTION_TIMETABLE_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelAllTimeTableReminders(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val allSlots = db.taskDao().getAllTimeTableSlotsDirect()
                for (slot in allSlots) {
                    cancelSlotReminder(context, slot.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun calculateNextTriggerMillis(dayOfWeek: String, startTime: String): Long {
        val parts = startTime.split(":").map { it.toInt() }
        val targetHour = parts[0]
        val targetMinute = parts[1]

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Schedule exactly 5 minutes BEFORE the class/slot begins
            add(Calendar.MINUTE, -5)
        }

        val targetDayInt = when (dayOfWeek.trim().lowercase()) {
            "monday", "mon" -> Calendar.MONDAY
            "tuesday", "tue" -> Calendar.TUESDAY
            "wednesday", "wed" -> Calendar.WEDNESDAY
            "thursday", "thu" -> Calendar.THURSDAY
            "friday", "fri" -> Calendar.FRIDAY
            "saturday", "sat" -> Calendar.SATURDAY
            "sunday", "sun" -> Calendar.SUNDAY
            else -> -1 // Daily
        }

        if (targetDayInt == -1) {
            // Daily recurring: if 5-mins-before time has already passed today, schedule for tomorrow
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // Specific day of week
            target.set(Calendar.DAY_OF_WEEK, targetDayInt)
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        return target.timeInMillis
    }

    private fun getRequestCode(slotId: String): Int {
        return abs(slotId.hashCode()) % 100000 + 4000
    }
}
