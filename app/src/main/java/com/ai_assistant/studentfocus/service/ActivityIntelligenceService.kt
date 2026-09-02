package com.ai_assistant.studentfocus.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ai_assistant.studentfocus.database.AppDatabase
import com.ai_assistant.studentfocus.models.AppActivitySessionEntity
import com.ai_assistant.studentfocus.utils.ActivityClassifier
import com.ai_assistant.studentfocus.utils.AppUsageClassifier
import kotlinx.coroutines.*
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

class ActivityIntelligenceService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // In-memory academic context & overrides cache for real-time goal detection
    private var cachedAcademicContext = ActivityClassifier.AcademicContext()
    private var cachedOverrides: Map<String, String> = emptyMap()
    private var lastContextSyncTime: Long = 0L

    // Active in-progress activity session state
    private var activeSessionId: String? = null
    private var currentPackage: String = ""
    private var currentAppName: String = ""
    private var currentActivityType: String = ""
    private var currentContentTitle: String = ""
    private var currentDomain: String? = null
    private var currentCategory: String = ActivityClassifier.CAT_OTHER
    private var currentGoalAlignment: String = ActivityClassifier.GoalAlignment.UNKNOWN.name
    private var currentGoalReason: String = ""
    private var currentGoalScore: Int = 0
    private var currentDetectedIntent: String = ActivityClassifier.UserIntent.UNKNOWN.name
    private var currentMatchedGoal: String = ""
    private var currentMatchedTopics: String = ""
    private var currentContentRelevance: String = "HIGH"
    private var currentPersonalRelevance: String = "HIGH"
    private var currentRoutineMatch: String = "HIGH"
    private var currentConfidence: Float = 0.5f
    private var sessionStartTime: Long = 0L
    private var lastSeenTime: Long = 0L

    // Temporary Title Loss / UI Overlay Grace State
    private var lastKnownValidTitle: String = ""
    private var lastValidTitleTime: Long = 0L

    // Screen Lock & System State
    private var isScreenLocked: Boolean = false
    private var isReceiverRegistered: Boolean = false

    private var systemUiStartTime: Long = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenLocked = true
                    flushAndResetCurrentSession(System.currentTimeMillis())
                    systemUiStartTime = 0L
                }
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> {
                    isScreenLocked = false
                    systemUiStartTime = 0L
                }
            }
        }
    }

    // Debounce to prevent flooding
    private var lastDetectedTitle: String = ""
    private var lastProcessTime: Long = 0L

    object ServicePerformanceMetrics {
        val eventsReceived = java.util.concurrent.atomic.AtomicLong(0)
        val eventsIgnored = java.util.concurrent.atomic.AtomicLong(0)
        val eventsProcessed = java.util.concurrent.atomic.AtomicLong(0)
        val dbWritesCount = java.util.concurrent.atomic.AtomicLong(0)
        val dbQueriesCount = java.util.concurrent.atomic.AtomicLong(0)
        val learningRunsCount = java.util.concurrent.atomic.AtomicLong(0)
        val totalLearningTimeMs = java.util.concurrent.atomic.AtomicLong(0)
    }

    companion object {
        const val SYSTEM_UI_INTERRUPTION_THRESHOLD_MS = 4000L
        const val CONTENT_IDENTITY_GRACE_PERIOD_MS = 6000L // 6s grace for temporary title loss/overlays

        var isServiceRunning = false
            private set

        fun isAccessibilityEnabled(context: Context): Boolean {
            if (isServiceRunning) return true

            // 1. AccessibilityManager API check (Most reliable on all Android versions)
            try {
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
                val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK) ?: emptyList()
                for (service in enabledServices) {
                    if (service.resolveInfo?.serviceInfo?.packageName == context.packageName) {
                        return true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Settings.Secure check fallback
            try {
                val accessibilityEnabled = android.provider.Settings.Secure.getInt(
                    context.contentResolver,
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
                )
                if (accessibilityEnabled == 1) {
                    val settingValue = android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                    )
                    if (settingValue != null) {
                        val simpleName = ActivityIntelligenceService::class.java.simpleName
                        val canonicalName = ActivityIntelligenceService::class.java.canonicalName ?: ""
                        return settingValue.contains(context.packageName) &&
                                (settingValue.contains(simpleName) || settingValue.contains(canonicalName))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true

        // Clean initial state to prevent phantom sessions on reboot/restart
        currentPackage = ""
        currentAppName = ""
        currentActivityType = ""
        currentContentTitle = ""
        currentDomain = null
        sessionStartTime = 0L
        systemUiStartTime = 0L

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 250
        }
        serviceInfo = info

        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(screenReceiver, filter)
            isReceiverRegistered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        refreshAcademicContextAsync()
        startPeriodicFlushTimer()
        startPeriodicContextRefreshTimer()
        startPeriodicLearningTimer()
    }

    /**
     * Lightweight academic context refresh: queries only settings/subjects/tasks (does NOT scan thousands of activity sessions).
     */
    private fun refreshAcademicContextAsync() {
        val now = System.currentTimeMillis()
        if (now - lastContextSyncTime < 30000 && cachedOverrides.isNotEmpty()) return
        lastContextSyncTime = now

        serviceScope.launch(Dispatchers.IO) {
            try {
                ServicePerformanceMetrics.dbQueriesCount.addAndGet(6)
                val db = AppDatabase.getDatabase(applicationContext)
                val subjects = db.taskDao().getAllAttendanceList().map { it.subjectName }
                val courses = db.taskDao().getAllCoursesList().map { it.name }
                val exams = db.taskDao().getAllExamsList().map { it.name }
                val allTasks = db.taskDao().getAllTasksList()
                val tasks = allTasks.map { it.title } + allTasks.mapNotNull { it.subject }.filter { it.isNotBlank() }
                val settings = db.taskDao().getAllSettingsList()
                val mainGoals = settings.filter { it.key.contains("goal", ignoreCase = true) || it.key.contains("focus", ignoreCase = true) }.map { it.value }
                val focusSkills = settings.filter { it.key.contains("skill", ignoreCase = true) }.map { it.value }
                val overrides = db.taskDao().getAllCategoryOverridesList().associate { it.targetKey to it.customCategory }

                // Retrieve cached profile from DB or keep in-memory profile
                val cachedProfileEntity = db.taskDao().getPersonalBehaviorProfileDirect()
                val profile = if (cachedProfileEntity != null) {
                    com.ai_assistant.studentfocus.utils.PersonalLearningEngine.fromEntity(cachedProfileEntity)
                } else {
                    cachedAcademicContext.personalProfile
                }

                cachedAcademicContext = ActivityClassifier.AcademicContext(
                    subjects = subjects.distinct(),
                    courses = courses.distinct(),
                    exams = exams.distinct(),
                    tasks = tasks.distinct(),
                    mainGoals = mainGoals.distinct(),
                    focusSkills = focusSkills.distinct(),
                    userCorrections = overrides,
                    personalProfile = profile
                )
                cachedOverrides = overrides
                ActivityClassifier.clearCache()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Periodic background context refresher (every 10 minutes) to stay in sync with user changes in app.
     */
    private fun startPeriodicContextRefreshTimer() {
        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(600000L) // 10 minutes
                refreshAcademicContextAsync()
            }
        }
    }

    private var lastLearnedSessionCount: Int = -1
    private var lastLearnedFeedbackCount: Int = -1

    /**
     * Periodic asynchronous background learning engine: runs every 30 minutes completely off the hot path.
     */
    private fun startPeriodicLearningTimer() {
        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val learnStartTime = System.currentTimeMillis()
                    val db = AppDatabase.getDatabase(applicationContext)
                    ServicePerformanceMetrics.dbQueriesCount.addAndGet(2)
                    val recentSessions = db.taskDao().getAllActivitySessionsList()
                    val feedbackList = db.taskDao().getAllUserFeedbackList()

                    val currentSessionCount = recentSessions.size
                    val currentFeedbackCount = feedbackList.size

                    // Skip redundant CPU-heavy re-computation if dataset has not changed
                    val hasNewData = currentSessionCount != lastLearnedSessionCount || currentFeedbackCount != lastLearnedFeedbackCount
                    if (hasNewData && (recentSessions.isNotEmpty() || feedbackList.isNotEmpty())) {
                        val profile = withContext(Dispatchers.Default) {
                            com.ai_assistant.studentfocus.utils.PersonalLearningEngine.buildPersonalProfile(
                                sessions = recentSessions,
                                userFeedbackList = feedbackList,
                                currentTimeMillis = System.currentTimeMillis()
                            )
                        }

                        val entity = com.ai_assistant.studentfocus.utils.PersonalLearningEngine.toEntity(profile)
                        db.taskDao().insertPersonalBehaviorProfile(entity)
                        ServicePerformanceMetrics.dbWritesCount.incrementAndGet()

                        cachedAcademicContext = cachedAcademicContext.copy(personalProfile = profile)
                        ActivityClassifier.clearCache()

                        lastLearnedSessionCount = currentSessionCount
                        lastLearnedFeedbackCount = currentFeedbackCount

                        ServicePerformanceMetrics.learningRunsCount.incrementAndGet()
                        ServicePerformanceMetrics.totalLearningTimeMs.addAndGet(System.currentTimeMillis() - learnStartTime)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(1800000L) // 30 minutes
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        ServicePerformanceMetrics.eventsReceived.incrementAndGet()
        if (event == null || isScreenLocked) {
            ServicePerformanceMetrics.eventsIgnored.incrementAndGet()
            return
        }
        val packageName = event.packageName?.toString() ?: run {
            ServicePerformanceMetrics.eventsIgnored.incrementAndGet()
            return
        }

        val eventType = event.eventType
        val now = System.currentTimeMillis()

        // Fast path 1: Ignore scroll, click, gesture, focus events for currently active app (0 CPU overhead)
        if ((eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
             eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
             eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
             eventType == AccessibilityEvent.TYPE_GESTURE_DETECTION_START ||
             eventType == AccessibilityEvent.TYPE_GESTURE_DETECTION_END) && packageName == currentPackage) {
            ServicePerformanceMetrics.eventsIgnored.incrementAndGet()
            lastSeenTime = now
            return
        }

        // Fast path 2: Debounce content change events if already inside same package and within 800ms
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && packageName == currentPackage && (now - lastProcessTime < 800L)) {
            ServicePerformanceMetrics.eventsIgnored.incrementAndGet()
            lastSeenTime = now
            return
        }

        val classification = AppUsageClassifier.classifyPackage(applicationContext, packageName)

        // 1. Keyboard / Input Method: User is typing inside the active user app!
        if (classification.packageType == AppUsageClassifier.PackageType.INPUT_METHOD) {
            ServicePerformanceMetrics.eventsIgnored.incrementAndGet()
            lastSeenTime = now
            return
        }

        // 2. Short vs Long System UI Interruption Handling (Notification shade, Volume slider, Status bar)
        if (classification.packageType == AppUsageClassifier.PackageType.SYSTEM_UI) {
            if (systemUiStartTime == 0L) {
                systemUiStartTime = now
            } else if (now - systemUiStartTime > 4000L) {
                // Long System UI period (> 4s): close active app session authoritatively
                if (currentPackage.isNotBlank() && sessionStartTime > 0) {
                    flushAndResetCurrentSession(systemUiStartTime)
                }
            }
            ServicePerformanceMetrics.eventsIgnored.incrementAndGet()
            return
        }

        // Returning to a user app resets system UI interruption timer
        systemUiStartTime = 0L

        // 3. Launchers, Tracking App, or Background Daemons:
        if (classification.packageType == AppUsageClassifier.PackageType.LAUNCHER ||
            classification.packageType == AppUsageClassifier.PackageType.TRACKING_APP ||
            classification.packageType == AppUsageClassifier.PackageType.SYSTEM_BACKGROUND ||
            !classification.isUserFacing) {
            if (currentPackage.isNotBlank() && sessionStartTime > 0) {
                flushAndResetCurrentSession(now)
            }
            ServicePerformanceMetrics.eventsIgnored.incrementAndGet()
            return
        }

        if (now - lastProcessTime < 400L && packageName == currentPackage) {
            ServicePerformanceMetrics.eventsIgnored.incrementAndGet()
            lastSeenTime = now
            return
        }
        lastProcessTime = now
        ServicePerformanceMetrics.eventsProcessed.incrementAndGet()

        val rootNode = try {
            event.source ?: rootInActiveWindow ?: return
        } catch (e: Exception) {
            try { rootInActiveWindow ?: return } catch (ex: Exception) { return }
        }

        // Privacy check: Do not inspect password screens or secure elements
        if (hasPasswordFields(rootNode)) {
            try { rootNode.recycle() } catch (e: Exception) {}
            return
        }

        val appName = getApplicationLabel(packageName)
        var detectedType = "App Activity"
        var detectedTitle = ""
        var detectedDomain: String? = null
        var confidence = 0.6f

        val pkgLower = packageName.lowercase(Locale.ROOT)

        try {
            // 1. YouTube Extraction
            if (pkgLower.contains("youtube")) {
                val (ytType, ytTitle) = extractYouTubeDetails(rootNode)
                detectedType = ytType
                detectedTitle = ytTitle
                detectedDomain = "youtube.com"
                confidence = if (detectedTitle.isNotBlank() && detectedTitle != "YouTube Video" && detectedTitle != "YouTube Shorts") 0.95f else 0.75f
            }
            // 2. Instagram Extraction
            else if (pkgLower.contains("instagram")) {
                val (igType, igTitle) = extractInstagramDetails(rootNode)
                detectedType = igType
                detectedTitle = igTitle
                detectedDomain = "instagram.com"
                confidence = 0.90f
            }
            // 3. Web Browsers (Chrome, Firefox, Brave, Edge, etc.)
            else if (isBrowserPackage(pkgLower)) {
                val (urlDomain, pageTitle) = extractBrowserDetails(rootNode)
                detectedType = "Web Browsing"
                detectedDomain = urlDomain
                detectedTitle = if (pageTitle.isNotBlank() && pageTitle != "Webpage Browsing") pageTitle else urlDomain ?: "Webpage"
                confidence = if (urlDomain != null) 0.92f else 0.70f
            }
            // 4. Spotify & Music Players
            else if (pkgLower.contains("spotify") || pkgLower.contains("music") || pkgLower.contains("jiosaavn")) {
                val (track, artist) = extractMusicDetails(rootNode)
                detectedType = "Music / Podcast"
                detectedTitle = if (artist.isNotBlank()) "$track • $artist" else track
                confidence = 0.90f
            }
            // 5. WhatsApp & Communication
            else if (pkgLower.contains("whatsapp") || pkgLower.contains("telegram")) {
                val (chatType, chatTitle) = extractMessagingDetails(rootNode)
                detectedType = chatType
                detectedTitle = chatTitle
                confidence = 0.85f
            }
            // 6. Generic Fallback Extraction
            else {
                val (genType, genTitle) = extractGenericDetails(rootNode, appName)
                detectedType = genType
                detectedTitle = genTitle
                confidence = 0.60f
            }
        } finally {
            try { rootNode.recycle() } catch (e: Exception) {}
        }

        if (detectedTitle.isBlank()) {
            detectedTitle = "Content details unavailable"
        }

        val classifiedResult = ActivityClassifier.classifyActivity(
            packageName = packageName,
            appName = appName,
            activityType = detectedType,
            contentTitle = detectedTitle,
            domainOrSubtext = detectedDomain,
            userOverrides = cachedOverrides,
            academicContext = cachedAcademicContext,
            timestamp = now
        )

        handleActivityTransition(
            now = now,
            packageName = packageName,
            appName = appName,
            activityType = detectedType,
            contentTitle = detectedTitle,
            domainOrSubtext = detectedDomain,
            category = classifiedResult.category,
            goalAlignment = classifiedResult.goalAlignment.name,
            goalReason = classifiedResult.primaryReason,
            goalScore = classifiedResult.goalScore,
            detectedIntent = classifiedResult.detectedIntent.name,
            matchedGoal = classifiedResult.matchedGoal ?: "",
            matchedTopics = classifiedResult.matchedTopics.joinToString(", "),
            contentRelevance = classifiedResult.contentRelevance,
            personalRelevance = classifiedResult.personalRelevance,
            routineMatch = classifiedResult.routineMatch,
            confidence = maxOf(confidence, classifiedResult.confidence)
        )
    }

    private fun normalizeTitle(title: String?): String {
        if (title.isNullOrBlank()) return ""
        var cleaned = title.trim().lowercase(Locale.ROOT)
        val suffixesToRemove = listOf(" - youtube", " • youtube", " - leetcode", " | leetcode", " - google chrome", " - chrome")
        for (s in suffixesToRemove) {
            if (cleaned.endsWith(s)) {
                cleaned = cleaned.removeSuffix(s).trim()
            }
        }
        return cleaned.replace("\\s+".toRegex(), " ")
    }

    private fun isSameActivityContent(
        pkg: String,
        type: String,
        title: String,
        now: Long
    ): Boolean {
        if (pkg != currentPackage || currentPackage.isBlank()) return false

        val normCurrent = normalizeTitle(currentContentTitle)
        val normNew = normalizeTitle(title)

        // 1. Direct match on normalized title
        if (normCurrent.isNotBlank() && normNew.isNotBlank() && normCurrent == normNew) {
            return true
        }

        // 2. Temporary Title Loss / UI Overlay Grace Period:
        // If the title temporarily becomes "Content details unavailable" (e.g. comment sheet opened, video controls fade),
        // preserve the active video session within CONTENT_IDENTITY_GRACE_PERIOD_MS
        val isNewUnavailable = normNew.isBlank() || title.contains("unavailable", ignoreCase = true)
        val isCurrentUnavailable = normCurrent.isBlank() || currentContentTitle.contains("unavailable", ignoreCase = true)

        if ((isNewUnavailable || isCurrentUnavailable) && (now - lastValidTitleTime <= CONTENT_IDENTITY_GRACE_PERIOD_MS)) {
            return true
        }

        // 3. Substring / Comment Panel overlay match (e.g., YouTube video title with comments expanded)
        if (normCurrent.isNotBlank() && normNew.isNotBlank()) {
            if (normCurrent.length >= 8 && normNew.contains(normCurrent)) return true
            if (normNew.length >= 8 && normCurrent.contains(normNew)) return true
        }

        // 4. Compatible Activity Types (e.g., YouTube Video with Comments/Controls overlay)
        val isCompatibleType = currentActivityType.equals(type, ignoreCase = true) ||
                (pkg.contains("youtube") && (currentActivityType == "Video" || currentActivityType == "Comments") && (type == "Video" || type == "Comments"))

        if (normCurrent.isNotBlank() && normNew.isNotBlank() && normCurrent == normNew && isCompatibleType) {
            return true
        }

        return false
    }

    private fun handleActivityTransition(
        now: Long,
        packageName: String,
        appName: String,
        activityType: String,
        contentTitle: String,
        domainOrSubtext: String?,
        category: String,
        goalAlignment: String,
        goalReason: String,
        goalScore: Int,
        detectedIntent: String,
        matchedGoal: String,
        matchedTopics: String,
        contentRelevance: String = "HIGH",
        personalRelevance: String = "HIGH",
        routineMatch: String = "HIGH",
        confidence: Float
    ) {
        val sameContent = isSameActivityContent(packageName, activityType, contentTitle, now)

        if (sameContent && activeSessionId != null) {
            // SAME CONTINUOUS ACTIVITY: Update in-memory state ONLY.
            // DO NOT write to SQLite on every event!
            lastSeenTime = now
            val isTitleValid = contentTitle.isNotBlank() && !contentTitle.contains("unavailable", ignoreCase = true)
            if (isTitleValid) {
                currentContentTitle = contentTitle
                lastKnownValidTitle = contentTitle
                lastValidTitleTime = now
            }
            if (domainOrSubtext != null) {
                currentDomain = domainOrSubtext
            }
            currentCategory = category
            currentGoalAlignment = goalAlignment
            currentGoalReason = goalReason
            currentGoalScore = goalScore
            currentDetectedIntent = detectedIntent
            currentMatchedGoal = matchedGoal
            currentMatchedTopics = matchedTopics
            currentContentRelevance = contentRelevance
            currentPersonalRelevance = personalRelevance
            currentRoutineMatch = routineMatch
            currentConfidence = maxOf(currentConfidence, confidence)
            return
        }

        // DIFFERENT CONTENT / APP TRANSITION:
        // 1. Finalize previous active session at exact timestamp `now` (Zero overlap)
        if (currentPackage.isNotBlank() && sessionStartTime > 0 && activeSessionId != null) {
            val duration = now - sessionStartTime
            if (duration >= 1500L) {
                upsertActiveSessionToDb(
                    sessionId = activeSessionId!!,
                    packageName = currentPackage,
                    appName = currentAppName,
                    activityType = currentActivityType,
                    contentTitle = currentContentTitle,
                    domainOrSubtext = currentDomain,
                    category = currentCategory,
                    goalAlignment = currentGoalAlignment,
                    goalReason = currentGoalReason,
                    goalScore = currentGoalScore,
                    detectedIntent = currentDetectedIntent,
                    matchedGoal = currentMatchedGoal,
                    matchedTopics = currentMatchedTopics,
                    contentRelevance = currentContentRelevance,
                    personalRelevance = currentPersonalRelevance,
                    routineMatch = currentRoutineMatch,
                    confidence = currentConfidence,
                    startTime = sessionStartTime,
                    endTime = now
                )
            }
        }

        // 2. Start a brand new activity session with a new UUID in memory
        val newSessionId = UUID.randomUUID().toString()
        activeSessionId = newSessionId
        currentPackage = packageName
        currentAppName = appName
        currentActivityType = activityType
        currentContentTitle = contentTitle
        currentDomain = domainOrSubtext
        currentCategory = category
        currentGoalAlignment = goalAlignment
        currentGoalReason = goalReason
        currentGoalScore = goalScore
        currentDetectedIntent = detectedIntent
        currentMatchedGoal = matchedGoal
        currentMatchedTopics = matchedTopics
        currentContentRelevance = contentRelevance
        currentPersonalRelevance = personalRelevance
        currentRoutineMatch = routineMatch
        currentConfidence = confidence
        sessionStartTime = now
        lastSeenTime = now

        val isTitleValid = contentTitle.isNotBlank() && !contentTitle.contains("unavailable", ignoreCase = true)
        if (isTitleValid) {
            lastKnownValidTitle = contentTitle
            lastValidTitleTime = now
        } else {
            lastKnownValidTitle = ""
            lastValidTitleTime = 0L
        }
    }

    private fun flushAndResetCurrentSession(now: Long) {
        if (currentPackage.isNotBlank() && sessionStartTime > 0 && activeSessionId != null) {
            val duration = now - sessionStartTime
            if (duration >= 1500L) {
                upsertActiveSessionToDb(
                    sessionId = activeSessionId!!,
                    packageName = currentPackage,
                    appName = currentAppName,
                    activityType = currentActivityType,
                    contentTitle = currentContentTitle,
                    domainOrSubtext = currentDomain,
                    category = currentCategory,
                    goalAlignment = currentGoalAlignment,
                    goalReason = currentGoalReason,
                    goalScore = currentGoalScore,
                    detectedIntent = currentDetectedIntent,
                    matchedGoal = currentMatchedGoal,
                    matchedTopics = currentMatchedTopics,
                    contentRelevance = currentContentRelevance,
                    personalRelevance = currentPersonalRelevance,
                    routineMatch = currentRoutineMatch,
                    confidence = currentConfidence,
                    startTime = sessionStartTime,
                    endTime = now
                )
            }
        }
        activeSessionId = null
        currentPackage = ""
        currentAppName = ""
        currentActivityType = ""
        currentContentTitle = ""
        currentDomain = null
        currentGoalAlignment = ActivityClassifier.GoalAlignment.UNKNOWN.name
        currentGoalReason = ""
        currentGoalScore = 0
        currentDetectedIntent = ActivityClassifier.UserIntent.UNKNOWN.name
        currentMatchedGoal = ""
        currentMatchedTopics = ""
        currentContentRelevance = "HIGH"
        currentPersonalRelevance = "HIGH"
        currentRoutineMatch = "HIGH"
    }

    private fun upsertActiveSessionToDb(
        sessionId: String,
        packageName: String,
        appName: String,
        activityType: String,
        contentTitle: String,
        domainOrSubtext: String?,
        category: String,
        goalAlignment: String,
        goalReason: String,
        goalScore: Int,
        detectedIntent: String,
        matchedGoal: String,
        matchedTopics: String,
        contentRelevance: String = "HIGH",
        personalRelevance: String = "HIGH",
        routineMatch: String = "HIGH",
        confidence: Float,
        startTime: Long,
        endTime: Long
    ) {
        val durationMillis = maxOf(0L, endTime - startTime)
        if (startTime > endTime || durationMillis <= 0) return

        val startDateStr = sdf.format(Date(startTime))
        val endDateStr = sdf.format(Date(endTime))

        // Check for midnight boundary crossing (e.g. 23:55 -> 00:05 next day)
        if (startDateStr != endDateStr) {
            val cal = Calendar.getInstance().apply {
                time = Date(startTime)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val midnightEnd = cal.timeInMillis
            val day1Duration = maxOf(0L, midnightEnd - startTime)
            val day2Start = midnightEnd + 1
            val day2Duration = maxOf(0L, endTime - day2Start)

            if (day1Duration > 0) {
                persistEntity(
                    id = sessionId,
                    date = startDateStr,
                    startTime = startTime,
                    endTime = midnightEnd,
                    durationMillis = day1Duration,
                    packageName = packageName,
                    appName = appName,
                    activityType = activityType,
                    contentTitle = contentTitle,
                    domainOrSubtext = domainOrSubtext,
                    category = category,
                    goalAlignment = goalAlignment,
                    goalReason = goalReason,
                    goalScore = goalScore,
                    detectedIntent = detectedIntent,
                    matchedGoal = matchedGoal,
                    matchedTopics = matchedTopics,
                    contentRelevance = contentRelevance,
                    personalRelevance = personalRelevance,
                    routineMatch = routineMatch,
                    confidence = confidence
                )
            }
            if (day2Duration > 0) {
                persistEntity(
                    id = "${sessionId}_day2",
                    date = endDateStr,
                    startTime = day2Start,
                    endTime = endTime,
                    durationMillis = day2Duration,
                    packageName = packageName,
                    appName = appName,
                    activityType = activityType,
                    contentTitle = contentTitle,
                    domainOrSubtext = domainOrSubtext,
                    category = category,
                    goalAlignment = goalAlignment,
                    goalReason = goalReason,
                    goalScore = goalScore,
                    detectedIntent = detectedIntent,
                    matchedGoal = matchedGoal,
                    matchedTopics = matchedTopics,
                    contentRelevance = contentRelevance,
                    personalRelevance = personalRelevance,
                    routineMatch = routineMatch,
                    confidence = confidence
                )
            }
        } else {
            persistEntity(
                id = sessionId,
                date = startDateStr,
                startTime = startTime,
                endTime = endTime,
                durationMillis = durationMillis,
                packageName = packageName,
                appName = appName,
                activityType = activityType,
                contentTitle = contentTitle,
                domainOrSubtext = domainOrSubtext,
                category = category,
                goalAlignment = goalAlignment,
                goalReason = goalReason,
                goalScore = goalScore,
                detectedIntent = detectedIntent,
                matchedGoal = matchedGoal,
                matchedTopics = matchedTopics,
                contentRelevance = contentRelevance,
                personalRelevance = personalRelevance,
                routineMatch = routineMatch,
                confidence = confidence
            )
        }
    }

    private fun persistEntity(
        id: String,
        date: String,
        startTime: Long,
        endTime: Long,
        durationMillis: Long,
        packageName: String,
        appName: String,
        activityType: String,
        contentTitle: String,
        domainOrSubtext: String?,
        category: String,
        goalAlignment: String,
        goalReason: String,
        goalScore: Int,
        detectedIntent: String,
        matchedGoal: String,
        matchedTopics: String,
        contentRelevance: String = "HIGH",
        personalRelevance: String = "HIGH",
        routineMatch: String = "HIGH",
        confidence: Float
    ) {
        val session = AppActivitySessionEntity(
            id = id,
            date = date,
            startTime = startTime,
            endTime = endTime,
            durationMillis = durationMillis,
            packageName = packageName,
            appName = appName,
            activityType = activityType,
            contentTitle = contentTitle.ifBlank { "Content details unavailable" },
            domainOrSubtext = domainOrSubtext,
            category = category,
            goalAlignment = goalAlignment,
            goalReason = goalReason,
            goalScore = goalScore,
            detectedIntent = detectedIntent,
            matchedGoal = matchedGoal,
            matchedTopics = matchedTopics,
            contentRelevance = contentRelevance,
            personalRelevance = personalRelevance,
            routineMatch = routineMatch,
            confidenceScore = confidence
        )

        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.taskDao().insertActivitySession(session)
                ServicePerformanceMetrics.dbWritesCount.incrementAndGet()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Periodically updates active in-progress session so long-running activities (e.g. 40m lecture)
     * are continuously updated in the database without generating new rows.
     */
    private fun startPeriodicFlushTimer() {
        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(120000L) // every 2 minutes checkpoint to conserve battery and CPU
                val now = System.currentTimeMillis()
                if (currentPackage.isNotBlank() && sessionStartTime > 0 && activeSessionId != null) {
                    val duration = now - sessionStartTime
                    if (duration >= 2000L) {
                        upsertActiveSessionToDb(
                            sessionId = activeSessionId!!,
                            packageName = currentPackage,
                            appName = currentAppName,
                            activityType = currentActivityType,
                            contentTitle = currentContentTitle,
                            domainOrSubtext = currentDomain,
                            category = currentCategory,
                            goalAlignment = currentGoalAlignment,
                            goalReason = currentGoalReason,
                            goalScore = currentGoalScore,
                            detectedIntent = currentDetectedIntent,
                            matchedGoal = currentMatchedGoal,
                            matchedTopics = currentMatchedTopics,
                            contentRelevance = currentContentRelevance,
                            personalRelevance = currentPersonalRelevance,
                            routineMatch = currentRoutineMatch,
                            confidence = currentConfidence,
                            startTime = sessionStartTime,
                            endTime = now
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // App-Specific Extraction Algorithms
    // -------------------------------------------------------------

    private fun extractYouTubeDetails(root: AccessibilityNodeInfo): Pair<String, String> {
        var isShorts = false
        var isSearching = false
        var isComments = false
        val candidateTitles = mutableListOf<String>()
        var exactTitle = ""
        var visitedCount = 0

        val junkTerms = setOf(
            "share", "subscribe", "subscribed", "like", "dislike", "download",
            "save", "remix", "thanks", "clip", "comments", "live chat", "join",
            "report", "auto-play", "captions", "settings", "fullscreen",
            "explore", "subscriptions", "library", "notifications", "youtube",
            "home", "trending", "history", "search"
        )

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null || visitedCount >= 35) return
            visitedCount++

            val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT) ?: ""
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val text = node.text?.toString()?.trim() ?: ""

            if (viewId.contains("shorts") || viewId.contains("reel") || desc.contains("shorts", ignoreCase = true)) {
                isShorts = true
            }
            if (viewId.contains("search_edit_text") || viewId.contains("searchbox")) {
                isSearching = true
                if (text.isNotBlank()) exactTitle = "Search: $text"
            }
            if (viewId.contains("comment") || desc.contains("comment", ignoreCase = true)) {
                isComments = true
            }

            // 1. Direct Video Title View IDs
            if (exactTitle.isBlank() && (viewId.contains("video_title") || viewId.contains("title_edit") ||
                        viewId.contains("player_video_title") || viewId.contains("watch_title") ||
                        viewId.contains("reel_video_title") || viewId.endsWith(":id/title") ||
                        viewId.contains("video_metadata_layout"))) {
                val candidate = if (text.isNotBlank()) text else desc
                if (candidate.isNotBlank() && candidate.length > 3 && candidate.lowercase(Locale.ROOT) !in junkTerms) {
                    exactTitle = candidate
                }
            }

            // 2. Collect rich non-junk text lines
            if (text.isNotBlank() && text.length in 6..140) {
                val lower = text.lowercase(Locale.ROOT)
                val isJunk = junkTerms.any { lower == it } ||
                        lower.contains("subscribers") ||
                        lower.contains("views") ||
                        lower.endsWith("ago") ||
                        lower.startsWith("http")
                if (!isJunk) {
                    candidateTitles.add(text)
                }
            }

            // 3. Clean contentDescription on video cards (e.g. "Title by Channel 10m views")
            if (desc.isNotBlank() && desc.length in 8..180 && desc.contains("by ", ignoreCase = true)) {
                val cleanedDesc = desc.substringBefore(" by ").trim()
                if (cleanedDesc.length > 5) {
                    candidateTitles.add(cleanedDesc)
                }
            }

            for (i in 0 until node.childCount) {
                if (visitedCount >= 25) break
                val child = node.getChild(i) ?: continue
                traverse(child)
                try { child.recycle() } catch (e: Exception) {}
            }
        }

        traverse(root)

        val finalTitle = when {
            exactTitle.isNotBlank() -> cleanTitle(exactTitle)
            candidateTitles.isNotEmpty() -> cleanTitle(candidateTitles.maxByOrNull { it.length } ?: candidateTitles.first())
            isShorts -> "YouTube Shorts"
            else -> "YouTube Video"
        }

        val activityType = when {
            isSearching -> "Search"
            isShorts -> "Shorts"
            finalTitle != "YouTube Video" && finalTitle != "YouTube Shorts" -> "Video"
            isComments -> "Comments"
            else -> "Video"
        }

        return Pair(activityType, finalTitle)
    }

    private fun extractInstagramDetails(root: AccessibilityNodeInfo): Pair<String, String> {
        var isReels = false
        var isDM = false
        var isStories = false
        var details = ""
        var visitedCount = 0

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null || visitedCount >= 25) return
            visitedCount++

            val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT) ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""

            if (viewId.contains("clips") || viewId.contains("reel") || desc.contains("reels", ignoreCase = true)) {
                isReels = true
            }
            if (viewId.contains("direct") || viewId.contains("thread") || viewId.contains("message")) {
                isDM = true
            }
            if (viewId.contains("story") || viewId.contains("stories") || desc.contains("story", ignoreCase = true)) {
                isStories = true
            }

            if (details.isBlank() && (viewId.contains("action_bar_title") || viewId.contains("username") || viewId.contains("title"))) {
                if (text.isNotBlank()) details = text
            }

            for (i in 0 until node.childCount) {
                if (visitedCount >= 25) break
                val child = node.getChild(i) ?: continue
                traverse(child)
                try { child.recycle() } catch (e: Exception) {}
            }
        }

        traverse(root)

        val type = when {
            isReels -> "Reels"
            isDM -> "Direct Messages"
            isStories -> "Stories"
            else -> "Feed"
        }

        val title = when {
            isReels -> "Instagram Reels"
            isDM -> if (details.isNotBlank()) "Chat with $details" else "Direct Messages"
            isStories -> if (details.isNotBlank()) "Stories: $details" else "Stories"
            else -> "Feed Browsing"
        }

        return Pair(type, title)
    }

    private fun extractBrowserDetails(root: AccessibilityNodeInfo): Pair<String?, String> {
        var extractedUrl: String? = null
        var extractedTitle: String = ""
        val candidateTitles = mutableListOf<String>()
        var visitedCount = 0

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null || visitedCount >= 25) return
            visitedCount++

            val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT) ?: ""
            val text = node.text?.toString()?.trim() ?: ""
            val desc = node.contentDescription?.toString()?.trim() ?: ""

            if (extractedUrl == null && (viewId.contains("url") || viewId.contains("location") ||
                        viewId.contains("address") || viewId.contains("search_box") ||
                        viewId.contains("omnibox") || viewId.contains("line_one"))) {
                if (text.isNotBlank()) {
                    extractedUrl = extractDomainFromUrl(text)
                }
            }

            if (extractedTitle.isBlank() && (viewId.contains("title") || viewId.contains("tab_title") ||
                        viewId.contains("page_title") || viewId.contains("heading") ||
                        viewId.contains("action_bar_title"))) {
                if (text.isNotBlank() && !text.startsWith("http") && text.length > 2) {
                    extractedTitle = text
                } else if (desc.isNotBlank() && !desc.startsWith("http") && desc.length > 2) {
                    extractedTitle = desc
                }
            }

            if (text.isNotBlank() && text.length in 5..120 && !text.startsWith("http") &&
                !text.contains("AM") && !text.contains("PM") && !text.contains("%")) {
                candidateTitles.add(text)
            }

            for (i in 0 until node.childCount) {
                if (visitedCount >= 25) break
                val child = node.getChild(i) ?: continue
                traverse(child)
                try { child.recycle() } catch (e: Exception) {}
            }
        }

        traverse(root)

        val finalTitle = when {
            extractedTitle.isNotBlank() -> cleanTitle(extractedTitle)
            candidateTitles.isNotEmpty() -> cleanTitle(candidateTitles.first())
            extractedUrl != null -> extractedUrl!!
            else -> "Webpage Browsing"
        }

        return Pair(extractedUrl, finalTitle)
    }

    private fun extractMusicDetails(root: AccessibilityNodeInfo): Pair<String, String> {
        var track = ""
        var artist = ""
        var visitedCount = 0

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null || visitedCount >= 25) return
            visitedCount++

            val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT) ?: ""
            val text = node.text?.toString() ?: ""

            if (track.isBlank() && (viewId.contains("track") || viewId.contains("title") || viewId.contains("song"))) {
                if (text.isNotBlank()) track = text
            }
            if (artist.isBlank() && (viewId.contains("artist") || viewId.contains("subtitle") || viewId.contains("author"))) {
                if (text.isNotBlank()) artist = text
            }

            for (i in 0 until node.childCount) {
                if (visitedCount >= 25) break
                val child = node.getChild(i) ?: continue
                traverse(child)
                try { child.recycle() } catch (e: Exception) {}
            }
        }

        traverse(root)

        if (track.isBlank()) track = "Music Listening"
        return Pair(track, artist)
    }

    private fun extractMessagingDetails(root: AccessibilityNodeInfo): Pair<String, String> {
        var chatName = ""
        var isCall = false
        var visitedCount = 0

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null || visitedCount >= 25) return
            visitedCount++

            val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT) ?: ""
            val text = node.text?.toString() ?: ""

            if (viewId.contains("call") || text.contains("calling", ignoreCase = true)) {
                isCall = true
            }
            if (chatName.isBlank() && (viewId.contains("conversation_contact_name") || viewId.contains("chat_name") || viewId.contains("action_bar_title"))) {
                if (text.isNotBlank()) chatName = text
            }

            for (i in 0 until node.childCount) {
                if (visitedCount >= 25) break
                val child = node.getChild(i) ?: continue
                traverse(child)
                try { child.recycle() } catch (e: Exception) {}
            }
        }

        traverse(root)

        val type = if (isCall) "Voice / Video Call" else "Messaging"
        val title = if (chatName.isNotBlank()) "Chat: $chatName" else "Conversations"
        return Pair(type, title)
    }

    private fun extractGenericDetails(root: AccessibilityNodeInfo, appName: String): Pair<String, String> {
        var title = ""
        var subType = "Screen View"
        val candidates = mutableListOf<String>()
        var visitedCount = 0

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null || visitedCount >= 25) return
            visitedCount++

            val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT) ?: ""
            val text = node.text?.toString()?.trim() ?: ""
            val desc = node.contentDescription?.toString()?.trim() ?: ""

            val isHeaderId = viewId.contains("title") || viewId.contains("action_bar") ||
                    viewId.contains("toolbar") || viewId.contains("header") ||
                    viewId.contains("heading") || viewId.contains("doc_name") ||
                    viewId.contains("chapter") || viewId.contains("lesson") ||
                    viewId.contains("problem") || viewId.contains("question")

            if (isHeaderId && title.isBlank()) {
                if (text.isNotBlank() && text.length > 2 && !text.equals(appName, ignoreCase = true)) {
                    title = text
                    subType = "Section / Topic"
                } else if (desc.isNotBlank() && desc.length > 2 && !desc.equals(appName, ignoreCase = true)) {
                    title = desc
                    subType = "Section / Topic"
                }
            }

            if (text.isNotBlank() && text.length in 4..100 && !text.equals(appName, ignoreCase = true) &&
                !text.contains("AM") && !text.contains("PM") && !text.contains(":") &&
                !text.contains("%") && !text.contains("Battery") && !text.contains("Wi-Fi")) {
                candidates.add(text)
            }

            for (i in 0 until node.childCount) {
                if (visitedCount >= 25) break
                val child = node.getChild(i) ?: continue
                traverse(child)
                try { child.recycle() } catch (e: Exception) {}
            }
        }

        traverse(root)

        if (title.isBlank() && candidates.isNotEmpty()) {
            title = candidates.first()
        }
        if (title.isBlank()) {
            title = "$appName Main"
        }

        return Pair(subType, cleanTitle(title))
    }

    // -------------------------------------------------------------
    // Helper Utilities
    // -------------------------------------------------------------

    private fun isBrowserPackage(pkg: String): Boolean {
        return pkg.contains("chrome") || pkg.contains("firefox") || pkg.contains("brave") ||
                pkg.contains("opera") || pkg.contains("edge") || pkg.contains("browser") ||
                pkg.contains("sbrowser")
    }

    private fun extractDomainFromUrl(rawUrl: String): String {
        return try {
            val url = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                "https://$rawUrl"
            } else rawUrl
            val uri = URI(url)
            val host = uri.host ?: rawUrl
            host.removePrefix("www.")
        } catch (e: Exception) {
            rawUrl.split("/").firstOrNull()?.removePrefix("www.") ?: rawUrl
        }
    }

    private fun hasPasswordFields(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isPassword) return true
        val text = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""
        if (text.contains("enter pin") || text.contains("enter password") || text.contains("cvv") || text.contains("otp")) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val hasPwd = hasPasswordFields(child)
            try { child.recycle() } catch (e: Exception) {}
            if (hasPwd) return true
        }
        return false
    }

    private val appLabelCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun getApplicationLabel(packageName: String): String {
        return appLabelCache.getOrPut(packageName) {
            try {
                val pm = applicationContext.packageManager
                val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
            }
        }
    }

    private fun cleanTitle(title: String): String {
        return title.replace("\n", " ").trim().take(120)
    }

    override fun onInterrupt() {
        isServiceRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(screenReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        serviceScope.cancel()
    }
}
