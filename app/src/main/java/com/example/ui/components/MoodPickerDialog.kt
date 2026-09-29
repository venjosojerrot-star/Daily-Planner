package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.window.Dialog
import com.example.util.DateTimeUtils

data class MoodCategory(
    val title: String,
    val emojis: List<Pair<String, String>> // Emoji to Label
)

@Composable
fun MoodPickerDialog(
    dateMillis: Long,
    currentMoodEmoji: String?,
    currentJournalText: String? = "",
    onSaveMood: (String) -> Unit,
    onRemoveMood: () -> Unit,
    onSaveJournal: (String) -> Unit = {},
    onRemoveJournal: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedMood by remember(currentMoodEmoji) { mutableStateOf(currentMoodEmoji ?: "") }
    var customEmojiInput by remember { mutableStateOf(currentMoodEmoji ?: "") }
    var selectedCategoryIndex by remember { mutableStateOf(0) }

    var journalInput by remember(currentJournalText) { mutableStateOf(currentJournalText ?: "") }
    var isJournalSaved by remember(currentJournalText) { mutableStateOf(!currentJournalText.isNullOrBlank()) }
    var showSavedFeedback by remember { mutableStateOf(false) }

    val categories = remember {
        listOf(
            MoodCategory(
                title = "Positive & Joyful",
                emojis = listOf(
                    "😊" to "Happy",
                    "😄" to "Joyful",
                    "🥰" to "Loved",
                    "😍" to "In Love",
                    "🤩" to "Excited",
                    "🥳" to "Party",
                    "✨" to "Sparkling",
                    "💖" to "Grateful",
                    "☀️" to "Bright",
                    "🌈" to "Optimistic"
                )
            ),
            MoodCategory(
                title = "Calm & Mindful",
                emojis = listOf(
                    "😌" to "Peaceful",
                    "🧘" to "Mindful",
                    "☕" to "Cozy",
                    "🍃" to "Refreshed",
                    "🌸" to "Serene",
                    "🕊️" to "Hopeful",
                    "🎧" to "Relaxed",
                    "📖" to "Thoughtful",
                    "🕯️" to "Quiet",
                    "🌊" to "Chill"
                )
            ),
            MoodCategory(
                title = "Motivated & Productive",
                emojis = listOf(
                    "💪" to "Strong",
                    "🔥" to "On Fire",
                    "⚡" to "Energetic",
                    "🚀" to "Ambitious",
                    "🎯" to "Focused",
                    "🏆" to "Winning",
                    "📈" to "Productive",
                    "💼" to "Hard Working",
                    "💡" to "Inspired",
                    "🚴" to "Active"
                )
            ),
            MoodCategory(
                title = "Tired & Low Energy",
                emojis = listOf(
                    "😴" to "Sleepy",
                    "🥱" to "Exhausted",
                    "😔" to "Sad",
                    "😢" to "Crying",
                    "🤕" to "Unwell",
                    "🤒" to "Sick",
                    "🌧️" to "Gloomy",
                    "🥀" to "Drained",
                    "🧊" to "Numb",
                    "🛋️" to "Lazy"
                )
            ),
            MoodCategory(
                title = "Stressed & Frustrated",
                emojis = listOf(
                    "😤" to "Frustrated",
                    "😠" to "Angry",
                    "😡" to "Furious",
                    "🤯" to "Overwhelmed",
                    "🌪️" to "Chaos",
                    "💥" to "Stressed",
                    "😶" to "Speechless",
                    "🤐" to "Reserved",
                    "🤷" to "Confused",
                    "🤦" to "Annoyed"
                )
            )
        )
    }

    val journalPrompts = remember {
        listOf(
            "✨ Gratitude",
            "🎯 Accomplishment",
            "💭 Reflections",
            "🌱 Lesson Learned",
            "🙏 Daily Prayer",
            "⭐ Memorable Moment"
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("mood_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Mood & Journal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = DateTimeUtils.formatFullDate(dateMillis),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Mood Tracker Header Label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mood,
                        contentDescription = "Mood Tracker",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Mood Tracker",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current Selected Mood Preview
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedMood.isNotBlank()) selectedMood else "❓",
                                    fontSize = 22.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (selectedMood.isNotBlank()) "Logged Mood: $selectedMood" else "No mood logged",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Select an emoji below",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (selectedMood.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    selectedMood = ""
                                    customEmojiInput = ""
                                    onRemoveMood()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Clear",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEachIndexed { index, cat ->
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = {
                                Text(
                                    text = cat.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Emojis Grid for current category
                val currentCategory = categories[selectedCategoryIndex]
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(currentCategory.emojis) { (emoji, label) ->
                        val isSelected = selectedMood == emoji
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(62.dp)
                                .clickable {
                                    selectedMood = emoji
                                    customEmojiInput = emoji
                                    onSaveMood(emoji)
                                }
                                .testTag("mood_option_$emoji")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = label,
                                    fontSize = 8.5.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Emoji / Freeform Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customEmojiInput,
                        onValueChange = { customEmojiInput = it },
                        placeholder = { Text("Custom emoji (e.g. 🍕, 🎯, 🎸)", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("custom_emoji_input"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            if (customEmojiInput.isNotBlank()) {
                                selectedMood = customEmojiInput.trim()
                                onSaveMood(customEmojiInput.trim())
                            }
                        },
                        enabled = customEmojiInput.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("save_custom_mood_btn")
                    ) {
                        Text("Apply", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // JOURNAL DIARY (RIGHT AFTER THE MOOD TRACKER)
                // ==========================================
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Journal Diary Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Journal Diary",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Journal Diary",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isJournalSaved && journalInput.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Saved",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Saved",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Write your thoughts, memories, reflections, or gratitude for this day.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Prompt Starter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    journalPrompts.forEach { prompt ->
                        SuggestionChip(
                            onClick = {
                                val tagToAdd = if (journalInput.isBlank()) "$prompt: " else "\n\n$prompt: "
                                journalInput += tagToAdd
                                isJournalSaved = false
                            },
                            label = { Text(prompt, fontSize = 10.5.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Diary Text Area
                OutlinedTextField(
                    value = journalInput,
                    onValueChange = {
                        journalInput = it
                        isJournalSaved = false
                        showSavedFeedback = false
                    },
                    placeholder = {
                        Text(
                            text = "Dear Diary, today was...\nRecord what went well, challenges faced, or thoughts on your mind...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_diary_text_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Diary Controls & Word Counter
                val wordCount = remember(journalInput) {
                    journalInput.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$wordCount words • ${journalInput.length} chars",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (journalInput.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    journalInput = ""
                                    isJournalSaved = false
                                    onRemoveJournal()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Clear",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (journalInput.isNotBlank()) {
                                    onSaveJournal(journalInput)
                                    isJournalSaved = true
                                    showSavedFeedback = true
                                }
                            },
                            enabled = journalInput.isNotBlank() && !isJournalSaved,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("save_journal_entry_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isJournalSaved) "Saved" else "Save Journal",
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Done / Close Button
                Button(
                    onClick = {
                        if (journalInput.isNotBlank() && !isJournalSaved) {
                            onSaveJournal(journalInput)
                        }
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mood_journal_dialog_done_btn")
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
