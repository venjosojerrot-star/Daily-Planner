package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlannerEvent
import com.example.data.model.PlannerTask
import com.example.util.DateTimeUtils
import java.util.Calendar

@Composable
fun MonthGridView(
    selectedDateMillis: Long,
    allEvents: List<PlannerEvent>,
    allTasks: List<PlannerTask>,
    dailyMoods: Map<String, String> = emptyMap(),
    dailyJournals: Map<String, String> = emptyMap(),
    onDateSelect: (Long) -> Unit,
    onNavigateMonth: (Int) -> Unit = {},
    onSaveMood: (Long, String) -> Unit = { _, _ -> },
    onRemoveMood: (Long) -> Unit = {},
    onSaveJournal: (Long, String) -> Unit = { _, _ -> },
    onRemoveJournal: (Long) -> Unit = {}
) {
    var moodPickerDateMillis by remember { mutableStateOf<Long?>(null) }
    var accumulatedDragX by remember { mutableStateOf(0f) }

    // Target Month Calendar setup
    val monthCal = Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
    }
    val currentDisplayMonth = monthCal.get(Calendar.MONTH)
    val currentDisplayYear = monthCal.get(Calendar.YEAR)

    // Calculate start of grid: First day of week of 1st day of month
    val gridStartCal = Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
        set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = get(Calendar.DAY_OF_WEEK)
        add(Calendar.DAY_OF_MONTH, -(firstDayOfWeek - 1))
    }

    val monthGridDays = remember(selectedDateMillis) {
        val days = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = gridStartCal.timeInMillis
        }
        for (i in 0 until 42) { // 6 weeks = 42 days
            days.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        days
    }

    val weekdays = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(selectedDateMillis) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        accumulatedDragX = 0f
                    },
                    onDragEnd = {
                        if (accumulatedDragX < -70f) {
                            // Swiped Left -> Next Month
                            onNavigateMonth(1)
                        } else if (accumulatedDragX > 70f) {
                            // Swiped Right -> Previous Month
                            onNavigateMonth(-1)
                        }
                        accumulatedDragX = 0f
                    },
                    onDragCancel = {
                        accumulatedDragX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragX += dragAmount
                    }
                )
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("month_grid_view")
    ) {
        // Month Navigation Header Toolbar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onNavigateMonth(-1) },
                    modifier = Modifier.size(36.dp).testTag("prev_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = DateTimeUtils.formatMonthYear(selectedDateMillis),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { onNavigateMonth(1) },
                    modifier = Modifier.size(36.dp).testTag("next_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Weekday Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekdays.forEach { day ->
                Text(
                    text = day,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (day == "SUN" || day == "SAT") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 6 Rows x 7 Columns
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (row in 0 until 6) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 7) {
                        val index = row * 7 + col
                        val dayMillis = monthGridDays[index]

                        val dayCal = Calendar.getInstance().apply { timeInMillis = dayMillis }
                        val isCurrentMonth = dayCal.get(Calendar.MONTH) == currentDisplayMonth &&
                                dayCal.get(Calendar.YEAR) == currentDisplayYear

                        val isSelected = DateTimeUtils.isSameDay(dayMillis, selectedDateMillis)
                        val isToday = DateTimeUtils.isToday(dayMillis)

                        val startOfDay = Calendar.getInstance().apply {
                            timeInMillis = dayMillis
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        val endOfDay = Calendar.getInstance().apply {
                            timeInMillis = dayMillis
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis

                        val dayEventCount = allEvents.count { it.startEpochMillis in startOfDay..endOfDay }
                        val dayTaskCount = allTasks.count {
                            it.dueDateEpochMillis in startOfDay..endOfDay
                        }

                        val dateKey = DateTimeUtils.formatDateKey(dayMillis)
                        val moodEmoji = dailyMoods[dateKey]
                        val journalText = dailyJournals[dateKey]

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .pointerInput(dayMillis) {
                                    detectTapGestures(
                                        onTap = {
                                            onDateSelect(dayMillis)
                                        },
                                        onLongPress = {
                                            moodPickerDateMillis = dayMillis
                                        }
                                    )
                                }
                                .testTag("month_grid_day_${DateTimeUtils.formatDayOfMonth(dayMillis)}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                else if (!isCurrentMonth) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 0.5.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 2.dp, vertical = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Day Number Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isToday) MaterialTheme.colorScheme.primary
                                                else Color.Transparent
                                            )
                                    ) {
                                        Text(
                                            text = DateTimeUtils.formatDayOfMonth(dayMillis),
                                            fontSize = 11.sp,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isToday) MaterialTheme.colorScheme.onPrimary
                                            else if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else if (!isCurrentMonth) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Mood & Journal indicator badges
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (!journalText.isNullOrBlank()) {
                                            Icon(
                                                imageVector = Icons.Default.AutoStories,
                                                contentDescription = "Journal Diary Entry",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(11.dp)
                                                    .testTag("journal_badge_$dateKey")
                                            )
                                        }

                                        if (!moodEmoji.isNullOrBlank()) {
                                            Text(
                                                text = moodEmoji,
                                                fontSize = 13.sp,
                                                modifier = Modifier.testTag("mood_badge_$dateKey")
                                            )
                                        }
                                    }
                                }

                                // Day Content: Event indicators & Task indicators
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    if (dayEventCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                                                .padding(horizontal = 2.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (dayEventCount == 1) "1 event" else "$dayEventCount events",
                                                fontSize = 7.5.sp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (dayTaskCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f))
                                                .padding(horizontal = 2.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (dayTaskCount == 1) "1 task" else "$dayTaskCount tasks",
                                                fontSize = 7.5.sp,
                                                color = MaterialTheme.colorScheme.onTertiary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
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
    }

    // Mood & Journal Diary Picker Dialog when long pressed on any day
    moodPickerDateMillis?.let { dateMillis ->
        val dateKey = DateTimeUtils.formatDateKey(dateMillis)
        val currentMood = dailyMoods[dateKey]
        val currentJournal = dailyJournals[dateKey]

        MoodPickerDialog(
            dateMillis = dateMillis,
            currentMoodEmoji = currentMood,
            currentJournalText = currentJournal,
            onSaveMood = { emoji ->
                onSaveMood(dateMillis, emoji)
            },
            onRemoveMood = {
                onRemoveMood(dateMillis)
            },
            onSaveJournal = { text ->
                onSaveJournal(dateMillis, text)
            },
            onRemoveJournal = {
                onRemoveJournal(dateMillis)
            },
            onDismiss = {
                moodPickerDateMillis = null
            }
        )
    }
}
