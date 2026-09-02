package com.ai_assistant.studentfocus.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ai_assistant.studentfocus.timer.GeneralTimerManager
import kotlinx.coroutines.flow.StateFlow

class TimerViewModel : ViewModel() {

    val elapsedSeconds: StateFlow<Int> = GeneralTimerManager.stopwatchElapsedSeconds
    val isRunning: StateFlow<Boolean> = GeneralTimerManager.isRunning
    val timerMode: StateFlow<Int> = GeneralTimerManager.timerMode
    val pomodoroTargetSeconds: StateFlow<Int> = GeneralTimerManager.pomodoroTargetSeconds
    val pomodoroRemainingSeconds: StateFlow<Int> = GeneralTimerManager.pomodoroRemainingSeconds

    var onPomodoroComplete: (() -> Unit)?
        get() = GeneralTimerManager.onPomodoroComplete
        set(value) {
            GeneralTimerManager.onPomodoroComplete = value
        }

    fun setTimerMode(mode: Int, context: Context? = null) {
        GeneralTimerManager.setTimerMode(mode, context)
    }

    fun setPomodoroTarget(minutes: Int, context: Context? = null) {
        GeneralTimerManager.setPomodoroTarget(minutes, context)
    }

    fun startTimer(context: Context) {
        GeneralTimerManager.startTimer(context)
    }

    fun pauseTimer(context: Context) {
        GeneralTimerManager.pauseTimer(context)
    }

    fun resetTimer(context: Context) {
        GeneralTimerManager.resetTimer(context)
    }

    fun getFormattedTime(): String {
        return if (timerMode.value == 0) {
            val totalSecs = elapsedSeconds.value
            val h = totalSecs / 3600
            val m = (totalSecs % 3600) / 60
            val s = totalSecs % 60
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            val totalSecs = pomodoroRemainingSeconds.value
            val m = totalSecs / 60
            val s = totalSecs % 60
            String.format("%02d:%02d", m, s)
        }
    }
}
