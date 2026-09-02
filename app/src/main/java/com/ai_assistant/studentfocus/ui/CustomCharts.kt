package com.ai_assistant.studentfocus.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_assistant.studentfocus.models.StudyHistoryEntity
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@Composable
fun StudyHeatmap(history: List<StudyHistoryEntity>) {
    val context = LocalContext.current

    // Continuous date calculation starting from installation date onwards
    val (days, historyMap) = remember(history) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()

        // Get app installation date / first launch time
        val installTimeMillis = try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.firstInstallTime
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        // Find the earliest date in history
        val earliestHistoryDate = history.mapNotNull {
            try { sdf.parse(it.date) } catch (e: Exception) { null }
        }.minOrNull()

        // Start date is the install date (or earliest record if user restored older data)
        val startDate = if (earliestHistoryDate != null && earliestHistoryDate.time < installTimeMillis) {
            earliestHistoryDate
        } else {
            Date(installTimeMillis)
        }

        val startCalendar = Calendar.getInstance().apply {
            time = startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // Align with Sunday of that install week
        }

        val endCalendar = today.clone() as Calendar
        endCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)

        val daysList = mutableListOf<String?>()
        val calendar = startCalendar.clone() as Calendar
        while (calendar.before(endCalendar) || calendar == endCalendar) {
            val dateStr = sdf.format(calendar.time)
            daysList.add(if (calendar.after(today)) null else dateStr)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val map = history.associate { it.date to it.studyMinutes.coerceIn(0, 1440) }
        Pair(daysList, map)
    }

    val chunkedDays = remember(days) { days.chunked(7) }
    val lazyListState = rememberLazyListState()

    // Smoothly ensure current week is in view on load
    LaunchedEffect(chunkedDays.size) {
        if (chunkedDays.isNotEmpty()) {
            lazyListState.scrollToItem(chunkedDays.size - 1)
        }
    }

    LazyRow(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(chunkedDays) { weekDays ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                weekDays.forEach { dateStr ->
                    if (dateStr == null) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFF020617), shape = RoundedCornerShape(2.dp))
                        )
                    } else {
                        val mins = historyMap[dateStr] ?: 0
                        val color = when {
                            mins == 0    -> Color(0xFF0F172A)
                            mins < 30    -> Color(0x4D8B5CF6)
                            mins < 60    -> Color(0x998B5CF6)
                            mins < 180   -> Color(0xFF8B5CF6)
                            mins < 360   -> Color(0xFFEC4899)
                            else         -> Color(0xFFF43F5E) // 6h to 24h
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, shape = RoundedCornerShape(2.dp))
                                .clickable {
                                    val h = mins / 60
                                    val m = mins % 60
                                    val label = if (mins == 0) "$dateStr: 0 minutes studied" else "$dateStr: ${if (h > 0) "${h}h " else ""}${m}m studied"
                                    Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyBarChart(
    history: List<StudyHistoryEntity>,
    selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
) {
    // All computation wrapped in remember
    val (weeklyMinutes, dayLabels, maxMinutes) = remember(history, selectedDate) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = try { sdf.parse(selectedDate) ?: Date() } catch (e: Exception) { Date() }
        val dates = (0..6).map { i ->
            val c = Calendar.getInstance().apply { time = parsedDate }
            c.add(Calendar.DAY_OF_YEAR, -i)
            sdf.format(c.time)
        }.reversed()

        val historyMap = history.associate { it.date to it.studyMinutes }
        val mins = dates.map { historyMap[it] ?: 0 }
        val labels = dates.map { it.substring(8, 10) }
        val max = mins.maxOrNull()?.coerceAtLeast(30) ?: 30
        Triple(mins, labels, max)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom
    ) {
        weeklyMinutes.forEachIndexed { idx, mins ->
            val targetHeightPercentage = mins.toFloat() / maxMinutes.toFloat()
            var animationTriggered by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                animationTriggered = true
            }
            
            val animatedHeightPercentage by animateFloatAsState(
                targetValue = if (animationTriggered) targetHeightPercentage else 0f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing),
                label = "barHeight"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "${mins}m", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .fillMaxHeight(animatedHeightPercentage.coerceIn(0.02f, 0.9f))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                            ),
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = dayLabels[idx], fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
fun CategoryDistributionChart(categoryCounts: Map<String, Int>) {
    val total = remember(categoryCounts) { categoryCounts.values.sum().coerceAtLeast(1) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (categoryCounts.isEmpty()) {
            Text(
                text = "No tasks categorized yet.",
                color = Color(0xFF475569),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            categoryCounts.forEach { (category, count) ->
                val percentage = (count.toFloat() / total.toFloat()) * 100
                val targetFraction = count.toFloat() / total.toFloat()
                var animationTriggered by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    animationTriggered = true
                }
                
                val animatedFraction by animateFloatAsState(
                    targetValue = if (animationTriggered) targetFraction else 0f,
                    animationSpec = tween(1000, easing = FastOutSlowInEasing),
                    label = "fraction"
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = category, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(text = "$count tasks (${percentage.toInt()}%)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color(0xFF020617), shape = RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedFraction)
                                .fillMaxHeight()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}
