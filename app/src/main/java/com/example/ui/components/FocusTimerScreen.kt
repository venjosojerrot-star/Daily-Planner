package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusSession
import com.example.data.model.PlannerTask
import com.example.util.AmbientSoundType
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    isTimerRunning: Boolean,
    remainingSeconds: Int,
    totalSeconds: Int,
    timerModeName: String,
    activeTask: PlannerTask?,
    availableTasks: List<PlannerTask>,
    ambientSound: AmbientSoundType,
    focusTimeTodaySeconds: Int,
    pastSessions: List<FocusSession>,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onSelectPreset: (Int, String) -> Unit,
    onSelectTask: (PlannerTask?) -> Unit,
    onSelectAmbientSound: (AmbientSoundType) -> Unit
) {
    var isTaskDropdownOpen by remember { mutableStateOf(false) }

    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "TimerProgress")

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preset selector chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = timerModeName == "Focus" && totalSeconds == 25 * 60,
                onClick = { onSelectPreset(25, "Focus") },
                label = { Text("25m Pomodoro") },
                modifier = Modifier.testTag("preset_25m")
            )
            FilterChip(
                selected = timerModeName == "Deep Work" && totalSeconds == 45 * 60,
                onClick = { onSelectPreset(45, "Deep Work") },
                label = { Text("45m Deep Work") },
                modifier = Modifier.testTag("preset_45m")
            )
            FilterChip(
                selected = timerModeName == "Short Break" && totalSeconds == 5 * 60,
                onClick = { onSelectPreset(5, "Short Break") },
                label = { Text("5m Break") },
                modifier = Modifier.testTag("preset_5m")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Linked Task Selection Dropdown
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isTaskDropdownOpen = true }
                .testTag("select_focus_task_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TaskAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FOCUS OBJECTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = activeTask?.title ?: "Select a task to focus on...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose Task")
            }

            DropdownMenu(
                expanded = isTaskDropdownOpen,
                onDismissRequest = { isTaskDropdownOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text("No Linked Task (Free Focus)") },
                    onClick = {
                        onSelectTask(null)
                        isTaskDropdownOpen = false
                    }
                )
                HorizontalDivider()
                availableTasks.filter { !it.isCompleted }.forEach { task ->
                    DropdownMenuItem(
                        text = { Text(task.title) },
                        onClick = {
                            onSelectTask(task)
                            isTaskDropdownOpen = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Main Animated Circular Timer Ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 14.dp,
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 14.dp,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timerModeName.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedTime,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isTimerRunning) "Focusing..." else "Paused",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Timer Play/Pause Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedIconButton(
                onClick = onResetTimer,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("reset_timer_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Timer")
            }

            Button(
                onClick = onToggleTimer,
                modifier = Modifier
                    .height(56.dp)
                    .width(160.dp)
                    .testTag("toggle_timer_button"),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTimerRunning) "PAUSE" else "START FOCUS",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Ambient Sound Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AMBIENT FOCUS SOUND",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = ambientSound == AmbientSoundType.NONE,
                        onClick = { onSelectAmbientSound(AmbientSoundType.NONE) },
                        label = { Text("Off") },
                        modifier = Modifier.testTag("sound_off")
                    )
                    FilterChip(
                        selected = ambientSound == AmbientSoundType.WHITE_NOISE,
                        onClick = { onSelectAmbientSound(AmbientSoundType.WHITE_NOISE) },
                        label = { Text("White Noise") },
                        modifier = Modifier.testTag("sound_white_noise")
                    )
                    FilterChip(
                        selected = ambientSound == AmbientSoundType.RAIN,
                        onClick = { onSelectAmbientSound(AmbientSoundType.RAIN) },
                        label = { Text("Soft Rain") },
                        modifier = Modifier.testTag("sound_rain")
                    )
                    FilterChip(
                        selected = ambientSound == AmbientSoundType.FOCUS_BINAURAL,
                        onClick = { onSelectAmbientSound(AmbientSoundType.FOCUS_BINAURAL) },
                        label = { Text("Theta Waves") },
                        modifier = Modifier.testTag("sound_binaural")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Today's Focus Stats Card
        val focusMinutesToday = focusTimeTodaySeconds / 60
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Today's Focus Activity",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$focusMinutesToday mins completed",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
