package com.ai_assistant.studentfocus

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ai_assistant.studentfocus.database.AppDatabase
import com.ai_assistant.studentfocus.database.TaskRepository
import com.ai_assistant.studentfocus.ui.DashboardUI
import com.ai_assistant.studentfocus.viewmodel.*
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var taskViewModel: TaskViewModel
    private var isUnlocked by mutableStateOf(false)

    // True while a BiometricPrompt dialog is currently on screen
    @Volatile private var promptActive = false

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { writeBackupToUri(it) } }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { readBackupFromUri(it) } }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore unlock state across configuration changes / screen rotation
        if (savedInstanceState != null) {
            isUnlocked = savedInstanceState.getBoolean("is_unlocked", false)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val database   = AppDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())

        taskViewModel = ViewModelProvider(
            this, TaskViewModelFactory(repository)
        )[TaskViewModel::class.java]

        val timerViewModel = ViewModelProvider(this)[TimerViewModel::class.java]
        val gemmaViewModel = ViewModelProvider(
            this, GemmaViewModelFactory(application, repository)
        )[GemmaViewModel::class.java]

        com.ai_assistant.studentfocus.timer.FocusSessionManager.init(this)
        com.ai_assistant.studentfocus.timer.GeneralTimerManager.init(this)
        com.ai_assistant.studentfocus.timer.TimeWasteManager.init(this)
        com.ai_assistant.studentfocus.timer.DailyReminderScheduler.scheduleDailyReminders(this)
        com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.scheduleAllTimeTableReminders(this)

        lifecycleScope.launch {
            taskViewModel.timeTableSlots.collect { slots ->
                com.ai_assistant.studentfocus.timer.TimeWasteManager.updateTimeTableSlots(slots)
            }
        }

        // Resolve lock state from DB on first load, then show content
        lifecycleScope.launch {
            if (isUnlocked) return@launch // Already unlocked

            // Wait for the FIRST real emission from settings (skip initial null)
            val settingsList = taskViewModel.settings.first { it != null } ?: emptyList()
            val isBiometricEnabled = settingsList.any { it.key == "biometric_enabled" && it.value == "true" }

            if (!isBiometricEnabled) {
                // No lock — go straight in
                isUnlocked = true
            } else {
                delay(300)
                triggerBiometricUnlock()
            }
        }

        setContent {
            DashboardUI(
                taskViewModel        = taskViewModel,
                timerViewModel       = timerViewModel,
                gemmaViewModel       = gemmaViewModel,
                onExportBackup       = { exportLauncher.launch("StudentOS_Backup.json") },
                onRestoreBackup      = { 
                    try {
                        importLauncher.launch("*/*")
                    } catch (e: Exception) {
                        Toast.makeText(this, "Could not launch file picker: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                isUnlockedOverride   = isUnlocked,
                onUnlocked           = { isUnlocked = true },
                onTriggerBiometric   = { triggerBiometricUnlock() }
            )
        }
    }

    /**
     * Shows the system biometric/credential prompt.
     * Guard: if a prompt is already visible, silently returns to avoid double-trigger.
     */
    fun triggerBiometricUnlock() {
        if (promptActive) return   // ← single guard, prevents all double-triggers

        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        when (BiometricManager.from(this).canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> { /* proceed */ }
            else -> {
                // Device has no biometrics enrolled → unlock directly
                isUnlocked = true
                return
            }
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    promptActive = false
                    isUnlocked   = true
                }
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    promptActive = false
                    // Lock screen stays visible; user can tap "Try again"
                }
                override fun onAuthenticationFailed() {
                    // Fingerprint not recognised — prompt stays open, don't clear flag
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock StudentOS")
            .setSubtitle("Use fingerprint, face, device PIN or pattern")
            .setAllowedAuthenticators(authenticators)
            .build()

        promptActive = true
        prompt.authenticate(promptInfo)
    }

    // ---------------------------------------------------------------
    // Backup helpers
    // ---------------------------------------------------------------
    private fun writeBackupToUri(uri: Uri) {
        taskViewModel.exportBackup { jsonString ->
            if (jsonString != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use { it.write(jsonString.toByteArray()) }
                    Toast.makeText(this, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to write backup: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Backup data was empty!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readBackupFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    val sb = StringBuilder()
                    var line: String? = reader.readLine()
                    while (line != null) { sb.append(line); line = reader.readLine() }
                    taskViewModel.restoreBackup(sb.toString()) { success ->
                        val msg = if (success) "Backup restored successfully!" else "Failed to parse backup!"
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to read backup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        com.ai_assistant.studentfocus.timer.TimeWasteManager.onAppResumed()
    }

    override fun onPause() {
        super.onPause()
        com.ai_assistant.studentfocus.timer.TimeWasteManager.onAppPaused()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("is_unlocked", isUnlocked)
    }
}