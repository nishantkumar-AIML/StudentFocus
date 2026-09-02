package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_category_overrides")
data class AppCategoryOverrideEntity(
    @PrimaryKey val targetKey: String, // packageName or "pkg:contentTitle"
    val customCategory: String, // "Learning", "Coding", "Productivity", "Communication", "Entertainment", "Social Media", "Gaming", "Browsing", "Other"
    val updatedAt: Long = System.currentTimeMillis()
)
