package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.compose.AsyncImage
import com.example.data.api.ChatMessage
import com.example.data.api.GeminiVerseService
import com.example.data.model.DailyVerseChatMessage
import com.example.ui.viewmodel.PlannerViewModel
import com.example.util.DateTimeUtils
import com.example.util.VerseUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Messenger Color Palette
private val MessengerBlueStart = Color(0xFF0084FF)
private val MessengerBlueEnd = Color(0xFF00C6FF)
private val MessengerGradient = Brush.linearGradient(listOf(MessengerBlueStart, MessengerBlueEnd))
private val MessengerAvatarGradient = Brush.linearGradient(listOf(Color(0xFF0084FF), Color(0xFFA855F7)))
private val MessengerOnlineGreen = Color(0xFF31A24C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyVerseDialog(
    selectedDateMillis: Long,
    monthlyVerses: List<String>,
    onDismiss: () -> Unit,
    onOpenImport: () -> Unit = {},
    viewModel: PlannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var viewingDateMillis by remember { mutableStateOf(selectedDateMillis) }
    val dateKey = remember(viewingDateMillis) {
        DateTimeUtils.formatDateKey(viewingDateMillis)
    }
    val verseText = remember(viewingDateMillis, monthlyVerses) {
        VerseUtils.getVerseForDay(monthlyVerses, viewingDateMillis)
    }

    // Persisted chat messages for this day from Room database
    val savedDbMessages by viewModel.getVerseChatMessages(dateKey).collectAsStateWithLifecycle(initialValue = emptyList())
    val chatMessages = remember(savedDbMessages) {
        savedDbMessages.map { ChatMessage(role = it.role, text = it.text) }
    }

    var inputQuery by remember { mutableStateOf("") }
    var isLoadingGemini by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<DailyVerseChatMessage?>(null) }
    var showDeleteSingleConfirmDialog by remember { mutableStateOf<DailyVerseChatMessage?>(null) }
    val chatListState = rememberLazyListState()

    val versePrefs = remember { context.getSharedPreferences("daily_verse_settings", Context.MODE_PRIVATE) }
    var verseImageTimestamp by remember {
        mutableStateOf(versePrefs.getLong("verse_profile_image_timestamp", 0L))
    }
    val verseAvatarFile = remember(verseImageTimestamp) {
        File(context.filesDir, "daily_verse_avatar.jpg")
    }
    val hasCustomAvatar = verseAvatarFile.exists() && verseAvatarFile.length() > 0

    val verseBotName = remember(versePrefs.getString("verse_bot_name", "Sister Emma")) {
        versePrefs.getString("verse_bot_name", "Sister Emma") ?: "Sister Emma"
    }
    val verseBotGender = remember(versePrefs.getString("verse_bot_gender", "Sister (Female)")) {
        versePrefs.getString("verse_bot_gender", "Sister (Female)") ?: "Sister (Female)"
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val targetFile = File(context.filesDir, "daily_verse_avatar.jpg")
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val newTimestamp = System.currentTimeMillis()
                versePrefs.edit().putLong("verse_profile_image_timestamp", newTimestamp).apply()
                verseImageTimestamp = newTimestamp
                Toast.makeText(context, "Daily Verse profile image updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Automatically scroll down when new messages are added or when loading starts
    LaunchedEffect(chatMessages.size, isLoadingGemini) {
        if (chatMessages.isNotEmpty() || isLoadingGemini) {
            val targetIndex = chatMessages.size + (if (isLoadingGemini) 2 else 1)
            chatListState.animateScrollToItem(targetIndex)
        }
    }

    fun askGemini(prompt: String? = null) {
        val defaultInitialPrompt = "Please explain this verse simply in 3 parts: Meaning, Life Application, and Cross Reference."
        val query = prompt ?: inputQuery.trim().ifBlank {
            if (chatMessages.isEmpty()) defaultInitialPrompt else ""
        }
        if (query.isBlank() && chatMessages.isNotEmpty()) return

        // 1. Immediately save user message to Room DB for this day
        viewModel.saveVerseChatMessage(dateKey = dateKey, role = "user", text = query)
        inputQuery = ""
        isLoadingGemini = true
        keyboardController?.hide()

        // 2. Call Gemini with past history on this day
        coroutineScope.launch {
            val history = chatMessages.filter { it.text.isNotBlank() }
            val result = GeminiVerseService.explainVerse(
                verseText = verseText,
                userQuestion = query,
                history = history,
                botName = verseBotName,
                botGender = verseBotGender
            )
            isLoadingGemini = false
            result.onSuccess { responseText ->
                // 3. Save AI explanation to Room DB for this day
                viewModel.saveVerseChatMessage(dateKey = dateKey, role = "model", text = responseText)
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    "Unable to connect: ${error.localizedMessage ?: "Please check connection"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Conversation?") },
            text = { Text("Are you sure you want to clear all conversation messages saved on this day?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearVerseChatForDate(dateKey)
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Conversation cleared for this day", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteSingleConfirmDialog != null) {
        val messageToDelete = showDeleteSingleConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteSingleConfirmDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Message?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this message from today's conversation?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVerseChatMessage(messageToDelete.id)
                        showDeleteSingleConfirmDialog = null
                        Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSingleConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedMessageForAction != null) {
        val targetMessage = selectedMessageForAction!!
        ModalBottomSheet(
            onDismissRequest = { selectedMessageForAction = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            MessageActionBottomSheetContent(
                message = targetMessage,
                onSelectReaction = { emoji ->
                    val newReaction = if (targetMessage.reaction == emoji) null else emoji
                    viewModel.updateVerseChatReaction(targetMessage.id, newReaction)
                    selectedMessageForAction = null
                },
                onDelete = {
                    showDeleteSingleConfirmDialog = targetMessage
                    selectedMessageForAction = null
                },
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val cleaned = VerseUtils.formatDevotionalExplanation(targetMessage.text)
                    val clip = ClipData.newPlainText("Devotional Message", cleaned)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    selectedMessageForAction = null
                },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        val cleaned = VerseUtils.formatDevotionalExplanation(targetMessage.text)
                        putExtra(Intent.EXTRA_TEXT, "✨ Devotional:\n\n$cleaned\n\nShared via Daily Planner")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Message"))
                    selectedMessageForAction = null
                },
                onSaveToNotebook = if (targetMessage.role == "model") {
                    {
                        val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(viewingDateMillis))
                        viewModel.savePublicSpeakingTalk(
                            title = "Devotional Talk ($formattedDate)",
                            content = targetMessage.text,
                            sourceQuote = verseText
                        )
                        Toast.makeText(context, "Saved to Notebook", Toast.LENGTH_SHORT).show()
                        selectedMessageForAction = null
                    }
                } else null
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { window ->
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                window.setDimAmount(0f)
            }
        }

        BackHandler { onDismiss() }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("daily_verse_fullscreen_chat"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .windowInsetsPadding(WindowInsets.ime)
            ) {
                // Messenger Header Bar
                MessengerHeaderBar(
                    viewingDateMillis = viewingDateMillis,
                    verseText = verseText,
                    hasMessages = chatMessages.isNotEmpty(),
                    hasCustomAvatar = hasCustomAvatar,
                    verseAvatarFile = verseAvatarFile,
                    botName = verseBotName,
                    onPickAvatar = { imagePickerLauncher.launch("image/*") },
                    onClearChat = { showClearConfirmDialog = true },
                    onBack = onDismiss,
                    onPrevDay = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = viewingDateMillis
                            add(Calendar.DAY_OF_MONTH, -1)
                        }
                        viewingDateMillis = cal.timeInMillis
                    },
                    onNextDay = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = viewingDateMillis
                            add(Calendar.DAY_OF_MONTH, 1)
                        }
                        viewingDateMillis = cal.timeInMillis
                    },
                    onToday = {
                        viewingDateMillis = System.currentTimeMillis()
                    }
                )

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                // Messenger Chat Stream
                LazyColumn(
                    state = chatListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
                ) {
                    // Bot Intro Header in Stream
                    item {
                        MessengerProfileHeader(
                            viewingDateMillis = viewingDateMillis,
                            botName = verseBotName,
                            botGender = verseBotGender,
                            hasCustomAvatar = hasCustomAvatar,
                            verseAvatarFile = verseAvatarFile
                        )
                    }

                    // Pinned Anchor: Today's Daily Verse Card
                    item {
                        MessengerVerseCard(
                            verseText = verseText,
                            dateMillis = viewingDateMillis,
                            onCopyClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Daily Verse", verseText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Verse copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onShareClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "✨ Daily Verse (${DateTimeUtils.formatDayMonth(viewingDateMillis)}):\n\n$verseText\n\nShared via Daily Planner")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Daily Verse"))
                            }
                        )
                    }

                    // Chat messages
                    items(savedDbMessages, key = { it.id }) { message ->
                        MessengerChatBubble(
                            message = message,
                            customAvatarFile = if (hasCustomAvatar) verseAvatarFile else null,
                            onLongPress = {
                                selectedMessageForAction = message
                            },
                            onReactionClick = {
                                selectedMessageForAction = message
                            },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val cleaned = VerseUtils.formatDevotionalExplanation(message.text)
                                val clip = ClipData.newPlainText("Devotional Insight", cleaned)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    val cleaned = VerseUtils.formatDevotionalExplanation(message.text)
                                    putExtra(Intent.EXTRA_TEXT, "✨ Devotional Insight:\n\n$cleaned\n\nShared via Daily Planner")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Devotional"))
                            }
                        )
                    }

                    // Messenger Typing Indicator
                    if (isLoadingGemini) {
                        item {
                            MessengerTypingBubble(
                                customAvatarFile = if (hasCustomAvatar) verseAvatarFile else null
                            )
                        }
                    }
                }

                // Quick Prompt Suggestion Carousel
                MessengerQuickPromptCarousel(
                    onSelectPrompt = { askGemini(it) },
                    isLoading = isLoadingGemini
                )

                // Messenger Bottom Input Composer
                MessengerComposerBar(
                    query = inputQuery,
                    onQueryChange = { inputQuery = it },
                    onSend = { askGemini() },
                    onQuickLike = {
                        askGemini("Please explain this verse simply in 3 parts: Meaning, Life Application, and Cross Reference.")
                    },
                    isLoading = isLoadingGemini
                )
            }
        }
    }
}

