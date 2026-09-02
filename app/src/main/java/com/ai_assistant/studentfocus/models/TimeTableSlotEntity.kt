package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "timetable_slots")
data class TimeTableSlotEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dayOfWeek: String, // "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday", "Daily"
    val startTime: String, // 24-hour format: "HH:mm" e.g. "09:00"
    val endTime: String,   // 24-hour format: "HH:mm" e.g. "10:30"
    val roomOrLocation: String? = null,
    val colorHex: String = "#6366F1", // Default Indigo
    val notes: String? = null,
    val isNotificationEnabled: Boolean = true,
    val linkUrl: String? = null, // Meeting/Lecture URL e.g. Zoom, Meet, YouTube, Drive
    val isTimeWasteAutoManaged: Boolean = false, // Automatically starts focus session & pauses time waste when slot begins
    val isGoalTrackingEnabled: Boolean = true, // When true, slot duration counts towards Study Pace mission & routine study budget
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFormattedStartTime(): String = format24HourTo12Hour(startTime)
    fun getFormattedEndTime(): String = format24HourTo12Hour(endTime)
    fun getFormattedTimeRange(): String = "${getFormattedStartTime()} - ${getFormattedEndTime()}"

    fun getDurationMinutes(): Int {
        return try {
            val startParts = startTime.split(":").map { it.toInt() }
            val endParts = endTime.split(":").map { it.toInt() }
            val startMins = startParts[0] * 60 + startParts[1]
            val endMins = endParts[0] * 60 + endParts[1]
            if (endMins >= startMins) endMins - startMins else (24 * 60 - startMins + endMins)
        } catch (e: Exception) {
            60
        }
    }

    companion object {
        fun format24HourTo12Hour(time24: String): String {
            return try {
                val parts = time24.split(":").map { it.toInt() }
                val hour = parts[0]
                val minute = parts[1]
                val amPm = if (hour < 12) "AM" else "PM"
                val hour12 = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                String.format(java.util.Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm)
            } catch (e: Exception) {
                time24
            }
        }
    }
}
