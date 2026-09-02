package com.ai_assistant.studentfocus.models

enum class ModelMaturity(val displayName: String, val icon: String, val description: String) {
    NEW_USER("New User", "🌱", "Learning your study habits and routine..."),
    LEARNING("Learning Routine", "🧠", "Collecting behavioral observations..."),
    PERSONALIZED("Personalized", "🎯", "Personal routine and patterns established."),
    ADAPTIVE("Adaptive", "⚡", "Continuously adapting to routine changes and feedback.")
}

data class LearnedTimeRoutine(
    val startHour: Int,
    val endHour: Int,
    val dominantCategory: String,
    val dominantIntent: String,
    val confidence: Float,
    val sampleCount: Int,
    val dayOfWeek: Int? = null // Calendar.MONDAY..SUNDAY, or null for general all-week routine
)

data class LearnedTopicAffinity(
    val topic: String,
    val goalAlignedCount: Int,
    val offGoalCount: Int,
    val userConfirmationCount: Int,
    val lastSeenMillis: Long,
    val relevanceScore: Float // 0.0 to 1.0
)

data class LearnedAppMeaning(
    val packageName: String,
    val appName: String,
    val categoryDistribution: Map<String, Int> = emptyMap(),
    val productiveRatio: Float = 0f,
    val sampleCount: Int = 0
)

data class LearnedTransition(
    val fromCategory: String,
    val toCategory: String,
    val fromPackage: String,
    val toPackage: String,
    val count: Int,
    val transitionType: String // "PRODUCTIVE_WORKFLOW", "DISTRACTION_LOOP", "NEUTRAL"
)

data class PersonalPrediction(
    val predictionText: String,
    val category: String,
    val intent: String,
    val confidence: String, // "HIGH", "MEDIUM", "LOW"
    val confidenceScore: Float,
    val supportingEvidence: String,
    val sampleCount: Int,
    val recencyDays: Int
)

data class PersonalBehaviorProfile(
    val maturity: ModelMaturity = ModelMaturity.NEW_USER,
    val totalSessionsObserved: Int = 0,
    val totalDaysTracked: Int = 0,
    val userCorrectionsCount: Int = 0,
    val timeRoutines: List<LearnedTimeRoutine> = emptyList(),
    val topicAffinities: Map<String, LearnedTopicAffinity> = emptyMap(),
    val appMeanings: Map<String, LearnedAppMeaning> = emptyMap(),
    val commonTransitions: List<LearnedTransition> = emptyList(),
    val topProductiveHours: List<Int> = emptyList(),
    val topDistractionHours: List<Int> = emptyList(),
    val avgProductiveSessionMins: Int = 0,
    val avgDistractionSessionMins: Int = 0,
    val activePredictions: List<PersonalPrediction> = emptyList(),
    val confidenceCoverageHigh: Int = 0,
    val confidenceCoverageMed: Int = 0,
    val confidenceCoverageLow: Int = 100,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)
