package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.PlannerEvent
import com.example.data.model.PlannerTask
import com.example.util.DateTimeUtils
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun DayScheduleView(
    selectedDateMillis: Long,
    events: List<PlannerEvent>,
    tasks: List<PlannerTask>,
    onEventClick: (PlannerEvent) -> Unit,
    onEventMove: (PlannerEvent, Long, Long) -> Unit = { _, _, _ -> },
    onTaskClick: (PlannerTask) -> Unit,
    onToggleTask: (PlannerTask) -> Unit,
    onAddClick: () -> Unit,
    onNavigateDate: (Int) -> Unit = {},
    isBatchCheckMode: Boolean = false,
    selectedEventIds: Set<Long> = emptySet(),
    onToggleEventSelection: (Long) -> Unit = {},
    onToggleHappened: (PlannerEvent) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val isToday = DateTimeUtils.isToday(selectedDateMillis)
    val currentOnNavigateDate by rememberUpdatedState(onNavigateDate)

    // Dynamic hour height state for pinch-to-zoom
    var hourHeightDp by remember { mutableFloatStateOf(64f) }
    val hourHeight = hourHeightDp.dp

    // Current hour / minute for indicator
    val currentCal = Calendar.getInstance()
    val currentHour = currentCal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentCal.get(Calendar.MINUTE)

    // Scroll to current hour on load
    LaunchedEffect(isToday) {
        if (isToday) {
            val targetY = (currentHour * hourHeightDp - 120).coerceAtLeast(0f)
            scrollState.scrollTo((targetY * 2.5f).toInt())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        hourHeightDp = (hourHeightDp * zoom).coerceIn(36f, 160f)
                    }
                }
            }
            .pointerInput(Unit) {
                var totalDragX = 0f
                var hasTriggered = false
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        hasTriggered = false
                    },
                    onDragEnd = {
                        if (!hasTriggered) {
                            if (totalDragX < -80f) { // Swipe Left -> Next Day (+1)
                                currentOnNavigateDate(1)
                            } else if (totalDragX > 80f) { // Swipe Right -> Previous Day (-1)
                                currentOnNavigateDate(-1)
                            }
                        }
                    },
                    onDragCancel = {
                        totalDragX = 0f
                        hasTriggered = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        totalDragX += dragAmount
                        if (!hasTriggered) {
                            if (totalDragX < -150f) { // Swipe Left -> Next Day (+1)
                                hasTriggered = true
                                currentOnNavigateDate(1)
                            } else if (totalDragX > 150f) { // Swipe Right -> Previous Day (-1)
                                hasTriggered = true
                                currentOnNavigateDate(-1)
                            }
                        }
                    }
                )
            }
    ) {
        // Single unified scroll container to keep grid and event cards permanently locked and aligned
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 24 Hour Slots Grid Layer
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                for (hour in 0..23) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(hourHeight)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Time Label Column centered on hour line
                            Text(
                                text = DateTimeUtils.formatTime24(hour, 0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB0B0B0),
                                modifier = Modifier
                                    .width(56.dp)
                                    .offset(y = (-8).dp)
                                    .padding(start = 12.dp)
                            )

                            // Divider Line at exact top of hour slot
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF2C2C2C),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }

            // Overlay Items Layer (Events & Tasks in exact same vertical coordinate space)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp, end = 2.dp)
            ) {
                val containerWidth = maxWidth

                // Placeholder canvas height matching 24 hours
                Spacer(modifier = Modifier.height(hourHeight * 24 + 24.dp))

                // Pre-calculate overlapping groups
                val sortedEvents = events.sortedWith(compareBy({ it.startEpochMillis }, { it.endEpochMillis }))
                val eventColumns = mutableMapOf<Long, Int>()
                val eventTotalColumns = mutableMapOf<Long, Int>()

                val currentCluster = mutableListOf<PlannerEvent>()
                var clusterEnd = -1L

                fun processCluster() {
                    if (currentCluster.isEmpty()) return
                    val columns = mutableListOf<MutableList<PlannerEvent>>()
                    for (ev in currentCluster) {
                        var placed = false
                        for ((colIdx, col) in columns.withIndex()) {
                            val lastInCol = col.last()
                            if (lastInCol.endEpochMillis <= ev.startEpochMillis) {
                                col.add(ev)
                                eventColumns[ev.id] = colIdx
                                placed = true
                                break
                            }
                        }
                        if (!placed) {
                            columns.add(mutableListOf(ev))
                            eventColumns[ev.id] = columns.size - 1
                        }
                    }
                    val numCols = columns.size
                    for (ev in currentCluster) {
                        eventTotalColumns[ev.id] = numCols
                    }
                }

                for (event in sortedEvents) {
                    if (currentCluster.isEmpty()) {
                        currentCluster.add(event)
                        clusterEnd = event.endEpochMillis
                    } else if (event.startEpochMillis < clusterEnd) {
                        currentCluster.add(event)
                        if (event.endEpochMillis > clusterEnd) {
                            clusterEnd = event.endEpochMillis
                        }
                    } else {
                        processCluster()
                        currentCluster.clear()
                        currentCluster.add(event)
                        clusterEnd = event.endEpochMillis
                    }
                }
                processCluster()

                // Render Events
                events.forEach { event ->
                    val startCal = Calendar.getInstance().apply { timeInMillis = event.startEpochMillis }
                    val endCal = Calendar.getInstance().apply { timeInMillis = event.endEpochMillis }

                    val startHour = startCal.get(Calendar.HOUR_OF_DAY)
                    val startMin = startCal.get(Calendar.MINUTE)
                    val endHour = endCal.get(Calendar.HOUR_OF_DAY)
                    val endMin = endCal.get(Calendar.MINUTE)

                    val startOffsetMinutes = startHour * 60 + startMin
                    val durationMinutes = ((endHour * 60 + endMin) - startOffsetMinutes).coerceAtLeast(15)

                    val topOffset = 12.dp + ((startOffsetMinutes / 60f) * hourHeightDp).dp
                    val itemHeight = ((durationMinutes / 60f) * hourHeightDp).dp

                    val colIdx = eventColumns[event.id] ?: 0
                    val totalCols = eventTotalColumns[event.id] ?: 1
                    val itemWidth = containerWidth / totalCols
                    val itemOffsetX = itemWidth * colIdx

                    EventCardOnTimeline(
                        event = event,
                        topOffset = topOffset,
                        itemHeight = itemHeight,
                        itemWidth = itemWidth,
                        itemOffsetX = itemOffsetX,
                        hourHeightDp = hourHeightDp,
                        onClick = { onEventClick(event) },
                        onMove = { newStart, newEnd -> onEventMove(event, newStart, newEnd) },
                        isBatchCheckMode = isBatchCheckMode,
                        isSelected = event.id in selectedEventIds,
                        onToggleSelection = { onToggleEventSelection(event.id) },
                        onToggleHappened = { onToggleHappened(event) },
                        scrollState = scrollState
                    )
                }

                // Tasks are no longer rendered on the timeline as requested by the user.
            }

            // Red Current Time Line if Today
            if (isToday) {
                val currentOffset = 12.dp + ((currentHour + currentMinute / 60f) * hourHeightDp).dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = currentOffset)
                        .zIndex(50f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 36.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFAB47BC))
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFAB47BC),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
fun EventCardOnTimeline(
    event: PlannerEvent,
    topOffset: Dp,
    itemHeight: Dp,
    itemWidth: Dp? = null,
    itemOffsetX: Dp? = null,
    hourHeightDp: Float,
    onClick: () -> Unit,
    onMove: (Long, Long) -> Unit,
    isBatchCheckMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onToggleHappened: () -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState
) {
    val eventColor = DateTimeUtils.parseColor(event.colorHex)
    val density = LocalDensity.current

    val currentOnMove by rememberUpdatedState(onMove)
    val currentStartEpoch by rememberUpdatedState(event.startEpochMillis)
    val currentEndEpoch by rememberUpdatedState(event.endEpochMillis)

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Reset local drag offset whenever event timestamps update from DB
    LaunchedEffect(event.startEpochMillis, event.endEpochMillis) {
        dragOffsetY = 0f
        isDragging = false
    }

    val hourHeightPx = with(density) { hourHeightDp.dp.toPx() }
    val pxPerMinute = hourHeightPx / 60f

    val snappedDeltaMinutes = if (pxPerMinute > 0f) {
        ((dragOffsetY / pxPerMinute) / 15f).roundToInt() * 15
    } else 0

    val duration = (event.endEpochMillis - event.startEpochMillis).coerceAtLeast(15 * 60 * 1000L)

    val startCal = Calendar.getInstance().apply { timeInMillis = event.startEpochMillis }
    val dayStartCal = (startCal.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val dayStartMillis = dayStartCal.timeInMillis
    val dayEndMillis = dayStartMillis + (24 * 60 * 60 * 1000L) - duration

    val rawStartMillis = event.startEpochMillis + (snappedDeltaMinutes * 60 * 1000L)
    val previewStartMillis = rawStartMillis.coerceIn(dayStartMillis, dayEndMillis)
    val previewEndMillis = previewStartMillis + duration

    // Snapped visual top offset for ghost placeholder
    val snappedTopOffset = topOffset + with(density) { (snappedDeltaMinutes * pxPerMinute).toDp() }

    // Smooth 1:1 visual movement during drag
    val visualTopOffset = if (isDragging) {
        topOffset + with(density) { dragOffsetY.toDp() }
    } else {
        topOffset
    }

    // Auto-scroll parent when dragging near top/bottom of scroll viewport
    LaunchedEffect(isDragging, dragOffsetY) {
        if (isDragging) {
            val topOffsetPx = with(density) { topOffset.toPx() }
            val currentY = topOffsetPx + dragOffsetY
            val relativeToViewport = currentY - scrollState.value

            // If dragging near the top of viewport, scroll up
            if (relativeToViewport < 100f && scrollState.value > 0) {
                val scrollAmount = ((100f - relativeToViewport) / 5f).coerceIn(2f, 25f).toInt()
                scrollState.scrollBy(-scrollAmount.toFloat())
                dragOffsetY -= scrollAmount
            } 
            // If dragging near the bottom of viewport, scroll down
            else {
                val totalHeightPx = hourHeightPx * 24 + with(density) { 24.dp.toPx() }
                val estimatedViewportHeight = (totalHeightPx - scrollState.maxValue).coerceAtLeast(800f)
                val distanceToBottom = estimatedViewportHeight - relativeToViewport
                if (distanceToBottom < 100f && scrollState.value < scrollState.maxValue) {
                    val scrollAmount = ((100f - distanceToBottom) / 5f).coerceIn(2f, 25f).toInt()
                    scrollState.scrollBy(scrollAmount.toFloat())
                    dragOffsetY += scrollAmount
                }
            }
        }
    }

    Box {
        // Ghost placeholder showing where the event will land on release
        if (isDragging && snappedDeltaMinutes != 0) {
            Box(
                modifier = (if (itemWidth != null) Modifier.width(itemWidth) else Modifier.fillMaxWidth())
                    .offset(x = itemOffsetX ?: 0.dp, y = snappedTopOffset)
                    .height(itemHeight)
                    .padding(end = 4.dp, bottom = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFF00BCD4).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .background(
                        Color(0xFF00BCD4).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    )
            )
        }

        // Main Event Card
        Box(
            modifier = (if (itemWidth != null) Modifier.width(itemWidth) else Modifier.fillMaxWidth())
                .offset(x = itemOffsetX ?: 0.dp, y = visualTopOffset)
                .height(itemHeight)
                .padding(end = 4.dp, bottom = 2.dp)
                .zIndex(if (isDragging) 20f else 1f)
                .graphicsLayer {
                    shadowElevation = if (isDragging) 16f else 0f
                    scaleX = if (isDragging) 1.03f else 1.0f
                    scaleY = if (isDragging) 1.03f else 1.0f
                    alpha = if (isDragging) 0.92f else 1.0f
                }
                .pointerInput(event.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isDragging = true
                            dragOffsetY = 0f
                        },
                        onDragEnd = {
                            if (isDragging) {
                                val finalStart = previewStartMillis
                                val finalEnd = previewEndMillis
                                val timeChanged = finalStart != currentStartEpoch || finalEnd != currentEndEpoch
                                isDragging = false
                                dragOffsetY = 0f
                                if (timeChanged) {
                                    currentOnMove(finalStart, finalEnd)
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY += dragAmount.y
                        }
                    )
                }
                .clickable {
                    if (!isDragging) {
                        if (isBatchCheckMode) {
                            onToggleSelection()
                        } else {
                            onClick()
                        }
                    }
                }
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isDragging) eventColor.copy(alpha = 0.95f) else {
                        if (event.isHappened) eventColor.copy(alpha = 0.5f) else eventColor
                    },
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = if (isDragging || isSelected) 2.dp else 0.dp,
                    color = if (isDragging) Color(0xFF00BCD4) else if (isSelected) Color(0xFF00E676) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp)
                )
                .testTag("event_card_${event.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = {
                        if (isBatchCheckMode) {
                            onToggleSelection()
                        } else {
                            onToggleHappened()
                        }
                    },
                    modifier = Modifier
                        .size(22.dp)
                        .offset(y = (-2).dp)
                        .testTag("event_toggle_happened_${event.id}")
                ) {
                    Icon(
                        imageVector = if (event.isHappened) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = if (event.isHappened) "Happened" else "Mark as Happened",
                        tint = if (event.isHappened) Color(0xFF00E676) else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = event.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (event.isHappened) Color.White.copy(alpha = 0.65f) else Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Floating Time Pill during dragging (Google Calendar Style)
            if (isDragging) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-22).dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF00BCD4),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = "${DateTimeUtils.formatTime(previewStartMillis)} - ${DateTimeUtils.formatTime(previewEndMillis)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
