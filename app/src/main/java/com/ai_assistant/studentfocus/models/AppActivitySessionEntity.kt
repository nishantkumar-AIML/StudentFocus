package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "app_activity_sessions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["startTime"]),
        Index(value = ["packageName"])
    ]
)
data class AppActivitySessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String, // Format: YYYY-MM-DD
    val startTime: Long,
    val endTime: Long,
    val durationMillis: Long,
    val packageName: String,
    val appName: String,
    val activityType: String, // e.g. "Video", "Shorts/Reels", "Web Browsing", "Chat", "Code/Practice", "Feed", "Screen View"
    val contentTitle: String, // e.g. "Python Machine Learning Tutorial", "LeetCode #121", "Feed", "Unavailable"
    val domainOrSubtext: String? = null, // e.g. "youtube.com", "leetcode.com", "instagram.com"
    val category: String = "Other", // "Learning", "Coding", "Productivity", "Communication", "Entertainment", "Social Media", "Gaming", "Browsing", "Other"
    val goalAlignment: String = "UNKNOWN", // "GOAL_ALIGNED", "HIGHLY_RELEVANT", "POSSIBLY_RELEVANT", "NEUTRAL", "OFF_GOAL", "UNKNOWN"
    val goalReason: String = "", // e.g. "Matches Subject: Operating Systems", "Entertainment intent (meme)", etc.
    val goalScore: Int = 0, // 0 to 100
    val detectedIntent: String = "UNKNOWN", // LEARNING, PRACTICING, CODING, ACADEMIC_WORK, etc.
    val matchedGoal: String = "", // e.g. "Machine Learning"
    val matchedTopics: String = "", // Comma-separated topics e.g. "RAG, LangChain"
    val contentRelevance: String = "HIGH", // "HIGH", "MEDIUM", "LOW", "NONE"
    val personalRelevance: String = "HIGH", // "HIGH", "MEDIUM", "LOW", "NONE"
    val routineMatch: String = "HIGH", // "HIGH", "MEDIUM", "LOW", "NONE"
    val confidenceScore: Float = 0.8f, // 0.0 to 1.0
    val isUserEdited: Boolean = false
)
