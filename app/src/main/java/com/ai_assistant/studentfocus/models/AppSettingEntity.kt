package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
