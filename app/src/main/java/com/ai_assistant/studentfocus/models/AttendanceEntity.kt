package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val subjectName: String,
    val presents: Int = 0,
    val absents: Int = 0
)
