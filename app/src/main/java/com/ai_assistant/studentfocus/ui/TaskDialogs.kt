package com.ai_assistant.studentfocus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import com.ai_assistant.studentfocus.models.TaskEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskFormDialog(
    existingCategories: List<String>,
    taskToEdit: TaskEntity? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String?, cat: String, sub: String?, date: String, prio: String, pinned: Boolean, rec: Boolean, linkUrl: String?) -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var desc by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var linkUrl by remember { mutableStateOf(taskToEdit?.linkUrl ?: "") }
    var category by remember { mutableStateOf(taskToEdit?.category ?: "College") }
    var customCategory by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(taskToEdit?.subject ?: "") }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var dueDate by remember { mutableStateOf(taskToEdit?.dueDate ?: sdf.format(Date())) }

    var priority by remember { mutableStateOf(taskToEdit?.priority ?: "medium") }
    var isPinned by remember { mutableStateOf(taskToEdit?.isPinned ?: false) }

    // Recurrence parameters
    var recurrenceType by remember { mutableStateOf(if (taskToEdit?.isRecurring == true) "Daily" else "None") }
    var recurrenceTillDate by remember { mutableStateOf(sdf.format(Date())) }

    val defaultCategories = listOf("College", "GATE", "Placement", "Projects", "Personal")
    val allCategories = remember(existingCategories) {
        (defaultCategories + existingCategories).distinct().filter { it.isNotBlank() && it != "Create Custom..." }
    }
    val priorities = listOf("low", "medium", "high")
    val recurrenceTypes = listOf("None", "Daily", "Weekly", "Monthly")

    val context = androidx.compose.ui.platform.LocalContext.current

    // Launch Android DatePickerDialog
    fun openDatePicker(initialDateStr: String, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        try {
            val parsedDate = sdf.parse(initialDateStr)
            if (parsedDate != null) {
                calendar.time = parsedDate
            }
        } catch (e: Exception) {}

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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.5f), Color(0xFFEC4899).copy(alpha = 0.1f))),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (taskToEdit == null) "✨ Add Task Target" else "✏️ Edit Task Target",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var catExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { catExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.08f)))
                            )
                        ) {
                            Text(text = if (category == "Create Custom...") "Category: Custom" else "Category: $category", color = Color.White, fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false },
                            modifier = Modifier.background(Color(0xFF0F172A))
                        ) {
                            allCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = Color.White) },
                                    onClick = {
                                        category = cat
                                        catExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Create Custom...", color = Color(0xFF818CF8), fontWeight = FontWeight.Bold) },
                                onClick = {
                                        category = "Create Custom..."
                                        catExpanded = false
                                    }
                            )
                        }
                    }

                    var prioExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { prioExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.08f)))
                            )
                        ) {
                            Text(text = "Priority: ${priority.uppercase()}", color = Color.White, fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = prioExpanded,
                            onDismissRequest = { prioExpanded = false },
                            modifier = Modifier.background(Color(0xFF0F172A))
                        ) {
                            priorities.forEach { prio ->
                                DropdownMenuItem(
                                    text = { Text(prio.uppercase(), color = Color.White) },
                                    onClick = {
                                        priority = prio
                                        prioExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Render custom category input field when chosen
                if (category == "Create Custom...") {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("Enter Custom Category Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Render subject select input ONLY when category is College
                if (category == "College") {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("College Subject (e.g. ML, Mathematics)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Interactive Due Date Calendar button
                OutlinedButton(
                    onClick = { openDatePicker(dueDate) { dueDate = it } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.08f)))
                    )
                ) {
                    Text(text = "📅 Due Date: $dueDate", color = Color.White)
                }

                // Recurrence scheduling configuration (only for new tasks)
                if (taskToEdit == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        var recExpanded by remember { mutableStateOf(false) }
                        Text(text = "Recurrence:", color = Color.White, fontSize = 14.sp)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { recExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.08f)))
                                )
                            ) {
                                Text(text = recurrenceType, color = Color.White)
                            }
                            DropdownMenu(
                                expanded = recExpanded,
                                onDismissRequest = { recExpanded = false },
                                modifier = Modifier.background(Color(0xFF0F172A))
                            ) {
                                recurrenceTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type, color = Color.White) },
                                        onClick = {
                                            recurrenceType = type
                                            recExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // If recurrence is scheduled, display End Date calendar picker
                    if (recurrenceType != "None") {
                        OutlinedButton(
                            onClick = { openDatePicker(recurrenceTillDate) { recurrenceTillDate = it } },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.08f)))
                            )
                        ) {
                            Text(text = "🔁 Repeat Till Date: $recurrenceTillDate", color = Color.White)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isPinned,
                            onCheckedChange = { isPinned = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF6366F1),
                                uncheckedColor = Color.White.copy(alpha = 0.3f),
                                checkmarkColor = Color.White
                            )
                        )
                        Text(text = "Pin Target", color = Color.White, fontSize = 14.sp)
                    }
                }

                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    label = { Text("🔗 Meeting / Resource / Notes Link (Optional)") },
                    placeholder = { Text("e.g. https://meet.google.com/xyz, Notion, GitHub") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description & Notes") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancel", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val finalCategory = if (category == "Create Custom...") customCategory.trim() else category
                            val finalLink = linkUrl.trim().ifBlank { null }
                            if (title.isNotBlank() && finalCategory.isNotBlank()) {
                                val startCal = Calendar.getInstance()
                                try {
                                    val parsed = sdf.parse(dueDate)
                                    if (parsed != null) {
                                        startCal.time = parsed
                                    }
                                } catch (e: Exception) {}

                                val endCal = Calendar.getInstance()
                                try {
                                    val parsed = sdf.parse(recurrenceTillDate)
                                    if (parsed != null) {
                                        endCal.time = parsed
                                    }
                                } catch (e: Exception) {}

                                if (recurrenceType == "None" || endCal.before(startCal)) {
                                    // Single instance
                                    onSave(
                                        title,
                                        desc.ifBlank { null },
                                        finalCategory,
                                        if (finalCategory == "College") subject.ifBlank { null } else null,
                                        dueDate,
                                        priority,
                                        isPinned,
                                        false,
                                        finalLink
                                    )
                                } else {
                                    // Loop generate recurring task targets
                                    val currentCal = startCal.clone() as Calendar
                                    while (currentCal.before(endCal) || currentCal.equals(endCal)) {
                                        val formattedDate = sdf.format(currentCal.time)
                                        onSave(
                                            title,
                                            desc.ifBlank { null },
                                            finalCategory,
                                            if (finalCategory == "College") subject.ifBlank { null } else null,
                                            formattedDate,
                                            priority,
                                            isPinned,
                                            true,
                                            finalLink
                                        )
                                        when (recurrenceType) {
                                            "Daily" -> currentCal.add(Calendar.DAY_OF_YEAR, 1)
                                            "Weekly" -> currentCal.add(Calendar.WEEK_OF_YEAR, 1)
                                            "Monthly" -> currentCal.add(Calendar.MONTH, 1)
                                            else -> break
                                        }
                                    }
                                }
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Save Task", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DayTasksDialog(
    date: String,
    tasks: List<TaskEntity>,
    onToggle: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.5f), Color(0xFFEC4899).copy(alpha = 0.1f))),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tasks for $date",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (tasks.isEmpty()) {
                    Text(
                        text = "No study targets scheduled for today.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(tasks) { task ->
                            TaskCard(
                                task = task,
                                onToggle = { onToggle(task) },
                                onEdit = { onEdit(task) },
                                onDelete = { onDelete(task) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    taskTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "🗑️ Delete Target?", color = Color.White, fontWeight = FontWeight.Bold) },
        text = { Text(text = "Are you sure you want to delete \"$taskTitle\"? This action cannot be undone.", color = Color(0xFF94A3B8)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF0F172A),
        modifier = Modifier.border(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.5f), Color.Transparent)),
            shape = RoundedCornerShape(28.dp)
        ),
        titleContentColor = Color.White,
        textContentColor = Color(0xFF94A3B8)
    )
}

