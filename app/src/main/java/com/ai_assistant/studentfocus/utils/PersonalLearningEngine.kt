package com.ai_assistant.studentfocus.utils

import com.ai_assistant.studentfocus.models.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln

object PersonalLearningEngine {

    private const val HALF_LIFE_MILLIS = 14.0 * 24.0 * 3600.0 * 1000.0 // 14 days decay half-life
    private const val LAMBDA = 0.69314718056 / HALF_LIFE_MILLIS // ln(2) / T_half

    /**
     * Calculates time-decay weight for an observation: w = e^(-lambda * deltaT)
     * Recent observations (0-3 days) have weight ~ 1.0, 14 days ~ 0.5, 28 days ~ 0.25.
     */
    fun calculateDecayWeight(timestamp: Long, currentTimeMillis: Long = System.currentTimeMillis()): Float {
        val deltaMillis = (currentTimeMillis - timestamp).coerceAtLeast(0L)
        return exp(-LAMBDA * deltaMillis).toFloat().coerceIn(0.1f, 1.0f)
    }

    /**
     * Computes a comprehensive PersonalBehaviorProfile from historical sessions and user feedback.
     */
    fun buildPersonalProfile(
        sessions: List<AppActivitySessionEntity>,
        userFeedbackList: List<UserFeedbackEntity> = emptyList(),
        currentTimeMillis: Long = System.currentTimeMillis()
    ): PersonalBehaviorProfile {
        if (sessions.isEmpty() && userFeedbackList.isEmpty()) {
            return PersonalBehaviorProfile(maturity = ModelMaturity.NEW_USER)
        }

        // 1. Data Sufficiency & Model Maturity
        val validSessions = sessions.filter { it.durationMillis >= 10000L }
        val distinctDates = validSessions.map { it.date }.distinct()
        val totalDays = distinctDates.size
        val totalCount = validSessions.size
        val feedbackCount = userFeedbackList.size

        val maturity = when {
            totalCount < 10 || totalDays < 2 -> ModelMaturity.NEW_USER
            totalCount < 50 || totalDays < 7 -> ModelMaturity.LEARNING
            feedbackCount > 0 && totalCount >= 50 -> ModelMaturity.ADAPTIVE
            else -> ModelMaturity.PERSONALIZED
        }

        // 2. Discover Time-of-Day Routines & Dominant Hours
        val hourCategoryWeights = Array(24) { mutableMapOf<String, Float>() }
        val hourRawCounts = IntArray(24)
        val cal = Calendar.getInstance()

        for (session in validSessions) {
            val weight = calculateDecayWeight(session.startTime, currentTimeMillis)
            cal.timeInMillis = session.startTime
            val hour = cal.get(Calendar.HOUR_OF_DAY)

            hourRawCounts[hour]++
            val cat = session.category
            hourCategoryWeights[hour][cat] = (hourCategoryWeights[hour][cat] ?: 0f) + (weight * (session.durationMillis / 60000f).coerceAtLeast(1f))
        }

        // Include explicit user feedback in hourly weights with 3.0x multiplier
        for (fb in userFeedbackList) {
            val weight = calculateDecayWeight(fb.timestamp, currentTimeMillis) * fb.weight
            cal.timeInMillis = fb.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourCategoryWeights[hour][fb.userCategory] = (hourCategoryWeights[hour][fb.userCategory] ?: 0f) + weight * 5f
        }

        val learnedRoutines = mutableListOf<LearnedTimeRoutine>()
        val topProductiveHours = mutableListOf<Int>()
        val topDistractionHours = mutableListOf<Int>()

        for (hour in 0..23) {
            val weights = hourCategoryWeights[hour]
            val totalWeight = weights.values.sum()
            val rawCount = hourRawCounts[hour]

            if (totalWeight > 5f && rawCount >= 3) {
                val dominantEntry = weights.maxByOrNull { it.value }
                if (dominantEntry != null && (dominantEntry.value / totalWeight) >= 0.45f) {
                    val confidence = (dominantEntry.value / totalWeight).coerceIn(0.55f, 0.95f)
                    val routine = LearnedTimeRoutine(
                        startHour = hour,
                        endHour = (hour + 1) % 24,
                        dominantCategory = dominantEntry.key,
                        dominantIntent = mapCategoryToIntent(dominantEntry.key),
                        confidence = confidence,
                        sampleCount = rawCount
                    )
                    learnedRoutines.add(routine)

                    if (dominantEntry.key in listOf(ActivityClassifier.CAT_LEARNING, ActivityClassifier.CAT_CODING, ActivityClassifier.CAT_PRODUCTIVITY)) {
                        topProductiveHours.add(hour)
                    } else if (dominantEntry.key in listOf(ActivityClassifier.CAT_ENTERTAINMENT, ActivityClassifier.CAT_SOCIAL, ActivityClassifier.CAT_GAMING)) {
                        topDistractionHours.add(hour)
                    }
                }
            }
        }

        // 3. Learn Topic-Level Behavioral Affinities
        val topicAlignedWeights = mutableMapOf<String, Float>()
        val topicOffWeights = mutableMapOf<String, Float>()
        val topicConfirmedCounts = mutableMapOf<String, Int>()
        val topicLastSeen = mutableMapOf<String, Long>()

        for (session in validSessions) {
            val weight = calculateDecayWeight(session.startTime, currentTimeMillis)
            val topics = session.matchedTopics.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
            val isAligned = session.goalAlignment == ActivityClassifier.GoalAlignment.GOAL_ALIGNED.name ||
                    session.goalAlignment == ActivityClassifier.GoalAlignment.HIGHLY_RELEVANT.name

            for (topic in topics) {
                topicLastSeen[topic] = maxOf(topicLastSeen[topic] ?: 0L, session.endTime)
                if (isAligned) {
                    topicAlignedWeights[topic] = (topicAlignedWeights[topic] ?: 0f) + weight
                } else {
                    topicOffWeights[topic] = (topicOffWeights[topic] ?: 0f) + weight
                }
            }
        }

        for (fb in userFeedbackList) {
            val titleLower = fb.contentTitle.lowercase(Locale.ROOT)
            val isPositive = fb.userCategory in listOf(ActivityClassifier.CAT_LEARNING, ActivityClassifier.CAT_CODING, ActivityClassifier.CAT_PRODUCTIVITY)
            val tokens = titleLower.split("\\s+".toRegex()).filter { it.length >= 3 }
            for (t in tokens) {
                if (isPositive) {
                    topicConfirmedCounts[t] = (topicConfirmedCounts[t] ?: 0) + 1
                    topicAlignedWeights[t] = (topicAlignedWeights[t] ?: 0f) + 3f
                } else {
                    topicOffWeights[t] = (topicOffWeights[t] ?: 0f) + 3f
                }
            }
        }

        val topicAffinities = mutableMapOf<String, LearnedTopicAffinity>()
        val allTopics = (topicAlignedWeights.keys + topicOffWeights.keys).distinct()
        for (topic in allTopics) {
            val aligned = topicAlignedWeights[topic] ?: 0f
            val off = topicOffWeights[topic] ?: 0f
            val total = aligned + off
            val relevance = if (total > 0) (aligned / total).coerceIn(0f, 1f) else 0.5f
            topicAffinities[topic] = LearnedTopicAffinity(
                topic = topic,
                goalAlignedCount = aligned.toInt(),
                offGoalCount = off.toInt(),
                userConfirmationCount = topicConfirmedCounts[topic] ?: 0,
                lastSeenMillis = topicLastSeen[topic] ?: currentTimeMillis,
                relevanceScore = relevance
            )
        }

        // 4. Learn App Meanings for THIS User
        val appGroups = validSessions.groupBy { it.packageName }
        val appMeanings = mutableMapOf<String, LearnedAppMeaning>()
        for ((pkg, list) in appGroups) {
            val catDist = list.groupBy { it.category }.mapValues { it.value.size }
            val productiveCount = list.count { it.category in listOf(ActivityClassifier.CAT_LEARNING, ActivityClassifier.CAT_CODING, ActivityClassifier.CAT_PRODUCTIVITY) }
            val productiveRatio = if (list.isNotEmpty()) productiveCount.toFloat() / list.size else 0f
            appMeanings[pkg] = LearnedAppMeaning(
                packageName = pkg,
                appName = list.firstOrNull()?.appName ?: pkg,
                categoryDistribution = catDist,
                productiveRatio = productiveRatio,
                sampleCount = list.size
            )
        }

        // 5. Activity Sequences & Transition Patterns
        val sortedSessions = validSessions.sortedBy { it.startTime }
        val transitions = mutableListOf<LearnedTransition>()
        val transitionCounts = mutableMapOf<Pair<String, String>, Int>()

        for (i in 0 until sortedSessions.size - 1) {
            val curr = sortedSessions[i]
            val next = sortedSessions[i + 1]
            // Must occur within 15 minutes
            if (next.startTime - curr.endTime in 0..(15 * 60 * 1000L)) {
                val key = Pair(curr.category, next.category)
                transitionCounts[key] = (transitionCounts[key] ?: 0) + 1
            }
        }

        for ((pair, count) in transitionCounts) {
            if (count >= 2) {
                val isProductiveWorkflow = pair.first in listOf(ActivityClassifier.CAT_LEARNING, ActivityClassifier.CAT_CODING) &&
                        pair.second in listOf(ActivityClassifier.CAT_CODING, ActivityClassifier.CAT_PRODUCTIVITY, ActivityClassifier.CAT_BROWSING)
                val isDistractionLoop = pair.first in listOf(ActivityClassifier.CAT_SOCIAL, ActivityClassifier.CAT_ENTERTAINMENT) &&
                        pair.second in listOf(ActivityClassifier.CAT_SOCIAL, ActivityClassifier.CAT_ENTERTAINMENT)

                val transType = when {
                    isProductiveWorkflow -> "PRODUCTIVE_WORKFLOW"
                    isDistractionLoop -> "DISTRACTION_LOOP"
                    else -> "NEUTRAL"
                }

                transitions.add(
                    LearnedTransition(
                        fromCategory = pair.first,
                        toCategory = pair.second,
                        fromPackage = "",
                        toPackage = "",
                        count = count,
                        transitionType = transType
                    )
                )
            }
        }

        // 6. Generate Probabilistic Predictions for Current Context
        cal.timeInMillis = currentTimeMillis
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val activePredictions = mutableListOf<PersonalPrediction>()

        val matchingRoutine = learnedRoutines.find { it.startHour == currentHour }
        if (matchingRoutine != null && maturity != ModelMaturity.NEW_USER) {
            val confLabel = if (matchingRoutine.confidence >= 0.75f) "HIGH" else if (matchingRoutine.confidence >= 0.60f) "MEDIUM" else "LOW"
            val categoryName = matchingRoutine.dominantCategory
            activePredictions.add(
                PersonalPrediction(
                    predictionText = "Based on ${matchingRoutine.sampleCount} sessions, you usually focus on $categoryName around this time (${matchingRoutine.startHour}:00 – ${matchingRoutine.endHour}:00).",
                    category = categoryName,
                    intent = matchingRoutine.dominantIntent,
                    confidence = confLabel,
                    confidenceScore = matchingRoutine.confidence,
                    supportingEvidence = "Learned from consistent historical routine (${matchingRoutine.sampleCount} samples).",
                    sampleCount = matchingRoutine.sampleCount,
                    recencyDays = totalDays
                )
            )
        }

        // 7. Average Session Durations
        val productiveSessions = validSessions.filter { it.category in listOf(ActivityClassifier.CAT_LEARNING, ActivityClassifier.CAT_CODING, ActivityClassifier.CAT_PRODUCTIVITY) }
        val distractionSessions = validSessions.filter { it.category in listOf(ActivityClassifier.CAT_ENTERTAINMENT, ActivityClassifier.CAT_SOCIAL, ActivityClassifier.CAT_GAMING) }

        val avgProdMins = if (productiveSessions.isNotEmpty()) (productiveSessions.map { it.durationMillis }.average() / 60000.0).toInt() else 0
        val avgDistMins = if (distractionSessions.isNotEmpty()) (distractionSessions.map { it.durationMillis }.average() / 60000.0).toInt() else 0

        // 8. Confidence Coverage
        val highConfCount = validSessions.count { it.confidenceScore >= 0.85f }
        val medConfCount = validSessions.count { it.confidenceScore in 0.60f..0.84f }
        val lowConfCount = validSessions.count { it.confidenceScore < 0.60f }
        val totalForCoverage = maxOf(1, validSessions.size)

        return PersonalBehaviorProfile(
            maturity = maturity,
            totalSessionsObserved = totalCount,
            totalDaysTracked = totalDays,
            userCorrectionsCount = feedbackCount,
            timeRoutines = learnedRoutines,
            topicAffinities = topicAffinities,
            appMeanings = appMeanings,
            commonTransitions = transitions,
            topProductiveHours = topProductiveHours.distinct(),
            topDistractionHours = topDistractionHours.distinct(),
            avgProductiveSessionMins = avgProdMins,
            avgDistractionSessionMins = avgDistMins,
            activePredictions = activePredictions,
            confidenceCoverageHigh = (highConfCount * 100) / totalForCoverage,
            confidenceCoverageMed = (medConfCount * 100) / totalForCoverage,
            confidenceCoverageLow = (lowConfCount * 100) / totalForCoverage,
            lastUpdatedMillis = currentTimeMillis
        )
    }

