package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "app_usage_history",
    indices = [
        Index(value = ["date"])
    ]
)
data class AppUsageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String, // Format: YYYY-MM-DD
    val packageName: String,
    val appName: String,
    val totalTimeMillis: Long,
    val category: String = "Other", // "Study", "Video", "Social", "Gaming", "Browser", "Other"
    val lastTimeUsed: Long = System.currentTimeMillis()
)
