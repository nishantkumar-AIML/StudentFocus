package com.ai_assistant.studentfocus.planner

import com.ai_assistant.studentfocus.models.StudyHistoryEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * CalendarDayView: Builds a month grid and provides daily stats from StudyHistoryEntity list.
 * Enforces a strict 24-hour (1440 minutes) limit per day.
 */
class CalendarDayView(private val history: List<StudyHistoryEntity>) {

    data class CalendarDay(
        val dayOfMonth: Int,
        val dateKey: String,      // "yyyy-MM-dd", empty string for blank leading cells
        val isToday: Boolean,
        val hasActivity: Boolean, // true if studyMinutes > 0 that day
        val studyMinutes: Int = 0
    )

    data class SelectedDayStats(
        val dateKey: String,
        val workedMinutes: Int,
        val completedTasks: Int,
        val progressPercentage: Int,
        val hasData: Boolean
    )

    /** Builds the grid of cells for the given month (0-based month like java.util.Calendar). */
    fun buildMonthGrid(year: Int, month: Int): List<CalendarDay> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayKey = sdf.format(Date())
        val historyMap = history.associate { it.date to minOf(1440, maxOf(0, it.studyMinutes)) }

        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<CalendarDay>()

        // Leading blanks so day 1 lands on the correct weekday column
        repeat(firstWeekday - 1) {
            cells.add(CalendarDay(dayOfMonth = 0, dateKey = "", isToday = false, hasActivity = false, studyMinutes = 0))
        }

        for (day in 1..daysInMonth) {
            cal.set(year, month, day)
            val key = sdf.format(cal.time)
            val mins = historyMap[key] ?: 0
            cells.add(
                CalendarDay(
                    dayOfMonth = day,
                    dateKey = key,
                    isToday = key == todayKey,
                    hasActivity = mins > 0,
                    studyMinutes = mins
                )
            )
        }

        return cells
    }

    /** Stats shown when the user inspects a specific date. */
    fun getStatsForDate(dateKey: String): SelectedDayStats {
        val record = history.find { it.date == dateKey }
        return if (record == null) {
            SelectedDayStats(
                dateKey = dateKey,
                workedMinutes = 0,
                completedTasks = 0,
                progressPercentage = 0,
                hasData = false
            )
        } else {
            SelectedDayStats(
                dateKey = dateKey,
                workedMinutes = minOf(1440, maxOf(0, record.studyMinutes)),
                completedTasks = record.completedTasks,
                progressPercentage = record.progressPercentage,
                hasData = true
            )
        }
    }

    /** Rolling totals for last N days. */
    fun totalMinutesForLastNDays(days: Int): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
        val limit = sdf.format(cal.time)
        return history.filter { it.date >= limit }.sumOf { minOf(1440, maxOf(0, it.studyMinutes)) }
    }
}
