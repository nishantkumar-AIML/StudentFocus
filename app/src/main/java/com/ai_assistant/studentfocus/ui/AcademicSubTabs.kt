package com.ai_assistant.studentfocus.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_assistant.studentfocus.R
import com.ai_assistant.studentfocus.models.*
import com.ai_assistant.studentfocus.viewmodel.TaskViewModel
import kotlinx.coroutines.isActive
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import android.content.Context
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.os.Build
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import com.ai_assistant.studentfocus.utils.AppUsageHelper
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val ACADEMIC_PREFS = "academic_hub_preferences"
private const val KEY_ACADEMIC_TAB_ORDER = "academic_subtab_order"

val DEFAULT_ACADEMIC_TABS = listOf(
    "timetable" to "📅 Time Table",
    "attendance" to "📋 Attendance",
    "cgpa" to "🎓 CGPA",
    "flashcards" to "🗂️ Cards",
    "habits" to "🗓️ Habits",
    "finance" to "₹ Ledger",
    "routine" to "⏱️ Routine",
    "appusage" to "⚡ Activity Intelligence"
)

fun getSavedAcademicTabOrder(context: Context): List<String> {
    val defaultKeys = DEFAULT_ACADEMIC_TABS.map { it.first }
    val prefs = context.getSharedPreferences(ACADEMIC_PREFS, Context.MODE_PRIVATE)
    val saved = prefs.getString(KEY_ACADEMIC_TAB_ORDER, null) ?: return defaultKeys
    val list = saved.split(",").filter { it.isNotBlank() && it in defaultKeys }
    val missing = defaultKeys.filterNot { it in list }
    return if (list.isNotEmpty()) list + missing else defaultKeys
}

