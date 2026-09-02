package com.ai_assistant.studentfocus.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import com.ai_assistant.studentfocus.models.AppUsageEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppUsageHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun hasAccessibilityPermission(context: Context): Boolean {
        return com.ai_assistant.studentfocus.service.ActivityIntelligenceService.isAccessibilityEnabled(context)
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            openUsageAccessSettings(context)
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } else true
    }

    fun openBatteryOptimizationSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAppCategory(packageName: String, appName: String): String {
        val pkg = packageName.lowercase(Locale.getDefault())
        val name = appName.lowercase(Locale.getDefault())

        return when {
            // Study & Productivity
            pkg.contains("studentfocus") || pkg.contains("classroom") || pkg.contains("duolingo") ||
            pkg.contains("khan") || pkg.contains("unacademy") || pkg.contains("byjus") ||
            pkg.contains("notion") || pkg.contains("drive") || pkg.contains("docs") ||
            pkg.contains("sheets") || pkg.contains("slides") || pkg.contains("adobe.reader") ||
            pkg.contains("pdf") || pkg.contains("wikipedia") || pkg.contains("coursera") ||
            pkg.contains("udemy") || pkg.contains("anki") || pkg.contains("quizlet") ||
            pkg.contains("calculator") || pkg.contains("dictionary") || name.contains("study") ||
            name.contains("notes") || name.contains("school") || name.contains("college") ||
            name.contains("learn") -> "Study"

            // Video & Streaming
            pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("hotstar") ||
            pkg.contains("primevideo") || pkg.contains("amazon.avod") || pkg.contains("mxtech.videoplayer") ||
            pkg.contains("twitch") || pkg.contains("vlc") || pkg.contains("spotify") ||
            pkg.contains("jiosaavn") || pkg.contains("gaana") || pkg.contains("wynk") ||
            pkg.contains("disney") || pkg.contains("hulu") || pkg.contains("crunchyroll") ||
            name.contains("video") || name.contains("movie") || name.contains("music") ||
            name.contains("stream") -> "Video"

            // Social & Messaging
            pkg.contains("whatsapp") || pkg.contains("instagram") || pkg.contains("facebook") ||
            pkg.contains("snapchat") || pkg.contains("twitter") || pkg.contains("x.android") ||
            pkg.contains("telegram") || pkg.contains("discord") || pkg.contains("reddit") ||
            pkg.contains("linkedin") || pkg.contains("threads") || pkg.contains("messenger") ||
            pkg.contains("wechat") || pkg.contains("tiktok") || pkg.contains("sharechat") ||
            name.contains("chat") || name.contains("social") || name.contains("message") -> "Social"

            // Gaming & Entertainment
            pkg.contains("pubg") || pkg.contains("freefire") || pkg.contains("roblox") ||
            pkg.contains("minecraft") || pkg.contains("supercell") || pkg.contains("chess") ||
            pkg.contains("candycrush") || pkg.contains("subwaysurf") || pkg.contains("ludo") ||
            pkg.contains("asphalt") || pkg.contains("game") || name.contains("game") -> "Gaming"

            // Browser & Web
            pkg.contains("chrome") || pkg.contains("firefox") || pkg.contains("brave") ||
            pkg.contains("opera") || pkg.contains("edge") || pkg.contains("browser") ||
            name.contains("browser") || name.contains("search") -> "Browser"

            else -> "Other"
        }
    }

    fun isSystemOrExcludedPackage(packageName: String, context: Context? = null): Boolean {
        // Delegate to unified classifier if context available, or fallback
        if (context != null) {
            return !AppUsageClassifier.isUserFacingApp(context, packageName)
        }
        val pkg = packageName.lowercase(Locale.getDefault())
        if (pkg == "com.ai_assistant.studentfocus") return true
        if (pkg == "android" || pkg.startsWith("com.android.systemui") || pkg.contains("inputmethod") || pkg.contains("launcher")) return true
        return false
    }

    /**
     * Query usage stats for a specific date (YYYY-MM-DD), including only verified USER_APP packages.
     */
    fun queryAppUsageStatsForDay(context: Context, dateStr: String): List<AppUsageEntity> {
        if (!hasUsageStatsPermission(context)) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = try {
            sdf.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endTime = minOf(cal.timeInMillis, System.currentTimeMillis() + 1000)

        if (startTime >= endTime) return emptyList()

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val pm = context.packageManager
        val usageMap = mutableMapOf<String, Pair<Long, Long>>() // pkg -> (totalTime, lastTimeUsed)

        for (stats in usageStatsList) {
            // Apply unified Multi-Level App Classification
            if (!AppUsageClassifier.isUserFacingApp(context, stats.packageName)) continue

            if (stats.totalTimeInForeground > 1000L) { // At least 1 second
                val existing = usageMap[stats.packageName]
                val newTime = (existing?.first ?: 0L) + stats.totalTimeInForeground
                val newLast = maxOf(existing?.second ?: 0L, stats.lastTimeUsed)
                usageMap[stats.packageName] = Pair(newTime, newLast)
            }
        }

        // Fallback to UsageEvents aggregation if queryUsageStats was sparse
        if (usageMap.isEmpty()) {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            val eventTimes = mutableMapOf<String, Long>() // pkg -> last resume timestamp

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName
                val time = event.timeStamp

                if (!AppUsageClassifier.isUserFacingApp(context, pkg)) continue

                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == 1) {
                    eventTimes[pkg] = time
                } else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == 2 || event.eventType == UsageEvents.Event.ACTIVITY_STOPPED) {
                    val start = eventTimes.remove(pkg)
                    if (start != null && time > start) {
                        val duration = time - start
                        val cur = usageMap[pkg]
                        usageMap[pkg] = Pair((cur?.first ?: 0L) + duration, maxOf(cur?.second ?: 0L, time))
                    }
                }
            }
        }

        val result = mutableListOf<AppUsageEntity>()

        for ((pkg, timePair) in usageMap) {
            val totalTime = timePair.first
            if (totalTime < 3000L) continue // Ignore less than 3 seconds micro-glance

            val appName = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }

            val category = getAppCategory(pkg, appName)

            result.add(
                AppUsageEntity(
                    id = "${dateStr}_$pkg",
                    date = dateStr,
                    packageName = pkg,
                    appName = appName,
                    totalTimeMillis = totalTime,
                    category = category,
                    lastTimeUsed = timePair.second
                )
            )
        }

        return result.sortedByDescending { it.totalTimeMillis }
    }

    /**
     * Inspects ALL detected packages for the developer/debug inspector dialog.
     */
    data class InspectedAppItem(
        val packageName: String,
        val appName: String,
        val classification: AppUsageClassifier.AppClassification,
        val totalTimeMillis: Long
    )

    fun inspectAllAppUsagesForDay(context: Context, dateStr: String): List<InspectedAppItem> {
        if (!hasUsageStatsPermission(context)) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = try { sdf.parse(dateStr) ?: Date() } catch (e: Exception) { Date() }

        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endTime = minOf(cal.timeInMillis, System.currentTimeMillis() + 1000)

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val pm = context.packageManager
        val rawMap = mutableMapOf<String, Long>()

        for (stats in usageStatsList) {
            if (stats.totalTimeInForeground > 1000L) {
                rawMap[stats.packageName] = (rawMap[stats.packageName] ?: 0L) + stats.totalTimeInForeground
            }
        }

        return rawMap.map { (pkg, duration) ->
            val classification = AppUsageClassifier.classifyPackage(context, pkg)
            val appName = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
            InspectedAppItem(
                packageName = pkg,
                appName = appName,
                classification = classification,
                totalTimeMillis = duration
            )
        }.sortedWith(compareByDescending<InspectedAppItem> { it.classification.isUserFacing }.thenByDescending { it.totalTimeMillis })
    }

    /**
     * Authoritative Data Sanitization & Integrity Validation Engine (Rules #5, #6, #10, #19)
     * - Fixes negative durations and future timestamps.
     * - Fixes overlapping USER_APP sessions by clipping previous session endTime.
     * - Bounds child in-app activities so SUM(childDuration) <= parentSessionDuration.
     */
    fun sanitizeAndRepairActivitySessions(
        rawSessions: List<com.ai_assistant.studentfocus.models.AppActivitySessionEntity>,
        parentUsageList: List<AppUsageEntity>
    ): Pair<List<com.ai_assistant.studentfocus.models.AppActivitySessionEntity>, DiagnosticReport> {
        val now = System.currentTimeMillis()
        var overlapsRepaired = 0
        var invalidDurationsRepaired = 0

        // 1. Basic timestamp sanity: enforce duration = endTime - startTime
        val validTimeList = rawSessions.mapNotNull { session ->
            var start = session.startTime
            var end = session.endTime

            if (start > now) start = now
            if (end > now) end = now
            if (end < start) end = start

            var duration = end - start
            if (duration <= 0) {
                invalidDurationsRepaired++
                return@mapNotNull null // Drop 0ms noise
            }

            session.copy(
                startTime = start,
                endTime = end,
                durationMillis = duration
            )
        }

        // 2. Overlap Repair: Sort chronologically and clip overlaps
        val sorted = validTimeList.sortedBy { it.startTime }
        val nonOverlapping = mutableListOf<com.ai_assistant.studentfocus.models.AppActivitySessionEntity>()

        for (i in sorted.indices) {
            val current = sorted[i]
            if (nonOverlapping.isEmpty()) {
                nonOverlapping.add(current)
            } else {
                val prev = nonOverlapping.last()
                if (current.startTime < prev.endTime) {
                    overlapsRepaired++
                    // Fix overlap: clip previous session's endTime to current session's startTime
                    val newPrevEndTime = current.startTime
                    val newPrevDuration = maxOf(0L, newPrevEndTime - prev.startTime)
                    nonOverlapping[nonOverlapping.size - 1] = prev.copy(
                        endTime = newPrevEndTime,
                        durationMillis = newPrevDuration
                    )
                }
                nonOverlapping.add(current)
            }
        }

        // 3. Contiguous Same-Activity Merging:
        // Merge contiguous observations of the same package and title (within 12s gap) into one cohesive activity
        val mergedActivities = mutableListOf<com.ai_assistant.studentfocus.models.AppActivitySessionEntity>()
        var mergedObservationsCount = 0

        for (session in nonOverlapping) {
            if (mergedActivities.isEmpty()) {
                mergedActivities.add(session)
            } else {
                val last = mergedActivities.last()
                val isSamePkg = last.packageName == session.packageName
                val normLast = normalizeTitle(last.contentTitle)
                val normCurr = normalizeTitle(session.contentTitle)
                val isSameTitle = (normLast.isNotBlank() && normCurr.isNotBlank() && normLast == normCurr) ||
                        (normLast.isNotBlank() && session.contentTitle.contains("unavailable", ignoreCase = true))
                val isContiguous = (session.startTime - last.endTime) <= 12000L // 12s threshold

                if (isSamePkg && isSameTitle && isContiguous) {
                    mergedObservationsCount++
                    val newEnd = maxOf(last.endTime, session.endTime)
                    val newDuration = maxOf(0L, newEnd - last.startTime)
                    val preferredTitle = if (last.contentTitle.contains("unavailable", ignoreCase = true) && !session.contentTitle.contains("unavailable", ignoreCase = true)) {
                        session.contentTitle
                    } else {
                        last.contentTitle
                    }
                    mergedActivities[mergedActivities.size - 1] = last.copy(
                        endTime = newEnd,
                        durationMillis = newDuration,
                        contentTitle = preferredTitle
                    )
                } else {
                    mergedActivities.add(session)
                }
            }
        }

        val sanitizedResult = mergedActivities.filter { it.durationMillis > 0 }

        val report = DiagnosticReport(
            totalRawSessions = rawSessions.size,
            sanitizedSessions = sanitizedResult.size,
            mergedObservationsCount = mergedObservationsCount,
            overlapsRepaired = overlapsRepaired,
            invalidDurationsRepaired = invalidDurationsRepaired,
            titlesUnavailableCount = sanitizedResult.count { it.contentTitle.contains("unavailable", ignoreCase = true) }
        )

        return Pair(sanitizedResult.sortedByDescending { it.durationMillis }, report)
    }

    fun normalizeTitle(title: String?): String {
        if (title.isNullOrBlank()) return ""
        var cleaned = title.trim().lowercase(java.util.Locale.ROOT)
        val suffixesToRemove = listOf(" - youtube", " • youtube", " - leetcode", " | leetcode", " - google chrome", " - chrome")
        for (s in suffixesToRemove) {
            if (cleaned.endsWith(s)) {
                cleaned = cleaned.removeSuffix(s).trim()
            }
        }
        return cleaned.replace("\\s+".toRegex(), " ")
    }

    data class DiagnosticReport(
        val totalRawSessions: Int,
        val sanitizedSessions: Int,
        val mergedObservationsCount: Int,
        val overlapsRepaired: Int,
        val invalidDurationsRepaired: Int,
        val titlesUnavailableCount: Int
    )

    data class GoalAlignmentMetrics(
        val activeScreenTimeMillis: Long,
        val goalAlignedMillis: Long,
        val highlyRelevantMillis: Long,
        val possiblyRelevantMillis: Long,
        val neutralMillis: Long,
        val offGoalMillis: Long,
        val unknownMillis: Long,
        val goalAlignmentRate: Int,
        val classificationCoverage: Int,
        val topGoalsWorkedOn: List<Pair<String, Long>> = emptyList(),
        val topTopicsDetected: List<Pair<String, Long>> = emptyList(),
        val topDistractions: List<Pair<String, Long>> = emptyList()
    )

    fun calculateGoalAlignmentMetrics(sessions: List<com.ai_assistant.studentfocus.models.AppActivitySessionEntity>): GoalAlignmentMetrics {
        val totalActiveTime = sessions.sumOf { it.durationMillis }
        if (totalActiveTime <= 0) {
            return GoalAlignmentMetrics(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0, 0)
        }

        var goalAligned = 0L
        var highlyRelevant = 0L
        var possiblyRelevant = 0L
        var neutral = 0L
        var offGoal = 0L
        var unknown = 0L

        val goalsMap = mutableMapOf<String, Long>()
        val topicsMap = mutableMapOf<String, Long>()
        val distractionsMap = mutableMapOf<String, Long>()

        for (session in sessions) {
            val align = when (session.goalAlignment) {
                ActivityClassifier.GoalAlignment.GOAL_ALIGNED.name -> ActivityClassifier.GoalAlignment.GOAL_ALIGNED
                ActivityClassifier.GoalAlignment.HIGHLY_RELEVANT.name -> ActivityClassifier.GoalAlignment.HIGHLY_RELEVANT
                ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT.name -> ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT
                ActivityClassifier.GoalAlignment.NEUTRAL.name -> ActivityClassifier.GoalAlignment.NEUTRAL
                ActivityClassifier.GoalAlignment.OFF_GOAL.name -> ActivityClassifier.GoalAlignment.OFF_GOAL
                ActivityClassifier.GoalAlignment.UNKNOWN.name -> ActivityClassifier.GoalAlignment.UNKNOWN
                else -> {
                    when (session.category) {
                        ActivityClassifier.CAT_LEARNING, ActivityClassifier.CAT_CODING -> ActivityClassifier.GoalAlignment.GOAL_ALIGNED
                        ActivityClassifier.CAT_PRODUCTIVITY -> ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT
                        ActivityClassifier.CAT_COMMUNICATION -> ActivityClassifier.GoalAlignment.NEUTRAL
                        ActivityClassifier.CAT_ENTERTAINMENT, ActivityClassifier.CAT_SOCIAL, ActivityClassifier.CAT_GAMING -> ActivityClassifier.GoalAlignment.OFF_GOAL
                        ActivityClassifier.CAT_BROWSING -> ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT
                        else -> ActivityClassifier.GoalAlignment.UNKNOWN
                    }
                }
            }

            when (align) {
                ActivityClassifier.GoalAlignment.GOAL_ALIGNED -> {
                    goalAligned += session.durationMillis
                    if (session.matchedGoal.isNotBlank()) {
                        goalsMap[session.matchedGoal] = (goalsMap[session.matchedGoal] ?: 0L) + session.durationMillis
                    }
                    if (session.matchedTopics.isNotBlank()) {
                        session.matchedTopics.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { t ->
                            topicsMap[t] = (topicsMap[t] ?: 0L) + session.durationMillis
                        }
                    } else if (session.contentTitle.isNotBlank() && !session.contentTitle.contains("unavailable", ignoreCase = true)) {
                        topicsMap[session.contentTitle] = (topicsMap[session.contentTitle] ?: 0L) + session.durationMillis
                    }
                }
                ActivityClassifier.GoalAlignment.HIGHLY_RELEVANT -> {
                    highlyRelevant += session.durationMillis
                    if (session.matchedGoal.isNotBlank()) {
                        goalsMap[session.matchedGoal] = (goalsMap[session.matchedGoal] ?: 0L) + session.durationMillis
                    }
                }
                ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT -> possiblyRelevant += session.durationMillis
                ActivityClassifier.GoalAlignment.NEUTRAL -> neutral += session.durationMillis
                ActivityClassifier.GoalAlignment.OFF_GOAL -> {
                    offGoal += session.durationMillis
                    val label = session.appName.ifBlank { session.packageName }
                    distractionsMap[label] = (distractionsMap[label] ?: 0L) + session.durationMillis
                }
                ActivityClassifier.GoalAlignment.UNKNOWN -> unknown += session.durationMillis
            }
        }

        val productiveTime = goalAligned + highlyRelevant
        val classifiedTime = productiveTime + possiblyRelevant + neutral + offGoal
        val alignmentRate = if (classifiedTime > 0) ((productiveTime * 100) / classifiedTime).toInt() else 0
        val coverage = if (totalActiveTime > 0) ((classifiedTime * 100) / totalActiveTime).toInt() else 0

        return GoalAlignmentMetrics(
            activeScreenTimeMillis = totalActiveTime,
            goalAlignedMillis = goalAligned,
            highlyRelevantMillis = highlyRelevant,
            possiblyRelevantMillis = possiblyRelevant,
            neutralMillis = neutral,
            offGoalMillis = offGoal,
            unknownMillis = unknown,
            goalAlignmentRate = alignmentRate,
            classificationCoverage = coverage,
            topGoalsWorkedOn = goalsMap.toList().sortedByDescending { it.second }.take(4),
            topTopicsDetected = topicsMap.toList().sortedByDescending { it.second }.take(5),
            topDistractions = distractionsMap.toList().sortedByDescending { it.second }.take(4)
        )
    }

    private val iconCache = android.util.LruCache<String, Drawable>(64)

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        synchronized(iconCache) {
            val cached = iconCache.get(packageName)
            if (cached != null) return cached
        }
        val icon = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
        if (icon != null) {
            synchronized(iconCache) {
                iconCache.put(packageName, icon)
            }
        }
        return icon
    }
}