    /**
     * Evaluates an observed activity against the student's personal profile to derive:
     * 1. Content Relevance (HIGH / MEDIUM / LOW / NONE)
     * 2. Personal Relevance (HIGH / MEDIUM / LOW / NONE)
     * 3. Routine Match (HIGH / MEDIUM / LOW / NONE)
     * Along with personal evidence bullets and behavioral score modifier.
     */
    fun evaluatePersonalContext(
        packageName: String,
        contentTitle: String,
        matchedGoal: String?,
        matchedTopics: List<String>,
        rawContentScore: Int,
        category: String,
        timestamp: Long,
        profile: PersonalBehaviorProfile
    ): PersonalEvaluation {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val personalEvidence = mutableListOf<String>()
        val historicalEvidence = mutableListOf<String>()

        // 1. Content Relevance (Strictly based on direct goals, subjects, and semantic content)
        val contentRelevance = when {
            rawContentScore >= 55 || matchedGoal != null -> "HIGH"
            rawContentScore >= 40 || matchedTopics.isNotEmpty() -> "MEDIUM"
            rawContentScore >= 20 -> "LOW"
            else -> "NONE"
        }

        // 2. Personal Relevance (Based on user's topic affinity & historical consistency)
        var personalScoreBonus = 0
        var matchingTopicCount = 0
        for (topic in matchedTopics) {
            val affinity = profile.topicAffinities[topic.lowercase(Locale.ROOT)]
            if (affinity != null && affinity.relevanceScore >= 0.65f && affinity.goalAlignedCount >= 2) {
                matchingTopicCount++
                personalScoreBonus += 15
                personalEvidence.add("✓ Topic '$topic' was historically goal-aligned ${affinity.goalAlignedCount} times")
            }
        }

        // Check app meaning for this user
        val appMeaning = profile.appMeanings[packageName]
        if (appMeaning != null && appMeaning.sampleCount >= 5) {
            if (appMeaning.productiveRatio >= 0.70f && category in listOf(ActivityClassifier.CAT_LEARNING, ActivityClassifier.CAT_CODING)) {
                personalScoreBonus += 10
                personalEvidence.add("✓ ${appMeaning.appName} is consistently productive for your study (${(appMeaning.productiveRatio * 100).toInt()}% productive)")
            }
        }

        val personalRelevance = when {
            matchingTopicCount > 0 || personalScoreBonus >= 20 -> "HIGH"
            profile.maturity == ModelMaturity.NEW_USER -> contentRelevance
            personalScoreBonus >= 10 -> "MEDIUM"
            rawContentScore >= 30 -> "LOW"
            else -> "NONE"
        }

        // 3. Routine Match (Based on time-of-day learned habit)
        val matchingRoutine = profile.timeRoutines.find { it.startHour == hour }
        var routineScoreBonus = 0
        val routineMatch = if (matchingRoutine != null && profile.maturity != ModelMaturity.NEW_USER) {
            if (matchingRoutine.dominantCategory == category) {
                routineScoreBonus += 10
                historicalEvidence.add("✓ You usually focus on ${matchingRoutine.dominantCategory} around this time (${matchingRoutine.startHour}:00 – ${matchingRoutine.endHour}:00)")
                "HIGH"
            } else if (matchingRoutine.dominantCategory in listOf(ActivityClassifier.CAT_LEARNING, ActivityClassifier.CAT_CODING) &&
                category in listOf(ActivityClassifier.CAT_LEARNING, ActivityClassifier.CAT_CODING)) {
                routineScoreBonus += 5
                historicalEvidence.add("✓ Fits your typical study/coding window (${matchingRoutine.startHour}:00 – ${matchingRoutine.endHour}:00)")
                "MEDIUM"
            } else {
                historicalEvidence.add("ℹ️ Outside your usual routine for ${category} (usually ${matchingRoutine.dominantCategory})")
                "LOW"
            }
        } else {
            if (profile.maturity == ModelMaturity.NEW_USER) {
                historicalEvidence.add("🌱 Learning your daily routines and focus windows...")
                "MEDIUM"
            } else {
                "LOW"
            }
        }

        return PersonalEvaluation(
            contentRelevance = contentRelevance,
            personalRelevance = personalRelevance,
            routineMatch = routineMatch,
            personalScoreBonus = personalScoreBonus,
            routineScoreBonus = routineScoreBonus,
            personalEvidence = personalEvidence,
            historicalEvidence = historicalEvidence
        )
    }