/**
 * Messenger Top App Bar with back navigation, avatar, active status, date switcher, and share.
 */
@Composable
private fun MessengerHeaderBar(
    viewingDateMillis: Long,
    verseText: String,
    hasMessages: Boolean = false,
    hasCustomAvatar: Boolean = false,
    verseAvatarFile: File? = null,
    botName: String = "Sister Emma",
    onPickAvatar: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onBack: () -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    val context = LocalContext.current
    val isViewingToday = DateTimeUtils.isToday(viewingDateMillis)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back button + Avatar + Title & Active status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MessengerBlueStart,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Messenger Avatar with Online indicator & tap to change image
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onPickAvatar() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MessengerAvatarGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasCustomAvatar && verseAvatarFile != null && verseAvatarFile.exists()) {
                                AsyncImage(
                                    model = verseAvatarFile,
                                    contentDescription = botName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Online green dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .padding(1.5.dp)
                                .background(MessengerOnlineGreen, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = botName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MessengerOnlineGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active now",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Right: Actions (Clear conversation if messages present, and Share)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasMessages) {
                        IconButton(
                            onClick = onClearChat,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear conversation for this day",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "✨ Daily Verse (${DateTimeUtils.formatDayMonth(viewingDateMillis)}):\n\n$verseText\n\nShared via Daily Planner"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Daily Verse"))
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MessengerBlueStart,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Compact Messenger Date Navigator
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevDay,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isViewingToday) MessengerBlueStart.copy(alpha = 0.12f) else Color.Transparent,
                        modifier = Modifier.clickable { onToday() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (isViewingToday) MessengerBlueStart else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isViewingToday) {
                                    "Today • ${DateTimeUtils.formatDayMonth(viewingDateMillis)}"
                                } else {
                                    DateTimeUtils.formatDayMonth(viewingDateMillis)
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isViewingToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isViewingToday) MessengerBlueStart else MaterialTheme.colorScheme.onSurface
                            )
                            if (!isViewingToday) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Jump to Today",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MessengerBlueStart
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onNextDay,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Messenger Profile Header inside the chat stream.
 */