@Composable
fun TaskCompletionDialog(
    taskTitle: String,
    alreadyLoggedMinutes: Int = 0,
    routineBudgetMinutes: Int = 1440,
    actualElapsedMinutes: Int? = null,
    targetFocusMinutes: Int? = null,
    onConfirm: (minutes: Int, notes: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val effectiveDailyLimit = if (routineBudgetMinutes in 1..1440) routineBudgetMinutes else 1440
    val remainingCapacity = maxOf(0, effectiveDailyLimit - alreadyLoggedMinutes)
    val maxAllowedMinutes = minOf(remainingCapacity, actualElapsedMinutes ?: remainingCapacity)
    val defaultInitial = if (maxAllowedMinutes > 0) "$maxAllowedMinutes" else "0"
    var minutesText by remember { mutableStateOf(defaultInitial) }
    var notesText by remember { mutableStateOf("") }

    val parsedMins = minutesText.toIntOrNull()
    val isZeroElapsed = actualElapsedMinutes != null && actualElapsedMinutes <= 0
    val isFocusLimitExceeded = actualElapsedMinutes != null && parsedMins != null && parsedMins > actualElapsedMinutes
    val isDailyLimitExceeded = (alreadyLoggedMinutes >= effectiveDailyLimit) || (parsedMins != null && parsedMins > remainingCapacity)
    val isLimitExceeded = isZeroElapsed || isFocusLimitExceeded || isDailyLimitExceeded
    val isInvalid = parsedMins == null || parsedMins <= 0 || isLimitExceeded

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Target: $taskTitle", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { input ->
                        minutesText = input.filter { it.isDigit() }
                    },
                    label = { Text("Time spent (minutes)", color = Color.LightGray) },
                    supportingText = {
                        val limitH = effectiveDailyLimit / 60
                        val limitM = effectiveDailyLimit % 60
                        val limitFormatted = if (limitH > 0) "${limitH}h ${limitM}m" else "${effectiveDailyLimit}m"

                        if (isZeroElapsed) {
                            Text(
                                text = "❌ You have only focused for less than 1 minute (0m)! Please study with the timer to accumulate time before completing.",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (isFocusLimitExceeded) {
                            val targetNote = if (targetFocusMinutes != null) " (Target: ${targetFocusMinutes}m)" else ""
                            Text(
                                text = "❌ Cannot exceed actual studied time (${actualElapsedMinutes}m)$targetNote! You have only studied for ${actualElapsedMinutes}m so far. You can log up to ${actualElapsedMinutes}m or less.",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (alreadyLoggedMinutes >= effectiveDailyLimit) {
                            Text(
                                text = "❌ Daily Study Task Budget reached ($limitFormatted)! You have already logged ${alreadyLoggedMinutes}m today. No more time can be added.",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (parsedMins != null && parsedMins > remainingCapacity) {
                            val alreadyH = alreadyLoggedMinutes / 60
                            val alreadyM = alreadyLoggedMinutes % 60
                            val remH = remainingCapacity / 60
                            val remM = remainingCapacity % 60
                            Text(
                                text = "❌ Exceeds Daily Study Task Budget! Your effective routine budget is $limitFormatted. You have logged ${if (alreadyH > 0) "${alreadyH}h " else ""}${alreadyM}m today. You can only add up to ${if (remH > 0) "${remH}h " else ""}${remM}m more.",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            val remH = remainingCapacity / 60
                            val remM = remainingCapacity % 60
                            val focusNote = if (actualElapsedMinutes != null) " (Studied: ${actualElapsedMinutes}m / Target: ${targetFocusMinutes ?: actualElapsedMinutes}m)" else ""
                            Text(
                                text = "Remaining daily capacity: ${if (remH > 0) "${remH}h " else ""}${remM}m (Logged: ${alreadyLoggedMinutes}m / $limitFormatted budget)$focusNote",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    },
                    isError = isLimitExceeded,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = if (isLimitExceeded) Color(0xFFEF4444) else Color(0xFF6366F1),
                        unfocusedBorderColor = if (isLimitExceeded) Color(0xFFEF4444) else Color.White.copy(alpha = 0.2f),
                        errorBorderColor = Color(0xFFEF4444),
                        errorLabelColor = Color(0xFFEF4444)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Completion Notes / Insights", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    parsedMins?.let { mins ->
                        if (!isInvalid) {
                            onConfirm(mins, notesText.trim().ifBlank { null })
                        }
                    }
                },
                enabled = !isInvalid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    disabledContainerColor = Color(0xFF334155)
                )
            ) {
                Text("Complete Target", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.LightGray)
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun StartFocusDialog(
    taskTitle: String,
    alreadyLoggedMinutes: Int = 0,
    routineBudgetMinutes: Int = 1440,
    onStart: (durationMinutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val effectiveDailyLimit = if (routineBudgetMinutes in 1..1440) routineBudgetMinutes else 1440
    val remainingCapacity = maxOf(0, effectiveDailyLimit - alreadyLoggedMinutes)
    val defaultInitial = if (remainingCapacity >= 25) "25" else if (remainingCapacity > 0) "$remainingCapacity" else "0"
    var durationText by remember { mutableStateOf(defaultInitial) }

    val parsedMins = durationText.toIntOrNull()
    val isLimitExceeded = (alreadyLoggedMinutes >= effectiveDailyLimit) || (parsedMins != null && parsedMins > remainingCapacity)
    val isInvalid = parsedMins == null || parsedMins <= 0 || isLimitExceeded

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⏳", fontSize = 20.sp)
                Text("Start Focus: $taskTitle", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "How long do you want to focus on this target?",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                // Quick preset duration chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15, 25, 45, 60, 90).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (durationText == preset.toString()) Color(0xFF6366F1) else Color(0xFF0F172A),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .clickable { durationText = preset.toString() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${preset}m",
                                color = if (durationText == preset.toString()) Color.White else Color(0xFF818CF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { input ->
                        durationText = input.filter { it.isDigit() }
                    },
                    label = { Text("Focus Duration (minutes)", color = Color.LightGray) },
                    supportingText = {
                        val limitH = effectiveDailyLimit / 60
                        val limitM = effectiveDailyLimit % 60
                        val limitFormatted = if (limitH > 0) "${limitH}h ${limitM}m" else "${effectiveDailyLimit}m"

                        if (alreadyLoggedMinutes >= effectiveDailyLimit) {
                            Text(
                                text = "❌ Daily Study Budget reached ($limitFormatted)! No more focus sessions can be started today.",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (parsedMins != null && parsedMins > remainingCapacity) {
                            val remH = remainingCapacity / 60
                            val remM = remainingCapacity % 60
                            Text(
                                text = "❌ Exceeds daily budget! You only have ${if (remH > 0) "${remH}h " else ""}${remM}m available today.",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            val remH = remainingCapacity / 60
                            val remM = remainingCapacity % 60
                            Text(
                                text = "Remaining daily capacity: ${if (remH > 0) "${remH}h " else ""}${remM}m (Budget: $limitFormatted)",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    },
                    isError = isLimitExceeded,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = if (isLimitExceeded) Color(0xFFEF4444) else Color(0xFF6366F1),
                        unfocusedBorderColor = if (isLimitExceeded) Color(0xFFEF4444) else Color.White.copy(alpha = 0.2f),
                        errorBorderColor = Color(0xFFEF4444),
                        errorLabelColor = Color(0xFFEF4444)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    parsedMins?.let { mins ->
                        if (!isInvalid) {
                            onStart(mins)
                        }
                    }
                },
                enabled = !isInvalid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    disabledContainerColor = Color(0xFF334155)
                )
            ) {
                Text("▶️ Start Focus", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.LightGray)
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun ExtendFocusDialog(
    taskTitle: String,
    currentDurationMinutes: Int,
    alreadyLoggedMinutes: Int = 0,
    routineBudgetMinutes: Int = 1440,
    onExtend: (extraMinutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val effectiveDailyLimit = if (routineBudgetMinutes in 1..1440) routineBudgetMinutes else 1440
    val maxAllowedExtension = maxOf(0, effectiveDailyLimit - (alreadyLoggedMinutes + currentDurationMinutes))
    val defaultInitial = if (maxAllowedExtension >= 15) "15" else if (maxAllowedExtension > 0) "$maxAllowedExtension" else "0"
    var extraText by remember { mutableStateOf(defaultInitial) }

    val parsedExtra = extraText.toIntOrNull()
    val isLimitExceeded = (maxAllowedExtension <= 0) || (parsedExtra != null && parsedExtra > maxAllowedExtension)
    val isInvalid = parsedExtra == null || parsedExtra <= 0 || isLimitExceeded

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⏳", fontSize = 20.sp)
                Text("Extend Focus Session", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add more time for '$taskTitle' (Current: ${currentDurationMinutes}m):",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                // Quick preset chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(10, 15, 25, 30, 45).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (extraText == preset.toString()) Color(0xFF6366F1) else Color(0xFF0F172A),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .clickable { extraText = preset.toString() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+${preset}m",
                                color = if (extraText == preset.toString()) Color.White else Color(0xFF818CF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = extraText,
                    onValueChange = { input ->
                        extraText = input.filter { it.isDigit() }
                    },
                    label = { Text("Extra Minutes to Add", color = Color.LightGray) },
                    supportingText = {
                        if (maxAllowedExtension <= 0) {
                            Text(
                                text = "❌ Cannot extend! Daily study budget limit reached.",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (parsedExtra != null && parsedExtra > maxAllowedExtension) {
                            Text(
                                text = "❌ Exceeds limit! You can only extend up to ${maxAllowedExtension}m more.",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "Max available extension: ${maxAllowedExtension}m",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    },
                    isError = isLimitExceeded,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = if (isLimitExceeded) Color(0xFFEF4444) else Color(0xFF6366F1),
                        unfocusedBorderColor = if (isLimitExceeded) Color(0xFFEF4444) else Color.White.copy(alpha = 0.2f),
                        errorBorderColor = Color(0xFFEF4444),
                        errorLabelColor = Color(0xFFEF4444)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    parsedExtra?.let { extra ->
                        if (!isInvalid) {
                            onExtend(extra)
                        }
                    }
                },
                enabled = !isInvalid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    disabledContainerColor = Color(0xFF334155)
                )
            ) {
                Text("➕ Extend Time", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.LightGray)
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun SessionExpiredDialog(
    session: com.ai_assistant.studentfocus.models.ActiveFocusSession,
    onComplete: () -> Unit,
    onExtend: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎯", fontSize = 22.sp)
                Text("Target Focus Time Completed!", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Awesome focus! You completed your ${session.targetDurationMinutes} minutes target for '${session.taskTitle}'.",
                    color = Color(0xFFC7D2FE),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = "Would you like to Save & Complete this target, or Extend your study time?",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("✅ Save & Complete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onExtend,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF818CF8))
            ) {
                Text("⏳ Extend Time")
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}