    private fun mapCategoryToIntent(category: String): String {
        return when (category) {
            ActivityClassifier.CAT_LEARNING -> ActivityClassifier.UserIntent.LEARNING.name
            ActivityClassifier.CAT_CODING -> ActivityClassifier.UserIntent.CODING.name
            ActivityClassifier.CAT_PRODUCTIVITY -> ActivityClassifier.UserIntent.PROJECT_WORK.name
            ActivityClassifier.CAT_COMMUNICATION -> ActivityClassifier.UserIntent.COMMUNICATION.name
            ActivityClassifier.CAT_ENTERTAINMENT -> ActivityClassifier.UserIntent.ENTERTAINMENT.name
            ActivityClassifier.CAT_SOCIAL -> ActivityClassifier.UserIntent.SOCIAL_BROWSING.name
            ActivityClassifier.CAT_GAMING -> ActivityClassifier.UserIntent.GAMING.name
            else -> ActivityClassifier.UserIntent.UNKNOWN.name
        }
    }

    fun toEntity(profile: PersonalBehaviorProfile): PersonalBehaviorProfileEntity {
        return PersonalBehaviorProfileEntity(
            maturityStage = profile.maturity.name,
            totalSessionsObserved = profile.totalSessionsObserved,
            totalDaysTracked = profile.totalDaysTracked,
            userCorrectionCount = profile.userCorrectionsCount,
            dominantProductiveHours = profile.topProductiveHours.joinToString(","),
            dominantDistractionHours = profile.topDistractionHours.joinToString(","),
            confidenceCoverageHigh = profile.confidenceCoverageHigh,
            confidenceCoverageMed = profile.confidenceCoverageMed,
            confidenceCoverageLow = profile.confidenceCoverageLow,
            lastUpdatedMillis = System.currentTimeMillis()
        )
    }

