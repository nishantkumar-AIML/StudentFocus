package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "routine_activities")
data class RoutineActivityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val activityName: String,
    val durationMinutes: Int, // Total minutes in 24-hour day (e.g. 420 = 7 hours)
    val classificationScore: Float, // 1.0 = Useful, 0.0 = Waste, 0.1..0.9 = Partial/Necessary
    val category: String = "Study", // "Study", "Sleep", "College", "Fitness", "Waste", "Routine", "Other"
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
