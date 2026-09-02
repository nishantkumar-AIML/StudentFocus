package com.ai_assistant.studentfocus

import com.ai_assistant.studentfocus.models.*
import com.ai_assistant.studentfocus.utils.ActivityClassifier
import com.ai_assistant.studentfocus.utils.ActivityClassifier.ConfidenceLevel
import com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment
import com.ai_assistant.studentfocus.utils.ActivityClassifier.UserIntent
import com.ai_assistant.studentfocus.utils.AppUsageHelper
import com.ai_assistant.studentfocus.utils.PersonalLearningEngine
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ActivityAggregationUnitTest {

    // -------------------------------------------------------------
    // 1. Cold Start & Maturity: NEW_USER
    // -------------------------------------------------------------
    @Test
    fun test1_ColdStartMaturityNewUser() {
        val emptyProfile = PersonalLearningEngine.buildPersonalProfile(emptyList(), emptyList())
        assertEquals(ModelMaturity.NEW_USER, emptyProfile.maturity)
        assertEquals(0, emptyProfile.totalSessionsObserved)
        assertEquals(0, emptyProfile.totalDaysTracked)
        assertEquals(100, emptyProfile.confidenceCoverageLow)
    }

    // -------------------------------------------------------------
    // 2. Maturity Progression: LEARNING Stage
    // -------------------------------------------------------------
    @Test
    fun test2_MaturityLearningStage() {
        val now = System.currentTimeMillis()
        val sessions = (1..15).map { i ->
            AppActivitySessionEntity(
                date = "2026-08-${20 + (i % 3)}",
                startTime = now - (i * 3600000L),
                endTime = now - (i * 3600000L) + 600000L,
                durationMillis = 600000L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                activityType = "Video",
                contentTitle = "Lecture $i",
                category = ActivityClassifier.CAT_LEARNING,
                goalAlignment = GoalAlignment.GOAL_ALIGNED.name
            )
        }
        val profile = PersonalLearningEngine.buildPersonalProfile(sessions, emptyList(), now)
        assertEquals(ModelMaturity.LEARNING, profile.maturity)
        assertTrue(profile.totalSessionsObserved >= 10)
    }

    // -------------------------------------------------------------
    // 3. Maturity Progression: PERSONALIZED Stage
    // -------------------------------------------------------------
    @Test
    fun test3_MaturityPersonalizedStage() {
        val now = System.currentTimeMillis()
        val sessions = (1..60).map { i ->
            val dayOffset = (i % 10) + 1
            AppActivitySessionEntity(
                date = "2026-08-${10 + dayOffset}",
                startTime = now - (dayOffset * 86400000L) + (i * 60000L),
                endTime = now - (dayOffset * 86400000L) + (i * 60000L) + 600000L,
                durationMillis = 600000L,
                packageName = "com.android.chrome",
                appName = "Chrome",
                activityType = "Web",
                contentTitle = "Documentation $i",
                category = ActivityClassifier.CAT_LEARNING,
                goalAlignment = GoalAlignment.GOAL_ALIGNED.name
            )
        }
        val profile = PersonalLearningEngine.buildPersonalProfile(sessions, emptyList(), now)
        assertEquals(ModelMaturity.PERSONALIZED, profile.maturity)
        assertTrue(profile.totalDaysTracked >= 7)
    }

    // -------------------------------------------------------------
    // 4. Maturity Progression: ADAPTIVE Stage with Feedback
    // -------------------------------------------------------------
    @Test
    fun test4_MaturityAdaptiveStage() {
        val now = System.currentTimeMillis()
        val sessions = (1..60).map { i ->
            val dayOffset = (i % 10) + 1
            AppActivitySessionEntity(
                date = "2026-08-${10 + dayOffset}",
                startTime = now - (dayOffset * 86400000L) + (i * 60000L),
                endTime = now - (dayOffset * 86400000L) + (i * 60000L) + 600000L,
                durationMillis = 600000L,
                packageName = "com.android.chrome",
                appName = "Chrome",
                activityType = "Web",
                contentTitle = "Documentation $i",
                category = ActivityClassifier.CAT_LEARNING,
                goalAlignment = GoalAlignment.GOAL_ALIGNED.name
            )
        }
        val feedback = listOf(
            UserFeedbackEntity(
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                contentTitle = "Podcast",
                originalCategory = "Entertainment",
                userCategory = "Learning",
                feedbackType = "CORRECTION"
            )
        )
        val profile = PersonalLearningEngine.buildPersonalProfile(sessions, feedback, now)
        assertEquals(ModelMaturity.ADAPTIVE, profile.maturity)
        assertEquals(1, profile.userCorrectionsCount)
    }

    // -------------------------------------------------------------
    // 5. Time Decay Recency Weighting
    // -------------------------------------------------------------
    @Test
    fun test5_TimeDecayRecencyWeight() {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 3600 * 1000L

        val weightToday = PersonalLearningEngine.calculateDecayWeight(now, now)
        val weight14DaysAgo = PersonalLearningEngine.calculateDecayWeight(now - (14 * dayMillis), now)
        val weight28DaysAgo = PersonalLearningEngine.calculateDecayWeight(now - (28 * dayMillis), now)

        assertEquals(1.0f, weightToday, 0.05f)
        assertEquals(0.5f, weight14DaysAgo, 0.06f)
        assertEquals(0.25f, weight28DaysAgo, 0.06f)
        assertTrue("Recent weight must exceed old weight", weightToday > weight14DaysAgo)
        assertTrue("14-day weight must exceed 28-day weight", weight14DaysAgo > weight28DaysAgo)
    }

    // -------------------------------------------------------------
    // 6. Time of Day Routine Discovery
    // -------------------------------------------------------------
    @Test
    fun test6_TimeOfDayRoutineDiscovery() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 15)
        }
        val nineAmTime = cal.timeInMillis

        val sessions = (1..5).map { i ->
            AppActivitySessionEntity(
                date = "2026-08-${15 + i}",
                startTime = nineAmTime,
                endTime = nineAmTime + 1800000L,
                durationMillis = 1800000L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                activityType = "Video",
                contentTitle = "Coding Practice Session $i",
                category = ActivityClassifier.CAT_CODING,
                goalAlignment = GoalAlignment.GOAL_ALIGNED.name
            )
        }
        val profile = PersonalLearningEngine.buildPersonalProfile(sessions, emptyList(), nineAmTime)
        val routineAt9 = profile.timeRoutines.find { it.startHour == 9 }
        assertNotNull(routineAt9)
        assertEquals(ActivityClassifier.CAT_CODING, routineAt9?.dominantCategory)
        assertTrue((routineAt9?.confidence ?: 0f) >= 0.55f)
        assertTrue(profile.topProductiveHours.contains(9))
    }

    // -------------------------------------------------------------
    // 7. Direct Goal Evidence NOT Overridden by Time Routine
    // -------------------------------------------------------------
    @Test
    fun test7_DirectGoalNotOverriddenByTimeRoutine() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }
        val lateNightTime = cal.timeInMillis

        val establishedProfile = PersonalBehaviorProfile(
            maturity = ModelMaturity.PERSONALIZED,
            timeRoutines = listOf(
                LearnedTimeRoutine(startHour = 9, endHour = 11, dominantCategory = ActivityClassifier.CAT_LEARNING, dominantIntent = "LEARNING", confidence = 0.85f, sampleCount = 10)
            )
        )
        val academicContext = ActivityClassifier.AcademicContext(
            mainGoals = listOf("Machine Learning"),
            personalProfile = establishedProfile
        )

        val res = ActivityClassifier.classifyActivity(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            activityType = "Video",
            contentTitle = "Complete Machine Learning Neural Network Tutorial",
            academicContext = academicContext,
            timestamp = lateNightTime
        )

        // Must remain GOAL_ALIGNED with HIGH content relevance
        assertEquals(GoalAlignment.GOAL_ALIGNED, res.goalAlignment)
        assertEquals("HIGH", res.contentRelevance)
        assertEquals("LOW", res.routineMatch) // Late night is outside routine, but goal alignment is preserved!
        assertTrue(res.goalScore >= 70)
    }

    // -------------------------------------------------------------
    // 8. User Feedback Higher Authority than Auto-Classification
    // -------------------------------------------------------------
    @Test
    fun test8_AntiSelfReinforcingUserFeedbackAuthority() {
        val now = System.currentTimeMillis()
        val autoSessions = (1..5).map {
            AppActivitySessionEntity(
                date = "2026-08-25",
                startTime = now - 3600000L,
                endTime = now - 3000000L,
                durationMillis = 600000L,
                packageName = "com.spotify.music",
                appName = "Spotify",
                activityType = "Screen View",
                contentTitle = "Classical Study Music",
                category = ActivityClassifier.CAT_ENTERTAINMENT,
                goalAlignment = GoalAlignment.OFF_GOAL.name
            )
        }

        val feedback = listOf(
            UserFeedbackEntity(
                packageName = "com.spotify.music",
                appName = "Spotify",
                contentTitle = "Classical Study Music",
                originalCategory = "Entertainment",
                userCategory = ActivityClassifier.CAT_LEARNING,
                feedbackType = "CORRECTION",
                weight = 3.0f
            )
        )

        val profile = PersonalLearningEngine.buildPersonalProfile(autoSessions, feedback, now)
        assertEquals(1, profile.userCorrectionsCount)
        assertTrue(profile.topicAffinities.containsKey("classical") || profile.topicAffinities.containsKey("study"))
    }

    // -------------------------------------------------------------
    // 9. Topic Affinity Learning
    // -------------------------------------------------------------
    @Test
    fun test9_TopicAffinityLearning() {
        val now = System.currentTimeMillis()
        val sessions = (1..4).map { i ->
            AppActivitySessionEntity(
                date = "2026-08-${20 + i}",
                startTime = now - (i * 3600000L),
                endTime = now - (i * 3600000L) + 600000L,
                durationMillis = 600000L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                activityType = "Video",
                contentTitle = "LangChain Agent Tutorial $i",
                category = ActivityClassifier.CAT_LEARNING,
                goalAlignment = GoalAlignment.GOAL_ALIGNED.name,
                matchedTopics = "langchain, agent"
            )
        }
        val profile = PersonalLearningEngine.buildPersonalProfile(sessions, emptyList(), now)
        val affinity = profile.topicAffinities["langchain"]
        assertNotNull(affinity)
        assertTrue((affinity?.relevanceScore ?: 0f) >= 0.80f)
        assertTrue((affinity?.goalAlignedCount ?: 0) >= 3)
    }

    // -------------------------------------------------------------
    // 10. App Meaning Personalization
    // -------------------------------------------------------------
    @Test
    fun test10_AppMeaningPersonalization() {
        val now = System.currentTimeMillis()
        val sessions = (1..10).map { i ->
            AppActivitySessionEntity(
                date = "2026-08-25",
                startTime = now - (i * 600000L),
                endTime = now - (i * 600000L) + 500000L,
                durationMillis = 500000L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                activityType = "Video",
                contentTitle = "Python Course Part $i",
                category = ActivityClassifier.CAT_LEARNING,
                goalAlignment = GoalAlignment.GOAL_ALIGNED.name
            )
        }
        val profile = PersonalLearningEngine.buildPersonalProfile(sessions, emptyList(), now)
        val ytMeaning = profile.appMeanings["com.google.android.youtube"]
        assertNotNull(ytMeaning)
        assertEquals(1.0f, ytMeaning?.productiveRatio ?: 0f, 0.01f)
        assertEquals(10, ytMeaning?.sampleCount)
    }

    // -------------------------------------------------------------
    // 11. Activity Sequence: Productive Workflow
    // -------------------------------------------------------------
    @Test
    fun test11_TransitionProductiveWorkflow() {
        val now = System.currentTimeMillis()
        val sessions = listOf(
            AppActivitySessionEntity(
                date = "2026-08-28",
                startTime = now - 1800000L,
                endTime = now - 1200000L,
                durationMillis = 600000L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                activityType = "Video",
                contentTitle = "Dynamic Programming Tutorial",
                category = ActivityClassifier.CAT_LEARNING
            ),
            AppActivitySessionEntity(
                date = "2026-08-28",
                startTime = now - 1100000L,
                endTime = now - 300000L,
                durationMillis = 800000L,
                packageName = "com.android.chrome",
                appName = "Chrome",
                activityType = "Web",
                contentTitle = "LeetCode #322 Coin Change Solution",
                category = ActivityClassifier.CAT_CODING
            ),
            AppActivitySessionEntity(
                date = "2026-08-28",
                startTime = now - 1800000L + 86400000L,
                endTime = now - 1200000L + 86400000L,
                durationMillis = 600000L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                activityType = "Video",
                contentTitle = "Dynamic Programming Tutorial 2",
                category = ActivityClassifier.CAT_LEARNING
            ),
            AppActivitySessionEntity(
                date = "2026-08-28",
                startTime = now - 1100000L + 86400000L,
                endTime = now - 300000L + 86400000L,
                durationMillis = 800000L,
                packageName = "com.android.chrome",
                appName = "Chrome",
                activityType = "Web",
                contentTitle = "LeetCode #300 Longest Increasing Subsequence",
                category = ActivityClassifier.CAT_CODING
            )
        )
        val profile = PersonalLearningEngine.buildPersonalProfile(sessions, emptyList(), now)
        val prodTrans = profile.commonTransitions.find { it.fromCategory == ActivityClassifier.CAT_LEARNING && it.toCategory == ActivityClassifier.CAT_CODING }
        assertNotNull(prodTrans)
        assertEquals("PRODUCTIVE_WORKFLOW", prodTrans?.transitionType)
    }

    // -------------------------------------------------------------
    // 12. Activity Sequence: Distraction Loop
    // -------------------------------------------------------------
    @Test
    fun test12_TransitionDistractionLoop() {
        val now = System.currentTimeMillis()
        val sessions = listOf(
            AppActivitySessionEntity(
                date = "2026-08-28",
                startTime = now - 1800000L,
                endTime = now - 1200000L,
                durationMillis = 600000L,
                packageName = "com.instagram.android",
                appName = "Instagram",
                activityType = "Reel",
                contentTitle = "Trending Reels",
                category = ActivityClassifier.CAT_SOCIAL
            ),
            AppActivitySessionEntity(
                date = "2026-08-28",
                startTime = now - 1100000L,
                endTime = now - 300000L,
                durationMillis = 800000L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                activityType = "Shorts",
                contentTitle = "Funny Shorts",
                category = ActivityClassifier.CAT_ENTERTAINMENT
            ),
            AppActivitySessionEntity(
                date = "2026-08-28",
                startTime = now - 1800000L + 86400000L,
                endTime = now - 1200000L + 86400000L,
                durationMillis = 600000L,
                packageName = "com.instagram.android",
                appName = "Instagram",
                activityType = "Reel",
                contentTitle = "Trending Reels 2",
                category = ActivityClassifier.CAT_SOCIAL
            ),
            AppActivitySessionEntity(
                date = "2026-08-28",
                startTime = now - 1100000L + 86400000L,
                endTime = now - 300000L + 86400000L,
                durationMillis = 800000L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                activityType = "Shorts",
                contentTitle = "Funny Shorts 2",
                category = ActivityClassifier.CAT_ENTERTAINMENT
            )
        )
        val profile = PersonalLearningEngine.buildPersonalProfile(sessions, emptyList(), now)
        val distTrans = profile.commonTransitions.find { it.fromCategory == ActivityClassifier.CAT_SOCIAL && it.toCategory == ActivityClassifier.CAT_ENTERTAINMENT }
        assertNotNull(distTrans)
        assertEquals("DISTRACTION_LOOP", distTrans?.transitionType)
    }

    // -------------------------------------------------------------
    // 13. Probabilistic Routine Prediction Generation
    // -------------------------------------------------------------
    @Test
    fun test13_ProbabilisticPredictionGeneration() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        val twoPmTime = cal.timeInMillis

        val sessions = (1..60).map { i ->
            val dayOffset = (i % 10) + 1
            AppActivitySessionEntity(
                date = "2026-08-${10 + dayOffset}",
                startTime = twoPmTime - (dayOffset * 86400000L),
                endTime = twoPmTime - (dayOffset * 86400000L) + 1800000L,
                durationMillis = 1800000L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                activityType = "Video",
                contentTitle = "Physics Mechanics Lecture $i",
                category = ActivityClassifier.CAT_LEARNING,
                goalAlignment = GoalAlignment.GOAL_ALIGNED.name
            )
        }
        val profile = PersonalLearningEngine.buildPersonalProfile(sessions, emptyList(), twoPmTime)
        assertTrue(profile.activePredictions.isNotEmpty())
        val pred = profile.activePredictions.first()
        assertTrue(pred.predictionText.contains("Learning", ignoreCase = true))
        assertTrue(pred.predictionText.contains("14:00"))
    }

    // -------------------------------------------------------------
    // 14. Anti-False-Positive Meme Shield
    // -------------------------------------------------------------
    @Test
    fun test14_AntiFalsePositiveMemeShield() {
        val academicContext = ActivityClassifier.AcademicContext(
            mainGoals = listOf("Machine Learning", "Python")
        )
        val res = ActivityClassifier.classifyActivity(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            activityType = "Video",
            contentTitle = "Python Programmers Be Like - Funny Coding Memes Compilation",
            academicContext = academicContext
        )
        assertEquals(GoalAlignment.OFF_GOAL, res.goalAlignment)
        assertEquals(ActivityClassifier.CAT_ENTERTAINMENT, res.category)
        assertTrue(res.negativeEvidence.any { it.contains("meme", ignoreCase = true) })
    }

    // -------------------------------------------------------------
    // 15. Shorts & Reels Distraction Classification
    // -------------------------------------------------------------
    @Test
    fun test15_ShortsAndReelsDistractionClassification() {
        val academicContext = ActivityClassifier.AcademicContext(
            mainGoals = listOf("Data Structures and Algorithms")
        )
        val res = ActivityClassifier.classifyActivity(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            activityType = "Shorts",
            contentTitle = "Top 5 Quick DSA Tricks in 60 seconds #shorts",
            academicContext = academicContext
        )
        assertEquals(GoalAlignment.OFF_GOAL, res.goalAlignment)
        assertTrue(res.negativeEvidence.any { it.contains("Short-form", ignoreCase = true) })
    }

    // -------------------------------------------------------------
    // 16. Unavailable Title Preserved as UNKNOWN
    // -------------------------------------------------------------
    @Test
    fun test16_UnavailableTitlePreservedAsUnknown() {
        val res = ActivityClassifier.classifyActivity(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            activityType = "Screen View",
            contentTitle = "Content details unavailable"
        )
        assertEquals(GoalAlignment.UNKNOWN, res.goalAlignment)
        assertEquals(ConfidenceLevel.LOW, res.confidenceLevel)
        assertEquals("NONE", res.contentRelevance)
    }

    // -------------------------------------------------------------
    // 17. User Explicit Override Highest Authority
    // -------------------------------------------------------------
    @Test
    fun test17_UserExplicitOverrideHighestAuthority() {
        val overrides = mapOf("com.google.android.youtube:Custom Video" to ActivityClassifier.CAT_CODING)
        val res = ActivityClassifier.classifyActivity(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            activityType = "Video",
            contentTitle = "Custom Video",
            userOverrides = overrides
        )
        assertEquals(GoalAlignment.GOAL_ALIGNED, res.goalAlignment)
        assertEquals(ActivityClassifier.CAT_CODING, res.category)
        assertEquals(100, res.goalScore)
        assertEquals("USER_OVERRIDE", res.classificationSource)
    }

    // -------------------------------------------------------------
    // 18. Exam Preparation Alignment
    // -------------------------------------------------------------
    @Test
    fun test18_ExamPreparationAlignment() {
        val academicContext = ActivityClassifier.AcademicContext(
            exams = listOf("Mid-Term OS Exam")
        )
        val res = ActivityClassifier.classifyActivity(
            packageName = "com.android.chrome",
            appName = "Chrome",
            activityType = "Web",
            contentTitle = "Operating Systems Paging and Virtual Memory Notes",
            academicContext = academicContext
        )
        assertEquals(GoalAlignment.GOAL_ALIGNED, res.goalAlignment)
        assertTrue(res.goalScore >= 65)
    }

    // -------------------------------------------------------------
    // 19. Active Task Relevance
    // -------------------------------------------------------------
    @Test
    fun test19_ActiveTaskRelevance() {
        val academicContext = ActivityClassifier.AcademicContext(
            tasks = listOf("Complete Binary Tree Assignment")
        )
        val res = ActivityClassifier.classifyActivity(
            packageName = "com.android.chrome",
            appName = "Chrome",
            activityType = "Web",
            contentTitle = "LeetCode #102 Binary Tree Level Order Traversal",
            domainOrSubtext = "leetcode.com",
            academicContext = academicContext
        )
        assertEquals(GoalAlignment.GOAL_ALIGNED, res.goalAlignment)
        assertEquals(ActivityClassifier.CAT_CODING, res.category)
    }

    // -------------------------------------------------------------
    // 20. Coding & Practice Universal Signal
    // -------------------------------------------------------------
    @Test
    fun test20_CodingAndPracticeUniversalSignal() {
        val res = ActivityClassifier.classifyActivity(
            packageName = "com.android.chrome",
            appName = "Chrome",
            activityType = "Web",
            contentTitle = "Two Sum - LeetCode Solution in Python",
            domainOrSubtext = "leetcode.com"
        )
        assertEquals(GoalAlignment.GOAL_ALIGNED, res.goalAlignment)
        assertEquals(ActivityClassifier.CAT_CODING, res.category)
        assertEquals(UserIntent.PRACTICING, res.detectedIntent)
    }

    // -------------------------------------------------------------
    // 21. Academic Communication Detection
    // -------------------------------------------------------------
    @Test
    fun test21_AcademicCommunicationDetection() {
        val res = ActivityClassifier.classifyActivity(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            activityType = "Chat",
            contentTitle = "College Batch 2026: Lab practical and syllabus discussion"
        )
        assertEquals(GoalAlignment.GOAL_ALIGNED, res.goalAlignment)
        assertEquals(ActivityClassifier.CAT_COMMUNICATION, res.category)
        assertTrue(res.positiveEvidence.any { it.contains("Academic", ignoreCase = true) })
    }

    // -------------------------------------------------------------
    // 22. Shopping & Gaming Intent
    // -------------------------------------------------------------
    @Test
    fun test22_ShoppingAndGamingDetection() {
        val resGame = ActivityClassifier.classifyActivity(
            packageName = "com.tencent.ig",
            appName = "PUBG Mobile",
            activityType = "Game",
            contentTitle = "Battle Royale Match Gameplay"
        )
        assertEquals(GoalAlignment.OFF_GOAL, resGame.goalAlignment)
        assertEquals(ActivityClassifier.CAT_GAMING, resGame.category)

        val resShop = ActivityClassifier.classifyActivity(
            packageName = "in.amazon.mShop.android.shopping",
            appName = "Amazon",
            activityType = "Shopping",
            contentTitle = "Electronics Sale - Cart Checkout"
        )
        assertEquals(GoalAlignment.OFF_GOAL, resShop.goalAlignment)
    }

    // -------------------------------------------------------------
    // 23. Confidence Coverage Distribution
    // -------------------------------------------------------------
    @Test
    fun test23_ConfidenceCoverageDistribution() {
        val now = System.currentTimeMillis()
        val sessions = listOf(
            AppActivitySessionEntity(
                date = "2026-08-28",
                startTime = now,
                endTime = now + 600000L,
                durationMillis = 600000L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                activityType = "Video",
                contentTitle = "Python Course",
                confidenceScore = 0.95f
            ),
            AppActivitySessionEntity(
                date = "2026-08-28",
                startTime = now + 700000L,
                endTime = now + 1300000L,
                durationMillis = 600000L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                activityType = "Video",
                contentTitle = "Web Dev",
                confidenceScore = 0.70f
            ),
            AppActivitySessionEntity(
                date = "2026-08-28",
                startTime = now + 1400000L,
                endTime = now + 2000000L,
                durationMillis = 600000L,
                packageName = "com.unknown.app",
                appName = "Unknown",
                activityType = "Screen View",
                contentTitle = "Content details unavailable",
                confidenceScore = 0.40f
            )
        )
        val profile = PersonalLearningEngine.buildPersonalProfile(sessions, emptyList(), now)
        assertEquals(33, profile.confidenceCoverageHigh)
        assertEquals(33, profile.confidenceCoverageMed)
        assertEquals(33, profile.confidenceCoverageLow)
    }

    // -------------------------------------------------------------
    // 24. Three Pillar Separation Integrity
    // -------------------------------------------------------------
    @Test
    fun test24_ThreePillarSeparationIntegrity() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }
        val tenAm = cal.timeInMillis

        val profile = PersonalBehaviorProfile(
            maturity = ModelMaturity.PERSONALIZED,
            timeRoutines = listOf(
                LearnedTimeRoutine(startHour = 10, endHour = 11, dominantCategory = ActivityClassifier.CAT_CODING, dominantIntent = "CODING", confidence = 0.9f, sampleCount = 10)
            ),
            topicAffinities = mapOf(
                "python" to LearnedTopicAffinity("python", goalAlignedCount = 8, offGoalCount = 0, userConfirmationCount = 2, lastSeenMillis = tenAm, relevanceScore = 1.0f)
            )
        )

        val academicContext = ActivityClassifier.AcademicContext(
            mainGoals = listOf("Python"),
            personalProfile = profile
        )

        val res = ActivityClassifier.classifyActivity(
            packageName = "com.android.chrome",
            appName = "Chrome",
            activityType = "Web",
            contentTitle = "Python Advanced Generators Tutorial",
            academicContext = academicContext,
            timestamp = tenAm
        )

        assertEquals("HIGH", res.contentRelevance)
        assertEquals("HIGH", res.personalRelevance)
        assertEquals("HIGH", res.routineMatch) // Fits exact learned coding routine window (10:00 - 11:00)
        assertEquals(GoalAlignment.GOAL_ALIGNED, res.goalAlignment)
    }

    // -------------------------------------------------------------
    // 25. Session Sanitization & Non-Overlapping Invariants
    // -------------------------------------------------------------
    @Test
    fun test25_SessionSanitizationNonOverlapping() {
        val rawSessions = listOf(
            AppActivitySessionEntity(
                id = "1",
                date = "2026-08-28",
                startTime = 1000L,
                endTime = 5000L,
                durationMillis = 4000L,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                activityType = "Video",
                contentTitle = "ML Tutorial"
            ),
            AppActivitySessionEntity(
                id = "2",
                date = "2026-08-28",
                startTime = 3000L, // Overlapping!
                endTime = 8000L,
                durationMillis = 5000L,
                packageName = "com.android.chrome",
                appName = "Chrome",
                activityType = "Web",
                contentTitle = "Docs"
            )
        )

        val (sanitized, report) = AppUsageHelper.sanitizeAndRepairActivitySessions(rawSessions, emptyList())
        assertEquals(2, sanitized.size)
        val session1 = sanitized.find { it.packageName == "com.google.android.youtube" }!!
        val session2 = sanitized.find { it.packageName == "com.android.chrome" }!!
        // Session 1 must end at 3000L, Session 2 must start at 3000L
        assertEquals(1000L, session1.startTime)
        assertEquals(3000L, session1.endTime)
        assertEquals(2000L, session1.durationMillis)
        assertEquals(3000L, session2.startTime)
        assertEquals(8000L, session2.endTime)
        assertEquals(5000L, session2.durationMillis)
    }
}