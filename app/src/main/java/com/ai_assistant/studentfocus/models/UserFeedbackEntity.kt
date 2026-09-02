package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_feedback_history")
data class UserFeedbackEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val contentTitle: String,
    val originalCategory: String,
    val userCategory: String,
    val feedbackType: String = "CORRECTION", // "CORRECTION", "CONFIRMATION"
    val weight: Float = 3.0f
)
