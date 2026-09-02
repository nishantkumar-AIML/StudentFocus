package com.ai_assistant.studentfocus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.ai_assistant.studentfocus.models.TaskEntity
import com.ai_assistant.studentfocus.models.StudyHistoryEntity
import com.ai_assistant.studentfocus.models.ExamEntity
import com.ai_assistant.studentfocus.R
import com.ai_assistant.studentfocus.viewmodel.GemmaViewModel
import com.ai_assistant.studentfocus.viewmodel.TaskViewModel
import com.ai_assistant.studentfocus.viewmodel.TimerViewModel
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(
    taskViewModel: TaskViewModel,
    timerViewModel: TimerViewModel,
    gemmaViewModel: GemmaViewModel,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    isUnlockedOverride: Boolean,
    onUnlocked: () -> Unit,
    onTriggerBiometric: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tasks by taskViewModel.tasks.collectAsState()
    val settings by taskViewModel.settings.collectAsState()
    val history by taskViewModel.studyHistory.collectAsState()

    LaunchedEffect(settings) {
        settings?.let {
            taskViewModel.performDailyXpDecayAndSetup(tasks, history, it)
        }
    }

    if (settings == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF3B82F6))
        }
        return
    }

    val biometricEnabled = settings?.find { it.key == "biometric_enabled" }?.value == "true"
    val isAppLocked = biometricEnabled && !isUnlockedOverride

    if (isAppLocked) {
        PinLockScreen(
            correctPin = "",
            onUnlocked = onUnlocked,
            onTriggerBiometric = onTriggerBiometric
        )
        return
    }

    val tabs = listOf(
        "dashboard" to "🏠",
        "tasks" to "📋",
        "notebook" to "📝",
        "academic" to "🎓",
        "tutor" to "💬",
        "settings" to "⚙️"
    )
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 4.dp, top = 0.dp)
                    .background(Color(0xE60F172A), RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, (tab, icon) ->
                        val isSelected = pagerState.currentPage == index
                        val contentColor = if (isSelected) Color(0xFF818CF8) else Color(0xFF94A3B8)
                        val scale by animateFloatAsState(if (isSelected) 1.15f else 1.0f, label = "tab_scale")
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = icon,
                                fontSize = 20.sp,
                                modifier = Modifier.scale(scale)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = tab.replaceFirstChar { it.uppercase() },
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            val currentTab = tabs[pagerState.currentPage].first
            if (currentTab == "dashboard" || currentTab == "tasks") {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF3B82F6))
                            ),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable { showAddDialog = true }
                        .padding(1.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        },
        containerColor = Color(0xFF030712)
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            beyondViewportPageCount = 1
        ) { page ->
            when (tabs[page].first) {
                "dashboard" -> {
                    val exams by taskViewModel.exams.collectAsState()
                    DashboardTab(
                        taskViewModel = taskViewModel,
                        timerViewModel = timerViewModel,
                        tasks = tasks,
                        history = history,
                        exams = exams,
                        onAddTaskClick = { showAddDialog = true },
                        onEditTaskClick = { taskToEdit = it },
                        onDeleteTaskClick = { taskToDelete = it }
                    )
                }
                "tasks" -> KanbanBoard(taskViewModel, tasks)
                "notebook" -> NotesTab(taskViewModel)
                "academic" -> AcademicHubScreen(taskViewModel)
                "tutor" -> AiAssistantTab(gemmaViewModel)
                "settings" -> SettingsScreen(taskViewModel, onExportBackup, onRestoreBackup)
            }
        }
    }

    if (showAddDialog) {
        val existingCategories = tasks.mapNotNull { it.category }.distinct()
        TaskFormDialog(
            existingCategories = existingCategories,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, cat, sub, date, prio, pinned, rec, linkUrl ->
                taskViewModel.addTask(
                    title = title,
                    description = desc,
                    category = cat,
                    subject = sub,
                    dueDate = date,
                    priority = prio,
                    isPinned = pinned,
                    isRecurring = rec,
                    linkUrl = linkUrl
                )
                showAddDialog = false
            }
        )
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
}

