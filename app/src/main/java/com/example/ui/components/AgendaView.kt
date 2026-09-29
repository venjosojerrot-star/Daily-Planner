package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlannerEvent
import com.example.data.model.PlannerTask
import com.example.util.DateTimeUtils

sealed class AgendaItem {
    data class EventItem(val event: PlannerEvent) : AgendaItem()
    data class TaskItem(val task: PlannerTask) : AgendaItem()

    val timeMillis: Long
        get() = when (this) {
            is EventItem -> event.startEpochMillis
            is TaskItem -> task.dueDateEpochMillis
        }
}

@Composable
fun AgendaView(
    events: List<PlannerEvent>,
    tasks: List<PlannerTask>,
    onEventClick: (PlannerEvent) -> Unit,
    onTaskClick: (PlannerTask) -> Unit,
    onToggleTask: (PlannerTask) -> Unit,
    isBatchCheckMode: Boolean = false,
    selectedEventIds: Set<Long> = emptySet(),
    onToggleEventSelection: (Long) -> Unit = {},
    onToggleHappened: (PlannerEvent) -> Unit = {}
) {
    // Sort logic: High, Medium, Low, then by time
    fun getSortRank(item: AgendaItem): Int {
        return when (item) {
            is AgendaItem.TaskItem -> {
                when (item.task.priority.uppercase()) {
                    "HIGH" -> 1
                    "MEDIUM" -> 2
                    "LOW" -> 3
                    else -> 4
                }
            }
            is AgendaItem.EventItem -> 4 // Put events together after Medium
        }
    }

    val allItems = (events.map { AgendaItem.EventItem(it) } + tasks.map { AgendaItem.TaskItem(it) })
        .sortedWith(compareBy({ getSortRank(it) }, { it.timeMillis }))

    // Split into Not Completed and Completed
    val notCompletedItems = allItems.filter {
        when (it) {
            is AgendaItem.EventItem -> !it.event.isHappened
            is AgendaItem.TaskItem -> !it.task.isCompleted
        }
    }

    val completedItems = allItems.filter {
        when (it) {
            is AgendaItem.EventItem -> it.event.isHappened
            is AgendaItem.TaskItem -> it.task.isCompleted
        }
    }

    if (allItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No upcoming items in agenda.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Not Completed (${notCompletedItems.size})",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Completed (${completedItems.size})",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            if (selectedTab == 0) {
                if (notCompletedItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All caught up!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(notCompletedItems, key = {
                            when (it) {
                                is AgendaItem.EventItem -> "notcomp_event_${it.event.id}"
                                is AgendaItem.TaskItem -> "notcomp_task_${it.task.id}"
                            }
                        }) { item ->
                            AgendaItemRow(
                                item = item,
                                selectedEventIds = selectedEventIds,
                                isBatchCheckMode = isBatchCheckMode,
                                onToggleEventSelection = onToggleEventSelection,
                                onToggleHappened = onToggleHappened,
                                onEventClick = onEventClick,
                                onTaskClick = onTaskClick,
                                onToggleTask = onToggleTask
                            )
                        }
                    }
                }
            } else {
                if (completedItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No completed items yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(completedItems, key = {
                            when (it) {
                                is AgendaItem.EventItem -> "comp_event_${it.event.id}"
                                is AgendaItem.TaskItem -> "comp_task_${it.task.id}"
                            }
                        }) { item ->
                            AgendaItemRow(
                                item = item,
                                selectedEventIds = selectedEventIds,
                                isBatchCheckMode = isBatchCheckMode,
                                onToggleEventSelection = onToggleEventSelection,
                                onToggleHappened = onToggleHappened,
                                onEventClick = onEventClick,
                                onTaskClick = onTaskClick,
                                onToggleTask = onToggleTask
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaItemRow(
    item: AgendaItem,
    selectedEventIds: Set<Long>,
    isBatchCheckMode: Boolean,
    onToggleEventSelection: (Long) -> Unit,
    onToggleHappened: (PlannerEvent) -> Unit,
    onEventClick: (PlannerEvent) -> Unit,
    onTaskClick: (PlannerTask) -> Unit,
    onToggleTask: (PlannerTask) -> Unit
) {
    when (item) {
        is AgendaItem.EventItem -> {
            val event = item.event
            val color = DateTimeUtils.parseColor(event.colorHex)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (event.isHappened) 0.6f else 1.0f)
                    .clickable {
                        if (isBatchCheckMode) {
                            onToggleEventSelection(event.id)
                        } else {
                            onEventClick(event)
                        }
                    }
                    .testTag("agenda_event_${event.id}"),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (event.isHappened) Color.Transparent else MaterialTheme.colorScheme.surface
                ),
                border = if (event.id in selectedEventIds) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else if (event.isHappened) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                } else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (isBatchCheckMode) {
                                onToggleEventSelection(event.id)
                            } else {
                                onToggleHappened(event)
                            }
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("agenda_toggle_happened_${event.id}")
                    ) {
                        Icon(
                            imageVector = if (event.isHappened) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = if (event.isHappened) "Happened" else "Mark as Happened",
                            tint = if (event.isHappened) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${DateTimeUtils.formatDayMonth(event.startEpochMillis)} • ${DateTimeUtils.formatTime(event.startEpochMillis)}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        is AgendaItem.TaskItem -> {
            val task = item.task

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (task.isCompleted) 0.6f else 1.0f)
                    .clickable { onTaskClick(task) }
                    .testTag("agenda_task_${task.id}"),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (task.isCompleted) Color.Transparent else MaterialTheme.colorScheme.surface
                ),
                border = if (task.isCompleted) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleTask(task) },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Task • ${task.category}",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    val priorityColor = when (task.priority.uppercase()) {
                        "HIGH" -> Color(0xFFD32F2F)
                        "MEDIUM" -> Color(0xFFFBC02D)
                        "LOW" -> Color(0xFF757575)
                        else -> Color(0xFF757575)
                    }
                    val labelColor = if (task.priority.uppercase() == "MEDIUM") Color.Black else Color.White
                    AssistChip(
                        onClick = {},
                        label = { Text(task.priority, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = labelColor) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = priorityColor,
                            labelColor = labelColor
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
        }
    }
}
