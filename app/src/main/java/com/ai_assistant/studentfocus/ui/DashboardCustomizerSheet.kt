package com.ai_assistant.studentfocus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_assistant.studentfocus.utils.DashboardCustomizationManager
import com.ai_assistant.studentfocus.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCustomizerSheet(
    taskViewModel: TaskViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var widgetOrder by remember { mutableStateOf(DashboardCustomizationManager.getWidgetOrder(context)) }
    var hiddenWidgets by remember { mutableStateOf(DashboardCustomizationManager.getHiddenWidgets(context)) }
    var activePreset by remember { mutableStateOf(DashboardCustomizationManager.getActivePresetName(context)) }

    val allWidgetsMap = remember { DashboardCustomizationManager.ALL_WIDGETS.associateBy { it.id } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.Gray.copy(alpha = 0.4f), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎨 Customize Dashboard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Reorder cards & toggle visible sections to your preference",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        DashboardCustomizationManager.resetToDefault(context, taskViewModel)
                        widgetOrder = DashboardCustomizationManager.getWidgetOrder(context)
                        hiddenWidgets = DashboardCustomizationManager.getHiddenWidgets(context)
                        activePreset = "default"
                    }
                ) {
                    Text("Reset", color = Color(0xFF818CF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Quick Presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "QUICK PRESETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(
                        Triple("default", "🌟 All (Default)", "Full suite"),
                        Triple("minimal", "⚡ Minimal Focus", "Timer + Tasks + Notes"),
                        Triple("scholar", "🎓 Academic Scholar", "Exams + Tasks + Pace"),
                        Triple("analytics", "📊 Analytics Heavy", "Heatmap + Chart + Waste")
                    )

                    items(presets.size) { index ->
                        val (presetId, title, sub) = presets[index]
                        val isSelected = activePreset.equals(presetId, ignoreCase = true)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF6366F1) else Color(0xFF1E293B),
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                DashboardCustomizationManager.applyPreset(context, presetId, taskViewModel)
                                widgetOrder = DashboardCustomizationManager.getWidgetOrder(context)
                                hiddenWidgets = DashboardCustomizationManager.getHiddenWidgets(context)
                                activePreset = presetId
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = sub,
                                    color = if (isSelected) Color(0xFFE0E7FF) else Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // Reorderable Card List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DASHBOARD SECTIONS (${widgetOrder.size - hiddenWidgets.size} visible)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Use ⬆️ ⬇️ to reorder",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(widgetOrder, key = { _, id -> id }) { index, widgetId ->
                    val info = allWidgetsMap[widgetId]
                    if (info != null) {
                        val isVisible = widgetId !in hiddenWidgets
                        val canMoveUp = index > 0
                        val canMoveDown = index < widgetOrder.size - 1

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isVisible) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isVisible) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Up / Down Reorder Buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (canMoveUp) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val mutable = widgetOrder.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index - 1, item)
                                                widgetOrder = mutable
                                                DashboardCustomizationManager.saveWidgetOrder(context, mutable, taskViewModel)
                                                activePreset = "custom"
                                            }
                                        },
                                        enabled = canMoveUp,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("⬆️", fontSize = 12.sp, color = if (canMoveUp) Color.White else Color.DarkGray)
                                    }

                                    IconButton(
                                        onClick = {
                                            if (canMoveDown) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val mutable = widgetOrder.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index + 1, item)
                                                widgetOrder = mutable
                                                DashboardCustomizationManager.saveWidgetOrder(context, mutable, taskViewModel)
                                                activePreset = "custom"
                                            }
                                        },
                                        enabled = canMoveDown,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("⬇️", fontSize = 12.sp, color = if (canMoveDown) Color.White else Color.DarkGray)
                                    }
                                }

                                // Title and Icon
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = info.icon, fontSize = 16.sp)
                                    Column {
                                        Text(
                                            text = info.title,
                                            color = if (isVisible) Color.White else Color.Gray,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = info.description,
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Visibility Toggle Switch
                                if (info.isRemovable) {
                                    Switch(
                                        checked = isVisible,
                                        onCheckedChange = { checked ->
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            DashboardCustomizationManager.setWidgetVisibility(context, widgetId, checked, taskViewModel)
                                            hiddenWidgets = DashboardCustomizationManager.getHiddenWidgets(context)
                                            activePreset = "custom"
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF6366F1),
                                            uncheckedThumbColor = Color.LightGray,
                                            uncheckedTrackColor = Color(0xFF334155)
                                        ),
                                        modifier = Modifier.height(24.dp)
                                    )
                                } else {
                                    Surface(
                                        color = Color(0xFF334155).copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Required",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Done Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text(
                    text = "Apply Changes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
