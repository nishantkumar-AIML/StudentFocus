package com.ai_assistant.studentfocus.models

data class ActiveFocusSession(
    val taskId: String,
    val taskTitle: String,
    val targetDurationMinutes: Int, // e.g. 25 mins
    val startTimestamp: Long = System.currentTimeMillis(),
    val accumulatedSeconds: Int = 0,
    val lastResumeTimestamp: Long = System.currentTimeMillis(),
    val isRunning: Boolean = true,
    val isPausedByExpiry: Boolean = false,
    val currentTick: Long = System.currentTimeMillis() // Changes every second to guarantee StateFlow emission and UI recomposition
) {
    /**
     * Calculates the total elapsed seconds considering live wall-clock time
     */
    fun getLiveElapsedSeconds(now: Long = System.currentTimeMillis()): Int {
        val currentRunSeconds = if (isRunning) {
            maxOf(0, ((now - lastResumeTimestamp) / 1000).toInt())
        } else {
            0
        }
        val totalSecs = accumulatedSeconds + currentRunSeconds
        val targetSecs = targetDurationMinutes * 60
        return minOf(totalSecs, targetSecs)
    }

    fun getLiveRemainingSeconds(now: Long = System.currentTimeMillis()): Int {
        val targetSecs = targetDurationMinutes * 60
        val elapsed = getLiveElapsedSeconds(now)
        return maxOf(0, targetSecs - elapsed)
    }

    fun isTargetReached(now: Long = System.currentTimeMillis()): Boolean {
        return getLiveRemainingSeconds(now) <= 0
    }

    fun getActualStudiedMinutes(now: Long = System.currentTimeMillis()): Int {
        val totalSecs = getLiveElapsedSeconds(now)
        // Return actual elapsed minutes passed so far:
        // If at least 30 seconds have passed, round to nearest minute (minimum 1 min), otherwise 0
        return if (totalSecs >= 30) maxOf(1, (totalSecs + 30) / 60) else 0
    }
}