    fun fromEntity(entity: PersonalBehaviorProfileEntity): PersonalBehaviorProfile {
        val maturity = try { ModelMaturity.valueOf(entity.maturityStage) } catch (e: Exception) { ModelMaturity.NEW_USER }
        val prodHours = entity.dominantProductiveHours.split(",").mapNotNull { it.trim().toIntOrNull() }
        val distHours = entity.dominantDistractionHours.split(",").mapNotNull { it.trim().toIntOrNull() }
        return PersonalBehaviorProfile(
            maturity = maturity,
            totalSessionsObserved = entity.totalSessionsObserved,
            totalDaysTracked = entity.totalDaysTracked,
            userCorrectionsCount = entity.userCorrectionCount,
            topProductiveHours = prodHours,
            topDistractionHours = distHours,
            confidenceCoverageHigh = entity.confidenceCoverageHigh,
            confidenceCoverageMed = entity.confidenceCoverageMed,
            confidenceCoverageLow = entity.confidenceCoverageLow
        )
    }

    data class PersonalEvaluation(
        val contentRelevance: String,
        val personalRelevance: String,
        val routineMatch: String,
        val personalScoreBonus: Int,
        val routineScoreBonus: Int,
        val personalEvidence: List<String>,
        val historicalEvidence: List<String>
    )
}
