package com.ai_assistant.studentfocus.utils

import android.content.Context
import com.ai_assistant.studentfocus.viewmodel.TaskViewModel

data class DashboardWidgetInfo(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isRemovable: Boolean = true
)

object DashboardCustomizationManager {

    const val PREFS_NAME = "dashboard_customization_prefs"
    const val KEY_DASHBOARD_ORDER = "dashboard_widget_order"
    const val KEY_DASHBOARD_HIDDEN = "dashboard_widget_hidden"
    const val KEY_DASHBOARD_PRESET = "dashboard_active_preset"

    // Widget Identifiers
    const val WIDGET_SCHOLAR = "scholar_card"
    const val WIDGET_GREETING = "greeting_header"
    const val WIDGET_STATS = "stats_row"
    const val WIDGET_TIMER = "focus_timer_card"
    const val WIDGET_TASKS = "priority_tasks_card"
    const val WIDGET_EXAMS = "exams_card"
    const val WIDGET_PACE = "study_pace_card"
    const val WIDGET_HEATMAP = "heatmap_card"
    const val WIDGET_WEEKLY_CHART = "weekly_chart_card"
    const val WIDGET_TIME_WASTE = "time_waste_card"
    const val WIDGET_DAILY_NOTES = "daily_notes_card"

    val ALL_WIDGETS = listOf(
        DashboardWidgetInfo(WIDGET_SCHOLAR, "Scholar Avatar & Level XP", "Level rank, XP bar and scholar avatar badge", "🦉", isRemovable = true),
        DashboardWidgetInfo(WIDGET_GREETING, "Greeting & Date Header", "Time-of-day greeting, user name and interactive date picker", "☀️", isRemovable = false),
        DashboardWidgetInfo(WIDGET_STATS, "Summary Stats Row", "Today progress, total completed tasks & fire streak", "📊", isRemovable = true),
        DashboardWidgetInfo(WIDGET_HEATMAP, "Continuous Study Heatmap", "Study activity grid starting from your download date", "🔥", isRemovable = true),
        DashboardWidgetInfo(WIDGET_TIME_WASTE, "Time Waste & Idle Tracker", "Real-time idle distraction tracker and unallocated waste timer", "⏳", isRemovable = true),
        DashboardWidgetInfo(WIDGET_WEEKLY_CHART, "Weekly Study Minutes", "Bar chart visualizing daily study duration over the past week", "📈", isRemovable = true),
        DashboardWidgetInfo(WIDGET_TIMER, "Focus Study & Pomodoro Timer", "Quick focus intervals, stopwatch and custom pomodoro timer", "🍅", isRemovable = true),
        DashboardWidgetInfo(WIDGET_PACE, "Study Pace & Goal Planner", "Target completion days and required hours vs routine budget", "🎯", isRemovable = true),
        DashboardWidgetInfo(WIDGET_EXAMS, "Exam Countdown Targets", "Upcoming exam schedules, countdown badges and urgency colors", "⏰", isRemovable = true),
        DashboardWidgetInfo(WIDGET_TASKS, "Priority Study Targets", "Today's task checklist with focus timer launcher and completion toggle", "📌", isRemovable = false),
        DashboardWidgetInfo(WIDGET_DAILY_NOTES, "Daily Notes & Scratchpad", "Quick thought scratchpad and daily study notes repository", "📝", isRemovable = true)
    )

    val DEFAULT_ORDER = listOf(
        WIDGET_SCHOLAR,
        WIDGET_GREETING,
        WIDGET_STATS,
        WIDGET_HEATMAP,
        WIDGET_TIME_WASTE,
        WIDGET_WEEKLY_CHART,
        WIDGET_TIMER,
        WIDGET_PACE,
        WIDGET_EXAMS,
        WIDGET_TASKS,
        WIDGET_DAILY_NOTES
    )

    // Presets definitions
    val PRESET_MINIMAL = listOf(
        WIDGET_GREETING,
        WIDGET_TIMER,
        WIDGET_TASKS,
        WIDGET_DAILY_NOTES
    )

    val PRESET_SCHOLAR = listOf(
        WIDGET_SCHOLAR,
        WIDGET_GREETING,
        WIDGET_EXAMS,
        WIDGET_TASKS,
        WIDGET_PACE,
        WIDGET_DAILY_NOTES
    )