// -------------------------------------------------------------
// TAB 1: MAIN DASHBOARD
// -------------------------------------------------------------
@Composable
fun DashboardTab(
    taskViewModel: TaskViewModel,
    timerViewModel: TimerViewModel,
    tasks: List<TaskEntity>,
    history: List<StudyHistoryEntity>,
    exams: List<ExamEntity>,
    onAddTaskClick: () -> Unit,
    onEditTaskClick: (TaskEntity) -> Unit,
    onDeleteTaskClick: (TaskEntity) -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var currentDateStr by remember { mutableStateOf(sdf.format(Date())) }
    var showNameEditDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }
    var examToDelete by remember { mutableStateOf<com.ai_assistant.studentfocus.models.ExamEntity?>(null) }

    val settings by taskViewModel.settings.collectAsState()
    val routineActivities by taskViewModel.routineActivities.collectAsState()
    val routineBudgetMinutes = remember(routineActivities) {
        val routineMins = routineActivities.sumOf { (it.durationMinutes * it.classificationScore).toInt() }
        if (routineMins in 1..1440) routineMins else 1440
    }
    val totalXp = remember(settings, tasks) {
        settings?.find { it.key == "user_xp" }?.value?.toIntOrNull()
            ?: (tasks.count { it.isCompleted } * 20)
    }
    val level = remember(totalXp) { (totalXp / 100) + 1 }
    val currentXp = remember(totalXp) { totalXp % 100 }
    val threshold = 100
    val totalDone = remember(tasks) { tasks.count { it.isCompleted } }

    val pet = remember(level) {
        when {
            level >= 20 -> "🦉"
            level >= 10 -> "🦅"
            level >= 5  -> "🐥"
            level >= 2  -> "🐣"
            else        -> "🥚"
        }
    }

    val title = remember(level) {
        when {
            level >= 20 -> "Wise Owl"
            level >= 10 -> "Soaring Eagle"
            level >= 5  -> "Curious Chick"
            level >= 2  -> "Hatching Scholar"
            else        -> "Novice Learner"
        }
    }

    val todayTasks       = remember(tasks, currentDateStr) { tasks.filter { it.dueDate == currentDateStr } }
    val completedToday   = remember(todayTasks) { todayTasks.filter { it.isCompleted } }
    val progressPercent  = remember(completedToday, todayTasks) {
        if (todayTasks.isNotEmpty()) (completedToday.size * 100) / todayTasks.size else 0
    }

    val streak = remember(history) { calculateStreak(history) }

    val dashboardDayTasks = remember(tasks, currentDateStr) {
        tasks.filter { it.dueDate == currentDateStr }
    }
    var completingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToStartFocus by remember { mutableStateOf<TaskEntity?>(null) }
    var sessionToExtend by remember { mutableStateOf<com.ai_assistant.studentfocus.models.ActiveFocusSession?>(null) }
    var expiredSession by remember { mutableStateOf<com.ai_assistant.studentfocus.models.ActiveFocusSession?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val taskSessions by com.ai_assistant.studentfocus.timer.FocusSessionManager.taskSessions.collectAsState()
    val activeSession by com.ai_assistant.studentfocus.timer.FocusSessionManager.activeSession.collectAsState()

    var showCustomizerSheet by remember { mutableStateOf(false) }
    var widgetOrder by remember { mutableStateOf(com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.getWidgetOrder(context)) }
    var hiddenWidgets by remember { mutableStateOf(com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.getHiddenWidgets(context)) }

    LaunchedEffect(settings) {
        val dbOrder = settings?.find { it.key == com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.KEY_DASHBOARD_ORDER }?.value
        if (!dbOrder.isNullOrBlank()) {
            val list = dbOrder.split(",").filter { it.isNotBlank() }
            if (list.isNotEmpty()) widgetOrder = list
        }
        val dbHidden = settings?.find { it.key == com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.KEY_DASHBOARD_HIDDEN }?.value
        if (dbHidden != null) {
            hiddenWidgets = dbHidden.split(",").filter { it.isNotBlank() }.toSet()
        }
    }

    LaunchedEffect(Unit) {
        com.ai_assistant.studentfocus.timer.FocusSessionManager.onSessionExpired = { expired ->
            expiredSession = expired
        }
        val current = com.ai_assistant.studentfocus.timer.FocusSessionManager.activeSession.value
        if (current != null && current.isPausedByExpiry) {
            expiredSession = current
        }
    }

    val activeExams = remember(exams) { exams.sortedBy { it.date } }
    val dayTasks = dashboardDayTasks

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (activeSession != null) {
            item(key = "active_session_card_${activeSession?.taskId}") {
                val session = activeSession!!
                val remainingSecs = session.getLiveRemainingSeconds()
                val mins = remainingSecs / 60
                val secs = remainingSecs % 60
                val formattedTimer = "%02d:%02d".format(mins, secs)
                val coroutineScope = rememberCoroutineScope()
                val offsetX = remember(session.taskId) { Animatable(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    // Background reveal on swipe
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color(0x33EF4444), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("➡️", fontSize = 16.sp)
                            Text(
                                text = "Slide right to dismiss",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Active Focus Card with right-swipe gesture
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(offsetX.value.toInt(), 0) }
                            .alpha((1f - (offsetX.value / 400f)).coerceIn(0f, 1f))
                            .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF10B981))), RoundedCornerShape(16.dp))
                            .pointerInput(session.taskId) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (offsetX.value > 250f) {
                                            coroutineScope.launch {
                                                offsetX.animateTo(800f, tween(200))
                                                com.ai_assistant.studentfocus.timer.FocusSessionManager.stopSession(context)
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            offsetX.animateTo(0f, spring())
                                        }
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        coroutineScope.launch {
                                            val nextVal = (offsetX.value + dragAmount).coerceAtLeast(0f)
                                            offsetX.snapTo(nextVal)
                                        }
                                    }
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (session.isRunning) "⏳" else "⏸️", fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = session.taskTitle,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "⏱️ $formattedTimer remaining (${session.targetDurationMinutes}m target)",
                                        color = Color(0xFFA5B4FC),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (session.isRunning) {
                                            com.ai_assistant.studentfocus.timer.FocusSessionManager.pauseSession(context)
                                        } else {
                                            com.ai_assistant.studentfocus.timer.FocusSessionManager.resumeSession(context)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(if (session.isRunning) Color(0xFFF59E0B) else Color(0xFF6366F1), CircleShape)
                                ) {
                                    Text(if (session.isRunning) "⏸️" else "▶️", fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = { sessionToExtend = session },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF3B82F6), CircleShape)
                                ) {
                                    Text("➕", fontSize = 12.sp, color = Color.White)
                                }
                                IconButton(
                                    onClick = {
                                        completingTask = tasks.find { it.id == session.taskId } ?: TaskEntity(
                                            id = session.taskId,
                                            title = session.taskTitle,
                                            description = null,
                                            category = "Study",
                                            subject = null,
                                            dueDate = currentDateStr,
                                            priority = "medium"
                                        )
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                ) {
                                    Text("✅", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        for (widgetId in widgetOrder) {
            if (widgetId in hiddenWidgets) continue

            when (widgetId) {
                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_SCHOLAR -> {
                    item(key = "widget_scholar") {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x9A0F172A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.25f), Color(0xFF3B82F6).copy(alpha = 0.05f))),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            Brush.radialGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.25f), Color.Transparent)),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.scholar_banner),
                                        contentDescription = "Scholar Avatar",
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text(text = pet, fontSize = 20.sp)
                                    }
                                    Text(text = "Level $level Scholar", color = Color(0xFF818CF8), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(text = "XP: $currentXp / $threshold", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .background(Color(0xFF020617), RoundedCornerShape(3.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(currentXp.toFloat() / threshold.toFloat())
                                                .fillMaxHeight()
                                                .background(
                                                    Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))),
                                                    RoundedCornerShape(3.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_GREETING -> {
                    item(key = "widget_greeting") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
                                val greetingPrefix = remember(hour) {
                                    when {
                                        hour in 5..11 -> "Good Morning"
                                        hour in 12..16 -> "Good Afternoon"
                                        hour in 17..20 -> "Good Evening"
                                        else -> "Good Night"
                                    }
                                }
                                val userName = settings?.find { it.key == "user_name" }?.value ?: ""
                                val displayName = if (userName.isBlank()) "Scholar" else userName
                                Text(
                                    text = "$greetingPrefix, $displayName! ✏️",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.clickable { showNameEditDialog = true }
                                )

                                if (showNameEditDialog) {
                                    var tempName by remember { mutableStateOf(userName) }
                                    AlertDialog(
                                        onDismissRequest = { showNameEditDialog = false },
                                        title = { Text("What is your name?", color = Color.White, fontWeight = FontWeight.Bold) },
                                        text = {
                                            OutlinedTextField(
                                                value = tempName,
                                                onValueChange = { tempName = it },
                                                placeholder = { Text("Enter your name...", color = Color.Gray) },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = Color(0xFF6366F1),
                                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                                ),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    taskViewModel.saveSetting("user_name", tempName.trim())
                                                    showNameEditDialog = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                            ) {
                                                Text("Save", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showNameEditDialog = false }) {
                                                Text("Cancel", color = Color.LightGray)
                                            }
                                        },
                                        containerColor = Color(0xFF1E293B)
                                    )
                                }
                                LiveDateTimeHeader(
                                    currentDateStr = currentDateStr,
                                    onDateClick = {
                                        val calendar = Calendar.getInstance().apply {
                                            try {
                                                time = sdf.parse(currentDateStr) ?: Date()
                                            } catch (e: Exception) {
                                                time = Date()
                                            }
                                        }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, year, month, day ->
                                                val selected = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, day)
                                                }
                                                currentDateStr = sdf.format(selected.time)
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showCustomizerSheet = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                ) {
                                    Text("🎨", fontSize = 16.sp)
                                }

                                Button(
                                    onClick = onAddTaskClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Add Task")
                                }
                            }
                        }
                    }
                }

                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_STATS -> {
                    item(key = "widget_stats") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DashboardStatWidget(
                                title = "Today Progress",
                                value = "$progressPercent%",
                                subtitle = "${completedToday.size}/${todayTasks.size} done",
                                modifier = Modifier.weight(1f)
                            )
                            DashboardStatWidget(
                                title = "Total Complete",
                                value = "$totalDone",
                                subtitle = "Study targets",
                                modifier = Modifier.weight(1f)
                            )
                            DashboardStatWidget(
                                title = "Fire Streak",
                                value = "$streak 🔥",
                                subtitle = "consecutive days",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_HEATMAP -> {
                    item(key = "widget_heatmap") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().glassmorphism()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "🔥 Continuous Study Heatmap", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                StudyHeatmap(history = history)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Less ", color = Color.Gray, fontSize = 10.sp)
                                    listOf(
                                        Color(0xFF0F172A),
                                        Color(0x4D8B5CF6),
                                        Color(0x998B5CF6),
                                        Color(0xFF8B5CF6),
                                        Color(0xFFEC4899),
                                        Color(0xFFF43F5E)
                                    ).forEach {
                                        Box(modifier = Modifier.size(8.dp).background(it, RoundedCornerShape(1.dp)))
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    Text(" More (Max 24h)", color = Color.Gray, fontSize = 10.sp)
                                }
                                StudyStatsPanel(
                                    history = history,
                                    tasks = tasks,
                                    selectedDate = currentDateStr,
                                    onDateSelected = { currentDateStr = it }
                                )
                            }
                        }
                    }
                }

                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_TIME_WASTE -> {
                    item(key = "widget_time_waste") {
                        TimeWasteTrackerCard()
                    }
                }

                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_WEEKLY_CHART -> {
                    item(key = "widget_weekly_chart") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().glassmorphism()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "📊 Weekly Study Minutes", color = Color.White, fontWeight = FontWeight.Bold)
                                WeeklyBarChart(history = history, selectedDate = currentDateStr)
                            }
                        }
                    }
                }

                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_TIMER -> {
                    item(key = "widget_focus_timer") {
                        FocusTimerCard(
                            timerViewModel = timerViewModel,
                            taskViewModel = taskViewModel,
                            currentDateStr = currentDateStr,
                            completedTodaySize = completedToday.size,
                            progressPercent = progressPercent
                        )
                    }
                }

                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_PACE -> {
                    item(key = "widget_study_pace") {
                        StudyGoalPaceCalculatorCard(
                            routineBudgetMinutes = routineBudgetMinutes,
                            taskViewModel = taskViewModel,
                            history = history,
                            currentDateStr = currentDateStr
                        )
                    }
                }

                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_EXAMS -> {
                    item(key = "widget_exams_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏰ Exam Countdown Targets",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { showAddExamDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("+ Add Exam", color = Color(0xFF818CF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (activeExams.isEmpty()) {
                        item(key = "widget_exams_empty") {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0x331E293B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showAddExamDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🎯", fontSize = 18.sp)
                                    Column {
                                        Text("No upcoming exams registered", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("Tap '+ Add Exam' to set an exam target & countdown", color = Color.Gray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        items(activeExams, key = { "widget_exam_${it.id}" }) { exam ->
                            val examDate = try { sdf.parse(exam.date) } catch (e: Exception) { null }
                            val daysLeft = if (examDate != null) {
                                val diff = examDate.time - System.currentTimeMillis()
                                Math.ceil(diff.toDouble() / (1000 * 60 * 60 * 24)).toInt()
                            } else {
                                -1
                            }

                            val countdownText = when {
                                daysLeft > 1 -> "$daysLeft days left"
                                daysLeft == 1 -> "Tomorrow! ⚡"
                                daysLeft == 0 -> "Today! 📝"
                                else -> "Completed"
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        daysLeft in 0..3 -> Color(0x33EF4444)
                                        daysLeft in 4..7 -> Color(0x33F59E0B)
                                        else -> Color(0xFF1E293B)
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        daysLeft in 0..3 -> Color(0xFFEF4444).copy(alpha = 0.4f)
                                        daysLeft in 4..7 -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                                        else -> Color.White.copy(alpha = 0.08f)
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = exam.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "📅 Target Date: ${exam.date}", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            color = when {
                                                daysLeft in 0..3 -> Color(0xFFEF4444)
                                                daysLeft in 4..7 -> Color(0xFFF59E0B)
                                                daysLeft > 7 -> Color(0xFF6366F1)
                                                else -> Color.Gray
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = countdownText,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { examToDelete = exam },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("🗑️", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_TASKS -> {
                    item(key = "widget_tasks_header") {
                        Text(
                            text = "📌 Priority Targets",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (dayTasks.isEmpty()) {
                        item(key = "widget_tasks_empty") {
                            Text(
                                text = "No study targets for this day.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(dayTasks, key = { "widget_task_${it.id}" }) { task ->
                            val sessionForTask = taskSessions[task.id]
                            TaskCard(
                                task = task,
                                activeSession = sessionForTask,
                                onToggle = { checked ->
                                    if (checked) {
                                        if (sessionForTask != null) {
                                            completingTask = task
                                        } else {
                                            taskToStartFocus = task
                                        }
                                    } else {
                                        taskViewModel.markTaskIncomplete(task)
                                    }
                                },
                                onStartFocus = {
                                    if (sessionForTask != null) {
                                        com.ai_assistant.studentfocus.timer.FocusSessionManager.resumeSession(context, task.id)
                                    } else {
                                        taskToStartFocus = task
                                    }
                                },
                                onPauseFocus = {
                                    com.ai_assistant.studentfocus.timer.FocusSessionManager.pauseSession(context, task.id)
                                },
                                onResumeFocus = {
                                    com.ai_assistant.studentfocus.timer.FocusSessionManager.resumeSession(context, task.id)
                                },
                                onExtendFocus = {
                                    sessionToExtend = sessionForTask
                                },
                                onCompleteFocus = {
                                    completingTask = task
                                },
                                onEdit = { onEditTaskClick(task) },
                                onDelete = { onDeleteTaskClick(task) }
                            )
                        }
                    }
                }

                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_DAILY_NOTES -> {
                    item(key = "widget_daily_notes") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val todayNotes by taskViewModel.getNotesForDate(currentDateStr).collectAsState(initial = emptyList())
                                var scratchpadNoteId by remember { mutableStateOf(UUID.randomUUID().toString()) }
                                var scratchpadText by remember { mutableStateOf("") }
                                var scratchpadTopic by remember { mutableStateOf("") }
                                var scratchpadCreatedAt by remember { mutableStateOf(System.currentTimeMillis()) }
                                var isScratchpadLoaded by remember { mutableStateOf(false) }

                                LaunchedEffect(currentDateStr) {
                                    isScratchpadLoaded = false
                                    val notes = taskViewModel.getNotesForDateDirect(currentDateStr)
                                    if (notes.isNotEmpty()) {
                                        val first = notes.first()
                                        scratchpadNoteId = first.id
                                        scratchpadText = first.content
                                        scratchpadTopic = first.topic
                                        scratchpadCreatedAt = first.createdAt
                                    } else {
                                        scratchpadNoteId = UUID.randomUUID().toString()
                                        scratchpadText = ""
                                        scratchpadTopic = ""
                                        scratchpadCreatedAt = System.currentTimeMillis()
                                    }
                                    isScratchpadLoaded = true
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "📝 Daily Notes",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    brush = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    scratchpadNoteId = UUID.randomUUID().toString()
                                                    scratchpadText = ""
                                                    scratchpadTopic = ""
                                                    scratchpadCreatedAt = System.currentTimeMillis()
                                                    isScratchpadLoaded = true
                                                    android.widget.Toast.makeText(context, "New note ready! ✍️", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    if (todayNotes.isNotEmpty()) {
                                        Text(
                                            text = "${todayNotes.size} note${if (todayNotes.size > 1) "s" else ""} today",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (todayNotes.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        item {
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (scratchpadNoteId !in todayNotes.map { it.id } && scratchpadText.isBlank()) Color(0xFF6366F1) else Color(0x336366F1),
                                                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                                                modifier = Modifier.clickable {
                                                    scratchpadNoteId = UUID.randomUUID().toString()
                                                    scratchpadText = ""
                                                    scratchpadTopic = ""
                                                    scratchpadCreatedAt = System.currentTimeMillis()
                                                    isScratchpadLoaded = true
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Text("+", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text("New", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                        items(todayNotes) { note ->
                                            val isSelected = note.id == scratchpadNoteId
                                            val titleDisplay = when {
                                                note.topic.isNotBlank() -> note.topic
                                                note.content.isNotBlank() -> note.content.take(12) + (if (note.content.length > 12) "..." else "")
                                                else -> "Note"
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isSelected) Color(0xFF3B82F6) else Color(0x331E293B),
                                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.1f)),
                                                modifier = Modifier.clickable {
                                                    scratchpadNoteId = note.id
                                                    scratchpadText = note.content
                                                    scratchpadTopic = note.topic
                                                    scratchpadCreatedAt = note.createdAt
                                                    isScratchpadLoaded = true
                                                }
                                            ) {
                                                Text(
                                                    text = "📝 $titleDisplay",
                                                    color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                LaunchedEffect(scratchpadText, scratchpadTopic, scratchpadNoteId) {
                                    if (isScratchpadLoaded && (scratchpadText.isNotEmpty() || scratchpadTopic.isNotEmpty())) {
                                        kotlinx.coroutines.delay(600)
                                        taskViewModel.saveNote(
                                            id = scratchpadNoteId,
                                            date = currentDateStr,
                                            content = scratchpadText.trim(),
                                            topic = scratchpadTopic.trim(),
                                            createdAt = scratchpadCreatedAt
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = scratchpadTopic,
                                    onValueChange = { scratchpadTopic = it },
                                    label = { Text("📌 Topic / Subject", color = Color(0xFFA5B4FC), fontSize = 11.sp) },
                                    placeholder = { Text("e.g. Chapter 4 - Thermodynamics", color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = scratchpadText,
                                    onValueChange = { scratchpadText = it },
                                    label = { Text("Study Notes", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                    placeholder = { Text("Quick auto-saving thoughts & key points...", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF3B82F6),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomizerSheet) {
        DashboardCustomizerSheet(
            taskViewModel = taskViewModel,
            onDismiss = {
                showCustomizerSheet = false
                widgetOrder = com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.getWidgetOrder(context)
                hiddenWidgets = com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.getHiddenWidgets(context)
            }
        )
    }

    // Start Focus Dialog
    taskToStartFocus?.let { task ->
        val targetDate = task.dueDate.ifBlank { currentDateStr }
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
        val currentDayStudyMinutes = history.find { it.date == currentDateStr }?.studyMinutes ?: 0

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
                    dueDate = currentDateStr,
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

    // Complete Task Dialog
    completingTask?.let { task ->
        val targetDate = task.dueDate.ifBlank { currentDateStr }
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

    // Add Exam Dialog
    if (showAddExamDialog) {
        var examName by remember { mutableStateOf("") }
        var examDate by remember { mutableStateOf(currentDateStr) }

        AlertDialog(
            onDismissRequest = { showAddExamDialog = false },
            title = { Text("🎯 Add Exam Target", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = examName,
                        onValueChange = { examName = it },
                        label = { Text("Exam / Test Name") },
                        placeholder = { Text("e.g. Physics Midterm, JEE Main...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance().apply {
                                try { time = sdf.parse(examDate) ?: Date() } catch (e: Exception) { time = Date() }
                            }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, day)
                                    }
                                    examDate = sdf.format(selected.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📅 Target Date: $examDate", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (examName.isNotBlank()) {
                            taskViewModel.addExam(examName.trim(), examDate)
                            showAddExamDialog = false
                            android.widget.Toast.makeText(context, "Exam added! 🎯", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add Exam", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExamDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Exam Confirmation Dialog
    examToDelete?.let { exam ->
        DeleteConfirmationDialog(
            taskTitle = "Exam: \"${exam.name}\"",
            onConfirm = {
                taskViewModel.deleteExam(exam)
                examToDelete = null
                android.widget.Toast.makeText(context, "Exam removed", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { examToDelete = null }
        )
    }
}

// -------------------------------------------------------------
// TAB 3: NOTE EDITOR
// -------------------------------------------------------------
@Composable
fun NotesTab(taskViewModel: TaskViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val todayDateStr = remember { sdf.format(Date()) }
    var selectedDate by remember { mutableStateOf(todayDateStr) }

    // Multi-note state for selected date
    val dayNotes by taskViewModel.getNotesForDate(selectedDate).collectAsState(initial = emptyList())
    var activeNoteId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var noteTopic by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var noteCreatedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var isLoaded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var lastSavedTime by remember { mutableStateOf<String?>(null) }

    // Load Notes when selectedDate changes
    LaunchedEffect(selectedDate) {
        isLoaded = false
        val notes = taskViewModel.getNotesForDateDirect(selectedDate)
        if (notes.isNotEmpty()) {
            val first = notes.first()
            activeNoteId = first.id
            noteTopic = first.topic
            noteContent = first.content
            noteCreatedAt = first.createdAt
        } else {
            activeNoteId = UUID.randomUUID().toString()
            noteTopic = ""
            noteContent = ""
            noteCreatedAt = System.currentTimeMillis()
        }
        lastSavedTime = null
        isLoaded = true
    }

    // Auto-save debounce — saves to Room DB 600ms after user finishes typing
    LaunchedEffect(noteTopic, noteContent, activeNoteId, selectedDate) {
        if (isLoaded && (noteTopic.isNotEmpty() || noteContent.isNotEmpty())) {
            isSaving = true
            kotlinx.coroutines.delay(600)
            taskViewModel.saveNote(
                id = activeNoteId,
                date = selectedDate,
                content = noteContent.trim(),
                topic = noteTopic.trim(),
                createdAt = noteCreatedAt
            )
            lastSavedTime = timeFmt.format(Date())
            isSaving = false
        }
    }

    val parsedDate = remember(selectedDate) {
        try {
            sdf.parse(selectedDate) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    val wordCount = remember(noteContent) {
        noteContent.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete this Note?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val displayName = if (noteTopic.isNotBlank()) "\"$noteTopic\"" else "this study note"
                Text(
                    text = "Are you sure you want to delete $displayName from $selectedDate? This cannot be undone.",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskViewModel.deleteNoteById(activeNoteId)
                        showDeleteConfirmDialog = false
                        val remaining = dayNotes.filter { it.id != activeNoteId }
                        if (remaining.isNotEmpty()) {
                            val nextNote = remaining.first()
                            activeNoteId = nextNote.id
                            noteTopic = nextNote.topic
                            noteContent = nextNote.content
                            noteCreatedAt = nextNote.createdAt
                        } else {
                            activeNoteId = UUID.randomUUID().toString()
                            noteTopic = ""
                            noteContent = ""
                            noteCreatedAt = System.currentTimeMillis()
                        }
                        lastSavedTime = null
                        android.widget.Toast.makeText(context, "Note deleted", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Bar with smaller title, + add symbol, Date & Save button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Title and Add (+) symbol in a Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📓 Study Notes",
                        fontSize = 15.sp, // Chhota title as requested
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    // Add (+) symbol right next to title
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(
                                brush = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
                                shape = CircleShape
                            )
                            .clickable {
                                activeNoteId = UUID.randomUUID().toString()
                                noteTopic = ""
                                noteContent = ""
                                noteCreatedAt = System.currentTimeMillis()
                                lastSavedTime = null
                                isLoaded = true
                                android.widget.Toast.makeText(context, "New note ready! ✍️", android.widget.Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                val notesCountLabel = if (dayNotes.isEmpty()) "0 notes" else "${dayNotes.size} note${if (dayNotes.size > 1) "s" else ""}"
                Text(
                    text = if (selectedDate == todayDateStr) "Today • $notesCountLabel" else "$selectedDate • $notesCountLabel",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selectedDate != todayDateStr) {
                    Button(
                        onClick = { selectedDate = todayDateStr },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                selectedDate = sdf.format(selected.time)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(text = "📅 $selectedDate", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Explicit Save Note Button
                Button(
                    onClick = {
                        if (noteTopic.isNotBlank() || noteContent.isNotBlank()) {
                            taskViewModel.saveNote(
                                id = activeNoteId,
                                date = selectedDate,
                                content = noteContent.trim(),
                                topic = noteTopic.trim(),
                                createdAt = noteCreatedAt
                            )
                            lastSavedTime = timeFmt.format(Date())
                            android.widget.Toast.makeText(context, "Note saved! ✅", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Please write something to save!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("💾 Save", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Horizontal Multi-Note Selector Chips for the selected day
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // [+ New Note] Action chip
            item {
                val isCreatingNew = activeNoteId !in dayNotes.map { it.id } && (noteTopic.isBlank() && noteContent.isBlank())
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isCreatingNew) Color(0xFF6366F1) else Color(0x336366F1),
                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.6f)),
                    modifier = Modifier.clickable {
                        activeNoteId = UUID.randomUUID().toString()
                        noteTopic = ""
                        noteContent = ""
                        noteCreatedAt = System.currentTimeMillis()
                        lastSavedTime = null
                        isLoaded = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("New Note", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Chips for existing notes on this day
            items(dayNotes) { note ->
                val isSelected = note.id == activeNoteId
                val noteTime = remember(note.createdAt) {
                    timeFmt.format(Date(note.createdAt))
                }
                val titleDisplay = when {
                    note.topic.isNotBlank() -> note.topic
                    note.content.isNotBlank() -> note.content.take(16) + (if (note.content.length > 16) "..." else "")
                    else -> "Untitled Note"
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFF3B82F6) else Color(0x331E293B),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.clickable {
                        activeNoteId = note.id
                        noteTopic = note.topic
                        noteContent = note.content
                        noteCreatedAt = note.createdAt
                        lastSavedTime = null
                        isLoaded = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📝 $titleDisplay",
                            color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = noteTime,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Title / Topic Input Field
        OutlinedTextField(
            value = noteTopic,
            onValueChange = { noteTopic = it },
            label = { Text("📌 Title / Topic", color = Color(0xFFA5B4FC), fontSize = 12.sp) },
            placeholder = { Text("Enter topic name (e.g. Thermodynamics, OS Scheduling...)", color = Color.DarkGray) },
            leadingIcon = { Text("🏷️", fontSize = 16.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedContainerColor = Color(0x661E293B),
                unfocusedContainerColor = Color(0x331E293B)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Notes Content Area - Spacious full height writing area
        OutlinedTextField(
            value = noteContent,
            onValueChange = { noteContent = it },
            label = { Text("📝 Notes Content", color = Color(0xFF94A3B8), fontSize = 12.sp) },
            placeholder = {
                Text(
                    "Start writing your study notes, formulas, summaries, and key concepts here...\n\n💡 You can write multiple notes per day and ask the AI Assistant directly about any saved notes!",
                    color = Color.DarkGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedContainerColor = Color(0x660F172A),
                unfocusedContainerColor = Color(0x330F172A)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Footer Status & Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        isSaving -> "⏳ Saving..."
                        lastSavedTime != null -> "✅ Saved ($lastSavedTime)"
                        noteContent.isNotBlank() || noteTopic.isNotBlank() -> "✅ Saved in DB"
                        else -> "📝 Ready"
                    },
                    color = if (isSaving) Color(0xFFF59E0B) else Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "•",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "$wordCount words",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (noteContent.isNotBlank() || noteTopic.isNotBlank()) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipText = if (noteTopic.isNotBlank()) "📌 $noteTopic\n\n$noteContent" else noteContent
                            val clip = android.content.ClipData.newPlainText("Study Note", clipText)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Note copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("📋 Copy", color = Color(0xFF818CF8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("🗑️ Delete", color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 5: ANALYTICS & CHARTS
// -------------------------------------------------------------
@Composable
fun AnalyticsTab(history: List<StudyHistoryEntity>, tasks: List<TaskEntity>) {
    val categoryCounts = remember(tasks) {
        tasks.filter { it.category != null }.groupBy { it.category!! }.mapValues { it.value.size }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Analytics Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🔥 Continuous Study Heatmap", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    StudyHeatmap(history = history)
                }
            }
        }

        // Time Waste & Inactivity Tracker (Above Weekly Study Minutes)
        item {
            TimeWasteTrackerCard()
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📊 Weekly Study minutes", color = Color.White, fontWeight = FontWeight.Bold)
                    WeeklyBarChart(history = history)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📁 Targets by Category", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    CategoryDistributionChart(categoryCounts = categoryCounts)
                }
            }
        }
    }
}

@Composable
fun DashboardStatWidget(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .glassmorphism()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        if (title == "Fire Streak") {
            val numberOnly = value.replace("🔥", "").trim()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = numberOnly,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "🔥",
                    fontSize = 22.sp
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF06B6D4), // Cyan neon subtitle
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TaskCard(
    task: TaskEntity,
    activeSession: com.ai_assistant.studentfocus.models.ActiveFocusSession? = null,
    onToggle: (Boolean) -> Unit,
    onStartFocus: () -> Unit = {},
    onPauseFocus: () -> Unit = {},
    onResumeFocus: () -> Unit = {},
    onExtendFocus: () -> Unit = {},
    onCompleteFocus: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isActive = activeSession?.taskId == task.id
    val priorityColor = when (task.priority.lowercase()) {
        "high" -> Color(0xFFF87171)
        "medium" -> Color(0xFFFACC15)
        else -> Color(0xFF38BDF8)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF1E1B4B).copy(alpha = 0.9f) else Color(0xBD0F172A)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                brush = if (isActive) {
                    Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF10B981)))
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent))
                },
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (isActive) onCompleteFocus() else onStartFocus()
                        } else {
                            onToggle(false)
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF10B981),
                        uncheckedColor = if (isActive) Color(0xFF818CF8) else Color.White.copy(alpha = 0.3f),
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (task.isCompleted) Color.Gray else Color.White
                    )

                    val taskDateFormatted = remember(task.dueDate) { DateFormatterUtils.formatTaskDate(task.dueDate) }
                    val taskDayStr = remember(task.dueDate) { DateFormatterUtils.formatTaskDay(task.dueDate) }
                    val taskCreatedTime = remember(task.createdAt) { DateFormatterUtils.formatTime(task.createdAt) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
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
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray.copy(alpha = 0.8f)
                        )
                    }

                    // Clickable Resource / Meeting Link
                    if (!task.linkUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
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
                                    text = if (task.linkUrl.length > 30) task.linkUrl.take(27) + "..." else task.linkUrl,
                                    color = Color(0xFFA5B4FC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("↗", color = Color(0xFF818CF8), fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Priority badge
                        Box(
                            modifier = Modifier
                                .background(priorityColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.priority.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Category badge
                        if (!task.category.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF818CF8).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = task.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFC7D2FE),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Status badge
                        val statusColor = when (task.status.lowercase()) {
                            "done" -> Color(0xFF10B981)
                            "doing" -> Color(0xFFFBBF24)
                            else -> Color(0xFF94A3B8)
                        }
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (task.status.lowercase()) {
                                    "done" -> "DONE"
                                    "doing" -> "IN PROGRESS"
                                    else -> "TO DO"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (task.timeSpentMinutes > 0) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "⏱️ ${task.timeSpentMinutes}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("✏️", fontSize = 13.sp)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("❌", fontSize = 13.sp)
                    }
                }
            }

            // ACTIVE FOCUS TIMER STRIP
            if (isActive && activeSession != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                val remainingSecs = activeSession.getLiveRemainingSeconds()
                val mins = remainingSecs / 60
                val secs = remainingSecs % 60
                val formattedTimer = "%02d:%02d".format(mins, secs)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF312E81).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (activeSession.isRunning) "⏳" else "⏸️",
                            fontSize = 14.sp
                        )
                        Column {
                            Text(
                                text = "$formattedTimer left",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Target: ${activeSession.targetDurationMinutes}m",
                                color = Color(0xFFA5B4FC),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause / Resume Button
                        Box(
                            modifier = Modifier
                                .background(if (activeSession.isRunning) Color(0xFFF59E0B) else Color(0xFF6366F1), RoundedCornerShape(8.dp))
                                .clickable {
                                    if (activeSession.isRunning) onPauseFocus() else onResumeFocus()
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (activeSession.isRunning) "⏸️ Pause" else "▶️ Resume",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Extend Button
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                                .clickable { onExtendFocus() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "➕ +Time",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Complete Button
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF10B981), RoundedCornerShape(8.dp))
                                .clickable { onCompleteFocus() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "✅ Save",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (!task.isCompleted) {
                // START FOCUS BUTTON
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onStartFocus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5).copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "▶️ Start Focus Study Session",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


fun Modifier.glassmorphism(): Modifier = this
    .background(
        color = Color(0xBD0F172A),
        shape = RoundedCornerShape(16.dp)
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.02f)
            )
        ),
        shape = RoundedCornerShape(16.dp)
    )

fun calculateStreak(history: List<StudyHistoryEntity>): Int {
    if (history.isEmpty()) return 0
    val studyDates = history.filter { it.studyMinutes > 0 }.map { it.date }.toSet()
    if (studyDates.isEmpty()) return 0

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    var streak = 0
    var checkDate = sdf.format(cal.time)

    if (!studyDates.contains(checkDate)) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
        checkDate = sdf.format(cal.time)
        if (!studyDates.contains(checkDate)) {
            return 0
        }
    }

    while (studyDates.contains(checkDate)) {
        streak++
        cal.add(Calendar.DAY_OF_YEAR, -1)
        checkDate = sdf.format(cal.time)
    }
    return streak
}

@Composable
fun StudyStatsPanel(
    history: List<StudyHistoryEntity>,
    tasks: List<TaskEntity>,
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val parsedDate = remember(selectedDate) {
        try {
            sdf.parse(selectedDate) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    val dayMinutes = remember(history, selectedDate) {
        history.find { it.date == selectedDate }?.studyMinutes ?: 0
    }

    val dayHours = dayMinutes / 60
    val dayMinsRem = dayMinutes % 60
    val dayStudyFormatted = if (dayHours > 0) "${dayHours}h ${dayMinsRem}m" else "$dayMinutes mins"

    val dayCompletedTasks = remember(tasks, selectedDate) {
        tasks.filter { it.dueDate == selectedDate && it.isCompleted }
    }

    val totalMinutes = remember(history) {
        history.sumOf { it.studyMinutes }
    }

    val weeklyMinutes = remember(history, selectedDate) {
        val cal = Calendar.getInstance().apply { time = parsedDate }
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val limit = sdf.format(cal.time)
        history.filter { it.date in limit..selectedDate }.sumOf { it.studyMinutes }
    }

    val monthlyMinutes = remember(history, selectedDate) {
        val cal = Calendar.getInstance().apply { time = parsedDate }
        cal.add(Calendar.DAY_OF_YEAR, -29)
        val limit = sdf.format(cal.time)
        history.filter { it.date in limit..selectedDate }.sumOf { it.studyMinutes }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 Study Analytics & Insights",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            
            // Date Selector Button
            Row(
                modifier = Modifier
                    .clickable {
                        val calendar = Calendar.getInstance().apply { time = parsedDate }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val selected = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, day)
                                }
                                onDateSelected(sdf.format(selected.time))
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "📅 " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsedDate),
                    color = Color(0xFF818CF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left block: Selected day minutes & notes
            Card(
                modifier = Modifier.weight(1.1f),
                colors = CardDefaults.cardColors(containerColor = Color(0x600F172A)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formattedDayLabel = SimpleDateFormat("MMM d", Locale.getDefault()).format(parsedDate)
                    Text("Selected Day ($formattedDayLabel) Study", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(dayStudyFormatted, color = Color(0xFFEC4899), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${dayCompletedTasks.size} tasks completed", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Completion Notes:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    
                    if (dayCompletedTasks.isEmpty()) {
                        Text("No tasks completed on this date.", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        dayCompletedTasks.forEach { t ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                Text(text = "✓ ${t.title}", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                if (t.timeSpentMinutes > 0) {
                                    val th = t.timeSpentMinutes / 60
                                    val tm = t.timeSpentMinutes % 60
                                    val tTime = if (th > 0) "${th}h ${tm}m" else "${tm}m"
                                    Text(text = "⏱ Spent: $tTime", color = Color.LightGray, fontSize = 10.sp)
                                }
                                if (!t.completionNotes.isNullOrBlank()) {
                                    Text(text = "📝 Notes: ${t.completionNotes}", color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Right block: Cumulative periods
            Column(
                modifier = Modifier.weight(0.9f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x600F172A)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Study Time", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        val totalH = totalMinutes / 60
                        val totalM = totalMinutes % 60
                        val formattedTotal = if (totalH > 0) "${totalH}h ${totalM}m" else "$totalMinutes mins"
                        Text(formattedTotal, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Past 7 Days
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x600F172A)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Past 7 Days", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        val wH = weeklyMinutes / 60
                        val wM = weeklyMinutes % 60
                        val formattedW = if (wH > 0) "${wH}h ${wM}m" else "$weeklyMinutes mins"
                        Text(formattedW, color = Color(0xFF6366F1), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Past 30 Days
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x600F172A)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Past 30 Days", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        val mH = monthlyMinutes / 60
                        val mM = monthlyMinutes % 60
                        val formattedM = if (mH > 0) "${mH}h ${mM}m" else "$monthlyMinutes mins"
                        Text(formattedM, color = Color(0xFF10B981), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FocusTimerCard(
    timerViewModel: TimerViewModel,
    taskViewModel: TaskViewModel,
    currentDateStr: String,
    completedTodaySize: Int,
    progressPercent: Int
) {
    val elapsedSeconds by timerViewModel.elapsedSeconds.collectAsState()
    val isTimerRunning by timerViewModel.isRunning.collectAsState()
    val timerMode by timerViewModel.timerMode.collectAsState()
    val pomodoroRemainingSeconds by timerViewModel.pomodoroRemainingSeconds.collectAsState()
    val pomodoroTargetSeconds by timerViewModel.pomodoroTargetSeconds.collectAsState()
    val isRainPlaying by taskViewModel.isRainPlaying.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    LaunchedEffect(pagerState.currentPage) {
        timerViewModel.setTimerMode(pagerState.currentPage, context)
    }

    val formattedStopwatchTime = remember(elapsedSeconds) {
        val h = elapsedSeconds / 3600
        val m = (elapsedSeconds % 3600) / 60
        val s = elapsedSeconds % 60
        String.format("%02d:%02d:%02d", h, m, s)
    }

    val formattedPomodoroTime = remember(pomodoroRemainingSeconds) {
        val m = pomodoroRemainingSeconds / 60
        val s = pomodoroRemainingSeconds % 60
        String.format("%02d:%02d", m, s)
    }

    // Write study history every minute (not every second)
    LaunchedEffect(elapsedSeconds) {
        if (elapsedSeconds > 0 && elapsedSeconds % 60 == 0) {
            taskViewModel.addStudyMinutes(
                date = currentDateStr,
                additionalMinutes = 1,
                completedTasks = completedTodaySize,
                progressPercentage = progressPercent
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x9A0F172A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(Color(0xFFEC4899).copy(alpha = 0.25f), Color(0xFF8B5CF6).copy(alpha = 0.05f))),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (page == 0) {
                        // STOPWATCH TIMER
                        Text(
                            text = "⏱️ Focus Study Stopwatch",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF472B6),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Counts up your active learning duration",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = formattedStopwatchTime,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        // POMODORO TIMER
                        Text(
                            text = "🍅 Pomodoro Focus Timer",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFEC4899),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set interval and count down to completion",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Presets Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(15, 25, 50, 60).forEach { mins ->
                                val isSelected = pomodoroTargetSeconds == mins * 60
                                Box(
                                    modifier = Modifier
                                        .clickable { timerViewModel.setPomodoroTarget(mins, context) }
                                        .background(
                                            color = if (isSelected) Color(0xFF6366F1) else Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = formattedPomodoroTime,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Dot indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            ) {
                repeat(2) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (isSelected) Color(0xFFF472B6) else Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            
            // Common Play / Reset Actions
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (isTimerRunning) timerViewModel.pauseTimer(context) else timerViewModel.startTimer(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isTimerRunning) "Pause" else "Start", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { timerViewModel.resetTimer(context) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f)))
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(
                    onClick = { taskViewModel.toggleRain(context) },
                    modifier = Modifier
                        .background(
                            color = if (isRainPlaying) Color(0xFF10B981) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isRainPlaying) Color(0xFF34D399) else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Text(text = "🧘", fontSize = 16.sp)
                }

                val rainVolume by taskViewModel.rainVolume.collectAsState()
                Text(text = "🔈", color = Color.Gray, fontSize = 12.sp)
                Slider(
                    value = rainVolume,
                    onValueChange = { taskViewModel.setRainVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF6366F1),
                        activeTrackColor = Color(0xFF6366F1),
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(text = "🔊", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StudyGoalPaceCalculatorCard(
    routineBudgetMinutes: Int,
    taskViewModel: TaskViewModel,
    history: List<StudyHistoryEntity>,
    currentDateStr: String,
    modifier: Modifier = Modifier
) {
    val settings by taskViewModel.settings.collectAsState()

    val isGoalActive = settings?.find { it.key == "goal_active" }?.value == "true"
    val savedDays = settings?.find { it.key == "goal_target_days" }?.value ?: "7"
    val savedHours = settings?.find { it.key == "goal_target_hours" }?.value ?: "20"
    val goalStartDate = settings?.find { it.key == "goal_start_date" }?.value ?: currentDateStr

    var daysText by rememberSaveable(savedDays) { mutableStateOf(savedDays) }
    var hoursText by rememberSaveable(savedHours) { mutableStateOf(savedHours) }
    var isEditing by rememberSaveable { mutableStateOf(false) }

    val daysCount = (daysText.toIntOrNull() ?: 1).coerceAtLeast(1)
    val targetHours = (hoursText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val totalTargetMinutes = (targetHours * 60).toInt()

    // Study minutes logged from goal start date onwards
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val studyMinutesSinceStart = remember(history, goalStartDate, isGoalActive) {
        if (isGoalActive) {
            history.filter { it.date >= goalStartDate }.sumOf { it.studyMinutes }
        } else {
            0
        }
    }

    val taskSessions by com.ai_assistant.studentfocus.timer.FocusSessionManager.taskSessions.collectAsState()
    val liveTaskMins = remember(taskSessions) {
        taskSessions.values.sumOf { it.getActualStudiedMinutes() }
    }
    val accumulatedStudiedMinutes = if (isGoalActive) studyMinutesSinceStart + liveTaskMins else 0

    val remainingMinutes = (totalTargetMinutes - accumulatedStudiedMinutes).coerceAtLeast(0)
    val completedHours = accumulatedStudiedMinutes / 60
    val completedMins = accumulatedStudiedMinutes % 60
    val remHours = remainingMinutes / 60
    val remMins = remainingMinutes % 60

    val progressPercent = if (totalTargetMinutes > 0) {
        ((accumulatedStudiedMinutes.toFloat() / totalTargetMinutes.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else {
        0
    }

    // Days elapsed and remaining
    val daysElapsed = remember(currentDateStr, goalStartDate, isGoalActive) {
        if (isGoalActive) {
            try {
                val start = sdf.parse(goalStartDate)?.time ?: System.currentTimeMillis()
                val current = sdf.parse(currentDateStr)?.time ?: System.currentTimeMillis()
                val diff = current - start
                (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            } catch (e: Exception) {
                0
            }
        } else {
            0
        }
    }
    val daysRemaining = (daysCount - daysElapsed).coerceAtLeast(0)

    val dailyRequiredMinutes = if (isGoalActive) {
        if (daysRemaining > 0) remainingMinutes / daysRemaining else remainingMinutes
    } else {
        if (daysCount > 0) totalTargetMinutes / daysCount else 0
    }

    val reqH = dailyRequiredMinutes / 60
    val reqM = dailyRequiredMinutes % 60
    val dailyReqFormatted = if (reqH > 0) "${reqH}h ${reqM}m" else "${reqM}m"

    val effectiveDailyLimit = routineBudgetMinutes
    val hasBudget = effectiveDailyLimit > 0
    val budgetH = effectiveDailyLimit / 60
    val budgetM = effectiveDailyLimit % 60
    val budgetFormatted = if (hasBudget) {
        if (budgetH > 0 && budgetM > 0) "${budgetH}h ${budgetM}m"
        else if (budgetH > 0) "${budgetH}h"
        else "${budgetM}m"
    } else {
        "0m"
    }

    val isWithinBudget = hasBudget && dailyRequiredMinutes <= effectiveDailyLimit
    val capacityPercent = if (hasBudget) {
        ((dailyRequiredMinutes.toFloat() / effectiveDailyLimit.toFloat()) * 100).toInt()
    } else {
        0
    }

    val minFeasibleDays = if (hasBudget && remainingMinutes > 0) {
        Math.ceil(remainingMinutes.toDouble() / effectiveDailyLimit).toInt().coerceAtLeast(1)
    } else {
        daysRemaining.coerceAtLeast(1)
    }

    val isMissionAccomplished = isGoalActive && remainingMinutes == 0 && totalTargetMinutes > 0

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x9A0F172A)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    if (isGoalActive) {
                        listOf(
                            Color(0xFF10B981).copy(alpha = 0.5f),
                            Color(0xFF6366F1).copy(alpha = 0.3f)
                        )
                    } else {
                        listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.35f),
                            Color(0xFF6366F1).copy(alpha = 0.15f)
                        )
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isGoalActive) "🎯 Active Study Mission" else "🎯 Study Pace & Goal Planner",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isGoalActive) Color(0xFF34D399) else Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isGoalActive) "Tracking dynamic study progress toward your target" else "Calculate daily study time needed to hit your target",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }

                if (isGoalActive) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🟢 ACTIVE MISSION",
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                if (!hasBudget) Color(0xFF38BDF8).copy(alpha = 0.15f)
                                else if (isWithinBudget) Color(0xFF10B981).copy(alpha = 0.15f)
                                else Color(0xFFEF4444).copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (!hasBudget) Color(0xFF38BDF8).copy(alpha = 0.4f)
                                else if (isWithinBudget) Color(0xFF10B981).copy(alpha = 0.4f)
                                else Color(0xFFEF4444).copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (!hasBudget) "ℹ️ No TimeTable Budget"
                                else if (isWithinBudget) "✅ Achievable ($capacityPercent%)"
                                else "⚠️ Over Capacity ($capacityPercent%)",
                            color = if (!hasBudget) Color(0xFF38BDF8)
                                else if (isWithinBudget) Color(0xFF34D399)
                                else Color(0xFFF87171),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // If goal is active & not editing, show Live Countdown and Progress Dashboard
            if (isGoalActive && !isEditing) {
                // Live Progress Big Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Studied So Far
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Completed", color = Color(0xFFA7F3D0), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "${completedHours}h ${completedMins}m", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$progressPercent% of ${targetHours.toInt()}h goal", color = Color(0xFF6EE7B7), fontSize = 10.sp)
                        }
                    }

                    // Remaining to Study
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Remaining Left", color = Color(0xFFA5B4FC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "${remHours}h ${remMins}m", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$daysRemaining days remaining", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }

                // Visual Dynamic Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Goal Progress (${accumulatedStudiedMinutes}m / ${totalTargetMinutes}m)",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$progressPercent%",
                            color = if (progressPercent >= 100) Color(0xFF34D399) else Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val progressFraction = if (totalTargetMinutes > 0) {
                        (accumulatedStudiedMinutes.toFloat() / totalTargetMinutes.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(Color(0xFF020617), RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF6366F1),
                                            Color(0xFF38BDF8),
                                            Color(0xFF10B981)
                                        )
                                    ),
                                    RoundedCornerShape(5.dp)
                                )
                        )
                    }
                }

                // Daily Pace Rate Status Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Daily Pace Needed
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Daily Pace Needed", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = dailyReqFormatted, color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "for next $daysRemaining days", color = Color.Gray, fontSize = 10.sp)
                        }
                    }

                    // Routine Study Budget
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Routine Budget", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = budgetFormatted, color = Color(0xFF818CF8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = if (hasBudget) "effective limit/day" else "no slots in timetable", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }

                // Celebratory or Feasibility Banner
                if (isMissionAccomplished) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF064E3B), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "🎉 MISSION ACCOMPLISHED! You have completely achieved your ${targetHours.toInt()}h study target! Outstanding discipline & dedication!",
                            color = Color(0xFFA7F3D0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (!hasBudget) Color(0xFF1E293B)
                                else if (isWithinBudget) Color(0xFF064E3B).copy(alpha = 0.4f)
                                else Color(0xFF7F1D1D).copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (!hasBudget) Color(0xFF64748B)
                                else if (isWithinBudget) Color(0xFF059669).copy(alpha = 0.3f)
                                else Color(0xFFDC2626).copy(alpha = 0.3f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        if (!hasBudget) {
                            Text(
                                text = "ℹ️ Pace Info: You need to study $dailyReqFormatted each day for the remaining $daysRemaining days to achieve your ${targetHours.toInt()}h goal.\n💡 Note: No Goal-tracked study slots are active in your TimeTable. Turn ON '🎯 Study Pace & Goal Planner' in TimeTable to schedule your daily hours.",
                                color = Color(0xFFBAE6FD),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        } else if (isWithinBudget) {
                            Text(
                                text = "✅ On Track! Studying $dailyReqFormatted each day for the remaining $daysRemaining days will complete your ${targetHours.toInt()}h goal comfortably within your $budgetFormatted daily routine budget.",
                                color = Color(0xFFA7F3D0),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        } else {
                            val excessMins = dailyRequiredMinutes - effectiveDailyLimit
                            val exH = excessMins / 60
                            val exM = excessMins % 60
                            val excessFormatted = if (exH > 0) "${exH}h ${exM}m" else "${exM}m"
                            Text(
                                text = "⚠️ Pace Alert: You need $dailyReqFormatted/day to finish on time, which is $excessFormatted/day over your scheduled TimeTable budget of $budgetFormatted.\n💡 Recommendation: Add more study slots in TimeTable or extend timeline by at least $minFeasibleDays days.",
                                color = Color(0xFFFECACA),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Goal Active Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                    ) {
                        Text(text = "✏️ Edit Target", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            taskViewModel.saveSetting("goal_active", "false")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                    ) {
                        Text(text = "⏹️ Reset / Finish", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // Planning / Editing Mode

                // Input 1: Target Days
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "📅 Target Timeline (Days)",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Quick Days Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1 to "1d", 2 to "2d", 3 to "3d", 7 to "7d (1w)", 14 to "14d (2w)", 30 to "30d (1m)").forEach { (d, label) ->
                            val isSelected = daysText == "$d"
                            Box(
                                modifier = Modifier
                                    .clickable { daysText = "$d" }
                                    .background(
                                        if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.06f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { input ->
                            daysText = input.filter { it.isDigit() }
                        },
                        label = { Text("Custom Days", color = Color.Gray, fontSize = 11.sp) },
                        placeholder = { Text("e.g. 7", color = Color.DarkGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Input 2: Total Target Hours
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "⏱️ Total Target Hours",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Quick Hours Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(5 to "5h", 10 to "10h", 20 to "20h", 40 to "40h", 70 to "70h", 100 to "100h").forEach { (h, label) ->
                            val isSelected = hoursText == "$h"
                            Box(
                                modifier = Modifier
                                    .clickable { hoursText = "$h" }
                                    .background(
                                        if (isSelected) Color(0xFF6366F1) else Color.White.copy(alpha = 0.06f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { input ->
                            hoursText = input.filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("Custom Target Hours", color = Color.Gray, fontSize = 11.sp) },
                        placeholder = { Text("e.g. 20", color = Color.DarkGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Calculated Output Preview Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Required Daily Study Time
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Daily Needed",
                                color = Color(0xFFA5B4FC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = dailyReqFormatted,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "for $daysCount days (${targetHours.toInt()}h total)",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Daily Routine Budget Limit
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Routine Budget",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = budgetFormatted,
                                color = Color(0xFF38BDF8),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (hasBudget) "effective study capacity/day" else "no timetable slots active",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Diagnostic Insights & Guidance Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (!hasBudget) Color(0xFF1E293B)
                            else if (isWithinBudget) Color(0xFF064E3B).copy(alpha = 0.4f)
                            else Color(0xFF7F1D1D).copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (!hasBudget) Color(0xFF64748B)
                            else if (isWithinBudget) Color(0xFF059669).copy(alpha = 0.3f)
                            else Color(0xFFDC2626).copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    if (!hasBudget) {
                        Text(
                            text = "ℹ️ Target Plan: Studying $dailyReqFormatted each day for $daysCount days will achieve your ${targetHours.toInt()}h goal.\n💡 Tip: To verify schedule feasibility, add your study subjects in TimeTable with '🎯 Study Pace & Goal Planner' enabled.",
                            color = Color(0xFFBAE6FD),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    } else if (isWithinBudget) {
                        val bufferMins = effectiveDailyLimit - dailyRequiredMinutes
                        val bufH = bufferMins / 60
                        val bufM = bufferMins % 60
                        val bufferFormatted = if (bufH > 0) "${bufH}h ${bufM}m" else "${bufM}m"
                        Text(
                            text = "✅ Target is achievable! Studying $dailyReqFormatted each day for $daysCount days will hit your ${targetHours.toInt()}h goal, leaving $bufferFormatted daily routine buffer in your scheduled $budgetFormatted TimeTable.",
                            color = Color(0xFFA7F3D0),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    } else {
                        val excessMins = dailyRequiredMinutes - effectiveDailyLimit
                        val exH = excessMins / 60
                        val exM = excessMins % 60
                        val excessFormatted = if (exH > 0) "${exH}h ${exM}m" else "${exM}m"
                        Text(
                            text = "⚠️ Target requires $dailyReqFormatted/day, which exceeds your $budgetFormatted scheduled TimeTable budget by $excessFormatted/day!\n💡 Recommendation: Increase timeline to at least $minFeasibleDays days, or allocate more study slots in TimeTable.",
                            color = Color(0xFFFECACA),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Beautiful Glowing CTA Button: Activate Study Goal
                Button(
                    onClick = {
                        taskViewModel.saveSetting("goal_active", "true")
                        taskViewModel.saveSetting("goal_target_days", "$daysCount")
                        taskViewModel.saveSetting("goal_target_hours", "$targetHours")
                        taskViewModel.saveSetting("goal_start_date", currentDateStr)
                        isEditing = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFF38BDF8),
                                        Color(0xFF10B981)
                                    )
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                ),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🚀", fontSize = 18.sp)
                            Text(
                                text = if (isEditing) "💾 Save & Resume Active Mission" else "🚀 Activate & Start Study Mission",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (isEditing) {
                    TextButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Editing", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

object DateFormatterUtils {
    private val dateInputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateDisplayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dayDisplayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    private val timeDisplayFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun formatTaskDate(dueDate: String): String {
        return try {
            val date = dateInputFormat.parse(dueDate)
            if (date != null) dateDisplayFormat.format(date) else dueDate
        } catch (e: Exception) {
            dueDate
        }
    }

    fun formatTaskDay(dueDate: String): String {
        return try {
            val date = dateInputFormat.parse(dueDate)
            if (date != null) dayDisplayFormat.format(date) else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun formatTime(timestamp: Long): String {
        return try {
            timeDisplayFormat.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }
}

@Composable
fun LiveDateTimeHeader(
    currentDateStr: String,
    onDateClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val parsedDate = remember(currentDateStr) {
        try {
            sdf.parse(currentDateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    var currentTimeStr by remember {
        mutableStateOf(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()))
    }

    // Updates only every 30 seconds, isolated to this small composable
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(30000L)
        }
    }

    val formattedDate = remember(parsedDate) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsedDate)
    }
    val formattedDay = remember(parsedDate) {
        SimpleDateFormat("EEEE", Locale.getDefault()).format(parsedDate)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(top = 4.dp)
            .clickable { onDateClick() }
    ) {
        Text(
            text = "📅 $formattedDate",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF60A5FA)
        )
        Text(text = "•", fontSize = 12.sp, color = Color(0xFF64748B))
        Text(
            text = "🗓️ $formattedDay",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF818CF8)
        )
        Text(text = "•", fontSize = 12.sp, color = Color(0xFF64748B))
        Text(
            text = "⏰ $currentTimeStr",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF34D399)
        )
    }
}

// -------------------------------------------------------------
// TIME WASTE & INACTIVITY TRACKER CARD
// -------------------------------------------------------------
@Composable
fun TimeWasteTrackerCard(
    modifier: Modifier = Modifier
) {
    val todayWasted by com.ai_assistant.studentfocus.timer.TimeWasteManager.todayWastedSeconds.collectAsState()
    val totalWasted by com.ai_assistant.studentfocus.timer.TimeWasteManager.totalWastedSeconds.collectAsState()
    val isTicking by com.ai_assistant.studentfocus.timer.TimeWasteManager.isWasteTicking.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xAA0F172A)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    if (isTicking) {
                        listOf(Color(0xFFEF4444).copy(alpha = 0.6f), Color(0xFFF59E0B).copy(alpha = 0.35f))
                    } else {
                        listOf(Color(0xFF10B981).copy(alpha = 0.6f), Color(0xFF3B82F6).copy(alpha = 0.35f))
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isTicking) "⏳" else "🛡️",
                        fontSize = 20.sp
                    )
                    Column {
                        Text(
                            text = "Time Waste Tracker",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isTicking) "No task running • Ticking +1s" else "Study task active • Paused",
                            color = if (isTicking) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Live Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isTicking) Color(0xFF7F1D1D).copy(alpha = 0.7f) else Color(0xFF064E3B).copy(alpha = 0.7f),
                    border = BorderStroke(
                        1.dp,
                        if (isTicking) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    if (isTicking) Color(0xFFEF4444) else Color(0xFF10B981),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (isTicking) "TICKING" else "PAUSED",
                            color = if (isTicking) Color(0xFFFCA5A5) else Color(0xFFA7F3D0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Metric Cards: 1 Day vs Total Time Wasted
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Today's Time Wasted (1 day)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0x661E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "📅 Today's Waste",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = com.ai_assistant.studentfocus.timer.TimeWasteManager.formatDuration(todayWasted),
                            color = if (isTicking) Color(0xFFF87171) else Color(0xFFFBBF24),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Resets at midnight",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                // Total Time Wasted (All-time)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0x661E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "⏳ Total Wasted",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = com.ai_assistant.studentfocus.timer.TimeWasteManager.formatDuration(totalWasted),
                            color = Color(0xFFF43F5E),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Cumulative inactivity",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Insight alert banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTicking) "⚠️ Start a study task to stop the waste counter!" else "🎉 Great discipline! Task focus is active, waste counter stopped.",
                    color = if (isTicking) Color(0xFFFDE68A) else Color(0xFFA7F3D0),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { showResetDialog = true },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Reset", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Time Waste Stats?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to reset today's and all-time wasted time to 0?", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        com.ai_assistant.studentfocus.timer.TimeWasteManager.resetStats()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

