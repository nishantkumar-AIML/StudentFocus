package com.ai_assistant.studentfocus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_assistant.studentfocus.R
import com.ai_assistant.studentfocus.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    taskViewModel: TaskViewModel,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    val settings by taskViewModel.settings.collectAsState()
    val exams by taskViewModel.exams.collectAsState()

    var examNameInput by remember { mutableStateOf("") }
    var examDateInput by remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        mutableStateOf(sdf.format(Date()))
    }
    var examToDelete by remember { mutableStateOf<com.ai_assistant.studentfocus.models.ExamEntity?>(null) }

    val isBiometricEnabled = settings?.find { it.key == "biometric_enabled" }?.value == "true"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712)),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Biometric Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x9A0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🖐️ Biometric Lock",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Protect StudentOS with fingerprint, face unlock, or device PIN/pattern.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isBiometricEnabled) "🔒 Lock Enabled" else "🔓 Lock Disabled",
                                color = if (isBiometricEnabled) Color(0xFF34D399) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isBiometricEnabled) "App will prompt on launch" else "Anyone can open the app",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    taskViewModel.saveSetting("biometric_enabled", "true")
                                } else {
                                    taskViewModel.saveSetting("biometric_enabled", "false")
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6366F1),
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }
            }
        }

        // Backup & Recovery Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x9A0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💾 Backup & Recovery",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onExportBackup,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Export Backup JSON", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onRestoreBackup,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Import Backup JSON", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Dashboard Customization Card
        item {
            var showCustomizerSheet by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x9A0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = "🎨 Dashboard Layout & UI",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reorder cards, toggle visible sections & choose layout presets",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = { showCustomizerSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Customize Dashboard Sections", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showCustomizerSheet) {
                DashboardCustomizerSheet(
                    taskViewModel = taskViewModel,
                    onDismiss = { showCustomizerSheet = false }
                )
            }
        }

        // Notification Settings Card (Comprehensive Granular Control Center)
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            val isNotificationEnabled = settings?.find { it.key == com.ai_assistant.studentfocus.timer.FocusNotificationHelper.KEY_NOTIF_ENABLED }?.value?.let { it == "true" } 
                ?: com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areNotificationsEnabled(context)
            val isOngoingTimerEnabled = settings?.find { it.key == com.ai_assistant.studentfocus.timer.FocusNotificationHelper.KEY_NOTIF_ONGOING_TIMER }?.value?.let { it == "true" }
                ?: com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areOngoingTimerNotificationsEnabled(context)
            val isSessionCompleteEnabled = settings?.find { it.key == com.ai_assistant.studentfocus.timer.FocusNotificationHelper.KEY_NOTIF_SESSION_COMPLETE }?.value?.let { it == "true" }
                ?: com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areCompletionNotificationsEnabled(context)
            val isDailyReminderEnabled = settings?.find { it.key == com.ai_assistant.studentfocus.timer.FocusNotificationHelper.KEY_NOTIF_DAILY_REMINDER }?.value?.let { it == "true" }
                ?: com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areDailyRemindersEnabled(context)
            val isTimeTableReminderEnabled = settings?.find { it.key == com.ai_assistant.studentfocus.timer.FocusNotificationHelper.KEY_NOTIF_TIMETABLE_REMINDER }?.value?.let { it == "true" }
                ?: com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areTimeTableRemindersEnabled(context)
            val isSoundEnabled = settings?.find { it.key == com.ai_assistant.studentfocus.timer.FocusNotificationHelper.KEY_SOUND_ENABLED }?.value?.let { it == "true" }
                ?: com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areSoundAlertsEnabled(context)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x9A0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Text(
                            text = "🔔 Notification Control Center",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Control and customize every notification sent by StudentFocus.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }

                    // Master Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isNotificationEnabled) "🔔 All Notifications (Active)" else "🔕 All Notifications (Muted)",
                                color = if (isNotificationEnabled) Color(0xFF34D399) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isNotificationEnabled) "Master switch is ON" else "All notifications are currently turned OFF",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = { enabled ->
                                taskViewModel.saveSetting(com.ai_assistant.studentfocus.timer.FocusNotificationHelper.KEY_NOTIF_ENABLED, if (enabled) "true" else "false")
                                com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setNotificationsEnabled(context, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6366F1),
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF0F172A)
                            )
                        )
                    }

                    if (isNotificationEnabled) {
                        Text(
                            text = "GRANULAR NOTIFICATION CONTROLS",
                            color = Color(0xFFA5B4FC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )

                        // 1. Live Ongoing Timer in Notification Tray
                        NotificationSettingToggleRow(
                            icon = "⏱️",
                            title = "Live Ongoing Timer Bar",
                            subtitle = "Real-time countdown with Pause/Resume in notification tray",
                            isChecked = isOngoingTimerEnabled,
                            onCheckedChange = { enabled ->
                                taskViewModel.saveSetting(com.ai_assistant.studentfocus.timer.FocusNotificationHelper.KEY_NOTIF_ONGOING_TIMER, if (enabled) "true" else "false")
                                com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setOngoingTimerNotificationsEnabled(context, enabled)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        // 2. Study Session & Pomodoro Completion Alerts
                        NotificationSettingToggleRow(
                            icon = "🎉",
                            title = "Session & Target Completion",
                            subtitle = "Popup alerts when focus target or pomodoro interval ends",
                            isChecked = isSessionCompleteEnabled,
                            onCheckedChange = { enabled ->
                                taskViewModel.saveSetting(com.ai_assistant.studentfocus.timer.FocusNotificationHelper.KEY_NOTIF_SESSION_COMPLETE, if (enabled) "true" else "false")
                                com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setCompletionNotificationsEnabled(context, enabled)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        // 3. Daily Morning & Evening Task Reminders
                        NotificationSettingToggleRow(
                            icon = "📅",
                            title = "Daily Task & Study Reminders",
                            subtitle = "Morning (9:00 AM) & Evening (7:00 PM) reminders for pending tasks",
                            isChecked = isDailyReminderEnabled,
                            onCheckedChange = { enabled ->
                                taskViewModel.saveSetting(com.ai_assistant.studentfocus.timer.FocusNotificationHelper.KEY_NOTIF_DAILY_REMINDER, if (enabled) "true" else "false")
                                com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setDailyRemindersEnabled(context, enabled)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        // 4. Academic Time Table Schedule Reminders
                        NotificationSettingToggleRow(
                            icon = "🗓️",
                            title = "Time Table & Routine Alarms",
                            subtitle = "Exact notifications at class or study slot start time",
                            isChecked = isTimeTableReminderEnabled,
                            onCheckedChange = { enabled ->
                                taskViewModel.saveSetting(com.ai_assistant.studentfocus.timer.FocusNotificationHelper.KEY_NOTIF_TIMETABLE_REMINDER, if (enabled) "true" else "false")
                                com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setTimeTableRemindersEnabled(context, enabled)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        // 5. Sound & Vibration Alerts
                        NotificationSettingToggleRow(
                            icon = "🔊",
                            title = "Sound & Vibration Alerts",
                            subtitle = if (isSoundEnabled) "Plays sound and vibrates on alerts" else "Silent notifications only",
                            isChecked = isSoundEnabled,
                            onCheckedChange = { enabled ->
                                taskViewModel.saveSetting(com.ai_assistant.studentfocus.timer.FocusNotificationHelper.KEY_SOUND_ENABLED, if (enabled) "true" else "false")
                                com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setSoundAlertsEnabled(context, enabled)
                            }
                        )
                    }
                }
            }
        }

        // AI Assistant Language Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x9A0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🤖 AI Assistant Language",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select the preferred language for Gemma AI Assistant responses.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val languages = listOf(
                        "auto" to "🌐 Auto-Detect",
                        "english" to "🇬🇧 English",
                        "hinglish" to "🇮🇳 Hinglish",
                        "tamil_english" to "🇮🇳 Tanglish (Tamil-English)",
                        "telugu_english" to "🇮🇳 Telugish (Telugu-English)"
                    )
                    
                    val currentLang = settings?.find { it.key == "ai_language" }?.value ?: "auto"

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        languages.forEach { (key, label) ->
                            val isSelected = currentLang == key
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) Color(0xFF6366F1).copy(alpha = 0.15f) else Color(0xFF020617),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF6366F1) else Color.White.copy(alpha = 0.04f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        taskViewModel.saveSetting("ai_language", key)
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (isSelected) {
                                    Text("✓", color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Exam Target Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x9A0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⏰ Exam Countdown Targets",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = examNameInput,
                            onValueChange = { examNameInput = it },
                            label = { Text("Exam Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val parsedDate = remember(examDateInput) {
                            try {
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(examDateInput) ?: Date()
                            } catch (e: Exception) {
                                Date()
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val calendar = Calendar.getInstance().apply { time = parsedDate }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val selected = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, day)
                                        }
                                        examDateInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selected.time)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.08f)))
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "📅 $examDateInput", color = Color.White, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (examNameInput.isNotBlank() && examDateInput.isNotBlank()) {
                                    taskViewModel.addExam(examNameInput, examDateInput)
                                    examNameInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            modifier = Modifier.align(Alignment.CenterVertically),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Add", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        exams.forEach { exam ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = exam.name, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(text = "Target Date: ${exam.date}", color = Color(0xFF64748B), fontSize = 12.sp)
                                }
                                IconButton(onClick = { examToDelete = exam }) {
                                    Text("❌", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    // Exam delete confirmation popup
    examToDelete?.let { exam ->
        DeleteConfirmationDialog(
            taskTitle = "${exam.name} (${exam.date})",
            onConfirm = {
                taskViewModel.deleteExam(exam)
                examToDelete = null
            },
            onDismiss = { examToDelete = null }
        )
    }
}

@Composable
private fun NotificationSettingToggleRow(
    icon: String,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF6366F1),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