fun saveAcademicTabOrder(context: Context, order: List<String>, taskViewModel: TaskViewModel? = null) {
    val prefs = context.getSharedPreferences(ACADEMIC_PREFS, Context.MODE_PRIVATE)
    val orderStr = order.joinToString(",")
    prefs.edit().putString(KEY_ACADEMIC_TAB_ORDER, orderStr).apply()
    taskViewModel?.saveSetting(KEY_ACADEMIC_TAB_ORDER, orderStr)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AcademicHubScreen(taskViewModel: TaskViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val settings by taskViewModel.settings.collectAsState()

    var tabOrderKeys by remember { mutableStateOf(getSavedAcademicTabOrder(context)) }
    var subTab by remember { mutableStateOf(tabOrderKeys.firstOrNull() ?: "attendance") }
    var showReorderDialog by remember { mutableStateOf(false) }
    var selectedForReorder by remember { mutableStateOf<String?>(null) }

    val tabMap = remember { DEFAULT_ACADEMIC_TABS.toMap() }
    val currentTabs = remember(tabOrderKeys) {
        tabOrderKeys.mapNotNull { key -> tabMap[key]?.let { key to it } }
    }

    // Sync tab order from Room DB setting if restored from backup
    LaunchedEffect(settings) {
        val dbOrderStr = settings?.find { it.key == KEY_ACADEMIC_TAB_ORDER }?.value
        if (!dbOrderStr.isNullOrBlank()) {
            val dbKeys = dbOrderStr.split(",").filter { it.isNotBlank() && it in DEFAULT_ACADEMIC_TABS.map { t -> t.first } }
            if (dbKeys.isNotEmpty() && dbKeys != tabOrderKeys) {
                val missing = DEFAULT_ACADEMIC_TABS.map { t -> t.first }.filterNot { it in dbKeys }
                val fullOrder = dbKeys + missing
                tabOrderKeys = fullOrder
                val prefs = context.getSharedPreferences(ACADEMIC_PREFS, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_ACADEMIC_TAB_ORDER, fullOrder.joinToString(",")).apply()
            }
        }
    }

    LaunchedEffect(tabOrderKeys) {
        if (subTab !in tabOrderKeys && tabOrderKeys.isNotEmpty()) {
            subTab = tabOrderKeys.first()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 4.dp)) {
        // Tab Bar Row with Long-Press Reorder Support
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x9A0F172A))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(currentTabs, key = { it.first }) { (tabKey, label) ->
                    val isSelected = subTab == tabKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected)
                                    Brush.horizontalGradient(listOf(Color(0xFF4F46E5), Color(0xFF6366F1)))
                                else
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) Color(0xFF818CF8).copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .combinedClickable(
                                onClick = { subTab = tabKey },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedForReorder = tabKey
                                    showReorderDialog = true
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Quick Reorder Action Icon
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedForReorder = subTab
                    showReorderDialog = true
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Text(text = "⇄", color = Color(0xFF818CF8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Content Area
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            AnimatedContent(
                targetState = subTab,
                transitionSpec = {
                    fadeIn(tween(200)).togetherWith(fadeOut(tween(160)))
                },
                label = "subtab_switch"
            ) { targetSubTab ->
                when (targetSubTab) {
                    "timetable" -> TimeTableSubTab(taskViewModel)
                    "attendance" -> AttendanceSubTab(taskViewModel)
                    "cgpa" -> CgpaSubTab(taskViewModel)
                    "flashcards" -> FlashcardsSubTab(taskViewModel)
                    "habits" -> HabitsSubTab(taskViewModel)
                    "finance" -> FinanceSubTab(taskViewModel)
                    "routine" -> RoutineSubTab(taskViewModel)
                    "appusage" -> AppUsageSubTab(taskViewModel)
                }
            }
        }
    }

    // Reorder Dialog
    if (showReorderDialog) {
        AlertDialog(
            onDismissRequest = { showReorderDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "⇄", fontSize = 20.sp, color = Color(0xFF818CF8))
                    Column {
                        Text(
                            text = "Arrange Academic Sections",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Move sections forward or backward (aage/piche) as you like",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentTabs.forEachIndexed { index, (key, label) ->
                        val isHighlighted = selectedForReorder == key
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isHighlighted) Color(0xFF312E81).copy(alpha = 0.6f) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isHighlighted) Color(0xFF818CF8) else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Move Left / Up (Aage)
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                val mutable = tabOrderKeys.toMutableList()
                                                Collections.swap(mutable, index, index - 1)
                                                tabOrderKeys = mutable
                                                saveAcademicTabOrder(context, mutable, taskViewModel)
                                                selectedForReorder = key
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Text(
                                            text = "◀",
                                            color = if (index > 0) Color(0xFF38BDF8) else Color(0xFF475569),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Move Right / Down (Piche)
                                    IconButton(
                                        onClick = {
                                            if (index < tabOrderKeys.size - 1) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                val mutable = tabOrderKeys.toMutableList()
                                                Collections.swap(mutable, index, index + 1)
                                                tabOrderKeys = mutable
                                                saveAcademicTabOrder(context, mutable, taskViewModel)
                                                selectedForReorder = key
                                            }
                                        },
                                        enabled = index < tabOrderKeys.size - 1,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Text(
                                            text = "▶",
                                            color = if (index < tabOrderKeys.size - 1) Color(0xFF38BDF8) else Color(0xFF475569),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showReorderDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done ✅", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val defaultKeys = DEFAULT_ACADEMIC_TABS.map { it.first }
                        tabOrderKeys = defaultKeys
                        saveAcademicTabOrder(context, defaultKeys, taskViewModel)
                        Toast.makeText(context, "Tab order reset to default! 🔄", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset Order 🔄", color = Color.Gray, fontSize = 12.sp)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// SUB-TAB 0: ACADEMIC TIME TABLE (Schedule & Exact Reminders)
// -------------------------------------------------------------
@Composable
fun TimeTableSubTab(taskViewModel: TaskViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val allSlots by taskViewModel.timeTableSlots.collectAsState()

    val dayOptions = remember {
        listOf("Today", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday", "All")
    }
    var selectedDay by remember { mutableStateOf("Today") }

    val todayName = remember { SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()) }
    val currentTimeStr = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var slotToEdit by remember { mutableStateOf<TimeTableSlotEntity?>(null) }
    var slotToDelete by remember { mutableStateOf<TimeTableSlotEntity?>(null) }

    // Filter slots according to selected day
    val displayedSlots = remember(allSlots, selectedDay, todayName) {
        val filtered = when (selectedDay) {
            "Today" -> allSlots.filter { it.dayOfWeek.equals(todayName, ignoreCase = true) || it.dayOfWeek.equals("Daily", ignoreCase = true) }
            "All" -> allSlots
            else -> allSlots.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) || it.dayOfWeek.equals("Daily", ignoreCase = true) }
        }
        filtered.sortedBy { it.startTime }
    }

    // Active Now slot check
    val activeNowSlot = remember(allSlots, todayName, currentTimeStr) {
        allSlots.firstOrNull { slot ->
            val isToday = slot.dayOfWeek.equals(todayName, ignoreCase = true) || slot.dayOfWeek.equals("Daily", ignoreCase = true)
            isToday && currentTimeStr >= slot.startTime && currentTimeStr <= slot.endTime
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner with Quick Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Weekly Time Table",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "📅 $todayName • ${allSlots.size} slots scheduled",
                    fontSize = 12.sp,
                    color = Color(0xFFA5B4FC)
                )
            }

            Button(
                onClick = {
                    slotToEdit = null
                    showAddEditDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("+ Add Slot", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // "Happening Now" Active Slot Highlight Card
        if (activeNowSlot != null) {
            val slotColor = try {
                Color(android.graphics.Color.parseColor(activeNowSlot.colorHex))
            } catch (e: Exception) {
                Color(0xFF10B981)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(slotColor, Color(0xFF10B981))))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Text(
                                text = "HAPPENING NOW",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = activeNowSlot.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏰ ${activeNowSlot.getFormattedTimeRange()} (${activeNowSlot.getDurationMinutes()}m)",
                                color = Color(0xFFC7D2FE),
                                fontSize = 12.sp
                            )

                            if (!activeNowSlot.roomOrLocation.isNullOrBlank()) {
                                Text(
                                    text = "📍 ${activeNowSlot.roomOrLocation}",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            com.ai_assistant.studentfocus.timer.FocusSessionManager.startFocusSession(
                                context = context,
                                taskId = activeNowSlot.id,
                                taskTitle = activeNowSlot.title,
                                targetDurationMinutes = activeNowSlot.getDurationMinutes()
                            )
                            Toast.makeText(context, "Focus started for '${activeNowSlot.title}'", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("▶️ Focus", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Day Selector Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(dayOptions) { day ->
                val isSelected = selectedDay == day
                val count = remember(allSlots, day, todayName) {
                    when (day) {
                        "Today" -> allSlots.count { it.dayOfWeek.equals(todayName, ignoreCase = true) || it.dayOfWeek.equals("Daily", ignoreCase = true) }
                        "All" -> allSlots.size
                        else -> allSlots.count { it.dayOfWeek.equals(day, ignoreCase = true) || it.dayOfWeek.equals("Daily", ignoreCase = true) }
                    }
                }

                val displayLabel = when (day) {
                    "Monday" -> "Mon"
                    "Tuesday" -> "Tue"
                    "Wednesday" -> "Wed"
                    "Thursday" -> "Thu"
                    "Friday" -> "Fri"
                    "Saturday" -> "Sat"
                    "Sunday" -> "Sun"
                    else -> day
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF6366F1) else Color(0x9A0F172A),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.clickable {
                        selectedDay = day
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                ) {
                    Text(
                        text = if (count > 0) "$displayLabel ($count)" else displayLabel,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Slot List or Empty View
        if (displayedSlots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
                    .background(Color(0x9A0F172A), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "📅", fontSize = 32.sp)
                    Text(
                        text = "No classes or tasks scheduled for $selectedDay",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Tap '+ Add Slot' to set up your subjects, labs, or study routine.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedSlots, key = { it.id }) { slot ->
                    TimeTableSlotCard(
                        slot = slot,
                        onEdit = {
                            slotToEdit = slot
                            showAddEditDialog = true
                        },
                        onDelete = {
                            slotToDelete = slot
                        },
                        onToggleNotification = {
                            taskViewModel.toggleTimeTableNotification(slot, context)
                            val status = if (!slot.isNotificationEnabled) "enabled" else "disabled"
                            Toast.makeText(context, "Reminder $status for '${slot.title}'", Toast.LENGTH_SHORT).show()
                        },
                        onStartFocus = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            com.ai_assistant.studentfocus.timer.FocusSessionManager.startFocusSession(
                                context = context,
                                taskId = slot.id,
                                taskTitle = slot.title,
                                targetDurationMinutes = slot.getDurationMinutes()
                            )
                            Toast.makeText(context, "Focus started for '${slot.title}'", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        val targetDay = if (selectedDay == "Today") todayName else if (selectedDay == "All") "Monday" else selectedDay
        TimeTableSlotDialog(
            slotToEdit = slotToEdit,
            targetDay = targetDay,
            onSave = { newOrUpdated, selectedDays, applyAllDays ->
                taskViewModel.saveTimeTableSlotMultiDays(
                    originalSlot = slotToEdit,
                    updatedSlot = newOrUpdated,
                    selectedDays = selectedDays,
                    isDailySeries = applyAllDays,
                    context = context
                )
                showAddEditDialog = false
                slotToEdit = null
                val scopeLabel = if (applyAllDays) "for all days" else "for ${selectedDays.joinToString(", ")}"
                Toast.makeText(context, "Time Table updated $scopeLabel!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showAddEditDialog = false
                slotToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog with 2 Scope Options (Same Day Only vs All Days)
    slotToDelete?.let { slot ->
        val currentTargetDay = if (selectedDay == "Today") todayName else if (selectedDay == "All") slot.dayOfWeek else selectedDay
        AlertDialog(
            onDismissRequest = { slotToDelete = null },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(text = "🗑️ Delete Slot Options", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose delete scope for '${slot.title}' (${slot.dayOfWeek} • ${slot.getFormattedTimeRange()}):",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                    Surface(
                        color = Color(0x33334155),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "1. Delete for $currentTargetDay Only: Removes slot on $currentTargetDay while preserving other days.",
                                color = Color(0xFFA5B4FC),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "2. Delete Across All Days: Deletes this slot series completely across the entire week.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: Delete for Same/This Day Only
                    Button(
                        onClick = {
                            taskViewModel.deleteTimeTableSlotScope(
                                slot = slot,
                                targetDay = currentTargetDay,
                                deleteAllDays = false,
                                context = context
                            )
                            slotToDelete = null
                            Toast.makeText(context, "Deleted for $currentTargetDay only", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🗓️ Delete for $currentTargetDay Only", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White)
                    }

                    // Option 2: Delete Across All Days
                    Button(
                        onClick = {
                            taskViewModel.deleteTimeTableSlotScope(
                                slot = slot,
                                targetDay = currentTargetDay,
                                deleteAllDays = true,
                                context = context
                            )
                            slotToDelete = null
                            Toast.makeText(context, "Deleted across all days", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🗑️ Delete Across All Days", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }

                    // Option 3: Cancel Button (cleanly structured below without overlapping)
                    OutlinedButton(
                        onClick = { slotToDelete = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        )
    }
}

@Composable
fun TimeTableSlotCard(
    slot: TimeTableSlotEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleNotification: () -> Unit,
    onStartFocus: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = remember(slot.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(slot.colorHex))
        } catch (e: Exception) {
            Color(0xFF6366F1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0x9A0F172A),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left color accent bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top Row: Day Pill + Time Range Badge + Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Day Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = slot.dayOfWeek,
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Time Range
                        Text(
                            text = "⏰ ${slot.getFormattedTimeRange()}",
                            color = Color(0xFFC7D2FE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "(${slot.getDurationMinutes()}m)",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }

                    // Action Icons
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        // Notification Toggle
                        IconButton(
                            onClick = onToggleNotification,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text(
                                text = if (slot.isNotificationEnabled) "🔔" else "🔕",
                                fontSize = 13.sp
                            )
                        }

                        // Start Focus Quick Action
                        IconButton(
                            onClick = onStartFocus,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("▶️", fontSize = 12.sp)
                        }

                        // Edit
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("✎", color = Color(0xFF818CF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Delete
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("✕", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Title
                Text(
                    text = slot.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                // Optional Location, Notes & Status Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!slot.roomOrLocation.isNullOrBlank()) {
                        Text(
                            text = "📍 ${slot.roomOrLocation}",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!slot.notes.isNullOrBlank()) {
                        Text(
                            text = "📝 ${slot.notes}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    if (slot.isTimeWasteAutoManaged) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x2210B981),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("⏳", fontSize = 9.sp)
                                Text(
                                    text = "Auto Waste Control",
                                    color = Color(0xFF34D399),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Study Pace & Goal Tracked Badge
                    if (slot.isGoalTrackingEnabled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x2238BDF8),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("🎯", fontSize = 9.sp)
                                Text(
                                    text = "Goal Tracked",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x2264748B),
                            border = BorderStroke(1.dp, Color(0xFF64748B).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("☕", fontSize = 9.sp)
                                Text(
                                    text = "Off Track",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Clickable Link Button
                if (!slot.linkUrl.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x336366F1),
                        border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.4f)),
                        modifier = Modifier.clickable {
                            try {
                                var url = slot.linkUrl.trim()
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    url = "https://$url"
                                }
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("🔗", fontSize = 11.sp)
                            Text(
                                text = if (slot.linkUrl.length > 32) slot.linkUrl.take(29) + "..." else slot.linkUrl,
                                color = Color(0xFFA5B4FC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("↗", color = Color(0xFF818CF8), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeTableSlotDialog(
    slotToEdit: TimeTableSlotEntity?,
    targetDay: String = "Monday",
    onSave: (TimeTableSlotEntity, Set<String>, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val allWeekDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var title by remember { mutableStateOf(slotToEdit?.title ?: "") }
    var selectedDays by remember {
        mutableStateOf(
            if (slotToEdit?.dayOfWeek?.equals("Daily", ignoreCase = true) == true) {
                allWeekDays.toSet()
            } else if (slotToEdit != null) {
                setOf(slotToEdit.dayOfWeek)
            } else {
                setOf(if (targetDay in allWeekDays) targetDay else "Monday")
            }
        )
    }
    var startTime by remember { mutableStateOf(slotToEdit?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(slotToEdit?.endTime ?: "10:30") }
    var roomOrLocation by remember { mutableStateOf(slotToEdit?.roomOrLocation ?: "") }
    var linkUrl by remember { mutableStateOf(slotToEdit?.linkUrl ?: "") }
    var isTimeWasteAutoManaged by remember { mutableStateOf(slotToEdit?.isTimeWasteAutoManaged ?: false) }
    var isGoalTrackingEnabled by remember { mutableStateOf(slotToEdit?.isGoalTrackingEnabled ?: true) }
    var notes by remember { mutableStateOf(slotToEdit?.notes ?: "") }
    var colorHex by remember { mutableStateOf(slotToEdit?.colorHex ?: "#6366F1") }
    var isNotificationEnabled by remember { mutableStateOf(slotToEdit?.isNotificationEnabled ?: true) }
    var applyScopeAllDays by remember { mutableStateOf(slotToEdit?.dayOfWeek?.equals("Daily", ignoreCase = true) == true || selectedDays.size >= 7) }

    val context = LocalContext.current

    fun formatTimeDisplay(timeStr: String): String {
        return try {
            val parts = timeStr.trim().split(":").mapNotNull { it.toIntOrNull() }
            val h = parts.getOrNull(0) ?: 9
            val m = parts.getOrNull(1) ?: 0
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
            }
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
        } catch (e: Exception) {
            timeStr
        }
    }

    fun calculateDurationString(startStr: String, endStr: String): Pair<String, Boolean> {
        return try {
            val sParts = startStr.trim().split(":").mapNotNull { it.toIntOrNull() }
            val eParts = endStr.trim().split(":").mapNotNull { it.toIntOrNull() }
            val sMins = (sParts.getOrNull(0) ?: 0) * 60 + (sParts.getOrNull(1) ?: 0)
            val eMins = (eParts.getOrNull(0) ?: 0) * 60 + (eParts.getOrNull(1) ?: 0)
            if (eMins <= sMins) {
                return Pair("End Time must be after Start Time", false)
            }
            val diff = eMins - sMins
            val h = diff / 60
            val m = diff % 60
            val label = when {
                h > 0 && m > 0 -> "${h}h ${m}m duration"
                h > 0 -> "${h}h duration"
                else -> "${m}m duration"
            }
            Pair(label, true)
        } catch (e: Exception) {
            Pair("", true)
        }
    }

    val sParts = startTime.split(":").mapNotNull { it.trim().toIntOrNull() }
    val sMins = (sParts.getOrNull(0) ?: 9) * 60 + (sParts.getOrNull(1) ?: 0)
    val eParts = endTime.split(":").mapNotNull { it.trim().toIntOrNull() }
    val eMins = (eParts.getOrNull(0) ?: 10) * 60 + (eParts.getOrNull(1) ?: 0)
    val isTimeRangeValid = eMins > sMins

    val colorPalette = listOf(
        "#6366F1" to "Indigo",
        "#10B981" to "Emerald",
        "#F59E0B" to "Amber",
        "#EC4899" to "Rose",
        "#06B6D4" to "Cyan",
        "#8B5CF6" to "Purple"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Text(
                text = if (slotToEdit != null) "Edit Time Table Slot" else "Add Time Table Slot",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task / Subject / Activity Name") },
                        placeholder = { Text("e.g. Maths Lecture, DSA Practice") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    // Multi-Day Selection Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select Days of Week", color = Color(0xFFA5B4FC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val isAllSelected = selectedDays.size >= 7 || applyScopeAllDays
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isAllSelected) Color(0xFF6366F1) else Color(0x33334155),
                                border = BorderStroke(1.dp, if (isAllSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.clickable {
                                    if (isAllSelected) {
                                        selectedDays = setOf(targetDay)
                                        applyScopeAllDays = false
                                    } else {
                                        selectedDays = allWeekDays.toSet()
                                        applyScopeAllDays = true
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isAllSelected) "✓ All Days (Daily)" else "🔁 Select All Days",
                                    color = if (isAllSelected) Color.White else Color(0xFFA5B4FC),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Day Multi-Select Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(allWeekDays) { d ->
                                val isSelected = selectedDays.contains(d)
                                val shortName = when (d) {
                                    "Monday" -> "Mon"
                                    "Tuesday" -> "Tue"
                                    "Wednesday" -> "Wed"
                                    "Thursday" -> "Thu"
                                    "Friday" -> "Fri"
                                    "Saturday" -> "Sat"
                                    "Sunday" -> "Sun"
                                    else -> d
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF6366F1) else Color(0x9A0F172A),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.clickable {
                                        val newSet = selectedDays.toMutableSet()
                                        if (isSelected) {
                                            if (newSet.size > 1) { // Prevent empty selection
                                                newSet.remove(d)
                                            }
                                        } else {
                                            newSet.add(d)
                                        }
                                        selectedDays = newSet
                                        applyScopeAllDays = newSet.size >= 7
                                    }
                                ) {
                                    Text(
                                        text = if (isSelected) "✓ $shortName" else shortName,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Selected Summary indicator
                        val summaryText = if (selectedDays.size >= 7 || applyScopeAllDays) {
                            "🗓️ Selected: Every Day (Daily Schedule)"
                        } else {
                            "🗓️ Selected (${selectedDays.size}): " + selectedDays.joinToString(", ") {
                                when (it) {
                                    "Monday" -> "Mon"
                                    "Tuesday" -> "Tue"
                                    "Wednesday" -> "Wed"
                                    "Thursday" -> "Thu"
                                    "Friday" -> "Fri"
                                    "Saturday" -> "Sat"
                                    "Sunday" -> "Sun"
                                    else -> it
                                }
                            }
                        }
                        Text(
                            text = summaryText,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                item {
                    // Interactive Time Pickers (Start & End Time)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Timing (Hour & Minute)", color = Color(0xFFA5B4FC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val (durationLabel, isValid) = calculateDurationString(startTime, endTime)
                            if (durationLabel.isNotBlank()) {
                                Surface(
                                    color = if (isValid) Color(0x336366F1) else Color(0x33EF4444),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (isValid) Color(0xFF818CF8).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = durationLabel,
                                        color = if (isValid) Color(0xFF818CF8) else Color(0xFFFCA5A5),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start Time Selector Card
                            val sHour = sParts.getOrNull(0) ?: 9
                            val sMin = sParts.getOrNull(1) ?: 0

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, selectedHour, selectedMinute ->
                                                startTime = "%02d:%02d".format(selectedHour, selectedMinute)
                                                val selectedStartTotal = selectedHour * 60 + selectedMinute
                                                val curEndParts = endTime.split(":").mapNotNull { it.trim().toIntOrNull() }
                                                val curEndTotal = (curEndParts.getOrNull(0) ?: 10) * 60 + (curEndParts.getOrNull(1) ?: 0)

                                                // Automatically ensure End Time > Start Time
                                                if (curEndTotal <= selectedStartTotal) {
                                                    val autoEndTotal = (selectedStartTotal + 60).coerceAtMost(1439)
                                                    endTime = "%02d:%02d".format(autoEndTotal / 60, autoEndTotal % 60)
                                                }
                                            },
                                            sHour,
                                            sMin,
                                            false
                                        ).show()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("🕒 Start Time", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatTimeDisplay(startTime),
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Tap to select time", color = Color(0xFF818CF8), fontSize = 9.sp)
                                }
                            }

                            // End Time Selector Card
                            val eHour = eParts.getOrNull(0) ?: 10
                            val eMin = eParts.getOrNull(1) ?: 30

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isTimeRangeValid) Color(0xFF6366F1).copy(alpha = 0.5f) else Color(0xFFEF4444)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, selectedHour, selectedMinute ->
                                                val selectedEndTotal = selectedHour * 60 + selectedMinute
                                                val curStartParts = startTime.split(":").mapNotNull { it.trim().toIntOrNull() }
                                                val curStartTotal = (curStartParts.getOrNull(0) ?: 9) * 60 + (curStartParts.getOrNull(1) ?: 0)

                                                if (selectedEndTotal <= curStartTotal) {
                                                    Toast.makeText(
                                                        context,
                                                        "End Time must be after Start Time (${formatTimeDisplay(startTime)})! Adjusted automatically.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    val autoEndTotal = (curStartTotal + 45).coerceAtMost(1439)
                                                    endTime = "%02d:%02d".format(autoEndTotal / 60, autoEndTotal % 60)
                                                } else {
                                                    endTime = "%02d:%02d".format(selectedHour, selectedMinute)
                                                }
                                            },
                                            eHour,
                                            eMin,
                                            false
                                        ).show()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("🏁 End Time", color = if (isTimeRangeValid) Color(0xFF94A3B8) else Color(0xFFFCA5A5), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatTimeDisplay(endTime),
                                        color = if (isTimeRangeValid) Color.White else Color(0xFFFCA5A5),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isTimeRangeValid) "Tap to select time" else "Must be > Start Time",
                                        color = if (isTimeRangeValid) Color(0xFF818CF8) else Color(0xFFEF4444),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        if (!isTimeRangeValid) {
                            Surface(
                                color = Color(0x33EF4444),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ End Time must be strictly after Start Time (${formatTimeDisplay(startTime)})",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Quick duration adder chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quick Add:", color = Color(0xFF64748B), fontSize = 10.sp)
                            val quickDurations = listOf(30 to "+30m", 45 to "+45m", 60 to "+1h", 90 to "+1.5h", 120 to "+2h")
                            quickDurations.forEach { (minsToAdd, label) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x33334155),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier.clickable {
                                        val sTotal = sMins
                                        val newEndTotal = (sTotal + minsToAdd).coerceAtMost(1439)
                                        endTime = "%02d:%02d".format(newEndTotal / 60, newEndTotal % 60)
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        color = Color(0xFFA5B4FC),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Meeting / Lecture / Notes URL Link
                    OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        label = { Text("🔗 Meeting / Notes / Lecture Link (Optional)") },
                        placeholder = { Text("e.g. https://meet.google.com/abc, Zoom, YouTube") },
                        leadingIcon = { Text("🔗", fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    // Auto Time Waste Tracker & Focus Control Switch
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTimeWasteAutoManaged) Color(0x2210B981) else Color(0x9A0F172A),
                        border = BorderStroke(1.dp, if (isTimeWasteAutoManaged) Color(0xFF10B981).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "⏳ Auto-Pause Waste Time",
                                    color = if (isTimeWasteAutoManaged) Color(0xFF34D399) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isTimeWasteAutoManaged)
                                        "ON: Pauses Time Waste Tracker during this class."
                                    else
                                        "OFF: Time Waste Tracker continues running.",
                                    color = if (isTimeWasteAutoManaged) Color(0xFFA7F3D0) else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(
                                checked = isTimeWasteAutoManaged,
                                onCheckedChange = { isTimeWasteAutoManaged = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF10B981)
                                )
                            )
                        }
                    }
                }

                item {
                    // Study Pace & Goal Planner Integration Switch
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGoalTrackingEnabled) Color(0x2238BDF8) else Color(0x9A0F172A),
                        border = BorderStroke(1.dp, if (isGoalTrackingEnabled) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "🎯 Count in Routine Budget",
                                    color = if (isGoalTrackingEnabled) Color(0xFF38BDF8) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isGoalTrackingEnabled)
                                        "ON: Adds this class time to your daily study schedule."
                                    else
                                        "OFF: Excluded from schedule (ideal for gym or breaks).",
                                    color = if (isGoalTrackingEnabled) Color(0xFFBAE6FD) else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(
                                checked = isGoalTrackingEnabled,
                                onCheckedChange = { isGoalTrackingEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF38BDF8)
                                )
                            )
                        }
                    }
                }

                item {
                    // Location / Room
                    OutlinedTextField(
                        value = roomOrLocation,
                        onValueChange = { roomOrLocation = it },
                        label = { Text("Room / Location (Optional)") },
                        placeholder = { Text("e.g. Room 302, Library, Home") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    // Color Accent Picker
                    Text("Tag Color", color = Color(0xFFA5B4FC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colorPalette.forEach { (hex, _) ->
                            val isChosen = colorHex.equals(hex, ignoreCase = true)
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFF6366F1) }
                            Box(
                                modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isChosen) 3.dp else 1.dp,
                                    color = if (isChosen) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                            )
                        }
                    }
                }

                item {
                    // Notification Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x9A0F172A), RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("🔔 5-Min Class Reminder", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isNotificationEnabled)
                                    "ON: Alerts you 5 minutes before class with link button."
                                else
                                    "OFF: No reminder notification.",
                                color = if (isNotificationEnabled) Color(0xFFA5B4FC) else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = { isNotificationEnabled = it }
                        )
                    }
                }

                item {
                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Description (Optional)") },
                        placeholder = { Text("e.g. Chapter 4 revision, Bring Lab manual") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val cleanedStart = if (startTime.contains(":")) startTime.trim() else "09:00"
                    val cleanedEnd = if (endTime.contains(":")) endTime.trim() else "10:00"

                    val isDailySeries = applyScopeAllDays || selectedDays.size >= 7
                    val primaryDay = if (isDailySeries) "Daily" else selectedDays.firstOrNull() ?: "Monday"

                    val slot = TimeTableSlotEntity(
                        id = slotToEdit?.id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        dayOfWeek = primaryDay,
                        startTime = cleanedStart,
                        endTime = cleanedEnd,
                        roomOrLocation = roomOrLocation.trim().ifBlank { null },
                        linkUrl = linkUrl.trim().ifBlank { null },
                        isTimeWasteAutoManaged = isTimeWasteAutoManaged,
                        isGoalTrackingEnabled = isGoalTrackingEnabled,
                        notes = notes.trim().ifBlank { null },
                        colorHex = colorHex,
                        isNotificationEnabled = isNotificationEnabled,
                        createdAt = slotToEdit?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(slot, selectedDays, isDailySeries)
                },
                enabled = title.isNotBlank() && isTimeRangeValid && selectedDays.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (slotToEdit != null) "Update" else "Save Slot", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

// -------------------------------------------------------------
// SUB-TAB 1: ATTENDANCE TRACKER (with Circular Indicators)
// -------------------------------------------------------------
@Composable
fun AttendanceSubTab(taskViewModel: TaskViewModel) {
    val attendanceList by taskViewModel.attendance.collectAsState()
    var subjectInput by remember { mutableStateOf("") }
    var subjectToDelete by remember { mutableStateOf<com.ai_assistant.studentfocus.models.AttendanceEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = subjectInput,
                onValueChange = { subjectInput = it },
                label = { Text("Subject (e.g. Mathematics)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (subjectInput.isNotBlank()) {
                        taskViewModel.addSubject(subjectInput)
                        subjectInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("Add")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(attendanceList) { item ->
                val total = item.presents + item.absents
                val percent = if (total > 0) (item.presents * 100) / total else 0
                val targetColor = if (percent >= 75) Color(0xFF4ADE80) else Color(0xFFF87171)

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
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Ring Indicator
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                            CircularProgressIndicator(
                                progress = { percent.toFloat() / 100f },
                                color = targetColor,
                                strokeWidth = 5.dp,
                                trackColor = Color(0xFF020617),
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "$percent%",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.subjectName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${item.presents} Present, ${item.absents} Absent",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { taskViewModel.incrementPresent(item) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("P +", fontSize = 11.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { taskViewModel.incrementAbsent(item) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("A +", fontSize = 11.sp, color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { subjectToDelete = item }, modifier = Modifier.size(28.dp)) {
                                Text("❌", fontSize = 12.sp)
                            }
                        }
                    }
                }

            }
        }
    }

    subjectToDelete?.let { subject ->
        DeleteConfirmationDialog(
            taskTitle = subject.subjectName,
            onConfirm = {
                taskViewModel.deleteSubject(subject)
                subjectToDelete = null
            },
            onDismiss = { subjectToDelete = null }
        )
    }
}

// -------------------------------------------------------------
// SUB-TAB 2: SATHYABAMA CGPA CALCULATOR & MATHS (Gradient Cards)
// -------------------------------------------------------------
@Composable
fun CgpaSubTab(taskViewModel: TaskViewModel) {
    val courses by taskViewModel.courses.collectAsState()

    // Mode Selector: "Grade" (Subject-wise GPA) or "CGPA" (Semester-wise CGPA)
    var calculationMode by remember { mutableStateOf("Grade") }

    // Subject GPA Inputs
    var nameInput by remember { mutableStateOf("") }
    var creditsInput by remember { mutableStateOf("3") }
    var gradeInput by remember { mutableStateOf("A++") }

    // Semester CGPA Inputs
    var semesterNameInput by remember { mutableStateOf("") }
    var semesterGpaInput by remember { mutableStateOf("") }
    var semesterCreditsInput by remember { mutableStateOf("20") }

    // In-memory list for semester-wise inputs
    val semestersList = remember { mutableStateListOf<Pair<String, Pair<Float, Int>>>() }

    val grades = listOf("A++", "A+", "B++", "B+", "C", "RA", "AAA", "W", "ABS")
    val gradeValues = mapOf(
        "A++" to 10, "A+" to 9, "B++" to 8, "B+" to 7, "C" to 6,
        "RA" to 0, "AAA" to 0, "W" to 0, "ABS" to 0
    )

    // Subject GPA Calculation
    val totalCredits = courses.sumOf { it.credits }
    val totalPoints = courses.sumOf { it.credits * (gradeValues[it.grade] ?: 0) }
    val calculatedGpa = if (totalCredits > 0) totalPoints.toFloat() / totalCredits.toFloat() else 0.0f

    // Semester CGPA Calculation
    val totalSemCredits = semestersList.sumBy { it.second.second }
    val totalSemPoints = semestersList.sumOf { (it.second.first * it.second.second).toDouble() }.toFloat()
    val calculatedCgpa = if (totalSemCredits > 0) totalSemPoints / totalSemCredits.toFloat() else 0.0f

    val displayValue = if (calculationMode == "Grade") calculatedGpa else calculatedCgpa
    val displayCredits = if (calculationMode == "Grade") totalCredits else totalSemCredits

    val classAward = when {
        displayValue >= 9.00f -> "First Class - Exemplary"
        displayValue >= 7.50f -> "First Class with Distinction"
        displayValue >= 6.00f -> "First Class"
        displayValue >= 5.00f -> "Second Class"
        else -> "RA (Reappear Target Required)"
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Mode Selector Pills
        item {
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
                listOf("Grade" to "📝 Subject GPA", "CGPA" to "🎓 Semester CGPA").forEach { (mode, label) ->
                    val isSelected = calculationMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5)))
                                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { calculationMode = mode }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Immersive Gradient GPA/CGPA Summary Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color(0xFFEC4899).copy(alpha = 0.3f), Color(0xFF6366F1).copy(alpha = 0.05f))),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (calculationMode == "Grade") "Calculated Semester GPA" else "Cumulative CGPA Result",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "%.2f".format(displayValue),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(text = classAward, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF34D399))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total Registered Credits: $displayCredits",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (calculationMode == "Grade") {
            // Subject-wise GPA Form
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

                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Add Semester Course", color = Color.White, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Course Name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1.5f)
                            )
                            OutlinedTextField(
                                value = creditsInput,
                                onValueChange = { creditsInput = it },
                                label = { Text("Cr.") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(0.7f)
                            )

                            var gradeExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(0.8f).align(Alignment.CenterVertically)) {
                                OutlinedButton(onClick = { gradeExpanded = true }) {
                                    Text(text = gradeInput, color = Color.White, fontSize = 11.sp)
                                }
                                DropdownMenu(expanded = gradeExpanded, onDismissRequest = { gradeExpanded = false }) {
                                    grades.forEach { g ->
                                        DropdownMenuItem(text = { Text(g) }, onClick = {
                                            gradeInput = g
                                            gradeExpanded = false
                                        })
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val credits = creditsInput.toIntOrNull() ?: 3
                                if (nameInput.isNotBlank()) {
                                    taskViewModel.addCourse(nameInput, credits, gradeInput)
                                    nameInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Course Details")
                        }
                    }
                }
            }

            // List courses
            items(courses) { course ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = course.name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Credits: ${course.credits}  |  Grade: ${course.grade}",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { taskViewModel.deleteCourse(course) }) {
                        Text("❌", fontSize = 12.sp)
                    }
                }
            }
        } else {
            // Semester-wise CGPA Form
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Add Semester GPA & Credits", color = Color.White, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = semesterNameInput,
                                onValueChange = { semesterNameInput = it },
                                label = { Text("Semester (e.g. Sem 1)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1.3f)
                            )
                            OutlinedTextField(
                                value = semesterGpaInput,
                                onValueChange = { semesterGpaInput = it },
                                label = { Text("GPA") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(0.9f)
                            )
                            OutlinedTextField(
                                value = semesterCreditsInput,
                                onValueChange = { semesterCreditsInput = it },
                                label = { Text("Credits") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(0.8f)
                            )
                        }

                        Button(
                            onClick = {
                                val gpaVal = semesterGpaInput.toFloatOrNull() ?: 0.0f
                                val creditsVal = semesterCreditsInput.toIntOrNull() ?: 20
                                if (semesterNameInput.isNotBlank() && gpaVal > 0f) {
                                    semestersList.add(semesterNameInput to (gpaVal to creditsVal))
                                    semesterNameInput = ""
                                    semesterGpaInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Semester Data")
                        }
                    }
                }
            }

            // List semesters
            items(semestersList) { sem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = sem.first, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            text = "GPA: ${sem.second.first}  |  Credits: ${sem.second.second}",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { semestersList.remove(sem) }) {
                        Text("❌", fontSize = 12.sp)
                    }
                }
            }
        }

        // CGPA Mathematical Formulas & Range of Marks display
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "🧮 GPA & CGPA Grading Guide", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Text(text = "Semester GPA Calculation:", color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(
                        text = "GPA = Sum of (Subject Credits × Grade Points) / Total Semester Credits\n\nExample: If you have 3 subjects of 3 credits each with A++, A+, and B++:\nGPA = ((3×10) + (3×9) + (3×8)) / 9 = 9.00",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    Text(text = "Cumulative CGPA Calculation:", color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(
                        text = "CGPA = Sum of (Semester GPA × Semester Credits) / Total Credits across all Semesters\n\nExample: Sem 1 (GPA 9.00, 20 Credits) and Sem 2 (GPA 8.00, 20 Credits):\nCGPA = ((9×20) + (8×20)) / 40 = 8.50",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    Text(text = "Grade Point Scale Table:", color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    val guides = listOf(
                        "90-100" to "A++ (10 GP)",
                        "80-89" to "A+ (9 GP)",
                        "70-79" to "B++ (8 GP)",
                        "60-69" to "B+ (7 GP)",
                        "50-59" to "C (6 GP)",
                        "00-49" to "RA (0 GP)",
                        "Absent" to "AAA (0 GP)",
                        "Withdrawal" to "W (0 GP)",
                        "Break of Study" to "ABS (0 GP)"
                    )
                    guides.forEach { (marks, gradePoints) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = marks, color = Color.LightGray, fontSize = 12.sp)
                            Text(text = gradePoints, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SUB-TAB 3: FLASHCARDS (spaced repetition)
// -------------------------------------------------------------
@Composable
fun FlashcardsSubTab(taskViewModel: TaskViewModel) {
    val decks by taskViewModel.decks.collectAsState()
    val allCards by taskViewModel.allFlashcards.collectAsState()
    var selectedDeckId by remember { mutableStateOf<String?>(null) }
    var deckNameInput by remember { mutableStateOf("") }
    var deckToDelete by remember { mutableStateOf<DeckEntity?>(null) }
    val context = LocalContext.current

    if (selectedDeckId != null) {
        val deckId = selectedDeckId!!
        val activeDeck = decks.find { it.id == deckId }
        if (activeDeck != null) {
            FlashcardReviewSession(
                taskViewModel = taskViewModel,
                deck = activeDeck,
                onBack = { selectedDeckId = null }
            )
        } else {
            selectedDeckId = null
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Deck creation row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = deckNameInput,
                    onValueChange = { deckNameInput = it },
                    label = { Text("New Study Deck Name", fontSize = 13.sp) },
                    placeholder = { Text("e.g. Physics, Biology, Formulae...", color = Color.Gray, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (deckNameInput.isNotBlank()) {
                            taskViewModel.addDeck(deckNameInput.trim())
                            android.widget.Toast.makeText(context, "Deck '${deckNameInput.trim()}' created!", android.widget.Toast.LENGTH_SHORT).show()
                            deckNameInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("+ Create", fontWeight = FontWeight.Bold)
                }
            }

            if (decks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🗂️", fontSize = 36.sp)
                        Text("No Flashcard Decks Yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Create a study deck above to add and practice cards!", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(decks) { deck ->
                        val deckCards = allCards.filter { it.deckId == deck.id }
                        val hardCount = deckCards.count { it.difficulty.equals("Hard", ignoreCase = true) }
                        val goodCount = deckCards.count { it.difficulty.equals("Good", ignoreCase = true) || it.difficulty.isBlank() }
                        val easyCount = deckCards.count { it.difficulty.equals("Easy", ignoreCase = true) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDeckId = deck.id }
                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📁 ${deck.name}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "🗂️ ${deckCards.size} Cards",
                                    color = Color(0xFFA5B4FC),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (hardCount > 0) {
                                        Surface(
                                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                                        ) {
                                            Text("🔴 $hardCount", color = Color(0xFFFCA5A5), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                    if (goodCount > 0) {
                                        Surface(
                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                                        ) {
                                            Text("🟡 $goodCount", color = Color(0xFFFDE68A), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                    if (easyCount > 0) {
                                        Surface(
                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                        ) {
                                            Text("🟢 $easyCount", color = Color(0xFF6EE7B7), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Tap to open", color = Color(0xFF818CF8), fontSize = 11.sp)
                                    TextButton(
                                        onClick = { deckToDelete = deck },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("Delete", color = Color(0xFFEF4444).copy(alpha = 0.8f), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Deck delete confirmation dialog
    deckToDelete?.let { deck ->
        DeleteConfirmationDialog(
            taskTitle = "Deck: ${deck.name} (and all its cards)",
            onConfirm = {
                taskViewModel.deleteDeck(deck)
                android.widget.Toast.makeText(context, "Deck deleted", android.widget.Toast.LENGTH_SHORT).show()
                deckToDelete = null
            },
            onDismiss = { deckToDelete = null }
        )
    }
}

@Composable
fun FlashcardReviewSession(
    taskViewModel: TaskViewModel,
    deck: DeckEntity,
    onBack: () -> Unit
) {
    val cardsFlow = remember(deck.id) { taskViewModel.getFlashcardsForDeck(deck.id) }
    val cards by cardsFlow.collectAsState(initial = emptyList())
    val context = LocalContext.current

    var selectedMode by remember { mutableStateOf("study") } // "study" or "manage"
    var selectedCategoryFilter by remember { mutableStateOf("All") } // "All", "Hard", "Good", "Easy"
    
    var currentIdx by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    var showAddCardDialog by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<FlashcardEntity?>(null) }

    // Filter cards based on selected category pill
    val filteredCards = remember(cards, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "Hard" -> cards.filter { it.difficulty.equals("Hard", ignoreCase = true) }
            "Good" -> cards.filter { it.difficulty.equals("Good", ignoreCase = true) || it.difficulty.isBlank() }
            "Easy" -> cards.filter { it.difficulty.equals("Easy", ignoreCase = true) }
            else -> cards
        }
    }

    // Category counts
    val hardCount = cards.count { it.difficulty.equals("Hard", ignoreCase = true) }
    val goodCount = cards.count { it.difficulty.equals("Good", ignoreCase = true) || it.difficulty.isBlank() }
    val easyCount = cards.count { it.difficulty.equals("Easy", ignoreCase = true) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("◀ Back", fontSize = 13.sp)
            }
            Text(
                text = "📁 ${deck.name}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = { showAddCardDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("+ Card", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Mode switch: Study Mode vs Card Manager Mode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { selectedMode = "study"; currentIdx = 0; isFlipped = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedMode == "study") Color(0xFF6366F1) else Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("🃏 Study & Review", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
            }
            Button(
                onClick = { selectedMode = "manage" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedMode == "manage") Color(0xFF6366F1) else Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("📋 Cards List (${cards.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
            }
        }

        // Category Filter Pills: All, Hard, Good, Easy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val categories = listOf(
                "All" to "All (${cards.size})",
                "Hard" to "🔴 Hard ($hardCount)",
                "Good" to "🟡 Good ($goodCount)",
                "Easy" to "🟢 Easy ($easyCount)"
            )
            categories.forEach { (catKey, catLabel) ->
                val isSelected = selectedCategoryFilter == catKey
                Surface(
                    color = if (isSelected) {
                        when (catKey) {
                            "Hard" -> Color(0xFFEF4444)
                            "Good" -> Color(0xFFF59E0B)
                            "Easy" -> Color(0xFF10B981)
                            else -> Color(0xFF6366F1)
                        }
                    } else Color(0xFF1E293B),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            selectedCategoryFilter = catKey
                            currentIdx = 0
                            isFlipped = false
                        }
                ) {
                    Text(
                        text = catLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }

        if (selectedMode == "study") {
            // ─────────────────────────────────────────────────────────────
            // 1. STUDY / FLIP CARD MODE
            // ─────────────────────────────────────────────────────────────
            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🗂️", fontSize = 40.sp)
                        Text("No flashcards in this deck yet!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Tap '+ Card' above to add your first question and answer.", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            } else if (filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✨", fontSize = 36.sp)
                        Text("No '$selectedCategoryFilter' Cards in this deck", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Button(
                            onClick = { selectedCategoryFilter = "All"; currentIdx = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Text("View All Cards")
                        }
                    }
                }
            } else if (currentIdx >= filteredCards.size) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("🎉", fontSize = 48.sp)
                        Text(
                            text = "All ${filteredCards.size} cards reviewed in '$selectedCategoryFilter' bucket!",
                            color = Color(0xFF4ADE80),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    currentIdx = 0
                                    isFlipped = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("🔁 Review Again")
                            }
                            if (selectedCategoryFilter != "All") {
                                Button(
                                    onClick = {
                                        selectedCategoryFilter = "All"
                                        currentIdx = 0
                                        isFlipped = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                ) {
                                    Text("All Cards")
                                }
                            }
                        }
                    }
                }
            } else {
                val card = filteredCards[currentIdx]
                
                // Top Info & Delete Card Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Card ${currentIdx + 1} of ${filteredCards.size}",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Current card difficulty badge
                        val badgeColor = when (card.difficulty) {
                            "Hard" -> Color(0xFFEF4444)
                            "Easy" -> Color(0xFF10B981)
                            else -> Color(0xFFF59E0B)
                        }
                        Surface(
                            color = badgeColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Category: ${card.difficulty}",
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Delete Single Card Button
                        IconButton(
                            onClick = { cardToDelete = card },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("🗑️", fontSize = 14.sp)
                        }
                    }
                }

                val rotation by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = tween(durationMillis = 400)
                )

                // 3D Flip Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clickable { isFlipped = !isFlipped }
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .border(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation <= 90f) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("❓ QUESTION", color = Color(0xFFA5B4FC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = card.front,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Tap card to reveal answer 🔄", color = Color.Gray, fontSize = 11.sp)
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.graphicsLayer { rotationY = 180f }
                            ) {
                                Text("💡 ANSWER", color = Color(0xFF6EE7B7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = card.back,
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Rating & Categorization Buttons (Hard / Good / Easy)
                if (isFlipped) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Rate & Categorize this card:",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Hard Button
                            Button(
                                onClick = {
                                    taskViewModel.reviewFlashcard(card, "Hard")
                                    isFlipped = false
                                    if (selectedCategoryFilter == "All") currentIdx++
                                    android.widget.Toast.makeText(context, "Card moved to Hard bucket 🔴", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔴 Hard", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Review Soon", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }

                            // Good Button
                            Button(
                                onClick = {
                                    taskViewModel.reviewFlashcard(card, "Good")
                                    isFlipped = false
                                    if (selectedCategoryFilter == "All") currentIdx++
                                    android.widget.Toast.makeText(context, "Card moved to Good bucket 🟡", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🟡 Good", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Normal Space", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }

                            // Easy Button
                            Button(
                                onClick = {
                                    taskViewModel.reviewFlashcard(card, "Easy")
                                    isFlipped = false
                                    if (selectedCategoryFilter == "All") currentIdx++
                                    android.widget.Toast.makeText(context, "Card moved to Easy bucket 🟢", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🟢 Easy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Mastered", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "💡 Tap card to flip & classify into Hard / Good / Easy...",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        } else {
            // ─────────────────────────────────────────────────────────────
            // 2. CARD MANAGER / LIST VIEW MODE
            // ─────────────────────────────────────────────────────────────
            if (filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No cards found in '$selectedCategoryFilter' filter.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCards) { c ->
                        val badgeColor = when (c.difficulty) {
                            "Hard" -> Color(0xFFEF4444)
                            "Easy" -> Color(0xFF10B981)
                            else -> Color(0xFFF59E0B)
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = c.difficulty.uppercase(),
                                            color = badgeColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Delete Card Button
                                    IconButton(
                                        onClick = { cardToDelete = c },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("🗑️", fontSize = 14.sp)
                                    }
                                }

                                Text(
                                    text = "Q: ${c.front}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                Text(
                                    text = "A: ${c.back}",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp
                                )

                                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))

                                // Quick Category Change Pills
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Change Bucket:", color = Color.Gray, fontSize = 11.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { taskViewModel.reviewFlashcard(c, "Hard") },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("🔴 Hard", color = Color(0xFFEF4444), fontSize = 10.sp)
                                        }
                                        TextButton(
                                            onClick = { taskViewModel.reviewFlashcard(c, "Good") },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("🟡 Good", color = Color(0xFFF59E0B), fontSize = 10.sp)
                                        }
                                        TextButton(
                                            onClick = { taskViewModel.reviewFlashcard(c, "Easy") },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("🟢 Easy", color = Color(0xFF10B981), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Flashcard Dialog
    if (showAddCardDialog) {
        var frontText by remember { mutableStateOf("") }
        var backText by remember { mutableStateOf("") }
        var selectedDifficulty by remember { mutableStateOf("Good") }

        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = { Text("Add Flashcard to '${deck.name}'", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = frontText,
                        onValueChange = { frontText = it },
                        label = { Text("Front (Question)") },
                        placeholder = { Text("Enter question or term...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = backText,
                        onValueChange = { backText = it },
                        label = { Text("Back (Answer / Definition)") },
                        placeholder = { Text("Enter answer...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Initial Category:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Hard" to "🔴 Hard", "Good" to "🟡 Good", "Easy" to "🟢 Easy").forEach { (dVal, dLabel) ->
                            val isSel = selectedDifficulty == dVal
                            Surface(
                                color = if (isSel) Color(0xFF6366F1) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF818CF8) else Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f).clickable { selectedDifficulty = dVal }
                            ) {
                                Text(
                                    text = dLabel,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (frontText.isNotBlank() && backText.isNotBlank()) {
                            taskViewModel.addFlashcard(deck.id, frontText.trim(), backText.trim(), selectedDifficulty)
                            android.widget.Toast.makeText(context, "Flashcard saved in database!", android.widget.Toast.LENGTH_SHORT).show()
                            showAddCardDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Card", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCardDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Single Card Confirmation Dialog
    cardToDelete?.let { card ->
        DeleteConfirmationDialog(
            taskTitle = "Flashcard: \"${card.front}\"",
            onConfirm = {
                taskViewModel.deleteFlashcard(card)
                android.widget.Toast.makeText(context, "Flashcard deleted", android.widget.Toast.LENGTH_SHORT).show()
                cardToDelete = null
            },
            onDismiss = { cardToDelete = null }
        )
    }
}

// -------------------------------------------------------------
// SUB-TAB 4: HABIT TRACKER GRID (Styled Circular Pills)
// -------------------------------------------------------------
@Composable
fun HabitsSubTab(taskViewModel: TaskViewModel) {
    val habitsList by taskViewModel.habits.collectAsState()
    var habitInput by remember { mutableStateOf("") }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayStr = remember { sdf.format(Date()) }
    var habitToDelete by remember { mutableStateOf<HabitEntity?>(null) }

    // Enforce streak reset check for all habits when this tab loads
    LaunchedEffect(habitsList) {
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = sdf.format(yesterdayCal.time)

        habitsList.forEach { habit ->
            val dateList = habit.completedDates.split(",").filter { it.isNotBlank() }
            if (dateList.isNotEmpty() && !dateList.contains(todayStr) && !dateList.contains(yesterdayStr)) {
                // User missed yesterday and today -> streak is broken! Auto-reset to 0.
                taskViewModel.resetHabitStreak(habit)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = habitInput,
                onValueChange = { habitInput = it },
                label = { Text("New 21-Day Habit Challenge") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (habitInput.isNotBlank()) {
                        taskViewModel.addHabit(habitInput)
                        habitInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("Start")
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(habitsList) { habit ->
                val completedList = habit.completedDates.split(",").filter { it.isNotBlank() }
                val completedCount = completedList.size
                val isTodayCompleted = completedList.contains(todayStr)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x9A0F172A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.25f), Color(0xFF3B82F6).copy(alpha = 0.05f))),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = habit.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (completedCount >= 21) "🎉 Challenge Completed!" else "Day $completedCount of 21 Challenge",
                                    color = if (completedCount >= 21) Color(0xFF34D399) else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = { habitToDelete = habit }, modifier = Modifier.size(28.dp)) {
                                Text("❌", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { completedCount.toFloat() / 21f },
                            color = if (completedCount >= 21) Color(0xFF10B981) else Color(0xFF6366F1),
                            trackColor = Color(0xFF020617),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 21 Day Grid (3 rows of 7 days)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (row in 0 until 3) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (col in 1..7) {
                                        val dayIdx = (row * 7) + col
                                        val isDone = dayIdx <= completedCount
                                        val isActive = dayIdx == completedCount + 1 && !isTodayCompleted
                                        val isTodayChecked = dayIdx == completedCount && isTodayCompleted

                                        val circleBackground = when {
                                            isDone -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)))
                                            isActive -> Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                                            else -> Brush.linearGradient(listOf(Color(0xFF020617), Color(0xFF020617)))
                                        }

                                        val borderColor = when {
                                            isDone -> Color.Transparent
                                            isActive -> Color(0xFF818CF8)
                                            else -> Color.White.copy(alpha = 0.05f)
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable(enabled = isActive || isTodayChecked) {
                                                    // Toggle check-in status
                                                    taskViewModel.toggleHabitDay(habit, todayStr)
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(circleBackground, CircleShape)
                                                    .border(1.5.dp, borderColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isDone) {
                                                    Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                } else {
                                                    Text(text = "$dayIdx", color = if (isActive) Color(0xFF818CF8) else Color(0xFF475569), fontSize = 10.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    // Delete confirmation dialog
    habitToDelete?.let { habit ->
        DeleteConfirmationDialog(
            taskTitle = habit.name,
            onConfirm = {
                taskViewModel.deleteHabit(habit)
                habitToDelete = null
            },
            onDismiss = { habitToDelete = null }
        )
    }
}

// -------------------------------------------------------------
// SUB-TAB 5: COMPREHENSIVE LEDGER & EXPENSES (English UI, CSV & PDF)
// -------------------------------------------------------------
@Composable
fun FinanceSubTab(taskViewModel: TaskViewModel) {
    val context = LocalContext.current
    val expenses by taskViewModel.expenses.collectAsState()

    var descInput by remember { mutableStateOf("") }
    var amtInput by remember { mutableStateOf("") }
    var typeInput by remember { mutableStateOf("expense") }
    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var dateInput by remember { mutableStateOf(sdfDate.format(Date())) }

    var editExpenseId by remember { mutableStateOf<String?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    // CSV File Picker Launcher for restoring data
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            parseAndImportExpensesCsv(context, it) { importedList ->
                taskViewModel.importExpenses(importedList)
            }
        }
    }

    // Search and Filters states
    var searchQuery by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf("All") }
    var fromDateFilter by remember { mutableStateOf("") }
    var toDateFilter by remember { mutableStateOf("") }

    // Populate dropdown options from parsed months
    val availableMonths = remember(expenses) {
        val months = expenses.map { it.date.take(7) }.distinct().sortedDescending()
        listOf("All") + months
    }

    // Filter calculations (search text + month + from date + to date range)
    val filteredList = expenses.filter { e ->
        val matchesSearch = e.description.contains(searchQuery, ignoreCase = true) ||
                e.amount.toString().contains(searchQuery)
        val matchesMonth = selectedMonth == "All" || e.date.startsWith(selectedMonth)
        val matchesFrom = fromDateFilter.isBlank() || e.date >= fromDateFilter
        val matchesTo = toDateFilter.isBlank() || e.date <= toDateFilter
        matchesSearch && matchesMonth && matchesFrom && matchesTo
    }

    val totalIncome = filteredList.filter { it.type.equals("income", ignoreCase = true) }.sumOf { it.amount }
    val totalExpense = filteredList.filter { it.type.equals("expense", ignoreCase = true) }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {

        // Summary Card with glowing green/red gradients
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            if (balance >= 0)
                                listOf(Color(0xFF10B981).copy(alpha = 0.3f), Color(0xFF0F766E).copy(alpha = 0.05f))
                            else
                                listOf(Color(0xFFEF4444).copy(alpha = 0.3f), Color(0xFF991B1B).copy(alpha = 0.05f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                val headerGradient = if (balance >= 0) {
                    Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF10B981))) // Teal/Emerald
                } else {
                    Brush.linearGradient(listOf(Color(0xFF991B1B), Color(0xFFEF4444))) // Red/Crimson
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerGradient)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Current Ledger Balance", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    Text(
                        text = "₹${"%.2f".format(balance)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total Income: ₹${"%.2f".format(totalIncome)}", color = Color(0xFFD1FAE5), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Total Expense: ₹${"%.2f".format(totalExpense)}", color = Color(0xFFFEE2E2), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Form Section Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (editExpenseId == null) "💸 Add Transaction Entry" else "✏️ Edit Transaction Entry",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    // Date Input with DatePickerDialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = dateInput,
                            onValueChange = { dateInput = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    try { time = sdfDate.parse(dateInput) ?: Date() } catch (e: Exception) { time = Date() }
                                }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val selected = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, day)
                                        }
                                        dateInput = sdfDate.format(selected.time)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("📅 Select", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    // Description Input
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Description") },
                        placeholder = { Text("e.g. Books, Food, Transport, Salary...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Amount Input
                    OutlinedTextField(
                        value = amtInput,
                        onValueChange = { amtInput = it },
                        label = { Text("Amount (Rs. / ₹)") },
                        placeholder = { Text("0.00") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Radio Button Selection for Expense vs Income
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { typeInput = "expense" }
                        ) {
                            RadioButton(
                                selected = typeInput == "expense",
                                onClick = { typeInput = "expense" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Expense", color = if (typeInput == "expense") Color(0xFFEF4444) else Color.LightGray, fontWeight = if (typeInput == "expense") FontWeight.Bold else FontWeight.Normal)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { typeInput = "income" }
                        ) {
                            RadioButton(
                                selected = typeInput == "income",
                                onClick = { typeInput = "income" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Income", color = if (typeInput == "income") Color(0xFF10B981) else Color.LightGray, fontWeight = if (typeInput == "income") FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    // Action Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (editExpenseId != null) {
                            Button(
                                onClick = {
                                    editExpenseId = null
                                    descInput = ""
                                    amtInput = ""
                                    typeInput = "expense"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }

                        Button(
                            onClick = {
                                val amt = amtInput.toDoubleOrNull() ?: 0.0
                                if (descInput.isNotBlank() && amt > 0.0 && dateInput.isNotBlank()) {
                                    val isUpdate = editExpenseId != null
                                    val entryId = editExpenseId ?: UUID.randomUUID().toString()
                                    taskViewModel.addExpense(
                                        ExpenseEntity(
                                            id = entryId,
                                            description = descInput.trim(),
                                            amount = amt,
                                            date = dateInput.trim(),
                                            type = typeInput
                                        )
                                    )
                                    // Clear Form
                                    descInput = ""
                                    amtInput = ""
                                    editExpenseId = null
                                    typeInput = "expense"
                                    Toast.makeText(
                                        context,
                                        if (isUpdate) "Transaction updated in database! ✅" else "Transaction saved in database! ✅",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(context, "Please enter valid description and amount!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (typeInput == "income") Color(0xFF10B981) else Color(0xFF6366F1)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (editExpenseId == null) "Add Entry ➕" else "Save Changes ✏️", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Search Filter, Date Range & Month Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "🔍 Search & Date Range Filters", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search by description or amount") },
                        placeholder = { Text("e.g. Books, 500...", color = Color.Gray, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // From Date & To Date Range Pickers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    try { time = if (fromDateFilter.isNotBlank()) sdfDate.parse(fromDateFilter) ?: Date() else Date() } catch (e: Exception) { time = Date() }
                                }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val sel = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, day)
                                        }
                                        fromDateFilter = sdfDate.format(sel.time)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = if (fromDateFilter.isNotBlank()) 0.5f else 0.15f))
                        ) {
                            Text(
                                text = if (fromDateFilter.isNotBlank()) "📅 From: $fromDateFilter" else "📅 From Date",
                                color = if (fromDateFilter.isNotBlank()) Color(0xFF38BDF8) else Color.LightGray,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    try { time = if (toDateFilter.isNotBlank()) sdfDate.parse(toDateFilter) ?: Date() else Date() } catch (e: Exception) { time = Date() }
                                }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val sel = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, day)
                                        }
                                        toDateFilter = sdfDate.format(sel.time)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = if (toDateFilter.isNotBlank()) 0.5f else 0.15f))
                        ) {
                            Text(
                                text = if (toDateFilter.isNotBlank()) "📅 To: $toDateFilter" else "📅 To Date",
                                color = if (toDateFilter.isNotBlank()) Color(0xFF38BDF8) else Color.LightGray,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Month Picker & Clear All Filters
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var filterMonthExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { filterMonthExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(text = "Month: $selectedMonth", color = Color.White, fontSize = 12.sp)
                            }
                            DropdownMenu(expanded = filterMonthExpanded, onDismissRequest = { filterMonthExpanded = false }) {
                                availableMonths.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = {
                                            selectedMonth = m
                                            filterMonthExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        val hasActiveFilter = searchQuery.isNotBlank() || selectedMonth != "All" || fromDateFilter.isNotBlank() || toDateFilter.isNotBlank()
                        if (hasActiveFilter) {
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedMonth = "All"
                                    fromDateFilter = ""
                                    toDateFilter = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Clear Filters", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // CSV Import / Export & PDF Export Action Row
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "📁 Data Backup, Restore & Export", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Import CSV
                        Button(
                            onClick = {
                                csvImportLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📥 Import CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. Export CSV
                        Button(
                            onClick = {
                                if (filteredList.isEmpty()) {
                                    Toast.makeText(context, "No transactions to export!", Toast.LENGTH_SHORT).show()
                                } else {
                                    exportExpensesToCsv(context, filteredList)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📤 Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 3. Export Multi-Page PDF
                        Button(
                            onClick = {
                                if (filteredList.isEmpty()) {
                                    Toast.makeText(context, "No data to export!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val rangeText = when {
                                        fromDateFilter.isNotBlank() && toDateFilter.isNotBlank() -> "$fromDateFilter to $toDateFilter"
                                        fromDateFilter.isNotBlank() -> "From $fromDateFilter"
                                        toDateFilter.isNotBlank() -> "Until $toDateFilter"
                                        selectedMonth != "All" -> "Month $selectedMonth"
                                        else -> "All Records"
                                    }
                                    exportToPdf(context, filteredList, totalIncome, totalExpense, balance, rangeText)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📄 PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Ledger logs list with Indexed items
        if (filteredList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x331E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🪙 No Transactions Found", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Add entries above or tap '📥 Import CSV' to restore transaction history into the database.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            itemsIndexed(filteredList, key = { _, item -> item.id }) { index, exp ->
                val isIncome = exp.type.equals("income", ignoreCase = true)
                val accentColor = if (isIncome) Color(0xFF28A745) else Color(0xFFDC3545)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
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
                            Text(
                                text = "${index + 1}. ${exp.date} - ${exp.description}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Rs. ${"%.2f".format(exp.amount)} (${exp.type.uppercase()})",
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    descInput = exp.description
                                    amtInput = exp.amount.toString()
                                    dateInput = exp.date
                                    typeInput = exp.type
                                    editExpenseId = exp.id
                                }
                            ) {
                                Text("Edit", color = Color(0xFF818CF8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            TextButton(
                                onClick = { expenseToDelete = exp }
                            ) {
                                Text("Delete", color = Color(0xFFDC3545), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog for ledger entry
    expenseToDelete?.let { exp ->
        DeleteConfirmationDialog(
            taskTitle = "${exp.description} (${if (exp.type == "income") "+" else "-"}₹${exp.amount})",
            onConfirm = {
                taskViewModel.deleteExpense(exp)
                Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }
}

// -------------------------------------------------------------
// CSV Export Function
// -------------------------------------------------------------
fun exportExpensesToCsv(context: Context, expenses: List<ExpenseEntity>) {
    if (expenses.isEmpty()) {
        Toast.makeText(context, "No transactions to export!", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val backupsDir = File(context.getExternalFilesDir(null), "Backups")
        if (!backupsDir.exists()) backupsDir.mkdirs()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(backupsDir, "Expense_Backup_$timeStamp.csv")

        val sb = StringBuilder()
        sb.append("id,date,description,amount,type\n")
        expenses.forEach { e ->
            val escapedDesc = "\"" + e.description.replace("\"", "\"\"") + "\""
            sb.append("${e.id},${e.date},$escapedDesc,${e.amount},${e.type}\n")
        }

        FileOutputStream(file).use { fos ->
            fos.write(sb.toString().toByteArray(Charsets.UTF_8))
        }

        Toast.makeText(context, "CSV exported: ${file.name} 📤", Toast.LENGTH_LONG).show()

        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                this.type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Expense CSV Backup"))
        } catch (e: Exception) {
            // Fallback
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error exporting CSV: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// -------------------------------------------------------------
// CSV Import & Restore Function
// -------------------------------------------------------------
fun parseAndImportExpensesCsv(
    context: Context,
    uri: Uri,
    onImportSuccess: (List<ExpenseEntity>) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines().filter { it.isNotBlank() }
        reader.close()

        if (lines.isEmpty()) {
            Toast.makeText(context, "Selected CSV file is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        val parsedEntities = mutableListOf<ExpenseEntity>()
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdfDate.format(Date())

        val firstLineCols = parseCsvLine(lines[0])

        // Check if first line is header
        val isHeader = firstLineCols.any { col ->
            val lower = col.lowercase().trim()
            lower in listOf("date", "desc", "description", "amount", "amt", "type", "category", "price", "id")
        }

        var headerIndices: Map<String, Int>? = null
        val startIdx = if (isHeader) {
            val map = mutableMapOf<String, Int>()
            firstLineCols.forEachIndexed { index, name ->
                val norm = name.lowercase().trim().replace("\"", "").replace("'", "")
                when {
                    norm.contains("date") -> map["date"] = index
                    norm.contains("desc") || norm.contains("title") || norm.contains("item") || norm.contains("particular") -> map["desc"] = index
                    norm.contains("amount") || norm.contains("amt") || norm.contains("price") || norm.contains("rs") || norm.contains("rupee") || norm.contains("cost") -> map["amount"] = index
                    norm.contains("type") -> map["type"] = index
                    norm == "id" -> map["id"] = index
                }
            }
            headerIndices = map
            1
        } else {
            0
        }

        for (i in startIdx until lines.size) {
            val cols = parseCsvLine(lines[i])
            if (cols.isEmpty()) continue

            var date = todayStr
            var desc = "Expense"
            var amount = 0.0
            var type = "expense"
            var id = UUID.randomUUID().toString()

            if (headerIndices != null) {
                headerIndices["date"]?.let { if (it < cols.size) date = sanitizeDate(cols[it], todayStr) }
                headerIndices["desc"]?.let { if (it < cols.size) desc = cols[it].trim().ifBlank { "Expense" } }
                headerIndices["amount"]?.let { if (it < cols.size) amount = parseCleanAmount(cols[it]) }
                headerIndices["type"]?.let { if (it < cols.size) type = sanitizeType(cols[it]) }
                headerIndices["id"]?.let { if (it < cols.size && cols[it].isNotBlank()) id = cols[it].trim() }
            } else {
                if (cols.size >= 4) {
                    date = sanitizeDate(cols[0], todayStr)
                    desc = cols[1].trim().ifBlank { "Expense" }
                    val col2Num = parseCleanAmount(cols[2])
                    val col3Num = parseCleanAmount(cols[3])
                    if (col2Num > 0 && col3Num == 0.0) {
                        amount = col2Num
                        type = sanitizeType(cols[3])
                    } else {
                        type = sanitizeType(cols[2])
                        amount = if (col3Num > 0) col3Num else col2Num
                    }
                } else if (cols.size == 3) {
                    date = sanitizeDate(cols[0], todayStr)
                    desc = cols[1].trim().ifBlank { "Expense" }
                    amount = parseCleanAmount(cols[2])
                } else if (cols.size == 2) {
                    desc = cols[0].trim().ifBlank { "Expense" }
                    amount = parseCleanAmount(cols[1])
                }
            }

            if (amount > 0.0) {
                parsedEntities.add(
                    ExpenseEntity(
                        id = id,
                        description = desc,
                        amount = amount,
                        date = date,
                        type = type
                    )
                )
            }
        }

        if (parsedEntities.isNotEmpty()) {
            onImportSuccess(parsedEntities)
            Toast.makeText(context, "Successfully restored ${parsedEntities.size} transactions into database! 📥", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "No valid transactions found in CSV file.", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error importing CSV: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun parseCsvLine(line: String): List<String> {
    val tokens = mutableListOf<String>()
    var inQuotes = false
    val sb = StringBuilder()
    for (c in line) {
        when {
            c == '\"' -> inQuotes = !inQuotes
            (c == ',' || c == '\t' || c == ';') && !inQuotes -> {
                tokens.add(sb.toString().trim())
                sb.clear()
            }
            else -> sb.append(c)
        }
    }
    tokens.add(sb.toString().trim())
    return tokens.map { it.replace("^\"|\"$".toRegex(), "").trim() }
}

fun sanitizeDate(raw: String, fallback: String): String {
    val clean = raw.trim().replace("/", "-")
    if (clean.matches(Regex("^\\d{4}-\\d{1,2}-\\d{1,2}$"))) {
        val parts = clean.split("-")
        val year = parts[0]
        val month = parts[1].padStart(2, '0')
        val day = parts[2].padStart(2, '0')
        return "$year-$month-$day"
    }
    if (clean.matches(Regex("^\\d{1,2}-\\d{1,2}-\\d{4}$"))) {
        val parts = clean.split("-")
        val day = parts[0].padStart(2, '0')
        val month = parts[1].padStart(2, '0')
        val year = parts[2]
        return "$year-$month-$day"
    }
    return fallback
}

fun parseCleanAmount(raw: String): Double {
    val cleaned = raw.replace("Rs.", "", ignoreCase = true)
        .replace("Rs", "", ignoreCase = true)
        .replace("₹", "")
        .replace(",", "")
        .replace("$", "")
        .trim()
    return cleaned.toDoubleOrNull() ?: 0.0
}

fun sanitizeType(raw: String): String {
    val lower = raw.lowercase().trim()
    return if (lower.contains("income") || lower.contains("credit") || lower.contains("aamdani") || lower.contains("gain") || lower.contains("salary")) "income" else "expense"
}

// -------------------------------------------------------------
// Multi-Page Dynamic Ultra-Lightweight PDF Generation for Expense Tracker
// -------------------------------------------------------------
fun exportToPdf(
    context: Context,
    expenses: List<ExpenseEntity>,
    totalIncome: Double,
    totalExpense: Double,
    balance: Double,
    dateRangeSubtitle: String = ""
) {
    if (expenses.isEmpty()) {
        Toast.makeText(context, "No transactions to export!", Toast.LENGTH_SHORT).show()
        return
    }

    val pdfDocument = PdfDocument()
    val pageWidth = 595 // Standard A4 points
    val pageHeight = 842

    val titlePaint = Paint().apply {
        textSize = 16f
        isFakeBoldText = true
        color = android.graphics.Color.rgb(15, 23, 42)
    }
    val subtitlePaint = Paint().apply {
        textSize = 9f
        color = android.graphics.Color.rgb(100, 116, 139)
    }
    val headerBgPaint = Paint().apply {
        color = android.graphics.Color.rgb(241, 245, 249)
    }
    val headerTextPaint = Paint().apply {
        textSize = 9.5f
        isFakeBoldText = true
        color = android.graphics.Color.rgb(15, 23, 42)
    }
    val rowTextPaint = Paint().apply {
        textSize = 9f
        color = android.graphics.Color.rgb(51, 65, 85)
    }
    val rowAltBgPaint = Paint().apply {
        color = android.graphics.Color.rgb(248, 250, 252)
    }
    val incomeTextPaint = Paint().apply {
        textSize = 9.5f
        isFakeBoldText = true
        color = android.graphics.Color.rgb(22, 163, 74)
    }
    val expenseTextPaint = Paint().apply {
        textSize = 9.5f
        isFakeBoldText = true
        color = android.graphics.Color.rgb(220, 38, 38)
    }
    val linePaint = Paint().apply {
        color = android.graphics.Color.rgb(226, 232, 240)
        strokeWidth = 0.8f
    }
    val darkLinePaint = Paint().apply {
        color = android.graphics.Color.rgb(148, 163, 184)
        strokeWidth = 1.2f
    }
    val pageNumberPaint = Paint().apply {
        textSize = 8.5f
        color = android.graphics.Color.rgb(148, 163, 184)
    }

    var currentPageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas: Canvas = page.canvas

    fun drawPageHeader(canvas: Canvas, pageNum: Int) {
        // App Title & Report Header
        canvas.drawText("Ledger & Expense Statement", 36f, 38f, titlePaint)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateInfo = if (dateRangeSubtitle.isNotBlank()) "Filter: $dateRangeSubtitle  |  Exported on: ${sdf.format(Date())}" else "Exported on: ${sdf.format(Date())}"
        canvas.drawText(dateInfo, 36f, 52f, subtitlePaint)

        // Column Header Background
        canvas.drawRect(36f, 65f, 559f, 83f, headerBgPaint)
        canvas.drawLine(36f, 65f, 559f, 65f, darkLinePaint)
        canvas.drawLine(36f, 83f, 559f, 83f, darkLinePaint)

        // Column Headers
        val headerY = 77f
        canvas.drawText("#", 42f, headerY, headerTextPaint)
        canvas.drawText("Date", 68f, headerY, headerTextPaint)
        canvas.drawText("Description", 150f, headerY, headerTextPaint)
        canvas.drawText("Type", 380f, headerY, headerTextPaint)
        canvas.drawText("Amount (Rs.)", 470f, headerY, headerTextPaint)
    }

    // Draw first page header
    drawPageHeader(canvas, currentPageNumber)
    var yPos = 100f

    expenses.forEachIndexed { index, exp ->
        // Compact height per row: 19pt (allows ~34 rows per page)
        if (yPos > 780f) {
            canvas.drawText("Page $currentPageNumber", 280f, 820f, pageNumberPaint)
            pdfDocument.finishPage(page)

            currentPageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas

            drawPageHeader(canvas, currentPageNumber)
            yPos = 100f
        }

        // Alternate row background
        if (index % 2 == 1) {
            canvas.drawRect(36f, yPos - 12f, 559f, yPos + 6f, rowAltBgPaint)
        }

        val isInc = exp.type.equals("income", ignoreCase = true)
        val numStr = "${index + 1}"
        val dateStr = exp.date
        val descStr = if (exp.description.length > 36) exp.description.take(33) + "..." else exp.description
        val typeStr = if (isInc) "Income" else "Expense"
        val amtStr = "Rs. ${"%.2f".format(exp.amount)}"

        canvas.drawText(numStr, 42f, yPos, rowTextPaint)
        canvas.drawText(dateStr, 68f, yPos, rowTextPaint)
        canvas.drawText(descStr, 150f, yPos, rowTextPaint)
        canvas.drawText(typeStr, 380f, yPos, if (isInc) incomeTextPaint else expenseTextPaint)
        canvas.drawText(amtStr, 470f, yPos, if (isInc) incomeTextPaint else expenseTextPaint)

        canvas.drawLine(36f, yPos + 6f, 559f, yPos + 6f, linePaint)
        yPos += 19f
    }

    // Check if totals section fits on current page, or create a new page
    if (yPos > 710f) {
        canvas.drawText("Page $currentPageNumber", 280f, 820f, pageNumberPaint)
        pdfDocument.finishPage(page)

        currentPageNumber++
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas

        drawPageHeader(canvas, currentPageNumber)
        yPos = 105f
    } else {
        yPos += 8f
    }

    // Totals Box
    val boxTop = yPos
    val boxBottom = yPos + 75f
    val boxBgPaint = Paint().apply {
        color = android.graphics.Color.rgb(241, 245, 249)
    }
    canvas.drawRect(36f, boxTop, 559f, boxBottom, boxBgPaint)
    canvas.drawLine(36f, boxTop, 559f, boxTop, darkLinePaint)
    canvas.drawLine(36f, boxBottom, 559f, boxBottom, darkLinePaint)
    canvas.drawLine(36f, boxTop, 36f, boxBottom, darkLinePaint)
    canvas.drawLine(559f, boxTop, 559f, boxBottom, darkLinePaint)

    val totalsHeaderPaint = Paint().apply {
        textSize = 11f
        isFakeBoldText = true
        color = android.graphics.Color.rgb(15, 23, 42)
    }
    canvas.drawText("Summary Totals (${expenses.size} Transactions)", 50f, boxTop + 18f, totalsHeaderPaint)

    val summaryIncomePaint = Paint().apply {
        textSize = 10f
        isFakeBoldText = true
        color = android.graphics.Color.rgb(22, 163, 74)
    }
    val summaryExpensePaint = Paint().apply {
        textSize = 10f
        isFakeBoldText = true
        color = android.graphics.Color.rgb(220, 38, 38)
    }
    val summaryBalancePaint = Paint().apply {
        textSize = 10.5f
        isFakeBoldText = true
        color = if (balance >= 0) android.graphics.Color.rgb(22, 163, 74) else android.graphics.Color.rgb(220, 38, 38)
    }

    canvas.drawText("Total Income: Rs. ${"%.2f".format(totalIncome)}", 50f, boxTop + 38f, summaryIncomePaint)
    canvas.drawText("Total Expense: Rs. ${"%.2f".format(totalExpense)}", 230f, boxTop + 38f, summaryExpensePaint)
    canvas.drawText("Net Balance: Rs. ${"%.2f".format(balance)}", 410f, boxTop + 38f, summaryBalancePaint)

    val notesPaint = Paint().apply {
        textSize = 8.5f
        color = android.graphics.Color.rgb(100, 116, 139)
    }
    canvas.drawText("Generated by Student Focus Ledger • Compact Lightweight PDF", 50f, boxTop + 58f, notesPaint)

    // Page number on last page
    canvas.drawText("Page $currentPageNumber", 280f, 820f, pageNumberPaint)
    pdfDocument.finishPage(page)

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "Expense_Report_$timeStamp.pdf"

    // 1. Always save a copy to cache / external reports for instant sharing via FileProvider
    val reportsDir = File(context.cacheDir, "Reports").apply { if (!exists()) mkdirs() }
    val cacheFile = File(reportsDir, fileName)

    try {
        FileOutputStream(cacheFile).use { out ->
            pdfDocument.writeTo(out)
        }

        // 2. Save directly to public Downloads directory (MediaStore on Q+ or DIRECTORY_DOWNLOADS)
        var savedToDownloads = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StudentFocus")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    java.io.FileInputStream(cacheFile).use { input ->
                        input.copyTo(out)
                    }
                }
                savedToDownloads = true
            }
        } else {
            val publicDownloads = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "StudentFocus")
            if (!publicDownloads.exists()) publicDownloads.mkdirs()
            val pubFile = File(publicDownloads, fileName)
            java.io.FileInputStream(cacheFile).use { input ->
                FileOutputStream(pubFile).use { out ->
                    input.copyTo(out)
                }
            }
            savedToDownloads = true
        }

        val sizeKb = (cacheFile.length() / 1024).coerceAtLeast(1)
        val locationMsg = if (savedToDownloads) "Downloads/StudentFocus" else "Documents"
        Toast.makeText(context, "✅ PDF Saved ($sizeKb KB, $currentPageNumber pgs) to $locationMsg!", Toast.LENGTH_LONG).show()

        // 3. Open Share / View Chooser Sheet
        try {
            val shareUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, "Expense & Ledger Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Open / Share Expense PDF ($sizeKb KB)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
    } finally {
        pdfDocument.close()
    }
}

// -------------------------------------------------------------
// SUB-TAB 6: 24-HOUR TIME ROUTINE & AUDIT (Invested vs. Wasted Time)
// -------------------------------------------------------------
@Composable
fun RoutineSubTab(taskViewModel: TaskViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val routineList by taskViewModel.routineActivities.collectAsState()
    val settings by taskViewModel.settings.collectAsState()

    var activityNameInput by remember { mutableStateOf("") }
    var hoursInput by remember { mutableStateOf("1") }
    var minsInput by remember { mutableStateOf("0") }
    var classificationScoreInput by remember { mutableFloatStateOf(1.0f) }
    var scoreTextInput by remember { mutableStateOf("1.0") }
    var categoryInput by remember { mutableStateOf("Study") }
    var notesInput by remember { mutableStateOf("") }
    var editActivityId by remember { mutableStateOf<String?>(null) }
    var activityToDelete by remember { mutableStateOf<RoutineActivityEntity?>(null) }

    // CSV File Picker Launcher for restoring routine data
    val routineImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            parseAndImportRoutineCsv(context, it) { importedList ->
                taskViewModel.importRoutineActivities(importedList)
            }
        }
    }

    val currentMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    val lastReviewedMonth = settings?.find { it.key == "last_routine_review_month" }?.value
    val isMonthlyReviewNeeded = lastReviewedMonth != currentMonth

    // Calculate routine metrics (bounded strictly to 24h = 1440 mins)
    val totalAllocatedMinutes = remember(routineList) { routineList.sumOf { it.durationMinutes } }
    val remainingUnallocatedMinutes = remember(totalAllocatedMinutes) { maxOf(0, 1440 - totalAllocatedMinutes) }

    val usefulMinutes = remember(routineList) {
        routineList.filter { it.classificationScore >= 0.7f }.sumOf { it.durationMinutes }
    }
    val necessaryMinutes = remember(routineList) {
        routineList.filter { it.classificationScore in 0.1f..0.69f }.sumOf { it.durationMinutes }
    }
    val wastedMinutes = remember(routineList) {
        routineList.filter { it.classificationScore == 0.0f }.sumOf { it.durationMinutes }
    }
    val effectiveProductiveMinutes = remember(routineList) {
        routineList.sumOf { (it.durationMinutes * it.classificationScore).toInt() }
    }
    val productivityRate = remember(totalAllocatedMinutes, effectiveProductiveMinutes) {
        if (totalAllocatedMinutes > 0) (effectiveProductiveMinutes * 100) / totalAllocatedMinutes else 0
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Monthly Routine Review Prompt
        if (isMonthlyReviewNeeded) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x336366F1)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🔔", fontSize = 18.sp)
                            Text(
                                text = "Monthly Routine Check-In",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "A new month has started! Review how your 24 hours are divided. Where did you invest time in high-value study vs. where did you waste time? Adjust your routine below to stay focused.",
                            color = Color(0xFFC7D2FE),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Button(
                            onClick = { taskViewModel.acknowledgeMonthlyRoutineReview() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("I've Reviewed My Routine ✓", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. 24-Hour Visual Time Allocation Bar & Metrics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱️ 24-Hour Time Routine Audit",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${totalAllocatedMinutes / 60}h ${totalAllocatedMinutes % 60}m / 24h",
                            color = if (totalAllocatedMinutes > 1440) Color(0xFFEF4444) else Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Segmented Progress Bar (1440 mins max)
                    val usefulFrac = (usefulMinutes.toFloat() / 1440f).coerceIn(0f, 1f)
                    val necFrac = (necessaryMinutes.toFloat() / 1440f).coerceIn(0f, 1f)
                    val wasteFrac = (wastedMinutes.toFloat() / 1440f).coerceIn(0f, 1f)
                    val freeFrac = (remainingUnallocatedMinutes.toFloat() / 1440f).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFF0F172A))
                    ) {
                        if (usefulFrac > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(usefulFrac)
                                    .fillMaxHeight()
                                    .background(Color(0xFF10B981))
                            )
                        }
                        if (necFrac > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(necFrac)
                                    .fillMaxHeight()
                                    .background(Color(0xFFFBBF24))
                            )
                        }
                        if (wasteFrac > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(wasteFrac)
                                    .fillMaxHeight()
                                    .background(Color(0xFFEF4444))
                            )
                        }
                        if (freeFrac > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(freeFrac)
                                    .fillMaxHeight()
                                    .background(Color(0xFF334155))
                            )
                        }
                    }

                    // Legend Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                            Text("Useful (${usefulMinutes / 60}h)", color = Color.White, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFBBF24), CircleShape))
                            Text("Necessary (${necessaryMinutes / 60}h)", color = Color.White, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFEF4444), CircleShape))
                            Text("Waste (${wastedMinutes / 60}h)", color = Color.White, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF334155), CircleShape))
                            Text("Free (${remainingUnallocatedMinutes / 60}h)", color = Color.White, fontSize = 10.sp)
                        }
                    }

                    // Key Metric Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0x3310B981), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+${effectiveProductiveMinutes / 60}h ${effectiveProductiveMinutes % 60}m Study Budget", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0x336366F1), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$productivityRate% Score", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Routine Backup & CSV Restore Action Row
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "📁 Routine Backup & Restore", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Import Routine CSV
                        Button(
                            onClick = {
                                routineImportLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📥 Import CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. Export Routine CSV
                        Button(
                            onClick = {
                                if (routineList.isEmpty()) {
                                    Toast.makeText(context, "No routine activities to export!", Toast.LENGTH_SHORT).show()
                                } else {
                                    exportRoutineToCsv(context, routineList)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📤 Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Quick Load Default Template (if empty)
        if (routineList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💡 Start with a Balanced Routine Template", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "Auto-allocates Sleep (7h), Self Study (4h), College (6h), Workout (2h), Relaxation (2h), and Commute Buffer (3h).",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { taskViewModel.loadDefaultStudentRoutine() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("⚡ Load Student Template", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. Add / Edit Activity Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (editActivityId == null) "➕ Add Routine Activity" else "✏️ Edit Routine Activity",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    // Quick activity templates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("Sleep", 7, 0.0f),
                            Triple("Study", 4, 1.0f),
                            Triple("College", 6, 0.3f),
                            Triple("Gym", 1, 0.7f),
                            Triple("Waste", 2, 0.0f)
                        ).forEach { (tName, tHours, tScore) ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        activityNameInput = tName
                                        hoursInput = tHours.toString()
                                        minsInput = "0"
                                        classificationScoreInput = tScore
                                        scoreTextInput = String.format(Locale.US, "%.1f", tScore)
                                        categoryInput = tName
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = tName, color = Color(0xFF818CF8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Activity Name
                    OutlinedTextField(
                        value = activityNameInput,
                        onValueChange = { activityNameInput = it },
                        label = { Text("Activity Name (e.g. Self Study, Sleep, Gaming)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Duration Pickers: Hours & Minutes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hoursInput,
                            onValueChange = { hoursInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Hours") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = minsInput,
                            onValueChange = { minsInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Minutes") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Classification Type & Direct Usefulness Score Input (0.0 to 1.0)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val scoreDesc = when {
                            classificationScoreInput >= 1.0f -> "🟢 1.0 - Fully Productive / Useful (Deep Study & Coding)"
                            classificationScoreInput >= 0.7f -> "🟢 ${"%.1f".format(Locale.US, classificationScoreInput)} - High Value (Prep & Problem Solving)"
                            classificationScoreInput >= 0.4f -> "🟡 ${"%.1f".format(Locale.US, classificationScoreInput)} - Necessary Routine (College, Fitness, Meals)"
                            classificationScoreInput >= 0.1f -> "🟠 ${"%.1f".format(Locale.US, classificationScoreInput)} - Low Utility (Commute, Chores)"
                            else -> "🔴 0.0 - Time Waste / Distraction (Doomscrolling, Procrastination)"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Usefulness Score (0.0 - 1.0):", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = scoreDesc,
                                color = if (classificationScoreInput == 0f) Color(0xFFEF4444) else Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Direct Manual Typing Input & Steppers Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decrement Button (-0.1)
                            IconButton(
                                onClick = {
                                    val newScore = (kotlin.math.round(classificationScoreInput * 10f) - 1f) / 10f
                                    val clamped = newScore.coerceIn(0f, 1f)
                                    classificationScoreInput = clamped
                                    scoreTextInput = String.format(Locale.US, "%.1f", clamped)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0x9A0F172A), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            ) {
                                Text("-0.1", color = Color(0xFF818CF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Manual Numeric Text Box (User can type 0.2, 0.4, 0.7, 1.0, etc.)
                            OutlinedTextField(
                                value = scoreTextInput,
                                onValueChange = { input ->
                                    scoreTextInput = input
                                    val parsed = input.trim().toFloatOrNull()
                                    if (parsed != null) {
                                        classificationScoreInput = parsed.coerceIn(0f, 1f)
                                    }
                                },
                                label = { Text("Type Score (0.0 - 1.0)") },
                                placeholder = { Text("e.g. 0.2, 0.4, 0.7, 1.0") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            // Increment Button (+0.1)
                            IconButton(
                                onClick = {
                                    val newScore = (kotlin.math.round(classificationScoreInput * 10f) + 1f) / 10f
                                    val clamped = newScore.coerceIn(0f, 1f)
                                    classificationScoreInput = clamped
                                    scoreTextInput = String.format(Locale.US, "%.1f", clamped)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0x9A0F172A), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            ) {
                                Text("+0.1", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Quick Preset Score Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val presets = listOf(
                                0.0f to "🔴 0.0 (Waste)",
                                0.2f to "🟠 0.2 (Low)",
                                0.4f to "🟡 0.4 (Routine)",
                                0.6f to "🟡 0.6 (Medium)",
                                0.8f to "🟢 0.8 (High)",
                                1.0f to "🟢 1.0 (Study)"
                            )
                            items(presets) { (scoreVal, label) ->
                                val isSelected = kotlin.math.abs(classificationScoreInput - scoreVal) < 0.05f
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF6366F1) else Color(0x9A0F172A),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier.clickable {
                                        classificationScoreInput = scoreVal
                                        scoreTextInput = String.format(Locale.US, "%.1f", scoreVal)
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Fixed Smooth Slider
                        Slider(
                            value = classificationScoreInput.coerceIn(0f, 1f),
                            onValueChange = {
                                val rounded = (kotlin.math.round(it * 10f) / 10f).coerceIn(0f, 1f)
                                classificationScoreInput = rounded
                                scoreTextInput = String.format(Locale.US, "%.1f", rounded)
                            },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF6366F1),
                                activeTrackColor = Color(0xFF6366F1),
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        val parsedDuration = ((hoursInput.toIntOrNull() ?: 0) * 60) + (minsInput.toIntOrNull() ?: 0)
                        val contribMins = (parsedDuration * classificationScoreInput).toInt()
                        Text(
                            text = "⚡ Adds +$contribMins mins ($parsedDuration mins total × ${"%.1f".format(Locale.US, classificationScoreInput)}) to your daily study budget.",
                            color = Color(0xFF818CF8),
                            fontSize = 11.sp
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (editActivityId != null) {
                            Button(
                                onClick = {
                                    editActivityId = null
                                    activityNameInput = ""
                                    hoursInput = "1"
                                    minsInput = "0"
                                    classificationScoreInput = 1.0f
                                    scoreTextInput = "1.0"
                                    notesInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }

                        val parsedDuration = ((hoursInput.toIntOrNull() ?: 0) * 60) + (minsInput.toIntOrNull() ?: 0)

                        Button(
                            onClick = {
                                if (activityNameInput.isNotBlank() && parsedDuration > 0) {
                                    val isUpdate = editActivityId != null
                                    taskViewModel.addRoutineActivity(
                                        RoutineActivityEntity(
                                            id = editActivityId ?: UUID.randomUUID().toString(),
                                            activityName = activityNameInput.trim(),
                                            durationMinutes = parsedDuration,
                                            classificationScore = classificationScoreInput,
                                            category = categoryInput,
                                            notes = notesInput.trim().ifBlank { null }
                                        )
                                    )
                                    editActivityId = null
                                    activityNameInput = ""
                                    hoursInput = "1"
                                    minsInput = "0"
                                    classificationScoreInput = 1.0f
                                    scoreTextInput = "1.0"
                                    notesInput = ""
                                    android.widget.Toast.makeText(
                                        context,
                                        if (isUpdate) "Routine activity updated in database! ✅" else "Routine activity saved in database! ✅",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = activityNameInput.isNotBlank() && parsedDuration > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (editActivityId == null) "Save Activity ➕" else "Update Activity ✏️", fontWeight = FontWeight.Bold)
                        }
                    }

                    val parsedDuration = ((hoursInput.toIntOrNull() ?: 0) * 60) + (minsInput.toIntOrNull() ?: 0)
                    if (totalAllocatedMinutes > 1440) {
                        Text(
                            text = "⚠️ Total allocated (${totalAllocatedMinutes / 60}h ${totalAllocatedMinutes % 60}m) exceeds 24 hours. You can adjust activity durations anytime.",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 5. Configured Routine Activities List
        if (routineList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x331E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📋 No Routine Activities Added Yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Create custom activities above or load the standard 24-hour student routine template.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Button(
                            onClick = {
                                taskViewModel.loadDefaultStudentRoutine()
                                android.widget.Toast.makeText(context, "Default student routine loaded! 🔄", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🔄 Load Standard 24h Routine", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        items(routineList) { act ->
            val actHours = act.durationMinutes / 60
            val actMins = act.durationMinutes % 60
            val contribMins = (act.durationMinutes * act.classificationScore).toInt()
            val badgeColor = when {
                act.classificationScore >= 0.7f -> Color(0xFF10B981)
                act.classificationScore in 0.1f..0.69f -> Color(0xFFFBBF24)
                else -> Color(0xFFEF4444)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = act.activityName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Box(
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (act.classificationScore == 0f) "WASTE (0.0)" else "SCORE ${"%.1f".format(act.classificationScore)}",
                                    color = badgeColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⏱ Duration: ${if (actHours > 0) "${actHours}h " else ""}${actMins}m",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                            Text("•", color = Color.Gray, fontSize = 11.sp)
                            Text(
                                text = if (contribMins > 0) "+${contribMins}m study budget" else "0m study budget",
                                color = if (contribMins > 0) Color(0xFF38BDF8) else Color(0xFFF87171),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                activityNameInput = act.activityName
                                hoursInput = (act.durationMinutes / 60).toString()
                                minsInput = (act.durationMinutes % 60).toString()
                                classificationScoreInput = act.classificationScore
                                scoreTextInput = String.format(Locale.US, "%.1f", act.classificationScore)
                                categoryInput = act.category
                                notesInput = act.notes ?: ""
                                editActivityId = act.id
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("✏️", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = { activityToDelete = act },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("❌", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    activityToDelete?.let { act ->
        DeleteConfirmationDialog(
            taskTitle = "${act.activityName} (${act.durationMinutes / 60}h ${act.durationMinutes % 60}m)",
            onConfirm = {
                taskViewModel.deleteRoutineActivity(act)
                android.widget.Toast.makeText(context, "Activity deleted", android.widget.Toast.LENGTH_SHORT).show()
                activityToDelete = null
            },
            onDismiss = { activityToDelete = null }
        )
    }
}

// -------------------------------------------------------------
// Routine CSV Export Function
// -------------------------------------------------------------
fun exportRoutineToCsv(context: Context, activities: List<RoutineActivityEntity>) {
    if (activities.isEmpty()) {
        Toast.makeText(context, "No routine activities to export!", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val backupsDir = File(context.getExternalFilesDir(null), "Backups")
        if (!backupsDir.exists()) backupsDir.mkdirs()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(backupsDir, "Routine_Backup_$timeStamp.csv")

        val sb = StringBuilder()
        sb.append("id,activityName,durationMinutes,classificationScore,category,notes\n")
        activities.forEach { a ->
            val escapedName = "\"" + a.activityName.replace("\"", "\"\"") + "\""
            val escapedNotes = "\"" + (a.notes ?: "").replace("\"", "\"\"") + "\""
            sb.append("${a.id},$escapedName,${a.durationMinutes},${a.classificationScore},${a.category},$escapedNotes\n")
        }

        FileOutputStream(file).use { fos ->
            fos.write(sb.toString().toByteArray(Charsets.UTF_8))
        }

        Toast.makeText(context, "Routine CSV exported: ${file.name} 📤", Toast.LENGTH_LONG).show()

        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                this.type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Routine CSV Backup"))
        } catch (e: Exception) {
            // Fallback
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error exporting Routine CSV: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// -------------------------------------------------------------
// Routine CSV Import & Restore Function
// -------------------------------------------------------------
fun parseAndImportRoutineCsv(
    context: Context,
    uri: Uri,
    onImportSuccess: (List<RoutineActivityEntity>) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines().filter { it.isNotBlank() }
        reader.close()

        if (lines.isEmpty()) {
            Toast.makeText(context, "Selected CSV file is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        val parsedEntities = mutableListOf<RoutineActivityEntity>()
        val firstLineCols = parseCsvLine(lines[0])

        val isHeader = firstLineCols.any { col ->
            val lower = col.lowercase().trim()
            lower in listOf("activity", "activityname", "duration", "durationminutes", "score", "classificationscore", "category", "notes", "id")
        }

        var headerIndices: Map<String, Int>? = null
        val startIdx = if (isHeader) {
            val map = mutableMapOf<String, Int>()
            firstLineCols.forEachIndexed { index, name ->
                val norm = name.lowercase().trim().replace("\"", "").replace("'", "")
                when {
                    norm.contains("activity") || norm.contains("title") || norm.contains("name") -> map["name"] = index
                    norm.contains("duration") || norm.contains("min") || norm.contains("time") -> map["duration"] = index
                    norm.contains("score") || norm.contains("utility") || norm.contains("classification") -> map["score"] = index
                    norm.contains("cat") -> map["category"] = index
                    norm.contains("note") || norm.contains("desc") -> map["notes"] = index
                    norm == "id" -> map["id"] = index
                }
            }
            headerIndices = map
            1
        } else {
            0
        }

        for (i in startIdx until lines.size) {
            val cols = parseCsvLine(lines[i])
            if (cols.isEmpty()) continue

            var name = "Activity"
            var duration = 60
            var score = 1.0f
            var category = "Study"
            var notes: String? = null
            var id = UUID.randomUUID().toString()

            if (headerIndices != null) {
                headerIndices["name"]?.let { if (it < cols.size && cols[it].isNotBlank()) name = cols[it].trim() }
                headerIndices["duration"]?.let { if (it < cols.size) duration = cols[it].filter { c -> c.isDigit() }.toIntOrNull() ?: 60 }
                headerIndices["score"]?.let { if (it < cols.size) score = cols[it].toFloatOrNull() ?: 1.0f }
                headerIndices["category"]?.let { if (it < cols.size && cols[it].isNotBlank()) category = cols[it].trim() }
                headerIndices["notes"]?.let { if (it < cols.size && cols[it].isNotBlank()) notes = cols[it].trim() }
                headerIndices["id"]?.let { if (it < cols.size && cols[it].isNotBlank()) id = cols[it].trim() }
            } else {
                if (cols.isNotEmpty() && cols[0].isNotBlank()) name = cols[0].trim()
                if (cols.size > 1) duration = cols[1].filter { c -> c.isDigit() }.toIntOrNull() ?: 60
                if (cols.size > 2) score = cols[2].toFloatOrNull() ?: 1.0f
                if (cols.size > 3 && cols[3].isNotBlank()) category = cols[3].trim()
                if (cols.size > 4 && cols[4].isNotBlank()) notes = cols[4].trim()
            }

            if (name.isNotBlank() && duration > 0) {
                parsedEntities.add(
                    RoutineActivityEntity(
                        id = id,
                        activityName = name,
                        durationMinutes = duration,
                        classificationScore = score.coerceIn(0.0f, 1.0f),
                        category = category,
                        notes = notes
                    )
                )
            }
        }

        if (parsedEntities.isNotEmpty()) {
            onImportSuccess(parsedEntities)
            Toast.makeText(context, "Successfully restored ${parsedEntities.size} routine activities into database! 📥", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "No valid routine activities found in CSV file.", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error importing Routine CSV: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// -------------------------------------------------------------
// SUB-TAB 7: APP USAGE & SCREEN TIME WATCH HISTORY
// -------------------------------------------------------------
@Composable
fun AppUsageSubTab(taskViewModel: TaskViewModel) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeSdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val displayDateSdf = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val todayDateStr = remember { sdf.format(Date()) }

    var selectedDateStr by remember { mutableStateOf(todayDateStr) }
    var hasUsagePermission by remember { mutableStateOf(AppUsageHelper.hasUsageStatsPermission(context)) }
    var hasAccessibilityPermission by remember { mutableStateOf(AppUsageHelper.hasAccessibilityPermission(context)) }
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var activeViewMode by remember { mutableStateOf("apps") } // "apps", "timeline", "insights"
    var showInspectorDialog by remember { mutableStateOf(false) }
    var showPersonalModelDialog by remember { mutableStateOf(false) }

    // Fingerprint Security Lock State
    var isSubTabUnlocked by rememberSaveable { mutableStateOf(false) }
    var biometricErrorMsg by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    fun triggerFingerprintAuth() {
        val fragmentActivity = context as? FragmentActivity ?: return
        val biometricManager = BiometricManager.from(fragmentActivity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(fragmentActivity)
                val prompt = BiometricPrompt(
                    fragmentActivity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            isAuthenticating = false
                            isSubTabUnlocked = true
                            biometricErrorMsg = null
                        }

                        override fun onAuthenticationError(code: Int, msg: CharSequence) {
                            isAuthenticating = false
                            biometricErrorMsg = msg.toString()
                        }

                        override fun onAuthenticationFailed() {
                            // Fingerprint not recognised — prompt stays open
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Activity Intelligence")
                    .setSubtitle("Fingerprint verification required")
                    .setDescription("Scan your registered fingerprint to view in-app usage & activity timeline")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(authenticators)
                    .build()

                isAuthenticating = true
                biometricErrorMsg = null
                prompt.authenticate(promptInfo)
            }
            else -> {
                val fallbackAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                if (biometricManager.canAuthenticate(fallbackAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
                    val executor = ContextCompat.getMainExecutor(fragmentActivity)
                    val prompt = BiometricPrompt(
                        fragmentActivity,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                isAuthenticating = false
                                isSubTabUnlocked = true
                                biometricErrorMsg = null
                            }
                            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                                isAuthenticating = false
                                biometricErrorMsg = msg.toString()
                            }
                            override fun onAuthenticationFailed() {}
                        }
                    )
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock Activity Intelligence")
                        .setSubtitle("Authenticate with Device Security / Fingerprint")
                        .setAllowedAuthenticators(fallbackAuthenticators)
                        .build()
                    isAuthenticating = true
                    prompt.authenticate(promptInfo)
                } else {
                    isSubTabUnlocked = true
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!isSubTabUnlocked) {
            kotlinx.coroutines.delay(200)
            triggerFingerprintAuth()
        }
    }

    // Dialog state for editing category
    var editingActivityTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) } // (packageName, contentTitle, currentCategory)
    var selectedActivityForDetail by remember { mutableStateOf<com.ai_assistant.studentfocus.models.AppActivitySessionEntity?>(null) }

    // Collect usage stats from Room DB for selected date
    val dbUsageFlow = remember(selectedDateStr) { taskViewModel.getAppUsageForDate(selectedDateStr) }
    val dbUsageList by dbUsageFlow.collectAsState(initial = emptyList())

    // Collect In-App Activity Sessions from Room DB for selected date
    val dbSessionsFlow = remember(selectedDateStr) { taskViewModel.getActivitySessionsForDate(selectedDateStr) }
    val dbSessionsList by dbSessionsFlow.collectAsState(initial = emptyList())

    // Observe precomputed Personal Behavior Profile from ViewModel (Zero computation on Main UI Thread)
    val personalProfile by taskViewModel.personalProfileState.collectAsState()

    // Category overrides from Room DB
    val categoryOverridesList by taskViewModel.allCategoryOverrides.collectAsState()
    val userOverridesMap = remember(categoryOverridesList) {
        categoryOverridesList.associate { it.targetKey to it.customCategory }
    }

    // Fetch and sync system usage stats for the date into Room DB
    val syncUsageForDate = rememberUpdatedState { dateStr: String ->
        hasUsagePermission = AppUsageHelper.hasUsageStatsPermission(context)
        hasAccessibilityPermission = AppUsageHelper.hasAccessibilityPermission(context)

        if (hasUsagePermission) {
            isRefreshing = true
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val systemList = AppUsageHelper.queryAppUsageStatsForDay(context, dateStr)
                    if (systemList.isNotEmpty()) {
                        taskViewModel.saveAppUsageList(systemList)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isRefreshing = false
                }
            }
        }
    }

    LaunchedEffect(selectedDateStr, isSubTabUnlocked) {
        hasUsagePermission = AppUsageHelper.hasUsageStatsPermission(context)
        hasAccessibilityPermission = AppUsageHelper.hasAccessibilityPermission(context)
        syncUsageForDate.value(selectedDateStr)
        taskViewModel.refreshPersonalBehaviorProfile()

        // Continuous Live-Sync Loop: Auto-updates stats every 10 seconds while screen is open
        if (isSubTabUnlocked) {
            val todayDateStr = sdf.format(Date())
            while (isActive) {
                kotlinx.coroutines.delay(10000L)
                if (selectedDateStr == todayDateStr) {
                    hasUsagePermission = AppUsageHelper.hasUsageStatsPermission(context)
                    hasAccessibilityPermission = AppUsageHelper.hasAccessibilityPermission(context)
                    syncUsageForDate.value(selectedDateStr)
                }
            }
        }
    }

    // Automatically re-verify permissions as soon as user returns from Android Settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = AppUsageHelper.hasUsageStatsPermission(context)
                hasAccessibilityPermission = AppUsageHelper.hasAccessibilityPermission(context)
                syncUsageForDate.value(selectedDateStr)
                taskViewModel.refreshPersonalBehaviorProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val parsedSelectedDate = remember(selectedDateStr) {
        try { sdf.parse(selectedDateStr) ?: Date() } catch (e: Exception) { Date() }
    }

    // Clean, user-facing app and activity datasets (strictly excluding system services, launcher, OEM UI daemons)
    val validUsageList = remember(dbUsageList) {
        dbUsageList.filter { com.ai_assistant.studentfocus.utils.AppUsageClassifier.isUserFacingApp(context, it.packageName) }
    }

    // Authoritatively sanitized, non-overlapping, and hierarchically bounded sessions
    val sanitizedSessionsData = remember(dbSessionsList, validUsageList) {
        val filtered = dbSessionsList.filter { com.ai_assistant.studentfocus.utils.AppUsageClassifier.isUserFacingApp(context, it.packageName) }
        AppUsageHelper.sanitizeAndRepairActivitySessions(filtered, validUsageList)
    }
    val validSessionsList = sanitizedSessionsData.first
    val diagnosticReport = sanitizedSessionsData.second
    val goalMetrics = remember(validSessionsList) {
        AppUsageHelper.calculateGoalAlignmentMetrics(validSessionsList)
    }

    // Merge in-app activity sessions into app breakdown
    val sessionsByApp = remember(validSessionsList) {
        validSessionsList.groupBy { it.packageName }
    }

    fun formatDurationSmart(millis: Long): String {
        if (millis <= 0L) return "0m"
        val hours = millis / (1000 * 60 * 60)
        val mins = (millis / (1000 * 60)) % 60
        val secs = (millis / 1000) % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 && mins == 0L -> "${hours}h"
            mins > 0 && secs > 0 && mins < 5 -> "${mins}m ${secs}s"
            mins > 0 -> "${mins}m"
            secs > 0 -> "${secs}s"
            else -> "< 1s"
        }
    }

    val totalTimeMillis = remember(validUsageList, validSessionsList) {
        val usageSum = validUsageList.sumOf { it.totalTimeMillis }
        val sessionsSum = validSessionsList.sumOf { it.durationMillis }
        maxOf(usageSum, sessionsSum)
    }
    val totalHours = totalTimeMillis / (1000 * 60 * 60)
    val totalMins = (totalTimeMillis / (1000 * 60)) % 60

    // Categorized Time Calculation based on both In-App Activities & High-Level Apps
    val learningMillis = remember(validSessionsList, validUsageList, userOverridesMap) {
        val sessionLearning = validSessionsList.filter { it.category == com.ai_assistant.studentfocus.utils.ActivityClassifier.CAT_LEARNING || it.category == com.ai_assistant.studentfocus.utils.ActivityClassifier.CAT_CODING }.sumOf { it.durationMillis }
        val appLearning = validUsageList.filter { it.category == "Study" && it.packageName !in sessionsByApp.keys }.sumOf { it.totalTimeMillis }
        sessionLearning + appLearning
    }
    val productivityMillis = remember(validSessionsList, validUsageList) {
        val sessionProd = validSessionsList.filter { it.category == com.ai_assistant.studentfocus.utils.ActivityClassifier.CAT_PRODUCTIVITY }.sumOf { it.durationMillis }
        sessionProd
    }
    val productiveTotalMillis = learningMillis + productivityMillis
    val productiveHours = productiveTotalMillis / (1000 * 60 * 60)
    val productiveMins = (productiveTotalMillis / (1000 * 60)) % 60

    val entertainmentMillis = remember(validSessionsList, validUsageList) {
        val sessionEnt = validSessionsList.filter { it.category == com.ai_assistant.studentfocus.utils.ActivityClassifier.CAT_ENTERTAINMENT }.sumOf { it.durationMillis }
        val appEnt = validUsageList.filter { it.category == "Video" && it.packageName !in sessionsByApp.keys }.sumOf { it.totalTimeMillis }
        sessionEnt + appEnt
    }
    val entertainmentHours = entertainmentMillis / (1000 * 60 * 60)
    val entertainmentMins = (entertainmentMillis / (1000 * 60)) % 60

    val communicationMillis = remember(validSessionsList, validUsageList) {
        val sessionComm = validSessionsList.filter { it.category == com.ai_assistant.studentfocus.utils.ActivityClassifier.CAT_COMMUNICATION }.sumOf { it.durationMillis }
        val appComm = validUsageList.filter { (it.category == "Social" && (it.packageName.contains("whatsapp") || it.packageName.contains("telegram"))) && it.packageName !in sessionsByApp.keys }.sumOf { it.totalTimeMillis }
        sessionComm + appComm
    }
    val communicationHours = communicationMillis / (1000 * 60 * 60)
    val communicationMins = (communicationMillis / (1000 * 60)) % 60

    val socialMillis = remember(validSessionsList, validUsageList) {
        val sessionSoc = validSessionsList.filter { it.category == com.ai_assistant.studentfocus.utils.ActivityClassifier.CAT_SOCIAL }.sumOf { it.durationMillis }
        val appSoc = validUsageList.filter { it.category == "Social" && it.packageName !in sessionsByApp.keys && !it.packageName.contains("whatsapp") && !it.packageName.contains("telegram") }.sumOf { it.totalTimeMillis }
        sessionSoc + appSoc
    }

    val gamingMillis = remember(validSessionsList, validUsageList) {
        val sessionGame = validSessionsList.filter { it.category == com.ai_assistant.studentfocus.utils.ActivityClassifier.CAT_GAMING }.sumOf { it.durationMillis }
        val appGame = validUsageList.filter { it.category == "Gaming" && it.packageName !in sessionsByApp.keys }.sumOf { it.totalTimeMillis }
        sessionGame + appGame
    }

    val otherMillis = remember(totalTimeMillis, productiveTotalMillis, entertainmentMillis, communicationMillis, socialMillis, gamingMillis) {
        maxOf(0L, totalTimeMillis - (productiveTotalMillis + entertainmentMillis + communicationMillis + socialMillis + gamingMillis))
    }

    // Filtered list of apps (Excluding all system processes)
    val filteredAppList = remember(validUsageList, validSessionsList, selectedCategoryFilter, searchQuery, userOverridesMap) {
        val allPkgs = (validUsageList.map { it.packageName } + validSessionsList.map { it.packageName })
            .filter { com.ai_assistant.studentfocus.utils.AppUsageClassifier.isUserFacingApp(context, it) }
            .distinct()

        allPkgs.mapNotNull { pkg ->
            val usage = validUsageList.find { it.packageName == pkg }
            val sessions = validSessionsList.filter { it.packageName == pkg }
            val appName = usage?.appName ?: sessions.firstOrNull()?.appName ?: pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            val duration = maxOf(usage?.totalTimeMillis ?: 0L, sessions.sumOf { it.durationMillis })
            val category = userOverridesMap[pkg] ?: usage?.category ?: sessions.firstOrNull()?.category ?: "Other"

            if (duration < 3000L) return@mapNotNull null // Ignore < 3s micro glances

            val matchesCategory = when (selectedCategoryFilter) {
                "All" -> true
                "Learning" -> category == "Learning" || category == "Study" || category == "Coding"
                "Coding" -> category == "Coding"
                "Productivity" -> category == "Productivity"
                "Communication" -> category == "Communication"
                "Entertainment" -> category == "Entertainment" || category == "Video"
                "Social" -> category == "Social" || category == "Social Media"
                "Gaming" -> category == "Gaming"
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                appName.contains(searchQuery, ignoreCase = true) ||
                        pkg.contains(searchQuery, ignoreCase = true) ||
                        sessions.any { it.contentTitle.contains(searchQuery, ignoreCase = true) }
            }

            if (matchesCategory && matchesSearch) {
                Triple(pkg, appName, duration) to Pair(category, sessions)
            } else null
        }.sortedByDescending { it.first.third }
    }

    // Filtered timeline sessions (Excluding all system processes)
    val filteredTimelineSessions = remember(validSessionsList, searchQuery, selectedCategoryFilter) {
        validSessionsList.filter { session ->
            if (!com.ai_assistant.studentfocus.utils.AppUsageClassifier.isUserFacingApp(context, session.packageName)) return@filter false

            val matchesCategory = when (selectedCategoryFilter) {
                "All" -> true
                "Learning" -> session.category == "Learning" || session.category == "Coding"
                "Coding" -> session.category == "Coding"
                "Productivity" -> session.category == "Productivity"
                "Communication" -> session.category == "Communication"
                "Entertainment" -> session.category == "Entertainment"
                "Social" -> session.category == "Social Media"
                "Gaming" -> session.category == "Gaming"
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                session.appName.contains(searchQuery, ignoreCase = true) ||
                        session.contentTitle.contains(searchQuery, ignoreCase = true) ||
                        session.activityType.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }.sortedByDescending { it.startTime }
    }

    if (!isSubTabUnlocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Fingerprint Glow Icon
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(Color(0xFF6366F1).copy(alpha = 0.4f), Color(0x006366F1))
                                ),
                                shape = CircleShape
                            )
                            .border(2.dp, Color(0xFF818CF8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👆", fontSize = 42.sp)
                    }

                    Text(
                        text = "Activity Intelligence Locked",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "This section tracks granular in-app activities, videos, and study sessions. Scan your fingerprint to verify your identity.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    if (biometricErrorMsg != null) {
                        Surface(
                            color = Color(0x33EF4444),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "⚠️ $biometricErrorMsg",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { triggerFingerprintAuth() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "🔓 Scan Fingerprint to Unlock",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Date Navigation & Calendar Picker + Inspector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    time = parsedSelectedDate
                                    add(Calendar.DAY_OF_MONTH, -1)
                                }
                                selectedDateStr = sdf.format(cal.time)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("◀", color = Color(0xFF818CF8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable {
                                    val cal = Calendar.getInstance().apply { time = parsedSelectedDate }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, day)
                                            }
                                            selectedDateStr = sdf.format(newCal.time)
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("📅", fontSize = 13.sp)
                                Text(
                                    text = if (selectedDateStr == todayDateStr) "Today (${displayDateSdf.format(parsedSelectedDate)})" else displayDateSdf.format(parsedSelectedDate),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("▾", color = Color(0xFF818CF8), fontSize = 11.sp)
                            }
                        }

                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    time = parsedSelectedDate
                                    add(Calendar.DAY_OF_MONTH, 1)
                                }
                                selectedDateStr = sdf.format(cal.time)
                            },
                            enabled = selectedDateStr != todayDateStr,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "▶",
                                color = if (selectedDateStr != todayDateStr) Color(0xFF818CF8) else Color(0xFF475569),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { showInspectorDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("🔍", fontSize = 15.sp)
                        }

                        IconButton(
                            onClick = { exportAppUsageToCsv(context, selectedDateStr, validUsageList) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("📤", fontSize = 15.sp)
                        }

                        IconButton(
                            onClick = { isSubTabUnlocked = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("🔒", fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // 2. Permission Banner: Accessibility Service (for deep in-app detection) & Usage Access
        if (!hasAccessibilityPermission || !hasUsagePermission) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🧠", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "Enable Deep In-App Activity Intelligence",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Detect YouTube video titles, Reels vs DMs, and website topics on-device.",
                                    color = Color(0xFFA5B4FC),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!hasAccessibilityPermission) {
                                Button(
                                    onClick = { AppUsageHelper.openAccessibilitySettings(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("⚙️ Enable Detection", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (!hasUsagePermission) {
                                Button(
                                    onClick = { AppUsageHelper.openUsageAccessSettings(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("🔐 Grant Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Refresh / Verify Permission Button
                        Button(
                            onClick = {
                                hasUsagePermission = AppUsageHelper.hasUsageStatsPermission(context)
                                hasAccessibilityPermission = AppUsageHelper.hasAccessibilityPermission(context)
                                if (hasUsagePermission && hasAccessibilityPermission) {
                                    Toast.makeText(context, "Permissions Active! Banner Removed ✅", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Checking permissions...", Toast.LENGTH_SHORT).show()
                                }
                                syncUsageForDate.value(selectedDateStr)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("🔄 Refresh Status (Check Permissions)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // 3. Screen Time & Multi-Level Goal Alignment Hero Card
        item {
            val totalGoalAlignedMillis = goalMetrics.goalAlignedMillis + goalMetrics.highlyRelevantMillis
            val goalAlignedHours = totalGoalAlignedMillis / (1000 * 60 * 60)
            val goalAlignedMins = (totalGoalAlignedMillis / (1000 * 60)) % 60

            val posHours = goalMetrics.possiblyRelevantMillis / (1000 * 60 * 60)
            val posMins = (goalMetrics.possiblyRelevantMillis / (1000 * 60)) % 60

            val offHours = goalMetrics.offGoalMillis / (1000 * 60 * 60)
            val offMins = (goalMetrics.offGoalMillis / (1000 * 60)) % 60

            val unkAndNeutralHours = (goalMetrics.neutralMillis + goalMetrics.unknownMillis) / (1000 * 60 * 60)
            val unkAndNeutralMins = ((goalMetrics.neutralMillis + goalMetrics.unknownMillis) / (1000 * 60)) % 60

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🎯 Estimated Goal Alignment",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "activity & registered goals",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }

                        // Total active screen time badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${formatDurationSmart(totalTimeMillis)} Active",
                                color = Color(0xFF818CF8),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // 6-Color Multi-Level Goal Alignment Split Bar
                    if (totalTimeMillis > 0) {
                        val totalActiveF = totalTimeMillis.toFloat()
                        val alignedFrac = (goalMetrics.goalAlignedMillis.toFloat() / totalActiveF).coerceIn(0f, 1f)
                        val highFrac = (goalMetrics.highlyRelevantMillis.toFloat() / totalActiveF).coerceIn(0f, 1f)
                        val posFrac = (goalMetrics.possiblyRelevantMillis.toFloat() / totalActiveF).coerceIn(0f, 1f)
                        val neutralFrac = (goalMetrics.neutralMillis.toFloat() / totalActiveF).coerceIn(0f, 1f)
                        val offFrac = (goalMetrics.offGoalMillis.toFloat() / totalActiveF).coerceIn(0f, 1f)
                        val unkFrac = (goalMetrics.unknownMillis.toFloat() / totalActiveF).coerceIn(0f, 1f)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color(0xFF0F172A))
                            ) {
                                if (alignedFrac > 0f) Box(modifier = Modifier.weight(alignedFrac).fillMaxHeight().background(Color(0xFF10B981)))
                                if (highFrac > 0f) Box(modifier = Modifier.weight(highFrac).fillMaxHeight().background(Color(0xFF14B8A6)))
                                if (posFrac > 0f) Box(modifier = Modifier.weight(posFrac).fillMaxHeight().background(Color(0xFFF59E0B)))
                                if (neutralFrac > 0f) Box(modifier = Modifier.weight(neutralFrac).fillMaxHeight().background(Color(0xFF38BDF8)))
                                if (offFrac > 0f) Box(modifier = Modifier.weight(offFrac).fillMaxHeight().background(Color(0xFFEF4444)))
                                if (unkFrac > 0f) Box(modifier = Modifier.weight(unkFrac).fillMaxHeight().background(Color(0xFF64748B)))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🎯 ${goalMetrics.goalAlignmentRate}% Goal Alignment Rate",
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "📊 Coverage: ${goalMetrics.classificationCoverage}%",
                                    color = Color(0xFFA5B4FC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 4 Stat Metric Boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Goal-Aligned
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0x2010B981), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("🎯 Aligned", color = Color(0xFF34D399), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(formatDurationSmart(totalGoalAlignedMillis), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Goals & Code", color = Color(0xFF10B981), fontSize = 8.sp)
                            }
                        }

                        // 2. Possibly Relevant
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0x20F59E0B), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("🟡 Relevant", color = Color(0xFFFBBF24), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(formatDurationSmart(goalMetrics.possiblyRelevantMillis), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Docs & Web", color = Color(0xFFF59E0B), fontSize = 8.sp)
                            }
                        }

                        // 3. Off-Goal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0x20EF4444), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("⚠️ Off-Goal", color = Color(0xFFF87171), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(formatDurationSmart(goalMetrics.offGoalMillis), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Entertainment", color = Color(0xFFEF4444), fontSize = 8.sp)
                            }
                        }

                        // 4. Neutral / Unknown
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0x2064748B), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF64748B).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("⚪ Neutral/Other", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(formatDurationSmart(goalMetrics.neutralMillis + goalMetrics.unknownMillis), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Chats & Misc", color = Color(0xFF94A3B8), fontSize = 8.sp)
                            }
                        }
                    }

                    // Top Goals & Topics Chips
                    if (goalMetrics.topGoalsWorkedOn.isNotEmpty() || goalMetrics.topTopicsDetected.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🏷️ Active Focus Topics Today:", color = Color(0xFFA5B4FC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val topPills = (goalMetrics.topGoalsWorkedOn.map { "🎯 ${it.first}" } + goalMetrics.topTopicsDetected.map { "💡 ${it.first}" }).take(3)
                                topPills.forEach { pill ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F172A),
                                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = pill,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // On-Device AI Focus Insights
                    val dailyAiInsight = remember(dbSessionsList) {
                        com.ai_assistant.studentfocus.utils.ActivityClassifier.generateDailyInsights(dbSessionsList)
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("🤖", fontSize = 15.sp)
                            Text(
                                text = dailyAiInsight,
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // 3b. Personal Behavioral Intelligence & Learned Routines Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Maturity Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🧠", fontSize = 18.sp)
                            Column {
                                Text(
                                    text = "Personal Intelligence Profile",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = personalProfile.maturity.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (personalProfile.maturity) {
                                com.ai_assistant.studentfocus.models.ModelMaturity.NEW_USER -> Color(0xFF10B981).copy(alpha = 0.2f)
                                com.ai_assistant.studentfocus.models.ModelMaturity.LEARNING -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                com.ai_assistant.studentfocus.models.ModelMaturity.PERSONALIZED -> Color(0xFF6366F1).copy(alpha = 0.2f)
                                com.ai_assistant.studentfocus.models.ModelMaturity.ADAPTIVE -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when (personalProfile.maturity) {
                                    com.ai_assistant.studentfocus.models.ModelMaturity.NEW_USER -> Color(0xFF10B981).copy(alpha = 0.4f)
                                    com.ai_assistant.studentfocus.models.ModelMaturity.LEARNING -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                                    com.ai_assistant.studentfocus.models.ModelMaturity.PERSONALIZED -> Color(0xFF6366F1).copy(alpha = 0.4f)
                                    com.ai_assistant.studentfocus.models.ModelMaturity.ADAPTIVE -> Color(0xFF8B5CF6).copy(alpha = 0.4f)
                                }
                            )
                        ) {
                            Text(
                                text = "${personalProfile.maturity.icon} ${personalProfile.maturity.displayName}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Learned Routines & Predictions Box
                    if (personalProfile.activePredictions.isNotEmpty()) {
                        val pred = personalProfile.activePredictions.first()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔮", fontSize = 16.sp)
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = pred.predictionText,
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Confidence: ${pred.confidence} (${(pred.confidenceScore * 100).toInt()}%) • ${pred.supportingEvidence}",
                                        color = Color(0xFFA5B4FC),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    } else if (personalProfile.topProductiveHours.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("☀️", fontSize = 16.sp)
                                Text(
                                    text = "Peak Focus Hours: ${personalProfile.topProductiveHours.take(3).joinToString(", ") { "${it}:00" }} (Avg focus: ${personalProfile.avgProductiveSessionMins} mins)",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // 3 Metric Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Observed Sessions", color = Color.Gray, fontSize = 8.sp)
                                Text("${personalProfile.totalSessionsObserved}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Learned Routines", color = Color.Gray, fontSize = 8.sp)
                                Text("${personalProfile.timeRoutines.size} Windows", color = Color(0xFF818CF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("User Feedback", color = Color.Gray, fontSize = 8.sp)
                                Text("${personalProfile.userCorrectionsCount} Corrections", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Action Button to Open Model Inspector
                    Button(
                        onClick = { showPersonalModelDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text(
                            text = "🔍 Inspect Personal Model & Routines",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. View Mode Switcher: "📱 App Deep-Dive" vs "⏱️ Timeline"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeViewMode == "apps") Color(0xFF6366F1) else Color.Transparent)
                        .clickable { activeViewMode = "apps" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📱 Apps & Activities (${filteredAppList.size})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (activeViewMode == "apps") FontWeight.Bold else FontWeight.Normal
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeViewMode == "timeline") Color(0xFF6366F1) else Color.Transparent)
                        .clickable { activeViewMode = "timeline" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏱️ Activity Timeline (${filteredTimelineSessions.size})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (activeViewMode == "timeline") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // 5. Search Bar & Category Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps, video titles, LeetCode, topics...", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val categories = listOf(
                        "All",
                        "Learning",
                        "Coding",
                        "Productivity",
                        "Communication",
                        "Entertainment",
                        "Social",
                        "Gaming"
                    )
                    items(categories) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        val label = when (cat) {
                            "All" -> "All Activities"
                            "Learning" -> "🎓 Learning"
                            "Coding" -> "💻 Coding"
                            "Productivity" -> "💼 Productivity"
                            "Communication" -> "💬 Communication"
                            "Entertainment" -> "🎬 Entertainment"
                            "Social" -> "📱 Social Media"
                            "Gaming" -> "🎮 Gaming"
                            else -> cat
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF6366F1) else Color(0xFF1E293B))
                                .border(1.dp, if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .clickable { selectedCategoryFilter = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // VIEW MODE 1: APPS WITH DEEP-DIVE IN-APP ACTIVITIES
        // -------------------------------------------------------------
        if (activeViewMode == "apps") {
            if (filteredAppList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📊", fontSize = 32.sp)
                            Text(
                                text = "No app activities recorded for $selectedDateStr.",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Open your apps or enable Accessibility Activity Detection above to track deep content.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredAppList, key = { it.first.first }) { itemData ->
                    val (pkg, appName, duration) = itemData.first
                    val (category, sessions) = itemData.second

                    var isExpanded by remember { mutableStateOf(true) }

                    val appTimeHours = duration / (1000 * 60 * 60)
                    val appTimeMins = (duration / (1000 * 60)) % 60
                    val appTimeSecs = (duration / 1000) % 60
                    val percent = if (totalTimeMillis > 0) ((duration * 100) / totalTimeMillis).toInt() else 0

                    val categoryColor = when (category) {
                        "Learning", "Study" -> Color(0xFF10B981)
                        "Coding" -> Color(0xFF3B82F6)
                        "Productivity" -> Color(0xFF6366F1)
                        "Communication" -> Color(0xFF38BDF8)
                        "Entertainment", "Video" -> Color(0xFF8B5CF6)
                        "Social", "Social Media" -> Color(0xFFEC4899)
                        "Gaming" -> Color(0xFFEF4444)
                        else -> Color(0xFF94A3B8)
                    }

                    // Group distinct in-app activities
                    val activityBreakdown = remember(sessions) {
                        sessions.groupBy { it.contentTitle }
                            .map { (title, list) ->
                                val totalActDuration = list.sumOf { it.durationMillis }
                                val firstSession = list.first()
                                Triple(title, totalActDuration, firstSession)
                            }
                            .sortedByDescending { it.second }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AppIconImage(
                                        drawable = AppUsageHelper.getAppIcon(context, pkg),
                                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                                    )

                                    Column {
                                        Text(
                                            text = appName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Category Pill (clickable to re-categorize)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = categoryColor.copy(alpha = 0.15f),
                                                modifier = Modifier.clickable {
                                                    editingActivityTarget = Triple(pkg, "", category)
                                                }
                                            ) {
                                                Text(
                                                    text = "$category ✎",
                                                    color = categoryColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            if (sessions.isNotEmpty()) {
                                                Text(
                                                    text = "${activityBreakdown.size} activities logged",
                                                    color = Color(0xFFA5B4FC),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatDurationSmart(duration),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "$percent% of screen time",
                                        color = categoryColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Expandable In-App Deep Dive Activities List
                            if (isExpanded) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                                if (activityBreakdown.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "🔍 What you did inside $appName (Topics & Content):",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        activityBreakdown.forEach { (title, actDuration, session) ->
                                            val actHours = actDuration / (1000 * 60 * 60)
                                            val actMins = (actDuration / (1000 * 60)) % 60
                                            val actSecs = (actDuration / 1000) % 60
                                            val actTimeStr = when {
                                                actHours > 0 -> "${actHours}h ${actMins}m"
                                                actMins > 0 -> "${actMins}m ${actSecs}s"
                                                else -> "${actSecs}s"
                                            }

                                            val actIcon = when {
                                                session.activityType.contains("Video", ignoreCase = true) -> "▶️"
                                                session.activityType.contains("Shorts", ignoreCase = true) || session.activityType.contains("Reel", ignoreCase = true) -> "🎬"
                                                session.activityType.contains("Code", ignoreCase = true) || title.contains("leetcode", ignoreCase = true) -> "💻"
                                                session.activityType.contains("Chat", ignoreCase = true) || session.activityType.contains("Message", ignoreCase = true) -> "💬"
                                                session.activityType.contains("Web", ignoreCase = true) || session.domainOrSubtext != null -> "🌐"
                                                session.activityType.contains("Music", ignoreCase = true) || session.activityType.contains("Podcast", ignoreCase = true) -> "🎵"
                                                else -> "📌"
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF0F172A),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedActivityForDetail = session }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.Top,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(actIcon, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
                                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                            Text(
                                                                text = title,
                                                                color = Color.White,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                lineHeight = 16.sp
                                                            )

                                                            val goalAlign = when (session.goalAlignment) {
                                                                "GOAL_ALIGNED" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.GOAL_ALIGNED
                                                                "HIGHLY_RELEVANT" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.HIGHLY_RELEVANT
                                                                "POSSIBLY_RELEVANT" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT
                                                                "NEUTRAL" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.NEUTRAL
                                                                "OFF_GOAL" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.OFF_GOAL
                                                                else -> when (session.category) {
                                                                    "Learning", "Coding" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.GOAL_ALIGNED
                                                                    "Productivity" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT
                                                                    "Communication", "Browsing" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.NEUTRAL
                                                                    "Entertainment", "Social Media", "Social", "Gaming" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.OFF_GOAL
                                                                    else -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.UNKNOWN
                                                                }
                                                            }

                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                // Goal Status Badge
                                                                Surface(
                                                                    shape = RoundedCornerShape(3.dp),
                                                                    color = when (goalAlign) {
                                                                        com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.GOAL_ALIGNED -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                                        com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.HIGHLY_RELEVANT -> Color(0xFF14B8A6).copy(alpha = 0.2f)
                                                                        com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                                        com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.NEUTRAL -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                                                        com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.OFF_GOAL -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                                        com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.UNKNOWN -> Color(0xFF64748B).copy(alpha = 0.2f)
                                                                    }
                                                                ) {
                                                                    Text(
                                                                        text = "${goalAlign.icon} ${goalAlign.displayName}",
                                                                        color = when (goalAlign) {
                                                                            com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.GOAL_ALIGNED -> Color(0xFF34D399)
                                                                            com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.HIGHLY_RELEVANT -> Color(0xFF2DD4BF)
                                                                            com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT -> Color(0xFFFBBF24)
                                                                            com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.NEUTRAL -> Color(0xFF38BDF8)
                                                                            com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.OFF_GOAL -> Color(0xFFF87171)
                                                                            com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.UNKNOWN -> Color(0xFF94A3B8)
                                                                        },
                                                                        fontSize = 8.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                    )
                                                                }

                                                                Surface(
                                                                    shape = RoundedCornerShape(3.dp),
                                                                    color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                                                    modifier = Modifier.clickable {
                                                                        editingActivityTarget = Triple(pkg, title, session.category)
                                                                    }
                                                                ) {
                                                                    Text(
                                                                        text = "${session.category} ✎",
                                                                        color = Color(0xFFA5B4FC),
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                    )
                                                                }

                                                                Text(
                                                                    text = "• ${session.activityType}",
                                                                    color = Color(0xFF94A3B8),
                                                                    fontSize = 10.sp
                                                                )

                                                                if (session.domainOrSubtext != null) {
                                                                    Text(
                                                                        text = "• ${session.domainOrSubtext}",
                                                                        color = Color(0xFF818CF8),
                                                                        fontSize = 10.sp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0x336366F1),
                                                        modifier = Modifier.padding(start = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = actTimeStr,
                                                            color = Color(0xFFA5B4FC),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Guidance when deep in-app sessions have not yet been recorded for this specific app
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F172A),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(if (hasAccessibilityPermission) "📱" else "⚡", fontSize = 20.sp)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (hasAccessibilityPermission)
                                                        "Foreground App Activity"
                                                    else
                                                        "Enable Activity Detection for deep in-app breakdown",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (hasAccessibilityPermission)
                                                        "Open $appName to auto-record specific video titles, coding problems, reels and web pages."
                                                    else
                                                        "Grant Accessibility Service to log exact YouTube titles, Instagram Reels, LeetCode topics & websites.",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 10.sp
                                                )
                                            }
                                            if (!hasAccessibilityPermission) {
                                                Button(
                                                    onClick = { AppUsageHelper.openAccessibilitySettings(context) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("Enable", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // VIEW MODE 2: CHRONOLOGICAL ACTIVITY TIMELINE
        // -------------------------------------------------------------
        if (activeViewMode == "timeline") {
            if (filteredTimelineSessions.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⏱️", fontSize = 32.sp)
                            Text(
                                text = "No timeline events recorded for $selectedDateStr.",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Enable Accessibility Activity Detection and use your phone to generate real-time chronological timeline.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredTimelineSessions, key = { it.id }) { session ->
                    val startStr = remember(session.startTime) { timeSdf.format(Date(session.startTime)) }
                    val endStr = remember(session.endTime) { timeSdf.format(Date(session.endTime)) }
                    val durationMins = session.durationMillis / (1000 * 60)
                    val durationSecs = (session.durationMillis / 1000) % 60
                    val durFormatted = if (durationMins > 0) "${durationMins}m ${durationSecs}s" else "${durationSecs}s"

                    val catColor = when (session.category) {
                        "Learning", "Study" -> Color(0xFF10B981)
                        "Coding" -> Color(0xFF3B82F6)
                        "Productivity" -> Color(0xFF6366F1)
                        "Communication" -> Color(0xFF38BDF8)
                        "Entertainment" -> Color(0xFF8B5CF6)
                        "Social Media" -> Color(0xFFEC4899)
                        "Gaming" -> Color(0xFFEF4444)
                        else -> Color(0xFF94A3B8)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Time Column with timeline dot
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(60.dp)
                        ) {
                            Text(startStr, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .size(8.dp)
                                    .background(catColor, CircleShape)
                            )
                            Text(endStr, color = Color.Gray, fontSize = 9.sp)
                        }

                        // Event Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .clickable { selectedActivityForDetail = session }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AppIconImage(
                                            drawable = AppUsageHelper.getAppIcon(context, session.packageName),
                                            modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
                                        )
                                        Text(
                                            text = session.appName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    val goalAlign = when (session.goalAlignment) {
                                        "GOAL_ALIGNED" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.GOAL_ALIGNED
                                        "HIGHLY_RELEVANT" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.HIGHLY_RELEVANT
                                        "POSSIBLY_RELEVANT" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT
                                        "NEUTRAL" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.NEUTRAL
                                        "OFF_GOAL" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.OFF_GOAL
                                        else -> when (session.category) {
                                            "Learning", "Coding" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.GOAL_ALIGNED
                                            "Productivity" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT
                                            "Communication", "Browsing" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.NEUTRAL
                                            "Entertainment", "Social Media", "Social", "Gaming" -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.OFF_GOAL
                                            else -> com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.UNKNOWN
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = when (goalAlign) {
                                                com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.GOAL_ALIGNED -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.HIGHLY_RELEVANT -> Color(0xFF14B8A6).copy(alpha = 0.2f)
                                                com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.NEUTRAL -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                                com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.OFF_GOAL -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.UNKNOWN -> Color(0xFF64748B).copy(alpha = 0.2f)
                                            }
                                        ) {
                                            Text(
                                                text = "${goalAlign.icon} ${goalAlign.displayName}",
                                                color = when (goalAlign) {
                                                    com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.GOAL_ALIGNED -> Color(0xFF34D399)
                                                    com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.HIGHLY_RELEVANT -> Color(0xFF2DD4BF)
                                                    com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.POSSIBLY_RELEVANT -> Color(0xFFFBBF24)
                                                    com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.NEUTRAL -> Color(0xFF38BDF8)
                                                    com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.OFF_GOAL -> Color(0xFFF87171)
                                                    com.ai_assistant.studentfocus.utils.ActivityClassifier.GoalAlignment.UNKNOWN -> Color(0xFF94A3B8)
                                                },
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = catColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = session.category,
                                                color = catColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = session.contentTitle,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${session.activityType} ${if (session.domainOrSubtext != null) "• ${session.domainOrSubtext}" else ""}",
                                        color = Color.Gray,
                                        fontSize = 9.sp
                                    )
                                    Text(
                                        text = "⏱️ $durFormatted",
                                        color = Color(0xFFA5B4FC),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // MODAL DIALOG: ACTIVITY INTELLIGENCE & GOAL EXPLANATION
    // -------------------------------------------------------------
    selectedActivityForDetail?.let { session ->
        val classification = remember(session) {
            com.ai_assistant.studentfocus.utils.ActivityClassifier.classifyActivity(
                packageName = session.packageName,
                appName = session.appName,
                activityType = session.activityType,
                contentTitle = session.contentTitle,
                domainOrSubtext = session.domainOrSubtext,
                userOverrides = userOverridesMap,
                academicContext = com.ai_assistant.studentfocus.utils.ActivityClassifier.AcademicContext(
                    personalProfile = personalProfile
                ),
                timestamp = session.startTime
            )
        }

        AlertDialog(
            onDismissRequest = { selectedActivityForDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(classification.goalAlignment.icon, fontSize = 24.sp)
                    Column {
                        Text(
                            text = "${classification.goalAlignment.displayName} (${classification.goalScore}/100)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Confidence: ${classification.confidenceLevel.name} (${(classification.confidence * 100).toInt()}%) • ${classification.detectedIntent.icon} ${classification.detectedIntent.displayName}",
                            color = Color(0xFFA5B4FC),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 3-Pillar Separation Status Box (Content vs Personal vs Routine)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 1. Content Relevance
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0F172A), modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎯 Content", color = Color.Gray, fontSize = 8.sp)
                                Text(
                                    text = classification.contentRelevance,
                                    color = when (classification.contentRelevance) {
                                        "HIGH" -> Color(0xFF10B981)
                                        "MEDIUM" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF94A3B8)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // 2. Personal Relevance
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0F172A), modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("👤 Personal", color = Color.Gray, fontSize = 8.sp)
                                Text(
                                    text = classification.personalRelevance,
                                    color = when (classification.personalRelevance) {
                                        "HIGH" -> Color(0xFF818CF8)
                                        "MEDIUM" -> Color(0xFFA5B4FC)
                                        else -> Color(0xFF94A3B8)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // 3. Routine Match
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0F172A), modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⏰ Routine", color = Color.Gray, fontSize = 8.sp)
                                Text(
                                    text = classification.routineMatch,
                                    color = when (classification.routineMatch) {
                                        "HIGH" -> Color(0xFF38BDF8)
                                        "MEDIUM" -> Color(0xFFA5B4FC)
                                        else -> Color(0xFF94A3B8)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // App & Title Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(session.appName, color = Color(0xFF818CF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(session.contentTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Type: ${session.activityType} • Category: ${session.category}", color = Color.Gray, fontSize = 10.sp)

                            if (classification.matchedGoal != null || classification.matchedTopics.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (classification.matchedGoal != null) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "🎯 ${classification.matchedGoal}",
                                                color = Color(0xFF34D399),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    classification.matchedTopics.take(2).forEach { t ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF6366F1).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "💡 $t",
                                                color = Color(0xFFA5B4FC),
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Positive Evidence
                    if (classification.positiveEvidence.isNotEmpty()) {
                        Text("✓ Content & Goal Evidence:", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            classification.positiveEvidence.forEach { reason ->
                                Text(reason, color = Color(0xFFE2E8F0), fontSize = 10.sp)
                            }
                        }
                    }

                    // Personal Behavioral Evidence
                    if (classification.personalEvidence.isNotEmpty() || classification.historicalEvidence.isNotEmpty()) {
                        Text("👤 Personal & Historical Profile:", color = Color(0xFF818CF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            (classification.personalEvidence + classification.historicalEvidence).forEach { reason ->
                                Text(reason, color = Color(0xFFC7D2FE), fontSize = 10.sp)
                            }
                        }
                    }

                    // Negative Evidence
                    if (classification.negativeEvidence.isNotEmpty()) {
                        Text("⚠️ Distraction Signals:", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            classification.negativeEvidence.forEach { reason ->
                                Text(reason, color = Color(0xFFFCA5A5), fontSize = 10.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // Observed vs Inferred
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(classification.observedEvidence, color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(
                            text = classification.inferenceNote,
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingActivityTarget = Triple(session.packageName, session.contentTitle, session.category)
                    selectedActivityForDetail = null
                }) {
                    Text("Edit / Override ✎", color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedActivityForDetail = null }) {
                    Text("Close", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // -------------------------------------------------------------
    // MODAL DIALOG: EDIT CATEGORY & CLASSIFICATION
    // -------------------------------------------------------------
    editingActivityTarget?.let { (targetPkg, targetTitle, currentCat) ->
        AlertDialog(
            onDismissRequest = { editingActivityTarget = null },
            title = {
                Text(
                    text = "Edit Category",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose category for '$targetPkg':",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    val categories = listOf(
                        "Learning",
                        "Coding",
                        "Productivity",
                        "Communication",
                        "Entertainment",
                        "Social Media",
                        "Gaming",
                        "Browsing",
                        "Other"
                    )

                    categories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentCat == cat) Color(0xFF6366F1) else Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (targetTitle.isNotBlank() && !targetTitle.contains("unavailable", ignoreCase = true)) {
                                        taskViewModel.updateActivityCategory(targetPkg, targetTitle, cat, originalCategory = currentCat)
                                    } else {
                                        taskViewModel.updateAppCategory(targetPkg, cat, originalCategory = currentCat)
                                    }
                                    editingActivityTarget = null
                                    Toast.makeText(context, "Feedback recorded! Model updated ✅", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = cat,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (currentCat == cat) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingActivityTarget = null }) {
                    Text("Close", color = Color(0xFF818CF8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }

    // -------------------------------------------------------------
    // MODAL DIALOG: PERSONAL INTELLIGENCE & MODEL INSPECTOR
    // -------------------------------------------------------------
    if (showPersonalModelDialog) {
        AlertDialog(
            onDismissRequest = { showPersonalModelDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🧠", fontSize = 22.sp)
                    Column {
                        Text(
                            text = "Personal Behavioral Model",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Maturity: ${personalProfile.maturity.displayName} (${personalProfile.maturity.icon})",
                            color = Color(0xFFA5B4FC),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Overview Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📊 Learning History Stats", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("• Tracked Over: ${personalProfile.totalDaysTracked} distinct active days", color = Color(0xFFE2E8F0), fontSize = 11.sp)
                                Text("• Observed Sessions: ${personalProfile.totalSessionsObserved} on-device activity logs", color = Color(0xFFE2E8F0), fontSize = 11.sp)
                                Text("• User Corrections: ${personalProfile.userCorrectionsCount} explicit feedback points", color = Color(0xFFE2E8F0), fontSize = 11.sp)
                                Text("• Confidence Coverage: ${personalProfile.confidenceCoverageHigh}% High, ${personalProfile.confidenceCoverageMed}% Med, ${personalProfile.confidenceCoverageLow}% Low", color = Color(0xFFA5B4FC), fontSize = 10.sp)
                            }
                        }
                    }

                    // 2. Learned Routines
                    item {
                        Text("⏰ Learned Time Routines (${personalProfile.timeRoutines.size})", color = Color(0xFF818CF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        if (personalProfile.timeRoutines.isEmpty()) {
                            Text("🌱 No clear recurring hour routines yet. As you study consistently, habits will be automatically detected.", color = Color.Gray, fontSize = 10.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                personalProfile.timeRoutines.forEach { r ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${r.startHour}:00 – ${r.endHour}:00", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text("${r.dominantCategory} (${(r.confidence * 100).toInt()}% conf)", color = Color(0xFF34D399), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Learned Topic Affinities
                    if (personalProfile.topicAffinities.isNotEmpty()) {
                        item {
                            Text("🏷️ Topic Affinities (${personalProfile.topicAffinities.size})", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                personalProfile.topicAffinities.values.take(6).forEach { t ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(t.topic, color = Color.White, fontSize = 11.sp)
                                            Text("${(t.relevanceScore * 100).toInt()}% goal-aligned (${t.goalAlignedCount} sessions)", color = Color(0xFFA5B4FC), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Common Transitions
                    if (personalProfile.commonTransitions.isNotEmpty()) {
                        item {
                            Text("⚡ Workflow Transitions (${personalProfile.commonTransitions.size})", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                personalProfile.commonTransitions.take(4).forEach { trans ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${trans.fromCategory} ➔ ${trans.toCategory}", color = Color.White, fontSize = 11.sp)
                                            Text(trans.transitionType, color = if (trans.transitionType == "PRODUCTIVE_WORKFLOW") Color(0xFF34D399) else Color(0xFFF87171), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Reset Action
                    item {
                        Button(
                            onClick = {
                                taskViewModel.resetPersonalLearningData()
                                Toast.makeText(context, "Personal behavioral model reset! 🌱", Toast.LENGTH_SHORT).show()
                                showPersonalModelDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🗑️ Reset Personal Model & Learning Data", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPersonalModelDialog = false }) {
                    Text("Close", color = Color(0xFF818CF8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // -------------------------------------------------------------
    // MODAL DIALOG: APP CLASSIFICATION & FILTER INSPECTOR
    // -------------------------------------------------------------
    if (showInspectorDialog) {
        val inspectedItems = remember(selectedDateStr) {
            AppUsageHelper.inspectAllAppUsagesForDay(context, selectedDateStr)
        }
        var filterCategory by remember { mutableStateOf("ALL") } // ALL, INCLUDED, EXCLUDED, UNKNOWN

        val filteredItems = remember(inspectedItems, filterCategory) {
            when (filterCategory) {
                "INCLUDED" -> inspectedItems.filter { it.classification.isUserFacing }
                "EXCLUDED" -> inspectedItems.filter { !it.classification.isUserFacing && it.classification.packageType != com.ai_assistant.studentfocus.utils.AppUsageClassifier.PackageType.UNKNOWN }
                "UNKNOWN" -> inspectedItems.filter { it.classification.packageType == com.ai_assistant.studentfocus.utils.AppUsageClassifier.PackageType.UNKNOWN }
                else -> inspectedItems
            }
        }

        AlertDialog(
            onDismissRequest = { showInspectorDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔍", fontSize = 20.sp)
                    Column {
                        Text("App Classification & Diagnostic Inspector", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Authoritative OS metadata, confidence scoring & session repair", color = Color(0xFFA5B4FC), fontSize = 11.sp)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Diagnostic Report Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("📊 Activity Aggregation & Diagnostic Report", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Aggregated: ${diagnosticReport.sanitizedSessions} (Merged: ${diagnosticReport.mergedObservationsCount})", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("Raw Events: ${diagnosticReport.totalRawSessions}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Overlaps Repaired: ${diagnosticReport.overlapsRepaired}", color = Color(0xFFA5B4FC), fontSize = 10.sp)
                                Text("Missing Titles: ${diagnosticReport.titlesUnavailableCount}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            }
                        }
                    }

                    // Filter Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL" to "All", "INCLUDED" to "Included", "EXCLUDED" to "Excluded", "UNKNOWN" to "Unknown").forEach { (key, label) ->
                            val isSelected = filterCategory == key
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFF6366F1) else Color(0xFF1E293B),
                                modifier = Modifier.clickable { filterCategory = key }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredItems.isEmpty()) {
                            item {
                                Text("No packages match the selected filter.", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            items(filteredItems) { item ->
                                val isIncluded = item.classification.isUserFacing
                                val durMin = item.totalTimeMillis / (1000 * 60)
                                val durSec = (item.totalTimeMillis / 1000) % 60
                                val durStr = if (durMin > 0) "${durMin}m ${durSec}s" else "${durSec}s"

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isIncluded) Color(0xFF1E293B) else Color(0xFF0F172A)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (isIncluded) Color(0xFF10B981).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.appName,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isIncluded) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF64748B).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = if (isIncluded) "✅ Included ($durStr)" else "🚫 Excluded",
                                                    color = if (isIncluded) Color(0xFF34D399) else Color(0xFF94A3B8),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = item.packageName,
                                            color = Color(0xFF818CF8),
                                            fontSize = 10.sp
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = Color(0xFF6366F1).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = item.classification.packageType.name,
                                                    color = Color(0xFFA5B4FC),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Conf: ${item.classification.confidence.name}",
                                                    color = Color(0xFF34D399),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }

                                            Text(
                                                text = "• ${item.classification.reason}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInspectorDialog = false }) {
                    Text("Close", color = Color(0xFF818CF8))
                }
            },
            containerColor = Color(0xFF1E1B4B)
        )
    }
}

@Composable
fun AppIconImage(drawable: Drawable?, modifier: Modifier = Modifier) {
    if (drawable != null) {
        val bitmap = remember(drawable) {
            try {
                if (drawable is BitmapDrawable) {
                    drawable.bitmap.asImageBitmap()
                } else {
                    val bmp = Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(48),
                        drawable.intrinsicHeight.coerceAtLeast(48),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bmp.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = modifier
            )
        } else {
            Text("📱", fontSize = 18.sp)
        }
    } else {
        Text("📱", fontSize = 18.sp)
    }
}

// -------------------------------------------------------------
// App Usage CSV Export Function
// -------------------------------------------------------------
fun exportAppUsageToCsv(context: Context, dateStr: String, usageList: List<AppUsageEntity>) {
    val cleanList = usageList.filter { com.ai_assistant.studentfocus.utils.AppUsageClassifier.isUserFacingApp(context, it.packageName) }
    if (cleanList.isEmpty()) {
        Toast.makeText(context, "No usage records to export!", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val backupsDir = File(context.getExternalFilesDir(null), "Backups")
        if (!backupsDir.exists()) backupsDir.mkdirs()
        val file = File(backupsDir, "AppUsage_Backup_$dateStr.csv")

        val sb = StringBuilder()
        sb.append("date,appName,packageName,durationMinutes,category,lastTimeUsed\n")
        cleanList.forEach { u ->
            val escapedName = "\"" + u.appName.replace("\"", "\"\"") + "\""
            val mins = u.totalTimeMillis / (1000 * 60)
            val lastUsedFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(u.lastTimeUsed))
            sb.append("${u.date},$escapedName,${u.packageName},$mins,${u.category},$lastUsedFmt\n")
        }

        FileOutputStream(file).use { fos ->
            fos.write(sb.toString().toByteArray(Charsets.UTF_8))
        }

        Toast.makeText(context, "App Usage CSV exported: ${file.name} 📤", Toast.LENGTH_LONG).show()

        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                this.type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Daily App Usage Backup"))
        } catch (e: Exception) {
            // Fallback
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error exporting App Usage CSV: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
