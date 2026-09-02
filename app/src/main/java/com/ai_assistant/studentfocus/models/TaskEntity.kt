package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val category: String?,
    val subject: String?,
    val dueDate: String,
    val priority: String,
    val isPinned: Boolean = false,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "todo", // "todo", "doing", "done"
    val timeSpentMinutes: Int = 0,
    val completionNotes: String? = null,
    val linkUrl: String? = null
)
