package com.ai_assistant.studentfocus.planner

import com.ai_assistant.studentfocus.models.TaskEntity
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

enum class PriorityTier { LOW, MODERATE, CRITICAL }

data class TaskPriority(
    val urgencyScore: Int,
    val dailyPaceNeeded: Double,
    val tier: PriorityTier
)

data class PlanBlock(
    val taskId: String,
    val taskTitle: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val breakMinutes: Int,
    val priorityTier: PriorityTier
)

data class SmartDailyPlan(
    val plannedHours: Double,
    val reserveHours: Double,
    val totalCapacity: Double,
    val timeline: List<PlanBlock>
)

/**
 * TaskPlanner: Computes task priority scores and generates an optimized daily timeline.
 * Strict Constraint: Daily time capacity can never exceed 24.0 hours (1440 minutes).
 */
object TaskPlanner {

    /**
     * Calculates urgency and daily pace for a task.
     * Enforces that daily pace cannot exceed 24.0 hours.
     */
    fun calculateTaskPriority(
        task: TaskEntity,
        targetHours: Double = 4.0,
        daysRemaining: Int = 1
    ): TaskPriority {
        val hoursWorked = (task.timeSpentMinutes / 60.0).coerceIn(0.0, 24.0)
        val hoursLeft = max(0.1, min(24.0, targetHours - hoursWorked))
        val daysLeft = max(1, daysRemaining)
        val dailyPaceNeeded = min(24.0, hoursLeft / daysLeft)

        val urgencyScore = min(99, max(15, round((dailyPaceNeeded / 3.5) * 100).toInt()))

        val tier = when {
            urgencyScore >= 75 -> PriorityTier.CRITICAL
            urgencyScore >= 50 -> PriorityTier.MODERATE
            else -> PriorityTier.LOW
        }

        return TaskPriority(
            urgencyScore = urgencyScore,
            dailyPaceNeeded = round(dailyPaceNeeded * 10) / 10.0,
            tier = tier
        )
    }

    /**
     * Generates a smart daily schedule with a 24-hour hard ceiling.
     * Sustainable capacity is strictly limited to <= 24 hours.
     */
    fun generateSmartDailyPlan(
        tasks: List<TaskEntity>,
        sustainableCapacity: Double = 4.0,
        startHour: Int = 8
    ): SmartDailyPlan {
        // Enforce maximum 24 hours per day capacity
        val boundedCapacity = min(24.0, max(0.5, sustainableCapacity))
        val reserveHours = round(boundedCapacity * 0.25 * 10) / 10.0
        val plannedHours = max(0.5, min(24.0 - reserveHours, round((boundedCapacity - reserveHours) * 10) / 10.0))

        val uncompletedTasks = tasks.filter { !it.isCompleted }
        val prioritized = uncompletedTasks
            .map { it to calculateTaskPriority(it) }
            .sortedByDescending { it.second.urgencyScore }

        var allocatedMinutes = 0
        val targetMinutes = min(1440, (plannedHours * 60).toInt())
        val timeline = mutableListOf<PlanBlock>()
        var currentClockMinutes = startHour * 60

        fun formatClock(mins: Int): String {
            val h = (mins / 60) % 24
            val m = mins % 60
            return "%02d:%02d".format(h, m)
        }

        for ((task, priority) in prioritized) {
            // Absolute limit: cannot exceed 24 hours (1440 minutes) in a single day
            if (allocatedMinutes >= targetMinutes || allocatedMinutes >= 1440) break

            val sessionDuration = 45 // 45 min focus session
            val breakDuration = 10   // 10 min break
            val taskHoursLeft = max(0.5, min(24.0, 3.0 - (task.timeSpentMinutes / 60.0)))
            val maxSessionsForTask = min(3, ceil((taskHoursLeft * 60) / sessionDuration).toInt())

            for (s in 0 until maxSessionsForTask) {
                if (allocatedMinutes + sessionDuration > targetMinutes + 15 || allocatedMinutes + sessionDuration > 1440) break

                val startTimeStr = formatClock(currentClockMinutes)
                val endTimeStr = formatClock(currentClockMinutes + sessionDuration)

                timeline.add(
                    PlanBlock(
                        taskId = task.id,
                        taskTitle = task.title,
                        startTime = startTimeStr,
                        endTime = endTimeStr,
                        durationMinutes = sessionDuration,
                        breakMinutes = breakDuration,
                        priorityTier = priority.tier
                    )
                )

                allocatedMinutes += sessionDuration
                currentClockMinutes += sessionDuration + breakDuration
            }
        }

        return SmartDailyPlan(
            plannedHours = plannedHours,
            reserveHours = reserveHours,
            totalCapacity = boundedCapacity,
            timeline = timeline
        )
    }
}
