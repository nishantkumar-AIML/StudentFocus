package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_behavior_profile")
data class PersonalBehaviorProfileEntity(
    @PrimaryKey val id: String = "PRIMARY_USER_PROFILE",
    val maturityStage: String = "NEW_USER", // NEW_USER, LEARNING, PERSONALIZED, ADAPTIVE
    val totalSessionsObserved: Int = 0,
    val totalDaysTracked: Int = 0,
    val userCorrectionCount: Int = 0,
    val dominantProductiveHours: String = "", // e.g. "9,10,14,15"
    val dominantDistractionHours: String = "", // e.g. "21,22,23"
    val learnedRoutinesJson: String = "[]",
    val learnedTransitionsJson: String = "[]",
    val confidenceCoverageHigh: Int = 0,
    val confidenceCoverageMed: Int = 0,
    val confidenceCoverageLow: Int = 100,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)
