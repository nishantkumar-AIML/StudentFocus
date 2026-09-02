package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_history")
data class StudyHistoryEntity(
    @PrimaryKey val date: String, // "YYYY-MM-DD"
    val completedTasks: Int,
    val studyMinutes: Int,
    val progressPercentage: Int
)
