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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.api.ChatMessage
import com.example.data.api.GeminiVerseService
import com.example.data.model.DailyVerseChatMessage
import com.example.ui.viewmodel.PlannerViewModel
import com.example.util.DateTimeUtils
import com.example.util.QuoteUtils
import com.example.util.VerseUtils
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
fun QuoteOfTheDayDialog(
    selectedDateMillis: Long,
    monthlyQuotes: List<String>,
    onDismiss: () -> Unit,
    onOpenImport: () -> Unit = {},
    viewModel: PlannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var viewingDateMillis by remember { mutableStateOf(selectedDateMillis) }
    
    val isFuture = remember(viewingDateMillis) {
        DateTimeUtils.isFutureDay(viewingDateMillis)
    }
    val isToday = remember(viewingDateMillis) {
        DateTimeUtils.isToday(viewingDateMillis)
    }
    val isTomorrow = remember(viewingDateMillis) {
        DateTimeUtils.isTomorrow(viewingDateMillis)
    }

    val quoteText = remember(viewingDateMillis, monthlyQuotes, isFuture) {
        if (isFuture) "" else QuoteUtils.getQuoteForDay(monthlyQuotes, viewingDateMillis)
    }

    // Persisted chat messages for this day's quote in Room database
    val dateKey = remember(viewingDateMillis) {
        "quote_" + DateTimeUtils.formatDateKey(viewingDateMillis)
    }
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

    val prefs = remember { context.getSharedPreferences("daily_quotes_settings", Context.MODE_PRIVATE) }
    var imageTimestamp by remember {
        mutableStateOf(prefs.getLong("quote_profile_image_timestamp", 0L))
    }
    val avatarFile = remember(imageTimestamp) {
        File(context.filesDir, "daily_quote_avatar.jpg")
    }
    val hasCustomAvatar = avatarFile.exists() && avatarFile.length() > 0

    val botName = remember(prefs.getString("quote_bot_name", "Maya")) {
        prefs.getString("quote_bot_name", "Maya") ?: "Maya"
    }
    val botGender = remember(prefs.getString("quote_bot_gender", "Girlfriend (Female)")) {
        prefs.getString("quote_bot_gender", "Girlfriend (Female)") ?: "Girlfriend (Female)"
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val targetFile = File(context.filesDir, "daily_quote_avatar.jpg")
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val newTimestamp = System.currentTimeMillis()
                prefs.edit().putLong("quote_profile_image_timestamp", newTimestamp).apply()
                imageTimestamp = newTimestamp
                Toast.makeText(context, "Profile image updated", Toast.LENGTH_SHORT).show()
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
        val defaultInitialPrompt = "Please convert this quote into a well-structured talk like a famous motivational speaker."
        val query = prompt ?: inputQuery.trim().ifBlank {
            if (chatMessages.isEmpty()) defaultInitialPrompt else ""
        }
        if (query.isBlank() && chatMessages.isNotEmpty()) return

        // Save user message to Room DB for this day's quote
        viewModel.saveVerseChatMessage(dateKey = dateKey, role = "user", text = query)
        inputQuery = ""
        isLoadingGemini = true
        keyboardController?.hide()

        // Call Gemini with past history for this day
        coroutineScope.launch {
            val history = chatMessages.filter { it.text.isNotBlank() }
            val result = GeminiVerseService.explainQuote(
                quoteText = quoteText,
                userQuestion = query,
                history = history,
                botName = botName,
                botGender = botGender
            )
            isLoadingGemini = false
            result.onSuccess { responseText ->
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
                    val clip = ClipData.newPlainText("Inspirational Insight", cleaned)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    selectedMessageForAction = null
                },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        val cleaned = VerseUtils.formatDevotionalExplanation(targetMessage.text)
                        putExtra(Intent.EXTRA_TEXT, "✨ Daily Quote Reflection:\n\n$cleaned\n\nShared via Daily Planner")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Message"))
                    selectedMessageForAction = null
                },
                onSaveToNotebook = if (targetMessage.role == "model") {
                    {
                        val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(viewingDateMillis))
                        viewModel.savePublicSpeakingTalk(
                            title = "Structured Talk ($formattedDate)",
                            content = targetMessage.text,
                            sourceQuote = quoteText
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
                .testTag("quote_of_the_day_fullscreen_chat"),
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
                MessengerQuoteHeaderBar(
                    viewingDateMillis = viewingDateMillis,
                    quoteText = quoteText,
                    hasMessages = chatMessages.isNotEmpty(),
                    hasCustomAvatar = hasCustomAvatar,
                    avatarFile = avatarFile,
                    botName = botName,
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
                        MessengerQuoteProfileHeader(
                            viewingDateMillis = viewingDateMillis,
                            botName = botName,
                            botGender = botGender,
                            hasCustomAvatar = hasCustomAvatar,
                            avatarFile = avatarFile
                        )
                    }

                    // Pinned Anchor: Today's Daily Quote Card
                    item {
                        MessengerQuoteCard(
                            quoteText = quoteText,
                            dateMillis = viewingDateMillis,
                            isFuture = isFuture,
                            isTomorrow = isTomorrow,
                            onCopyClick = {
                                if (!isFuture && quoteText.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Daily Quote", quoteText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Quote copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShareClick = {
                                if (!isFuture && quoteText.isNotBlank()) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "✨ Daily Quote (${DateTimeUtils.formatDayMonth(viewingDateMillis)}):\n\n$quoteText\n\nShared via Daily Planner")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Daily Quote"))
                                }
                            }
                        )
                    }

                    // Chat messages
                    items(savedDbMessages, key = { it.id }) { message ->
                        MessengerChatBubble(
                            message = message,
                            customAvatarFile = if (hasCustomAvatar) avatarFile else null,
                            onLongPress = {
                                selectedMessageForAction = message
                            },
                            onReactionClick = {
                                selectedMessageForAction = message
                            },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val cleaned = VerseUtils.formatDevotionalExplanation(message.text)
                                val clip = ClipData.newPlainText("Quote Reflection", cleaned)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    val cleaned = VerseUtils.formatDevotionalExplanation(message.text)
                                    putExtra(Intent.EXTRA_TEXT, "✨ Quote Reflection:\n\n$cleaned\n\nShared via Daily Planner")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Reflection"))
                            }
                        )
                    }

                    // Messenger Typing Indicator
                    if (isLoadingGemini) {
                        item {
                            MessengerTypingBubble(
                                customAvatarFile = if (hasCustomAvatar) avatarFile else null
                            )
                        }
                    }
                }

                // Quick Prompt Suggestion Preset Chip
                if (!isFuture && quoteText.isNotBlank()) {
                    MessengerQuoteQuickPromptCarousel(
                        onSelectPrompt = { askGemini(it) },
                        isLoading = isLoadingGemini
                    )
                }

                // Messenger Bottom Input Composer
                MessengerQuoteComposerBar(
                    query = inputQuery,
                    onQueryChange = { inputQuery = it },
                    onSend = { askGemini() },
                    onQuickLike = {
                        askGemini("Please explain this quote simply in 3 parts: Meaning, Life Application, and Cross Reference.")
                    },
                    isLoading = isLoadingGemini,
                    enabled = !isFuture && quoteText.isNotBlank()
                )
            }
        }
    }
}