@Composable
private fun MessengerProfileHeader(
    viewingDateMillis: Long,
    botName: String = "Sister Emma",
    botGender: String = "Sister (Female)",
    hasCustomAvatar: Boolean = false,
    verseAvatarFile: File? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar Circle
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MessengerAvatarGradient),
            contentAlignment = Alignment.Center
        ) {
            if (hasCustomAvatar && verseAvatarFile != null && verseAvatarFile.exists()) {
                AsyncImage(
                    model = verseAvatarFile,
                    contentDescription = botName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = botName,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Messenger Card showing today's scripture verse in an elegant shareable card.
 */
@Composable
private fun MessengerVerseCard(
    verseText: String,
    dateMillis: Long,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MessengerBlueStart.copy(alpha = 0.25f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MessengerBlueStart.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MessengerBlueStart,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TODAY'S SCRIPTURE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MessengerBlueStart,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = DateTimeUtils.formatDayMonth(dateMillis),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "“$verseText”",
                fontSize = 15.sp,
                lineHeight = 23.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Copy and Share Verse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onShareClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Messenger-styled Chat Bubble (Handles User messages & Gemini AI 3-part responses).
 */
@Composable
internal fun MessengerChatBubble(
    message: DailyVerseChatMessage,
    customAvatarFile: File? = null,
    onLongPress: () -> Unit = {},
    onReactionClick: () -> Unit = {},
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isUser = message.role == "user"
    val primaryColor = MessengerBlueStart
    val secondaryColor = Color(0xFFD97706) // Warm Amber for Life Application
    val tertiaryColor = Color(0xFF0D9488) // Teal for Cross Reference

    val sections = remember(message.text, isUser) {
        if (!isUser) {
            parseDevotionalSections(message.text, primaryColor, secondaryColor, tertiaryColor)
        } else {
            null
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Mini Avatar
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MessengerAvatarGradient),
                contentAlignment = Alignment.Center
            ) {
                if (customAvatarFile != null && customAvatarFile.exists()) {
                    AsyncImage(
                        model = customAvatarFile,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = if (!isUser && sections != null) Modifier.fillMaxWidth(0.92f) else Modifier.widthIn(max = 300.dp)
        ) {
            // Messenger Bubble shape: rounded with pinched corner on active side
            val bubbleShape = if (isUser) {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
            }

            Box(
                modifier = Modifier
                    .pointerInput(message.id) {
                        detectTapGestures(
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress()
                            }
                        )
                    }
                    .testTag("chat_bubble_${message.id}")
            ) {
                Surface(
                    shape = bubbleShape,
                    color = if (isUser) {
                        Color.Transparent // will use gradient
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    modifier = if (isUser) {
                        Modifier
                            .clip(bubbleShape)
                            .background(MessengerGradient)
                    } else {
                        Modifier
                    }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (!isUser) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MessengerBlueStart,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (sections != null) "3-Part Explanation" else "Devotional Insight",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MessengerBlueStart
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = onCopy,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy insight",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onShare,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share insight",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (sections != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                sections.forEach { section ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, section.accentColor.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = section.accentColor.copy(alpha = 0.14f),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = section.icon,
                                                            contentDescription = null,
                                                            tint = section.accentColor,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = section.title,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = section.accentColor
                                                )
                                            }

                                            // Render paragraphs with generous line height and clean spacing
                                            val formattedContent = VerseUtils.formatDevotionalExplanation(section.content)
                                            val paragraphs = formattedContent.split("\n\n").filter { it.isNotBlank() }
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                paragraphs.forEach { paragraph ->
                                                    Text(
                                                        text = paragraph.trim(),
                                                        fontSize = 13.sp,
                                                        lineHeight = 21.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val formattedMessage = if (!isUser) {
                                VerseUtils.formatDevotionalExplanation(message.text)
                            } else {
                                message.text
                            }
                            val paragraphs = formattedMessage.split("\n\n").filter { it.isNotBlank() }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                paragraphs.forEach { p ->
                                    Text(
                                        text = p.trim(),
                                        fontSize = 13.5.sp,
                                        lineHeight = 21.sp,
                                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Reaction Emoji Badge positioned in the lower right bottom corner
                if (!message.reaction.isNullOrBlank()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 6.dp)
                            .clickable { onReactionClick() }
                            .testTag("reaction_badge_${message.id}")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = message.reaction,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (!message.reaction.isNullOrBlank()) 6.dp else 2.dp))

            // Small status label
            Text(
                text = if (isUser) "Delivered • Hold to react" else "Devotional • Hold to react",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

/**
 * Bottom Sheet menu shown when user long-presses a message in Daily Verse.
 * Allows adding emoji reactions, copying, sharing, or deleting the message.
 */
@Composable
internal fun MessageActionBottomSheetContent(
    message: DailyVerseChatMessage,
    onSelectReaction: (String) -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSaveToNotebook: (() -> Unit)? = null
) {
    val isUser = message.role == "user"
    val reactions = listOf("❤️", "🙏", "✨", "🙌", "💡", "🕊️", "📖", "🔥", "😊", "✝️", "👏", "⭐")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & message preview header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isUser) Icons.Default.Person else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MessengerBlueStart,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isUser) "Your Message" else "Daily Devotional Response",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MessengerBlueStart
                )
            }
            Text(
                text = message.text.take(120) + if (message.text.length > 120) "..." else "",
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        // Quick Emoji Reactions
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "React with Emoji",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(reactions) { emoji ->
                    val isSelected = message.reaction == emoji
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) BorderStroke(2.dp, MessengerBlueStart) else null,
                        modifier = Modifier
                            .size(46.dp)
                            .clickable { onSelectReaction(emoji) }
                            .testTag("emoji_react_$emoji")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = emoji,
                                fontSize = 22.sp
                            )
                        }
                    }
                }
            }

            if (!message.reaction.isNullOrBlank()) {
                TextButton(
                    onClick = { onSelectReaction(message.reaction) },
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove reaction",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove Reaction", fontSize = 12.sp)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        // Action Buttons: Copy, Share, Delete
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopy() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Copy Message Text",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShare() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Share Message",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (onSaveToNotebook != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSaveToNotebook() }
                        .testTag("save_to_notebook_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Save to Notebook",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Save to Public Speaking Notebook",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDelete() }
                    .testTag("delete_message_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Delete Message",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Messenger 3-dot typing indicator bubble.
 */
@Composable
internal fun MessengerTypingBubble(
    customAvatarFile: File? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "messenger_typing")
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 130, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 260, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MessengerAvatarGradient),
            contentAlignment = Alignment.Center
        ) {
            if (customAvatarFile != null && customAvatarFile.exists()) {
                AsyncImage(
                    model = customAvatarFile,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = dot1Offset.dp)
                        .size(7.dp)
                        .background(MessengerBlueStart, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot2Offset.dp)
                        .size(7.dp)
                        .background(MessengerBlueStart, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot3Offset.dp)
                        .size(7.dp)
                        .background(MessengerBlueStart, CircleShape)
                )
            }
        }
    }
}

/**
 * Messenger Quick Prompt Chips Carousel (like Messenger quick replies).
 */
@Composable
private fun MessengerQuickPromptCarousel(
    onSelectPrompt: (String) -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        MessengerQuickReplyChip(
            text = "✨ 3 Part Explanation",
            onClick = {
                onSelectPrompt("Please explain this verse simply in 3 parts: Meaning, Life Application, and Cross Reference.")
            },
            enabled = !isLoading,
            modifier = Modifier.testTag("preset_tab_3_part_explanation")
        )
    }
}

@Composable
private fun MessengerQuickReplyChip(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MessengerBlueStart.copy(alpha = 0.4f)),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled) { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MessengerBlueStart else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Messenger Bottom Composer Bar with text field, plus quick action icon and send/like buttons.
 */
@Composable
private fun MessengerComposerBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickLike: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sparkle action button
            IconButton(
                onClick = onQuickLike,
                enabled = !isLoading,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Quick Devotional",
                    tint = MessengerBlueStart,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Messenger Pill Text Input
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = "Message about this verse...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (query.isNotBlank() && !isLoading) onSend() }),
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_chat_input")
                )
            }

            // Send / Like Button (Messenger style: 👍 when empty, Send arrow when typed)
            if (query.isNotBlank()) {
                IconButton(
                    onClick = onSend,
                    enabled = !isLoading,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MessengerGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onQuickLike,
                    enabled = !isLoading,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Quick Explain",
                        tint = MessengerBlueStart,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

data class DevotionalSection(
    val title: String,
    val content: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color
)

fun parseDevotionalSections(
    rawText: String,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color
): List<DevotionalSection>? {
    // Regex matching line headers (e.g., "1. Meaning", "Meaning:", "## Meaning")
    val headerRegex = Regex(
        """(?im)^[\s#*>\d.-]*(?:📖|💡|🔗|✨)?\s*(Meaning|Life\s+Application|Cross\s+References?)\s*[:*#-]*\s*$"""
    )
    val matches = headerRegex.findAll(rawText).toList()

    if (matches.size >= 2) {
        val sections = mutableListOf<DevotionalSection>()
        for (i in matches.indices) {
            val match = matches[i]
            val rawHeader = match.groupValues[1]
            val contentStart = match.range.last + 1
            val contentEnd = if (i + 1 < matches.size) matches[i + 1].range.first else rawText.length
            val sectionContent = VerseUtils.formatDevotionalExplanation(rawText.substring(contentStart, contentEnd))

            if (sectionContent.isNotBlank()) {
                val normalizedTitle = when {
                    rawHeader.contains("Meaning", ignoreCase = true) -> "Meaning"
                    rawHeader.contains("Life", ignoreCase = true) -> "Life Application"
                    rawHeader.contains("Cross", ignoreCase = true) -> "Cross Reference"
                    else -> rawHeader
                }

                val (icon, color) = when (normalizedTitle) {
                    "Meaning" -> Icons.Default.MenuBook to primaryColor
                    "Life Application" -> Icons.Default.Lightbulb to secondaryColor
                    "Cross Reference" -> Icons.Default.Bookmark to tertiaryColor
                    else -> Icons.Default.AutoAwesome to primaryColor
                }

                sections.add(
                    DevotionalSection(
                        title = normalizedTitle,
                        content = sectionContent,
                        icon = icon,
                        accentColor = color
                    )
                )
            }
        }
        if (sections.isNotEmpty()) return sections
    }

    // Secondary check for inline headers (e.g., "**1. Meaning:** ...")
    val inlineHeaderRegex = Regex(
        """(?im)(?:^|\n)\s*[\s#*>\d.-]*(?:📖|💡|🔗|✨)?\s*(Meaning|Life\s+Application|Cross\s+References?)\s*[:*#-]+\s*"""
    )
    val inlineMatches = inlineHeaderRegex.findAll(rawText).toList()
    if (inlineMatches.size >= 2) {
        val sections = mutableListOf<DevotionalSection>()
        for (i in inlineMatches.indices) {
            val match = inlineMatches[i]
            val rawHeader = match.groupValues[1]
            val contentStart = match.range.last + 1
            val contentEnd = if (i + 1 < inlineMatches.size) inlineMatches[i + 1].range.first else rawText.length
            val sectionContent = VerseUtils.formatDevotionalExplanation(rawText.substring(contentStart, contentEnd))

            if (sectionContent.isNotBlank()) {
                val normalizedTitle = when {
                    rawHeader.contains("Meaning", ignoreCase = true) -> "Meaning"
                    rawHeader.contains("Life", ignoreCase = true) -> "Life Application"
                    rawHeader.contains("Cross", ignoreCase = true) -> "Cross Reference"
                    else -> rawHeader
                }

                val (icon, color) = when (normalizedTitle) {
                    "Meaning" -> Icons.Default.MenuBook to primaryColor
                    "Life Application" -> Icons.Default.Lightbulb to secondaryColor
                    "Cross Reference" -> Icons.Default.Bookmark to tertiaryColor
                    else -> Icons.Default.AutoAwesome to primaryColor
                }

                sections.add(
                    DevotionalSection(
                        title = normalizedTitle,
                        content = sectionContent,
                        icon = icon,
                        accentColor = color
                    )
                )
            }
        }
        if (sections.isNotEmpty()) return sections
    }

    return null
}

