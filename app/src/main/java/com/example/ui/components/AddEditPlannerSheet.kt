package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlannerEvent
import com.example.data.model.PlannerTask
import com.example.ui.viewmodel.PlannerEntryType
import com.example.util.DateTimeUtils
import com.example.util.SoundUtils
import java.util.Calendar

data class CategoryPreset(val name: String, val colorName: String, val colorHex: String)

val categoryPresets = listOf(
    CategoryPreset("Plan", "Grape", "#8E24AA"),
    CategoryPreset("Exercise", "Dark Green", "#1B5E20"),
    CategoryPreset("Sports", "Sage", "#558B2F"),
    CategoryPreset("Meal", "Tomato", "#D32F2F"),
    CategoryPreset("Prepare / Clean / Service", "Banana", "#FBC02D"),
    CategoryPreset("Work", "Peacock", "#039BE5"),
    CategoryPreset("Meeting / Class", "Blue Green", "#00897B"),
    CategoryPreset("Study", "Pink", "#EC407A"),
    CategoryPreset("Sleep", "Brown", "#795548"),
    CategoryPreset("Travel", "Flamingo", "#E91E63"),
    CategoryPreset("Leisure Time, Rest / Others", "Grey", "#757575")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlannerSheet(
    initialType: PlannerEntryType,
    selectedDateMillis: Long,
    editingEvent: PlannerEvent?,
    editingTask: PlannerTask?,
    onSaveEvent: (PlannerEvent) -> Unit,
    onSaveTask: (PlannerTask) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onDeleteEventSeries: (PlannerEvent) -> Unit = {},
    onDismiss: () -> Unit,
    existingEvents: List<PlannerEvent> = emptyList(),
    preselectedCategory: String? = null
) {
    val context = LocalContext.current
    var entryType by remember { mutableStateOf(initialType) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && editingEvent != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Repeating Event") },
            text = { Text("This event repeats (${editingEvent.recurrence}). Would you like to delete only this event or multiple events in this series?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteEventSeries(editingEvent)
                    }
                ) {
                    Text("Delete Multiple Events", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteEvent(editingEvent.id)
                            onDismiss()
                        }
                    ) {
                        Text("This Event Only")
                    }
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Event fields
    var eventTitle by remember { mutableStateOf(editingEvent?.title ?: "") }
    var eventDesc by remember { mutableStateOf(editingEvent?.description ?: "") }
    var eventLocation by remember { mutableStateOf(editingEvent?.location ?: "") }
    var eventCategory by remember { mutableStateOf(editingEvent?.category ?: "Work") }
    var eventColorHex by remember { mutableStateOf(editingEvent?.colorHex ?: "#039BE5") }
    var eventIsHappened by remember { mutableStateOf(editingEvent?.isHappened ?: false) }
    var eventRecurrence by remember { mutableStateOf(editingEvent?.recurrence ?: "Does not repeat") }
    var isRecurrenceDropdownOpen by remember { mutableStateOf(false) }

    var startEpochMillis by remember {
        mutableLongStateOf(
            editingEvent?.startEpochMillis ?: run {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis
                    val now = Calendar.getInstance()
                    set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
        )
    }

    var endEpochMillis by remember {
        mutableLongStateOf(
            editingEvent?.endEpochMillis ?: (startEpochMillis + 60 * 60 * 1000L)
        )
    }

    val daySchedules = remember(existingEvents, startEpochMillis, editingEvent) {
        existingEvents.filter { event ->
            DateTimeUtils.isSameDay(event.startEpochMillis, startEpochMillis) && event.id != editingEvent?.id
        }.sortedBy { it.startEpochMillis }
    }

    fun showStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = startEpochMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = startEpochMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                val duration = (endEpochMillis - startEpochMillis).coerceAtLeast(15 * 60 * 1000L)
                startEpochMillis = newCal.timeInMillis
                endEpochMillis = startEpochMillis + duration
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showStartTimePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = startEpochMillis }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = startEpochMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                val duration = (endEpochMillis - startEpochMillis).coerceAtLeast(15 * 60 * 1000L)
                startEpochMillis = newCal.timeInMillis
                endEpochMillis = startEpochMillis + duration
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    fun showEndDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = endEpochMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = endEpochMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                if (newCal.timeInMillis > startEpochMillis) {
                    endEpochMillis = newCal.timeInMillis
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showEndTimePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = endEpochMillis }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = endEpochMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                if (newCal.timeInMillis <= startEpochMillis) {
                    newCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                endEpochMillis = newCal.timeInMillis
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    // Task fields
    var taskTitle by remember { mutableStateOf(editingTask?.title ?: "") }
    var taskDesc by remember { mutableStateOf(editingTask?.description ?: "") }
    var taskCategory by remember { mutableStateOf(editingTask?.category ?: preselectedCategory ?: "Plan") }
    var taskPriority by remember { mutableStateOf(editingTask?.priority ?: "MEDIUM") }
    var taskColorHex by remember { mutableStateOf(editingTask?.colorHex ?: "#8E24AA") }
    var taskDurationMinutes by remember { mutableIntStateOf(editingTask?.durationMinutes ?: 30) }

    val colorOptions = listOf("#039BE5", "#8E24AA", "#0B8043", "#E67C73", "#D50000", "#F4511E", "#F6BF26")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Type Segmented Switcher if creating new item
            if (editingEvent == null && editingTask == null) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = entryType == PlannerEntryType.EVENT,
                        onClick = { entryType = PlannerEntryType.EVENT },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("switch_to_event")
                    ) {
                        Text("Calendar Event")
                    }
                    SegmentedButton(
                        selected = entryType == PlannerEntryType.TASK,
                        onClick = { entryType = PlannerEntryType.TASK },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("switch_to_task")
                    ) {
                        Text("To-Do Task")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (entryType == PlannerEntryType.EVENT) {
                if (editingEvent != null) {
                    Text(
                        text = "Edit Event",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = eventTitle,
                    onValueChange = { eventTitle = it },
                    label = { Text("Event Title", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("event_title_input")
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = eventDesc,
                    onValueChange = { eventDesc = it },
                    label = { Text("Description (Optional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("event_desc_input")
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Start/End & Notification Split Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left Half: Start & End Time Controls
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Start Row
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(text = "Start", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        onClick = { showStartDatePicker() },
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1f).testTag("event_start_date_btn")
                                    ) {
                                        Text(
                                            text = DateTimeUtils.formatDayMonth(startEpochMillis),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        onClick = { showStartTimePicker() },
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1f).testTag("event_start_time_btn")
                                    ) {
                                        Text(
                                            text = DateTimeUtils.formatTime12(startEpochMillis),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            // End Row
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                val isMultiDay = !DateTimeUtils.isSameDay(startEpochMillis, endEpochMillis)
                                Text(
                                    text = "End", 
                                    fontSize = 10.sp, 
                                    color = if (isMultiDay) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        onClick = { showEndDatePicker() },
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isMultiDay) Color.Red.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1f).testTag("event_end_date_btn")
                                    ) {
                                        Text(
                                            text = DateTimeUtils.formatDayMonth(endEpochMillis),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isMultiDay) Color.Red else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        onClick = { showEndTimePicker() },
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isMultiDay) Color.Red.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1f).testTag("event_end_time_btn")
                                    ) {
                                        Text(
                                            text = DateTimeUtils.formatTime12(endEpochMillis),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isMultiDay) Color.Red else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Right Half: Repeat Dropdown
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Repeat",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("event_repeat_dropdown")
                            ) {
                                Surface(
                                    onClick = { isRecurrenceDropdownOpen = true },
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = eventRecurrence,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Repeat",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = isRecurrenceDropdownOpen,
                                    onDismissRequest = { isRecurrenceDropdownOpen = false }
                                ) {
                                    val dayName = remember(startEpochMillis) {
                                        java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(java.util.Date(startEpochMillis))
                                    }
                                    val repeatOptions = listOf(
                                        "Does not repeat",
                                        "Everyday",
                                        "Weekly (every $dayName)",
                                        "Every week",
                                        "Every month"
                                    )
                                    repeatOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (eventRecurrence == option) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                eventRecurrence = option
                                                isRecurrenceDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Category selection
                Text("Category (Preset Colors)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categoryPresets.forEach { preset ->
                        val presetColor = DateTimeUtils.parseColor(preset.colorHex)
                        FilterChip(
                            selected = eventCategory == preset.name,
                            onClick = {
                                eventCategory = preset.name
                                eventColorHex = preset.colorHex
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(presetColor)
                                )
                            },
                            label = {
                                Text(
                                    text = preset.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (eventCategory == preset.name) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (editingEvent != null) {
                        IconButton(
                            onClick = {
                                if (editingEvent.recurrence != "Does not repeat") {
                                    showDeleteDialog = true
                                } else {
                                    onDeleteEvent(editingEvent.id)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.testTag("delete_event_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editingEvent != null) {
                            OutlinedButton(
                                onClick = {
                                    eventIsHappened = !eventIsHappened
                                    if (eventIsHappened) {
                                        SoundUtils.playAccomplishedSound()
                                    } else {
                                        SoundUtils.playClickSound()
                                    }
                                },
                                modifier = Modifier.testTag("event_happened_btn"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (eventIsHappened) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (eventIsHappened) Color(0xFF00E676) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(
                                    imageVector = if (eventIsHappened) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = "Happened Icon",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (eventIsHappened) "Happened" else "Mark Happened",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        val isMultiDayEvent = !DateTimeUtils.isSameDay(startEpochMillis, endEpochMillis)
                        Button(
                            onClick = {
                                if (eventTitle.isNotBlank()) {
                                    if (endEpochMillis <= startEpochMillis) {
                                        Toast.makeText(context, "End time must be after start time", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onSaveEvent(
                                            PlannerEvent(
                                                id = editingEvent?.id ?: 0L,
                                                title = eventTitle,
                                                description = eventDesc,
                                                startEpochMillis = startEpochMillis,
                                                endEpochMillis = endEpochMillis,
                                                colorHex = eventColorHex,
                                                location = eventLocation,
                                                category = eventCategory,
                                                recurrence = eventRecurrence,
                                                isHappened = eventIsHappened
                                            )
                                        )
                                    }
                                }
                            },
                            enabled = !isMultiDayEvent,
                            modifier = Modifier.testTag("save_event_btn")
                        ) {
                            Text("Save Event")
                        }
                    }
                }
            } else {
                // Task Creation
                Text(
                    text = if (editingTask == null) "New To-Do Task" else "Edit Task",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = taskDesc,
                    onValueChange = { taskDesc = it },
                    label = { Text("Details / Subtasks (Optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("task_desc_input")
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Priority Level", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { priority ->
                        FilterChip(
                            selected = taskPriority == priority,
                            onClick = { taskPriority = priority },
                            label = { Text(priority) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categoryPresets.forEach { preset ->
                        FilterChip(
                            selected = taskCategory == preset.name,
                            onClick = {
                                taskCategory = preset.name
                                taskColorHex = preset.colorHex
                            },
                            label = {
                                Text(
                                    text = preset.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (taskCategory == preset.name) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (editingTask != null) {
                        IconButton(
                            onClick = { onDeleteTask(editingTask.id) },
                            modifier = Modifier.testTag("delete_task_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (taskTitle.isNotBlank()) {
                                val dueCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis
                                }

                                onSaveTask(
                                    PlannerTask(
                                        id = editingTask?.id ?: 0L,
                                        title = taskTitle,
                                        description = taskDesc,
                                        dueDateEpochMillis = dueCal.timeInMillis,
                                        durationMinutes = taskDurationMinutes,
                                        isCompleted = editingTask?.isCompleted ?: false,
                                        priority = taskPriority,
                                        category = taskCategory,
                                        colorHex = taskColorHex
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("save_task_btn")
                    ) {
                        Text("Save Task")
                    }
                }
            }
        }
    }
}
