package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String, // "YYYY-MM-DD"
    val content: String,
    val topic: String = "", // Topic / Subject Name
    val createdAt: Long = System.currentTimeMillis()
)
