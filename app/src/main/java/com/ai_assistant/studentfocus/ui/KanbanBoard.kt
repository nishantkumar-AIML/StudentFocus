package com.ai_assistant.studentfocus.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_assistant.studentfocus.R
import com.ai_assistant.studentfocus.models.TaskEntity
import com.ai_assistant.studentfocus.viewmodel.TaskViewModel
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun KanbanBoard(
    taskViewModel: TaskViewModel,
    tasks: List<TaskEntity>
) {
    var selectedColumn by remember { mutableStateOf("uncomplete") }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var completingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToStartFocus by remember { mutableStateOf<TaskEntity?>(null) }
    var sessionToExtend by remember { mutableStateOf<com.ai_assistant.studentfocus.models.ActiveFocusSession?>(null) }
    var expiredSession by remember { mutableStateOf<com.ai_assistant.studentfocus.models.ActiveFocusSession?>(null) }

    val history by taskViewModel.studyHistory.collectAsState(initial = emptyList())
    val routineActivities by taskViewModel.routineActivities.collectAsState(initial = emptyList())
    val routineBudgetMinutes = remember(routineActivities) {
        val routineMins = routineActivities.sumOf { (it.durationMinutes * it.classificationScore).toInt() }
        if (routineMins in 1..1440) routineMins else 1440
    }
    val taskSessions by com.ai_assistant.studentfocus.timer.FocusSessionManager.taskSessions.collectAsState()
    val activeSession by com.ai_assistant.studentfocus.timer.FocusSessionManager.activeSession.collectAsState()

    LaunchedEffect(Unit) {
        com.ai_assistant.studentfocus.timer.FocusSessionManager.onSessionExpired = { expired ->
            expiredSession = expired
        }
        val current = com.ai_assistant.studentfocus.timer.FocusSessionManager.activeSession.value
        if (current != null && current.isPausedByExpiry) {
            expiredSession = current
        }
    }

    val columns = listOf(
        "uncomplete" to "❌ UNCOMPLETE",
        "todo" to "📋 TO DO",
        "doing" to "⚡ IN PROGRESS",
        "done" to "✅ DONE"
    )

    val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    var selectedDate by remember { mutableStateOf(sdf.format(java.util.Date())) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val parsedDate = remember(selectedDate) {
        try {
            sdf.parse(selectedDate) ?: java.util.Date()
        } catch (e: Exception) {
            java.util.Date()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Study Board",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "📅 " + java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(parsedDate),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF818CF8),
                modifier = Modifier
                    .clickable {
                        val calendar = java.util.Calendar.getInstance().apply { time = parsedDate }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val selected = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.YEAR, year)
                                    set(java.util.Calendar.MONTH, month)
                                    set(java.util.Calendar.DAY_OF_MONTH, day)
                                }
                                selectedDate = sdf.format(selected.time)
                            },
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .background(Color(0x9A0F172A), RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Premium Pill Selector System (New Era Glassmorphism)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x9A0F172A), RoundedCornerShape(14.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.02f))),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            columns.forEach { (colName, label) ->
                val isSelected = selectedColumn == colName
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5)))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { selectedColumn = colName }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }

        val todayStr = remember { sdf.format(java.util.Date()) }
        val colTasks = remember(tasks, selectedDate, selectedColumn, todayStr) {
            when (selectedColumn) {
                "uncomplete" -> {
                    if (selectedDate == todayStr) {
                        tasks.filter { it.dueDate < todayStr && !it.isCompleted }
                    } else if (selectedDate < todayStr) {
                        tasks.filter { it.dueDate == selectedDate && !it.isCompleted }
                    } else {
                        emptyList()
                    }
                }
                "todo" -> {
                    if (selectedDate >= todayStr) {
                        tasks.filter { it.dueDate == selectedDate && it.status == "todo" && !it.isCompleted }
                    } else {
                        emptyList()
                    }
                }
                "doing" -> {
                    if (selectedDate >= todayStr) {
                        tasks.filter { it.dueDate == selectedDate && it.status == "doing" && !it.isCompleted }
                    } else {
                        emptyList()
                    }
                }
                "done" -> {
                    tasks.filter { it.dueDate == selectedDate && it.isCompleted }
                }
                else -> emptyList()
            }
        }

        AnimatedContent(
            targetState = colTasks,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center, initialScale = 0.95f)).togetherWith(
                    fadeOut(tween(180))
                )
            },
            modifier = Modifier.weight(1f),
            label = "board_content"
        ) { targetTasks ->
            if (targetTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No study targets here yet.",
                        color = Color(0xFF475569),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(targetTasks, key = { it.id }) { task ->
                        val currentCardColumn = if (selectedColumn == "uncomplete") "uncomplete" else task.status
                        val sessionForTask = taskSessions[task.id]
                        KanbanCard(
                            task = task,
                            activeSession = sessionForTask,
                            showMoveLeft = (selectedColumn != "uncomplete" && task.status != "todo"),
                            showMoveRight = (selectedColumn == "uncomplete" || task.status != "done"),
                            onMoveLeft = {
                                when (currentCardColumn) {
                                    "doing" -> taskViewModel.updateTaskStatus(task, "todo")
                                    "done" -> taskViewModel.updateTaskStatus(task, "doing")
                                }
                            },
                            onMoveRight = {
                                when (currentCardColumn) {
                                    "uncomplete" -> {
                                        taskViewModel.updateTask(
                                            id = task.id,
                                            title = task.title,
                                            description = task.description,
                                            category = task.category,
                                            subject = task.subject,
                                            dueDate = selectedDate,
                                            priority = task.priority,
                                            isPinned = task.isPinned,
                                            isRecurring = task.isRecurring,
                                            status = "todo",
                                            createdAt = task.createdAt
                                        )
                                    }
                                    "todo" -> taskViewModel.updateTaskStatus(task, "doing")
                                    "doing" -> {
                                        if (sessionForTask != null) {
                                            completingTask = task
                                        } else {
                                            taskToStartFocus = task
                                        }
                                    }
                                }
                            },
                            onStartFocus = {
                                if (sessionForTask != null) {
                                    com.ai_assistant.studentfocus.timer.FocusSessionManager.resumeSession(context, task.id)
                                } else {
                                    taskToStartFocus = task
                                }
                            },
                            onPauseFocus = { com.ai_assistant.studentfocus.timer.FocusSessionManager.pauseSession(context, task.id) },
                            onResumeFocus = { com.ai_assistant.studentfocus.timer.FocusSessionManager.resumeSession(context, task.id) },
                            onExtendFocus = { sessionToExtend = sessionForTask },
                            onCompleteFocus = { completingTask = task },
                            onEdit = { taskToEdit = task },
                            onDelete = { taskToDelete = task }
                        )
                    }
                }
            }
        }
    }

    taskToDelete?.let { task ->
        DeleteConfirmationDialog(
            taskTitle = task.title,
            onConfirm = {
                taskViewModel.deleteTask(task)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }

    taskToEdit?.let { task ->
        val existingCategories = tasks.mapNotNull { it.category }.distinct()
        TaskFormDialog(
            existingCategories = existingCategories,
            taskToEdit = task,
            onDismiss = { taskToEdit = null },
            onSave = { title, desc, cat, sub, date, prio, pinned, rec, linkUrl ->
                taskViewModel.updateTask(
                    id = task.id,
                    title = title,
                    description = desc,
                    category = cat,
                    subject = sub,
                    dueDate = date,
                    priority = prio,
                    isPinned = pinned,
                    isRecurring = rec,
                    status = task.status,
                    createdAt = task.createdAt,
                    linkUrl = linkUrl
                )
                taskToEdit = null
            }
        )
    }

    // Start Focus Dialog
    taskToStartFocus?.let { task ->
        val targetDate = task.dueDate.ifBlank { selectedDate }
        val existingRecord = history.find { it.date == targetDate }
        val currentDayStudyMinutes = existingRecord?.studyMinutes ?: 0

        StartFocusDialog(
            taskTitle = task.title,
            alreadyLoggedMinutes = currentDayStudyMinutes,
            routineBudgetMinutes = routineBudgetMinutes,
            onStart = { durationMinutes ->
                com.ai_assistant.studentfocus.timer.FocusSessionManager.startFocusSession(
                    context = context,
                    taskId = task.id,
                    taskTitle = task.title,
                    targetDurationMinutes = durationMinutes
                )
                taskToStartFocus = null
            },
            onDismiss = { taskToStartFocus = null }
        )
    }

    // Extend Focus Dialog
    sessionToExtend?.let { session ->
        val currentDayStudyMinutes = history.find { it.date == selectedDate }?.studyMinutes ?: 0

        ExtendFocusDialog(
            taskTitle = session.taskTitle,
            currentDurationMinutes = session.targetDurationMinutes,
            alreadyLoggedMinutes = currentDayStudyMinutes,
            routineBudgetMinutes = routineBudgetMinutes,
            onExtend = { extraMinutes ->
                com.ai_assistant.studentfocus.timer.FocusSessionManager.extendSession(context, extraMinutes)
                sessionToExtend = null
            },
            onDismiss = { sessionToExtend = null }
        )
    }

    // Session Expired Dialog
    expiredSession?.let { session ->
        SessionExpiredDialog(
            session = session,
            onComplete = {
                val targetTask = tasks.find { it.id == session.taskId } ?: TaskEntity(
                    id = session.taskId,
                    title = session.taskTitle,
                    description = null,
                    category = "Study",
                    subject = null,
                    dueDate = selectedDate,
                    priority = "medium",
                    status = "doing"
                )
                completingTask = targetTask
                expiredSession = null
            },
            onExtend = {
                sessionToExtend = session
                expiredSession = null
            },
            onDismiss = { expiredSession = null }
        )
    }

    completingTask?.let { task ->
        val targetDate = task.dueDate.ifBlank { selectedDate }
        val existingRecord = history.find { it.date == targetDate }
        val currentDayStudyMinutes = existingRecord?.studyMinutes ?: 0
        val sessionForTask = taskSessions[task.id]
        val actualElapsedMins = sessionForTask?.getActualStudiedMinutes()
        val targetFocusMins = sessionForTask?.targetDurationMinutes

        TaskCompletionDialog(
            taskTitle = task.title,
            alreadyLoggedMinutes = currentDayStudyMinutes,
            routineBudgetMinutes = routineBudgetMinutes,
            actualElapsedMinutes = actualElapsedMins,
            targetFocusMinutes = targetFocusMins,
            onConfirm = { minutes, notes ->
                taskViewModel.logTaskCompletionTimeAndNotes(task.id, minutes, notes)
                com.ai_assistant.studentfocus.timer.FocusSessionManager.stopSession(context, task.id)
                completingTask = null
            },
            onDismiss = {
                completingTask = null
            }
        )
    }
}

@Composable
fun KanbanCard(
    task: TaskEntity,
    activeSession: com.ai_assistant.studentfocus.models.ActiveFocusSession?,
    showMoveLeft: Boolean,
    showMoveRight: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onStartFocus: () -> Unit,
    onPauseFocus: () -> Unit,
    onResumeFocus: () -> Unit,
    onExtendFocus: () -> Unit,
    onCompleteFocus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isActive = activeSession?.taskId == task.id
    val priorityColor = when (task.priority.lowercase()) {
        "high" -> Color(0xFFEF4444)
        "medium" -> Color(0xFFFBBF24)
        else -> Color(0xFF06B6D4)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF1E1B4B).copy(alpha = 0.95f) else Color(0x9A0F172A)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                brush = if (isActive) {
                    Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF10B981)))
                } else {
                    Brush.linearGradient(
                        listOf(
                            priorityColor.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge
                if (!task.category.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF818CF8).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = task.category!!.uppercase(),
                            color = Color(0xFFC7D2FE),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("✏️", fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("❌", fontSize = 12.sp)
                    }
                }
            }

            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            val taskDateFormatted = remember(task.dueDate) { DateFormatterUtils.formatTaskDate(task.dueDate) }
            val taskDayStr = remember(task.dueDate) { DateFormatterUtils.formatTaskDay(task.dueDate) }
            val taskCreatedTime = remember(task.createdAt) { DateFormatterUtils.formatTime(task.createdAt) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(text = "📅 $taskDateFormatted", fontSize = 11.sp, color = Color(0xFF94A3B8))
                if (taskDayStr.isNotBlank()) {
                    Text(text = "•", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(text = "🗓️ $taskDayStr", fontSize = 11.sp, color = Color(0xFF818CF8))
                }
                Text(text = "•", fontSize = 11.sp, color = Color(0xFF64748B))
                Text(text = "⏰ $taskCreatedTime", fontSize = 11.sp, color = Color(0xFF38BDF8))
            }

            if (!task.description.isNullOrBlank()) {
                Text(
                    text = task.description,
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Clickable Resource / Meeting Link
            if (!task.linkUrl.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x336366F1),
                    border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.35f)),
                    modifier = Modifier.clickable {
                        try {
                            val url = if (!task.linkUrl.startsWith("http://") && !task.linkUrl.startsWith("https://")) {
                                "https://" + task.linkUrl
                            } else task.linkUrl
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(browserIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🔗", fontSize = 11.sp)
                        Text(
                            text = if (task.linkUrl.length > 28) task.linkUrl.take(25) + "..." else task.linkUrl,
                            color = Color(0xFFA5B4FC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("↗", color = Color(0xFF818CF8), fontSize = 10.sp)
                    }
                }
            }

            // ACTIVE FOCUS TIMER STRIP IN KANBAN CARD
            if (isActive && activeSession != null) {
                val remainingSecs = activeSession.getLiveRemainingSeconds()
                val mins = remainingSecs / 60
                val secs = remainingSecs % 60
                val formattedTimer = "%02d:%02d".format(mins, secs)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF312E81).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (activeSession.isRunning) "⏳" else "⏸️", fontSize = 14.sp)
                            Text(
                                text = "$formattedTimer left",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Target: ${activeSession.targetDurationMinutes}m",
                            color = Color(0xFFA5B4FC),
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (activeSession.isRunning) Color(0xFFF59E0B) else Color(0xFF6366F1), RoundedCornerShape(6.dp))
                                .clickable { if (activeSession.isRunning) onPauseFocus() else onResumeFocus() }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (activeSession.isRunning) "⏸️ Pause" else "▶️ Resume", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF3B82F6), RoundedCornerShape(6.dp))
                                .clickable { onExtendFocus() }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("➕ +Time", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF10B981), RoundedCornerShape(6.dp))
                                .clickable { onCompleteFocus() }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✅ Save", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (task.status != "done") {
                Button(
                    onClick = onStartFocus,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5).copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("▶️ Start Focus Study Session", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority pill
                Box(
                    modifier = Modifier
                        .background(priorityColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = task.priority.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showMoveLeft) {
                        IconButton(
                            onClick = onMoveLeft,
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Text("◀", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (showMoveRight) {
                        IconButton(
                            onClick = onMoveRight,
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF6366F1), CircleShape)
                        ) {
                            Text("▶", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