    val PRESET_ANALYTICS = listOf(
        WIDGET_GREETING,
        WIDGET_STATS,
        WIDGET_HEATMAP,
        WIDGET_WEEKLY_CHART,
        WIDGET_TIME_WASTE,
        WIDGET_TASKS
    )

    fun getWidgetOrder(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_DASHBOARD_ORDER, null) ?: return DEFAULT_ORDER
        val list = saved.split(",").filter { it.isNotBlank() && it in DEFAULT_ORDER }
        val missing = DEFAULT_ORDER.filterNot { it in list }
        return if (list.isNotEmpty()) list + missing else DEFAULT_ORDER
    }

    fun saveWidgetOrder(context: Context, order: List<String>, taskViewModel: TaskViewModel? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val orderStr = order.joinToString(",")
        prefs.edit().putString(KEY_DASHBOARD_ORDER, orderStr).apply()
        taskViewModel?.saveSetting(KEY_DASHBOARD_ORDER, orderStr)
    }

    fun getHiddenWidgets(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_DASHBOARD_HIDDEN, "") ?: ""
        return saved.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun setWidgetVisibility(context: Context, widgetId: String, isVisible: Boolean, taskViewModel: TaskViewModel? = null) {
        val hidden = getHiddenWidgets(context).toMutableSet()
        if (isVisible) {
            hidden.remove(widgetId)
        } else {
            hidden.add(widgetId)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hiddenStr = hidden.joinToString(",")
        prefs.edit().putString(KEY_DASHBOARD_HIDDEN, hiddenStr).apply()
        taskViewModel?.saveSetting(KEY_DASHBOARD_HIDDEN, hiddenStr)
    }

    fun isWidgetVisible(context: Context, widgetId: String): Boolean {
        return widgetId !in getHiddenWidgets(context)
    }

    fun applyPreset(context: Context, presetName: String, taskViewModel: TaskViewModel? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(KEY_DASHBOARD_PRESET, presetName)

        when (presetName.lowercase()) {
            "minimal", "minimal_focus", "focus" -> {
                saveWidgetOrder(context, PRESET_MINIMAL + (DEFAULT_ORDER - PRESET_MINIMAL.toSet()), taskViewModel)
                val hidden = (DEFAULT_ORDER - PRESET_MINIMAL.toSet()).toSet()
                val hiddenStr = hidden.joinToString(",")
                editor.putString(KEY_DASHBOARD_HIDDEN, hiddenStr).apply()
                taskViewModel?.saveSetting(KEY_DASHBOARD_HIDDEN, hiddenStr)
            }
            "scholar", "academic_scholar", "academic" -> {
                saveWidgetOrder(context, PRESET_SCHOLAR + (DEFAULT_ORDER - PRESET_SCHOLAR.toSet()), taskViewModel)
                val hidden = (DEFAULT_ORDER - PRESET_SCHOLAR.toSet()).toSet()
                val hiddenStr = hidden.joinToString(",")
                editor.putString(KEY_DASHBOARD_HIDDEN, hiddenStr).apply()
                taskViewModel?.saveSetting(KEY_DASHBOARD_HIDDEN, hiddenStr)
            }
            "analytics", "analytics_heavy", "stats" -> {
                saveWidgetOrder(context, PRESET_ANALYTICS + (DEFAULT_ORDER - PRESET_ANALYTICS.toSet()), taskViewModel)
                val hidden = (DEFAULT_ORDER - PRESET_ANALYTICS.toSet()).toSet()
                val hiddenStr = hidden.joinToString(",")
                editor.putString(KEY_DASHBOARD_HIDDEN, hiddenStr).apply()
                taskViewModel?.saveSetting(KEY_DASHBOARD_HIDDEN, hiddenStr)
            }
            else -> { // Default
                saveWidgetOrder(context, DEFAULT_ORDER, taskViewModel)
                editor.putString(KEY_DASHBOARD_HIDDEN, "").apply()
                taskViewModel?.saveSetting(KEY_DASHBOARD_HIDDEN, "")
            }
        }
    }

    fun resetToDefault(context: Context, taskViewModel: TaskViewModel? = null) {
        applyPreset(context, "default", taskViewModel)
    }

    fun getActivePresetName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DASHBOARD_PRESET, "default") ?: "default"
    }
}
