package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlin.math.ceil
import java.util.Calendar
import com.example.data.model.PlannerEvent
import com.example.util.DateTimeUtils
import com.example.util.CategoryGoalsManager

@Composable
fun EventsStatsScreen(
    selectedDateMillis: Long,
    allEvents: List<PlannerEvent>,
    onToggleHappened: ((PlannerEvent) -> Unit)? = null
) {
    val context = LocalContext.current
    var activeWeekCenterMillis by remember(selectedDateMillis) { mutableStateOf(selectedDateMillis) }
    var goalsVersion by remember { mutableIntStateOf(0) }
    var showAllGoalsDialog by remember { mutableStateOf(false) }
    var editingGoalCategory by remember { mutableStateOf<String?>(null) }

    val categoryGoals = remember(goalsVersion) {
        CategoryGoalsManager.getAllDailyGoals(context)
    }
    // Determine the week
    val weekDays = remember(activeWeekCenterMillis) { DateTimeUtils.getWeekDays(activeWeekCenterMillis) }
    val startOfWeek = weekDays.first()
    val endOfWeek = weekDays.last()
    
    // Start of Sunday 00:00:00.000 to end of Saturday 23:59:59.999
    val weekStartMillis = remember(startOfWeek) {
        Calendar.getInstance().apply {
            timeInMillis = startOfWeek
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val weekEndMillis = remember(endOfWeek) {
        Calendar.getInstance().apply {
            timeInMillis = endOfWeek
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
    
    // An event is recorded in weekly stats ONLY when it has happened (isHappened == true)
    // and overlaps with the week window
    val weekEvents = remember(activeWeekCenterMillis, allEvents, weekStartMillis, weekEndMillis) {
        allEvents.filter { event ->
            event.isHappened && run {
                val eventStart = event.startEpochMillis
                val eventEnd = if (event.endEpochMillis > event.startEpochMillis) event.endEpochMillis else (event.startEpochMillis + 3600000L)
                eventStart <= weekEndMillis && eventEnd >= weekStartMillis
            }
        }
    }
    
    val totalEvents = weekEvents.size
    
    // Accurately compute duration clipped to the weekly window for happened events
    val totalHours = remember(weekEvents, weekStartMillis, weekEndMillis) {
        val totalMillis = weekEvents.sumOf { event ->
            val eventStart = event.startEpochMillis
            val eventEnd = if (event.endEpochMillis > event.startEpochMillis) event.endEpochMillis else (event.startEpochMillis + 3600000L)
            val clippedStart = maxOf(eventStart, weekStartMillis)
            val clippedEnd = minOf(eventEnd, weekEndMillis)
            if (clippedEnd > clippedStart) clippedEnd - clippedStart else 0L
        }
        totalMillis / (1000.0 * 60 * 60)
    }
    
    // Calculate accurate stats by category for happened events
    val statsByCategory = remember(weekEvents, weekStartMillis, weekEndMillis) {
        weekEvents.groupBy { it.category.ifBlank { "Uncategorized" } }
            .mapValues { (_, events) ->
                val count = events.size
                val millis = events.sumOf { event ->
                    val eventStart = event.startEpochMillis
                    val eventEnd = if (event.endEpochMillis > event.startEpochMillis) event.endEpochMillis else (event.startEpochMillis + 3600000L)
                    val clippedStart = maxOf(eventStart, weekStartMillis)
                    val clippedEnd = minOf(eventEnd, weekEndMillis)
                    if (clippedEnd > clippedStart) clippedEnd - clippedStart else 0L
                }
                val hours = millis / (1000.0 * 60 * 60)
                val colorHex = events.firstOrNull()?.colorHex ?: "#808080"
                CategoryStat(count, hours, colorHex)
            }
            .toList()
            .sortedByDescending { it.second.hours }
    }

    var selectedCategoryForDetail by remember { mutableStateOf<Pair<String, CategoryStat>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Centered Header with Title and Week Navigation below
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Weekly Events Stats",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Previous / Next Week Selector directly below the centered title
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            activeWeekCenterMillis -= 7 * 24 * 60 * 60 * 1000L
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous week",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "${DateTimeUtils.formatDayMonth(weekStartMillis)} - ${DateTimeUtils.formatDayMonth(weekEndMillis)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            activeWeekCenterMillis += 7 * 24 * 60 * 60 * 1000L
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next week",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Total Events",
                value = "$totalEvents",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Total Hours",
                value = String.format("%.1f", totalHours),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (statsByCategory.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp).fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Happened Events Recorded",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Only events marked as happened are recorded in weekly stats.\nClick the checkbox on any event in your schedule to record it here.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Category Breakdown & Donut Chart
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Distribution",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { showAllGoalsDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Set Goals", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Circle Graph centered above Category Distribution list
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChart(statsByCategory, totalHours)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.1f", totalHours),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Total Hrs",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Distribution List
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statsByCategory.forEach { (category, stat) ->
                            val goalHours = categoryGoals[category] ?: 0.0
                            CategoryStatRow(
                                category = category,
                                stat = stat,
                                totalHours = totalHours,
                                goalHours = goalHours,
                                onSetGoal = {
                                    editingGoalCategory = category
                                },
                                onClick = {
                                    selectedCategoryForDetail = category to stat
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showAllGoalsDialog) {
        SetCategoryGoalsDialog(
            allEvents = allEvents,
            onDismiss = { showAllGoalsDialog = false },
            onGoalsSaved = { goalsVersion++ }
        )
    }

    if (editingGoalCategory != null) {
        val catName = editingGoalCategory!!
        val colorHex = allEvents.firstOrNull { it.category.equals(catName, ignoreCase = true) }?.colorHex
            ?: categoryPresets.firstOrNull { it.name.equals(catName, ignoreCase = true) }?.colorHex
            ?: "#808080"
        EditSingleCategoryGoalDialog(
            categoryName = catName,
            colorHex = colorHex,
            onDismiss = { editingGoalCategory = null },
            onGoalSaved = {
                goalsVersion++
                editingGoalCategory = null
            }
        )
    }

    if (selectedCategoryForDetail != null) {
        val (categoryName, stat) = selectedCategoryForDetail!!
        CategoryDetailDialog(
            categoryName = categoryName,
            colorHex = stat.colorHex,
            allEvents = allEvents,
            initialDateMillis = selectedDateMillis,
            onDismiss = { selectedCategoryForDetail = null },
            onGoalsUpdated = { goalsVersion++ },
            onToggleHappened = onToggleHappened
        )
    }
}

@Composable
fun DonutChart(stats: List<Pair<String, CategoryStat>>, totalHours: Double) {
    if (totalHours <= 0) return
    Canvas(modifier = Modifier.size(120.dp)) {
        var startAngle = -90f
        for ((_, stat) in stats) {
            val sweepAngle = ((stat.hours / totalHours) * 360).toFloat()
            val color = DateTimeUtils.parseColor(stat.colorHex)
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

data class CategoryStat(val count: Int, val hours: Double, val colorHex: String)

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.5.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Helper to check if a category is a leisure/others category, where daily targets
 * represent a maximum limit hour rather than a minimum productivity goal.
 */
fun isLimitCategory(category: String): Boolean {
    val lower = category.lowercase().trim()
    return lower.contains("leisure") || lower.contains("other") || lower.contains("rest / other")
}

@Composable
fun CategoryStatRow(
    category: String,
    stat: CategoryStat,
    totalHours: Double,
    goalHours: Double = 0.0,
    onSetGoal: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val color = DateTimeUtils.parseColor(stat.colorHex)
    val hasGoal = goalHours > 0.0
    val isLimit = isLimitCategory(category)
    val weeklyTarget = goalHours * 7.0
    val progress = if (hasGoal) {
        (stat.hours / weeklyTarget).toFloat().coerceIn(0f, 1f)
    } else {
        if (totalHours > 0) (stat.hours / totalHours).toFloat() else 0f
    }
    val isGoalMet = hasGoal && !isLimit && stat.hours >= weeklyTarget
    val isOverLimit = hasGoal && isLimit && stat.hours > weeklyTarget
    
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = category,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (hasGoal) {
                        Spacer(modifier = Modifier.width(6.dp))
                        if (isLimit) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isOverLimit) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isOverLimit) "⚠️ Over Limit" else "🛡️ ${String.format(Locale.US, "%.1f", goalHours)}h limit",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverLimit) Color(0xFFDC2626) else Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isGoalMet) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isGoalMet) "🎯 Met" else "${String.format(Locale.US, "%.1f", goalHours)}h/d",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGoalMet) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = if (hasGoal) {
                        if (isLimit) {
                            "${stat.count} ev • ${String.format(Locale.US, "%.1f", stat.hours)}h / ${String.format(Locale.US, "%.1f", weeklyTarget)}h limit (${(stat.hours / weeklyTarget * 100).toInt()}%)"
                        } else {
                            "${stat.count} ev • ${String.format(Locale.US, "%.1f", stat.hours)}h / ${String.format(Locale.US, "%.1f", weeklyTarget)}h (${(stat.hours / weeklyTarget * 100).toInt()}%)"
                        }
                    } else {
                        "${stat.count} ev • ${String.format(Locale.US, "%.1f", stat.hours)}h"
                    },
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            // Progress bar indicator
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            if (isLimit) {
                                if (isOverLimit) Color(0xFFEF4444) else Color(0xFF10B981)
                            } else {
                                if (isGoalMet) Color(0xFF10B981) else color
                            },
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun CategoryDetailDialog(
    categoryName: String,
    colorHex: String,
    allEvents: List<PlannerEvent>,
    initialDateMillis: Long,
    onDismiss: () -> Unit,
    onGoalsUpdated: () -> Unit = {},
    onToggleHappened: ((PlannerEvent) -> Unit)? = null
) {
    val context = LocalContext.current
    var currentDateMillis by remember { mutableStateOf(initialDateMillis) }
    var detailGoalsVersion by remember { mutableIntStateOf(0) }
    var showSingleGoalEdit by remember { mutableStateOf(false) }

    val isLimit = remember(categoryName) { isLimitCategory(categoryName) }

    val dailyGoalHours = remember(categoryName, detailGoalsVersion) {
        CategoryGoalsManager.getDailyGoalHours(context, categoryName)
    }
    
    val weekDays = remember(currentDateMillis) { DateTimeUtils.getWeekDays(currentDateMillis) }
    val startOfWeek = weekDays.first()
    val endOfWeek = weekDays.last()
    
    val weekStartMillis = startOfWeek
    val weekEndMillis = endOfWeek + (24 * 60 * 60 * 1000L) - 1L

    // Only events that have happened are recorded in category stats
    val categoryEvents = remember(allEvents, categoryName, weekStartMillis, weekEndMillis) {
        allEvents.filter { event ->
            event.isHappened &&
            event.category.ifBlank { "Uncategorized" } == categoryName &&
            run {
                val eventStart = event.startEpochMillis
                val eventEnd = if (event.endEpochMillis > event.startEpochMillis) event.endEpochMillis else (event.startEpochMillis + 3600000L)
                eventStart <= weekEndMillis && eventEnd >= weekStartMillis
            }
        }
    }
    
    val dailyHours = remember(categoryEvents, weekDays) {
        weekDays.map { dayMillis ->
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = dayMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = dayCal.timeInMillis
            dayCal.set(Calendar.HOUR_OF_DAY, 23)
            dayCal.set(Calendar.MINUTE, 59)
            dayCal.set(Calendar.SECOND, 59)
            dayCal.set(Calendar.MILLISECOND, 999)
            val dayEnd = dayCal.timeInMillis

            val millis = categoryEvents.sumOf { event ->
                val eventStart = event.startEpochMillis
                val eventEnd = if (event.endEpochMillis > event.startEpochMillis) event.endEpochMillis else (event.startEpochMillis + 3600000L)
                val clippedStart = maxOf(eventStart, dayStart)
                val clippedEnd = minOf(eventEnd, dayEnd)
                if (clippedEnd > clippedStart) clippedEnd - clippedStart else 0L
            }
            millis / (1000.0 * 60 * 60)
        }
    }
    
    val currentDayStart = Calendar.getInstance().apply { 
        timeInMillis = currentDateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val currentDayEnd = Calendar.getInstance().apply {
        timeInMillis = currentDateMillis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    val currentDayMillis = categoryEvents.sumOf { event ->
        val eventStart = event.startEpochMillis
        val eventEnd = if (event.endEpochMillis > event.startEpochMillis) event.endEpochMillis else (event.startEpochMillis + 3600000L)
        val clippedStart = maxOf(eventStart, currentDayStart)
        val clippedEnd = minOf(eventEnd, currentDayEnd)
        if (clippedEnd > clippedStart) clippedEnd - clippedStart else 0L
    }
    val currentDayHours = currentDayMillis / (1000.0 * 60 * 60)
    val currentDayTotalMinutes = (currentDayMillis / (1000.0 * 60)).toInt()
    val hoursPart = currentDayTotalMinutes / 60
    val minsPart = currentDayTotalMinutes % 60
    val hoursText = if (hoursPart > 0 && minsPart > 0) {
        "$hoursPart hr, $minsPart min"
    } else if (hoursPart > 0) {
        "$hoursPart hr"
    } else if (minsPart > 0) {
        "$minsPart min"
    } else {
        "0 hr"
    }
    
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val dateText = dateFormat.format(Date(currentDateMillis))

    // Events on the selected day for this category
    val currentDayHappenedEvents = remember(allEvents, currentDateMillis, categoryName) {
        allEvents.filter { event ->
            event.isHappened &&
            event.category.ifBlank { "Uncategorized" } == categoryName &&
            run {
                val eventStart = event.startEpochMillis
                val eventEnd = if (event.endEpochMillis > event.startEpochMillis) event.endEpochMillis else (event.startEpochMillis + 3600000L)
                eventStart <= currentDayEnd && eventEnd >= currentDayStart
            }
        }
    }

    val currentDayPendingEvents = remember(allEvents, currentDateMillis, categoryName) {
        allEvents.filter { event ->
            !event.isHappened &&
            event.category.ifBlank { "Uncategorized" } == categoryName &&
            run {
                val eventStart = event.startEpochMillis
                val eventEnd = if (event.endEpochMillis > event.startEpochMillis) event.endEpochMillis else (event.startEpochMillis + 3600000L)
                eventStart <= currentDayEnd && eventEnd >= currentDayStart
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Category Name Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DateTimeUtils.parseColor(colorHex).copy(alpha = 0.15f),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(DateTimeUtils.parseColor(colorHex), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = categoryName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DateTimeUtils.parseColor(colorHex)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = hoursText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = dateText,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Daily Hours Goal / Limit Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSingleGoalEdit = true }
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isLimit) Icons.Default.Timer else Icons.Default.Flag,
                                contentDescription = null,
                                tint = if (dailyGoalHours > 0) {
                                    if (isLimit) Color(0xFFF59E0B) else Color(0xFFE11D48)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (dailyGoalHours > 0) {
                                        if (isLimit) {
                                            "Limit Hour: ${String.format(Locale.US, "%.1f", dailyGoalHours)} hrs/day limit"
                                        } else {
                                            "Daily Goal: ${String.format(Locale.US, "%.1f", dailyGoalHours)} hrs/day"
                                        }
                                    } else {
                                        if (isLimit) "Limit Hour: Not set" else "Daily Goal: Not set"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (dailyGoalHours > 0) {
                                    if (isLimit) {
                                        val isDayOverLimit = currentDayHours > dailyGoalHours
                                        Text(
                                            text = if (isDayOverLimit) {
                                                "⚠️ Over Limit! (+${String.format(Locale.US, "%.1f", currentDayHours - dailyGoalHours)}h over)"
                                            } else {
                                                "🛡️ Within Limit: ${String.format(Locale.US, "%.1f", currentDayHours)} / ${String.format(Locale.US, "%.1f", dailyGoalHours)} hrs (${(currentDayHours / dailyGoalHours * 100).toInt()}%)"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDayOverLimit) Color(0xFFDC2626) else Color(0xFF059669)
                                        )
                                    } else {
                                        val isDayGoalMet = currentDayHours >= dailyGoalHours
                                        Text(
                                            text = if (isDayGoalMet) {
                                                "🎉 Goal Met! (+${String.format(Locale.US, "%.1f", currentDayHours - dailyGoalHours)}h)"
                                            } else {
                                                "${String.format(Locale.US, "%.1f", currentDayHours)} / ${String.format(Locale.US, "%.1f", dailyGoalHours)} hrs (${(currentDayHours / dailyGoalHours * 100).toInt()}%)"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDayGoalMet) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (isLimit) "Tap to set a daily limit hour" else "Tap to set a daily hours goal",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { showSingleGoalEdit = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = if (isLimit) "Edit Limit Hour" else "Edit Goal",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
                
                BarChart(
                    dailyHours = dailyHours,
                    weekDays = weekDays,
                    currentDateMillis = currentDateMillis,
                    colorHex = colorHex,
                    goalHours = dailyGoalHours,
                    isLimit = isLimit,
                    onDayClick = { newDateMillis -> currentDateMillis = newDateMillis }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentDateMillis -= 24 * 60 * 60 * 1000L }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                    }
                    
                    Text(
                        text = dateText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    IconButton(onClick = { currentDateMillis += 24 * 60 * 60 * 1000L }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                    }
                }

                // Day Events Breakdown
                if (currentDayHappenedEvents.isNotEmpty() || currentDayPendingEvents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentDayHappenedEvents.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recorded in Stats (${currentDayHappenedEvents.size})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        currentDayHappenedEvents.forEach { event ->
                            val startStr = DateTimeUtils.formatTime(event.startEpochMillis)
                            val endStr = DateTimeUtils.formatTime(event.endEpochMillis)
                            val durationMillis = (event.endEpochMillis - event.startEpochMillis).coerceAtLeast(0L)
                            val durationHours = durationMillis / (1000.0 * 60 * 60)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onToggleHappened?.invoke(event) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckBox,
                                            contentDescription = "Unmark Happened",
                                            tint = Color(0xFF00E676),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = event.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$startStr - $endStr",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.US, "%.1fh", durationHours),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676)
                                    )
                                }
                            }
                        }
                    }

                    if (currentDayPendingEvents.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scheduled (Not Happened Yet)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        currentDayPendingEvents.forEach { event ->
                            val startStr = DateTimeUtils.formatTime(event.startEpochMillis)
                            val endStr = DateTimeUtils.formatTime(event.endEpochMillis)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onToggleHappened?.invoke(event) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = "Mark Happened",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = event.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$startStr - $endStr • Tap to mark happened",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSingleGoalEdit) {
        EditSingleCategoryGoalDialog(
            categoryName = categoryName,
            colorHex = colorHex,
            onDismiss = { showSingleGoalEdit = false },
            onGoalSaved = {
                detailGoalsVersion++
                onGoalsUpdated()
            }
        )
    }
}

@Composable
fun BarChart(
    dailyHours: List<Double>,
    weekDays: List<Long>,
    currentDateMillis: Long,
    colorHex: String,
    goalHours: Double = 0.0,
    isLimit: Boolean = false,
    onDayClick: (Long) -> Unit
) {
    val maxActualHours = maxOf(dailyHours.maxOrNull() ?: 0.0, goalHours)
    // Dynamic max scale that provides a realistic ceiling without squishing small values
    val yMax = if (maxActualHours <= 2.0) {
        2
    } else if (maxActualHours <= 4.0) {
        4
    } else if (maxActualHours <= 8.0) {
        8
    } else if (maxActualHours <= 12.0) {
        12
    } else {
        (ceil(maxActualHours / 2.0).toInt() * 2).coerceAtLeast(14)
    }
    
    val step = when {
        yMax <= 4 -> 1
        yMax <= 10 -> 2
        yMax <= 16 -> 4
        else -> 6
    }
    val yLabels = (0..yMax step step).toList()
    val yMaxFloat = yMax.toFloat()

    val baseColor = DateTimeUtils.parseColor(colorHex)
    val unselectedColor = baseColor.copy(alpha = 0.35f)
    
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    Column(modifier = Modifier.fillMaxWidth().height(235.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Background grid lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    yLabels.reversed().forEach { _ ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )
                    }
                }

                // Dashed Daily Goal / Limit line across the chart
                if (goalHours > 0.0 && yMaxFloat > 0f) {
                    val goalFraction = (goalHours.toFloat() / yMaxFloat).coerceIn(0f, 1f)
                    val lineColor = if (isLimit) Color(0xFFF59E0B) else Color(0xFFE11D48)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val y = size.height * (1f - goalFraction)
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    }
                }
                
                // Bars with values
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailyHours.forEachIndexed { index, hours ->
                        val dayMillis = weekDays[index]
                        val isSelected = DateTimeUtils.isSameDay(dayMillis, currentDateMillis)
                        val isGoalMet = !isLimit && goalHours > 0.0 && hours >= goalHours
                        val isOverLimit = isLimit && goalHours > 0.0 && hours > goalHours
                        val barHeightFraction = if (yMaxFloat > 0f) {
                            (hours / yMaxFloat).toFloat().coerceIn(0f, 1f)
                        } else 0f
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onDayClick(dayMillis) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val topWeight = (1f - barHeightFraction).coerceAtLeast(0.001f)
                            val barWeight = barHeightFraction.coerceAtLeast(0.001f)

                            Spacer(modifier = Modifier.weight(topWeight))

                            if (isOverLimit) {
                                Text(
                                    text = "⚠️",
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            } else if (isGoalMet) {
                                Text(
                                    text = "⭐",
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }
                            if (hours > 0.05) {
                                Text(
                                    text = if (hours >= 10.0) String.format(Locale.US, "%.0f", hours) else String.format(Locale.US, "%.1f", hours),
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected || isGoalMet || isOverLimit) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isOverLimit) {
                                        Color(0xFFDC2626)
                                    } else if (isGoalMet) {
                                        Color(0xFF059669)
                                    } else if (isSelected) {
                                        baseColor
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .weight(barWeight)
                                    .background(
                                        color = if (isOverLimit) {
                                            Color(0xFFEF4444)
                                        } else if (isGoalMet) {
                                            Color(0xFF10B981)
                                        } else if (isSelected) {
                                            baseColor
                                        } else {
                                            unselectedColor
                                        },
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                        }
                    }
                }
            }
            
            // Y-axis labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yLabels.reversed().forEach { label ->
                    Text(
                        text = "${label}h",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // X-axis Day labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDays.forEach { dayMillis ->
                val isSelected = DateTimeUtils.isSameDay(dayMillis, currentDateMillis)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayFormat.format(Date(dayMillis)),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) baseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        if (goalHours > 0.0) {
            val indicatorColor = if (isLimit) Color(0xFFF59E0B) else Color(0xFFE11D48)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(indicatorColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isLimit) {
                        "Daily Limit: ${String.format(Locale.US, "%.1f", goalHours)}h (⚠️ = Over)"
                    } else {
                        "Daily Goal: ${String.format(Locale.US, "%.1f", goalHours)}h (⭐ = Met)"
                    },
                    fontSize = 10.5.sp,
                    color = indicatorColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SetCategoryGoalsDialog(
    allEvents: List<PlannerEvent>,
    onDismiss: () -> Unit,
    onGoalsSaved: () -> Unit
) {
    val context = LocalContext.current
    val savedGoals = remember { CategoryGoalsManager.getAllDailyGoals(context) }
    
    // Collect candidate categories
    val allCategories = remember(allEvents) {
        val fromEvents = allEvents.map { it.category.ifBlank { "Uncategorized" } }
        val fromPresets = categoryPresets.map { it.name }
        val fromGoals = savedGoals.keys
        (fromEvents + fromPresets + fromGoals).distinct().sorted()
    }
    
    val workingGoals = remember {
        mutableStateMapOf<String, Double>().apply {
            allCategories.forEach { cat ->
                put(cat, savedGoals[cat] ?: 0.0)
            }
        }
    }
    
    var newCategoryInput by remember { mutableStateOf("") }
    var isAddingNewCategory by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Category Goals & Limits",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Set daily goals or limit hours for each category",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val categoriesList = workingGoals.keys.toList().sorted()
                    items(categoriesList) { category ->
                        val currentGoal = workingGoals[category] ?: 0.0
                        val isLimit = isLimitCategory(category)
                        val colorHex = allEvents.firstOrNull { it.category.equals(category, ignoreCase = true) }?.colorHex
                            ?: categoryPresets.firstOrNull { it.name.equals(category, ignoreCase = true) }?.colorHex
                            ?: "#808080"
                        val color = DateTimeUtils.parseColor(colorHex)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentGoal > 0.0) {
                                    if (isLimit) Color(0xFFF59E0B).copy(alpha = 0.08f) else color.copy(alpha = 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = category,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isLimit) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Limit",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD97706),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (currentGoal > 0.0) {
                                            if (isLimit) "${String.format(Locale.US, "%.1f", currentGoal)}h limit/d" else "${String.format(Locale.US, "%.1f", currentGoal)} hrs/day"
                                        } else {
                                            if (isLimit) "No limit" else "No goal"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (currentGoal > 0.0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentGoal > 0.0) {
                                            if (isLimit) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                val next = (currentGoal - 0.5).coerceAtLeast(0.0)
                                                workingGoals[category] = (Math.round(next * 10.0) / 10.0)
                                            },
                                            modifier = Modifier.size(30.dp),
                                            enabled = currentGoal > 0.0
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                        }

                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", currentGoal)}h",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )

                                        FilledTonalIconButton(
                                            onClick = {
                                                val next = (currentGoal + 0.5).coerceAtMost(24.0)
                                                workingGoals[category] = (Math.round(next * 10.0) / 10.0)
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    if (currentGoal > 0.0) {
                                        TextButton(
                                            onClick = { workingGoals[category] = 0.0 },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Clear", color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add Custom Category Section
                    item {
                        if (isAddingNewCategory) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Add Custom Category Goal",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = newCategoryInput,
                                        onValueChange = { newCategoryInput = it },
                                        placeholder = { Text("Category name...", fontSize = 13.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { isAddingNewCategory = false; newCategoryInput = "" }) {
                                            Text("Cancel", fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = {
                                                val trimmed = newCategoryInput.trim()
                                                if (trimmed.isNotBlank()) {
                                                    workingGoals[trimmed] = 1.0
                                                    newCategoryInput = ""
                                                    isAddingNewCategory = false
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            enabled = newCategoryInput.isNotBlank()
                                        ) {
                                            Text("Add Category", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { isAddingNewCategory = true },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Custom Category Goal", fontSize = 12.5.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            workingGoals.forEach { (category, hours) ->
                                CategoryGoalsManager.setDailyGoalHours(context, category, hours)
                            }
                            onGoalsSaved()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Goals")
                    }
                }
            }
        }
    }
}

@Composable
fun EditSingleCategoryGoalDialog(
    categoryName: String,
    colorHex: String,
    onDismiss: () -> Unit,
    onGoalSaved: () -> Unit
) {
    val context = LocalContext.current
    val isLimit = remember(categoryName) { isLimitCategory(categoryName) }
    var currentGoal by remember {
        mutableDoubleStateOf(CategoryGoalsManager.getDailyGoalHours(context, categoryName))
    }
    val categoryColor = DateTimeUtils.parseColor(colorHex)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with category badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = categoryColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(categoryColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = categoryName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor
                        )
                    }
                }

                Text(
                    text = if (isLimit) "Daily Limit Hour" else "Daily Hours Goal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isLimit) {
                        "Set a maximum limit of hours per day for this category"
                    } else {
                        "How many hours per day do you aim to spend?"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                // Large Hours Display with Steppers
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            val next = (currentGoal - 0.5).coerceAtLeast(0.0)
                            currentGoal = Math.round(next * 10.0) / 10.0
                        },
                        modifier = Modifier.size(44.dp),
                        enabled = currentGoal > 0.0
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentGoal > 0.0) String.format(Locale.US, "%.1f", currentGoal) else "0.0",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentGoal > 0.0) {
                                if (isLimit) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = if (isLimit) "limit hours / day" else "hours / day",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    FilledTonalIconButton(
                        onClick = {
                            val next = (currentGoal + 0.5).coerceAtMost(24.0)
                            currentGoal = Math.round(next * 10.0) / 10.0
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(20.dp))
                    }
                }

                if (currentGoal > 0.0) {
                    Text(
                        text = if (isLimit) {
                            "Weekly limit: ~${String.format(Locale.US, "%.1f", currentGoal * 7)} hours / week max"
                        } else {
                            "Weekly target: ~${String.format(Locale.US, "%.1f", currentGoal * 7)} hours / week"
                        },
                        fontSize = 12.sp,
                        color = if (isLimit) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentGoal > 0.0) {
                        TextButton(
                            onClick = {
                                CategoryGoalsManager.removeDailyGoal(context, categoryName)
                                onGoalSaved()
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = if (isLimit) "Clear Limit" else "Clear Goal",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                CategoryGoalsManager.setDailyGoalHours(context, categoryName, currentGoal)
                                onGoalSaved()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isLimit) "Save Limit" else "Save Goal")
                        }
                    }
                }
            }
        }
    }
}