/**
 * Header bar for Daily Quote Chat
 */
@Composable
private fun MessengerQuoteHeaderBar(
    viewingDateMillis: Long,
    quoteText: String,
    hasMessages: Boolean = false,
    hasCustomAvatar: Boolean = false,
    avatarFile: File? = null,
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
                            if (hasCustomAvatar && avatarFile != null && avatarFile.exists()) {
                                AsyncImage(
                                    model = avatarFile,
                                    contentDescription = botName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasMessages) {
                        IconButton(
                            onClick = onClearChat,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear conversation",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (quoteText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "✨ Daily Quote (${DateTimeUtils.formatDayMonth(viewingDateMillis)}):\n\n$quoteText\n\nShared via Daily Planner"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Daily Quote"))
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
            }

            // Compact Messenger Date Navigator
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevDay,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = DateTimeUtils.formatDayHeader(viewingDateMillis),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!isViewingToday) {
                            Text(
                                text = "• Jump to Today",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MessengerBlueStart,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onToday() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNextDay,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Profile intro header shown at top of chat
 */
@Composable
private fun MessengerQuoteProfileHeader(
    viewingDateMillis: Long,
    botName: String,
    botGender: String,
    hasCustomAvatar: Boolean,
    avatarFile: File?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MessengerAvatarGradient),
                contentAlignment = Alignment.Center
            ) {
                if (hasCustomAvatar && avatarFile != null && avatarFile.exists()) {
                    AsyncImage(
                        model = avatarFile,
                        contentDescription = botName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = botName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Pinned card displaying the daily quote
 */
@Composable
private fun MessengerQuoteCard(
    quoteText: String,
    dateMillis: Long,
    isFuture: Boolean,
    isTomorrow: Boolean,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DAILY QUOTE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                if (!isFuture && quoteText.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCopyClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Quote",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onShareClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Quote",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isFuture) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Hidden",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isTomorrow) "Tomorrow's Quote is Hidden" else "Daily Quote Hidden",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Check back on ${DateTimeUtils.formatDayMonth(dateMillis)} to reveal it!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = quoteText,
                    fontSize = 15.5.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Quick preset button chip above input bar
 */
@Composable
private fun MessengerQuoteQuickPromptCarousel(
    onSelectPrompt: (String) -> Unit,
    isLoading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(enabled = !isLoading) {
                        onSelectPrompt("Please convert this quote into a well-structured talk like a famous motivational speaker.")
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🎤 Structured Talk",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Messenger Input Bar at the bottom
 */
@Composable
private fun MessengerQuoteComposerBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickLike: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean = true
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled && !isLoading,
                placeholder = {
                    Text(
                        text = if (!enabled) "Quote is locked" else "Ask about today's quote...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("quote_chat_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MessengerBlueStart.copy(alpha = 0.5f)
                ),
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (enabled && !isLoading) onSend() })
            )

            Spacer(modifier = Modifier.width(6.dp))

            if (query.isNotBlank()) {
                IconButton(
                    onClick = onSend,
                    enabled = enabled && !isLoading,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (enabled && !isLoading) MessengerGradient else Brush.linearGradient(listOf(Color.Gray, Color.LightGray)))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onQuickLike,
                    enabled = enabled && !isLoading,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Quick Explanation",
                        tint = if (enabled && !isLoading) MessengerBlueStart else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuoteImportDialog(
    currentQuotes: List<String>,
    onDismiss: () -> Unit,
    onSaveQuotes: (String) -> Unit,
    onResetDefault: () -> Unit
) {
    val initialText = remember(currentQuotes) {
        currentQuotes.mapIndexed { index, q -> "${index + 1}. $q" }.joinToString("\n")
    }
    var rawInputText by remember { mutableStateOf(initialText) }
    val parsedCount = remember(rawInputText) { QuoteUtils.parseQuotesFromText(rawInputText).size }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Import 1-Month Quotes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Paste or edit daily quotes for 1 month (up to 31 quotes). Each line or numbered item represents a day of the month. The quote will automatically reset every 24 hours.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quotes detected: $parsedCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (parsedCount >= 28) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    TextButton(
                        onClick = {
                            rawInputText = QuoteUtils.DEFAULT_31_QUOTES.mapIndexed { index, q -> "${index + 1}. $q" }.joinToString("\n")
                            Toast.makeText(context, "Loaded 31 default inspiration quotes", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Load Sample Pack", fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = rawInputText,
                    onValueChange = { rawInputText = it },
                    placeholder = {
                        Text(
                            "1. The secret of getting ahead is getting started. - Mark Twain\n2. It always seems impossible until it's done. - Nelson Mandela\n...",
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .testTag("import_quotes_textfield"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            rawInputText = ""
                        }
                    ) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = {
                            onResetDefault()
                            onDismiss()
                            Toast.makeText(context, "Reset to default 31 quotes", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Reset to Defaults", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawInputText.trim().isNotEmpty()) {
                        onSaveQuotes(rawInputText)
                        Toast.makeText(context, "Saved $parsedCount quotes for the month!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Please enter at least one quote", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("save_quotes_btn")
            ) {
                Text("Save & Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
