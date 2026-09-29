package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PublicSpeakingTalk
import com.example.ui.viewmodel.PlannerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookDialog(
    onDismiss: () -> Unit,
    viewModel: PlannerViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val talks by viewModel.allTalks.collectAsStateWithLifecycle(initialValue = emptyList())

    // UI state
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyFavorites by remember { mutableStateOf(false) }
    
    // Dialog Screens: "LIST", "ADD_EDIT", "PRESENTER"
    var currentScreen by remember { mutableStateOf("LIST") }
    var selectedTalkForEdit by remember { mutableStateOf<PublicSpeakingTalk?>(null) }
    var selectedTalkForPresenter by remember { mutableStateOf<PublicSpeakingTalk?>(null) }

    // Forms
    var talkTitle by remember { mutableStateOf("") }
    var talkContent by remember { mutableStateOf("") }
    var talkSourceQuote by remember { mutableStateOf("") }

    // Filter talks
    val filteredTalks = remember(talks, searchQuery, showOnlyFavorites) {
        talks.filter { talk ->
            val matchesSearch = searchQuery.isBlank() || 
                    talk.title.contains(searchQuery, ignoreCase = true) ||
                    talk.content.contains(searchQuery, ignoreCase = true) ||
                    talk.sourceQuote.contains(searchQuery, ignoreCase = true)
            val matchesFavorite = !showOnlyFavorites || talk.isFavorite
            matchesSearch && matchesFavorite
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("notebook_fullscreen_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            BackHandler {
                when (currentScreen) {
                    "ADD_EDIT" -> currentScreen = "LIST"
                    "PRESENTER" -> currentScreen = "LIST"
                    else -> onDismiss()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            ) {
                when (currentScreen) {
                    "LIST" -> {
                        NotebookListScreen(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            showOnlyFavorites = showOnlyFavorites,
                            onToggleFavoritesFilter = { showOnlyFavorites = !showOnlyFavorites },
                            talksList = filteredTalks,
                            onAddNewTalk = {
                                selectedTalkForEdit = null
                                talkTitle = ""
                                talkContent = ""
                                talkSourceQuote = ""
                                currentScreen = "ADD_EDIT"
                            },
                            onEditTalk = { talk ->
                                selectedTalkForEdit = talk
                                talkTitle = talk.title
                                talkContent = talk.content
                                talkSourceQuote = talk.sourceQuote
                                currentScreen = "ADD_EDIT"
                            },
                            onPresentTalk = { talk ->
                                selectedTalkForPresenter = talk
                                currentScreen = "PRESENTER"
                            },
                            onToggleFavorite = { talk ->
                                viewModel.updatePublicSpeakingTalkFavorite(talk.id, !talk.isFavorite)
                            },
                            onDeleteTalk = { talk ->
                                viewModel.deletePublicSpeakingTalk(talk)
                                Toast.makeText(context, "Talk deleted", Toast.LENGTH_SHORT).show()
                            },
                            onBack = onDismiss
                        )
                    }
                    "ADD_EDIT" -> {
                        NotebookAddEditScreen(
                            title = talkTitle,
                            onTitleChange = { talkTitle = it },
                            content = talkContent,
                            onContentChange = { talkContent = it },
                            sourceQuote = talkSourceQuote,
                            onSourceQuoteChange = { talkSourceQuote = it },
                            isEditing = selectedTalkForEdit != null,
                            viewModel = viewModel,
                            onSave = {
                                if (talkTitle.isBlank() || talkContent.isBlank()) {
                                    Toast.makeText(context, "Please fill in title and content", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (selectedTalkForEdit != null) {
                                        val updated = selectedTalkForEdit!!.copy(
                                            title = talkTitle,
                                            content = talkContent,
                                            sourceQuote = talkSourceQuote,
                                            timestamp = System.currentTimeMillis()
                                        )
                                        viewModel.updatePublicSpeakingTalk(updated)
                                        Toast.makeText(context, "Talk updated successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.savePublicSpeakingTalk(
                                            title = talkTitle,
                                            content = talkContent,
                                            sourceQuote = talkSourceQuote
                                        )
                                        Toast.makeText(context, "Talk saved successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    currentScreen = "LIST"
                                }
                            },
                            onCancel = {
                                currentScreen = "LIST"
                            }
                        )
                    }
                    "PRESENTER" -> {
                        selectedTalkForPresenter?.let { talk ->
                            NotebookPresenterScreen(
                                talk = talk,
                                onBack = { currentScreen = "LIST" }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookListScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showOnlyFavorites: Boolean,
    onToggleFavoritesFilter: () -> Unit,
    talksList: List<PublicSpeakingTalk>,
    onAddNewTalk: () -> Unit,
    onEditTalk: (PublicSpeakingTalk) -> Unit,
    onPresentTalk: (PublicSpeakingTalk) -> Unit,
    onToggleFavorite: (PublicSpeakingTalk) -> Unit,
    onDeleteTalk: (PublicSpeakingTalk) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // App Bar
        TopAppBar(
            title = {
                Text(
                    "Public Speaking Notebook",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF6750A4),
                titleContentColor = Color.White
            )
        )

        // Search & Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search talks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("notebook_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            IconButton(
                onClick = onToggleFavoritesFilter,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (showOnlyFavorites) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Filter Favorites",
                    tint = if (showOnlyFavorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Talks List
        if (talksList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (showOnlyFavorites) "No favorite talks found" else "No talks saved yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Save structured talks directly from daily quotes chat, or click the + button to manually compose a public speaking talk.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(talksList, key = { it.id }) { talk ->
                    TalkCard(
                        talk = talk,
                        onCardClick = { onEditTalk(talk) },
                        onPresentClick = { onPresentTalk(talk) },
                        onFavoriteClick = { onToggleFavorite(talk) },
                        onDeleteClick = { onDeleteTalk(talk) },
                        onCopyClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Public Speaking Talk", talk.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // Floating Action Button to Add Manual Talk
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = onAddNewTalk,
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("notebook_add_talk_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Talk")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TalkCard(
    talk: PublicSpeakingTalk,
    onCardClick: () -> Unit,
    onPresentClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    val formattedDate = remember(talk.timestamp) {
        val sdf = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        sdf.format(Date(talk.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onCardClick)
            .testTag("talk_card_${talk.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title, Date, Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = talk.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
                ) {
                    Icon(
                        imageVector = if (talk.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (talk.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Source quote preview if present
            if (talk.sourceQuote.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Based on: \"${talk.sourceQuote}\"",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body preview
            Text(
                text = talk.content,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Primary use-case: Present/Speak Button
                Button(
                    onClick = onPresentClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6750A4),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Present", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Helper Quick Buttons: Copy, Share, Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onCopyClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Talk Content",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookAddEditScreen(
    title: String,
    onTitleChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    sourceQuote: String,
    onSourceQuoteChange: (String) -> Unit,
    isEditing: Boolean,
    viewModel: PlannerViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = if (isEditing) "Edit Public Speaking Talk" else "Create New Talk",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                TextButton(onClick = onSave) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF6750A4),
                titleContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Text Field
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Talk Title") },
                placeholder = { Text("e.g. Master Your Destiny") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notebook_editor_title"),
                shape = RoundedCornerShape(12.dp)
            )

            // AI Talk Script Generator Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notebook_generator_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                var isGenerating by remember { mutableStateOf(false) }
                var talkDescription by remember { mutableStateOf("") }
                var includeInspirationalStory by remember { mutableStateOf(false) }
                var includeScriptureVerse by remember { mutableStateOf(false) }
                var includeFunnyStory by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "AI Talk Script Generator",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Customize and craft your speech structure",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Describe the talk option
                    OutlinedTextField(
                        value = talkDescription,
                        onValueChange = { talkDescription = it },
                        label = { Text("Describe the talk (Optional)") },
                        placeholder = { Text("e.g. Focus on perseverance, overcoming doubt, or graduation theme") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notebook_generator_description"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    )

                    // Options to add: inspirational story, scripture verse, funny story
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Add to your talk:",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = includeInspirationalStory,
                                onClick = { includeInspirationalStory = !includeInspirationalStory },
                                label = { Text("Inspirational Story", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (includeInspirationalStory) Icons.Default.Check else Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("generator_chip_inspirational")
                            )

                            FilterChip(
                                selected = includeScriptureVerse,
                                onClick = { includeScriptureVerse = !includeScriptureVerse },
                                label = { Text("Scripture Verse", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (includeScriptureVerse) Icons.Default.Check else Icons.Default.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("generator_chip_scripture")
                            )

                            FilterChip(
                                selected = includeFunnyStory,
                                onClick = { includeFunnyStory = !includeFunnyStory },
                                label = { Text("Funny Story", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (includeFunnyStory) Icons.Default.Check else Icons.Default.SentimentSatisfiedAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("generator_chip_funny")
                            )
                        }
                    }

                    // Generator Button
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Please enter a Talk Title first", Toast.LENGTH_SHORT).show()
                            } else {
                                isGenerating = true
                                viewModel.generateTalkWithAI(
                                    title = title,
                                    description = talkDescription,
                                    includeInspirationalStory = includeInspirationalStory,
                                    includeScriptureVerse = includeScriptureVerse,
                                    includeFunnyStory = includeFunnyStory,
                                    onSuccess = { generatedScript ->
                                        onContentChange(generatedScript)
                                        isGenerating = false
                                        Toast.makeText(context, "Talk generated successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { error ->
                                        isGenerating = false
                                        Toast.makeText(context, "Failed to generate talk: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        enabled = !isGenerating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("notebook_generate_ai_btn")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Talk Script...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Talk Script with AI", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Optional Source Quote Info
            OutlinedTextField(
                value = sourceQuote,
                onValueChange = onSourceQuoteChange,
                label = { Text("Source Quote / Inspiration (Optional)") },
                placeholder = { Text("e.g. Failure is the opportunity to begin again...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            // Content Text Field (Multi-line)
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                label = { Text("Structured Speech Content") },
                placeholder = {
                    Text(
                        "Write or paste your speech here. Organize it with clear sections:\n\n" +
                                "1. Hook & Introduction\n" +
                                "2. Core Message & Inspiring Story\n" +
                                "3. Call to Action & Conclusion"
                    )
                },
                minLines = 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notebook_editor_content"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Presenter Mode / Teleprompter Screen for easy reading during Public Speaking.
 * Includes adjustable font size and automatic autoscroll support!
 */
@Composable
fun NotebookPresenterScreen(
    talk: PublicSpeakingTalk,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var fontSize by remember { mutableStateOf(24f) } // Default presenting font size
    var isAutoScrolling by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableStateOf(3) } // 1 (Slow) to 10 (Fast)

    val scrollState = androidx.compose.foundation.rememberScrollState()

    // Speaking Duration Timer State
    var elapsedTimeSeconds by remember { mutableStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(true) }

    // Handle stopwatch increment
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (true) {
                delay(1000L)
                elapsedTimeSeconds += 1
            }
        }
    }

    // Helper to format seconds to MM:SS
    val formattedTime = remember(elapsedTimeSeconds) {
        val minutes = elapsedTimeSeconds / 60
        val seconds = elapsedTimeSeconds % 60
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    // Automatically pause auto-scrolling when user manually drags the screen to keep it stable
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress && isAutoScrolling) {
            isAutoScrolling = false
        }
    }

    // Handle Teleprompter automatic scrolling in coroutine with sub-pixel accumulator for 60FPS fluid motion
    LaunchedEffect(isAutoScrolling, scrollSpeed) {
        if (isAutoScrolling) {
            var accumulator = 0f
            while (isAutoScrolling) {
                delay(16L) // ~60 FPS update interval
                val speedFactor = when (scrollSpeed) {
                    1 -> 0.15f
                    2 -> 0.3f
                    3 -> 0.5f
                    4 -> 0.8f
                    5 -> 1.2f
                    6 -> 1.8f
                    7 -> 2.6f
                    8 -> 3.6f
                    9 -> 5.0f
                    10 -> 7.0f
                    else -> 1.0f
                }
                accumulator += speedFactor
                if (accumulator >= 1f) {
                    val pixelsToScroll = accumulator.toInt()
                    accumulator -= pixelsToScroll
                    if (scrollState.value < scrollState.maxValue) {
                        scrollState.scrollTo(scrollState.value + pixelsToScroll)
                    } else {
                        isAutoScrolling = false
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14)) // Cinematic dark backdrop
    ) {
        // Immersive Control Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Presentation",
                    tint = Color.White
                )
            }

            Text(
                text = talk.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )

            // Font Sizing Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (fontSize > 16) fontSize -= 2f }) {
                    Icon(
                        imageVector = Icons.Default.TextFormat,
                        contentDescription = "Decrease Font Size",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "${fontSize.toInt()}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { if (fontSize < 48) fontSize += 2f }) {
                    Icon(
                        imageVector = Icons.Default.TextFormat,
                        contentDescription = "Increase Font Size",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Speaking Timer Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E24))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Speaking Duration",
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Speaking Duration:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Text(
                    text = formattedTime,
                    color = Color(0xFFFFB300),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("presenter_timer_text")
                )
            }

            // Quick Timer Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { isTimerRunning = !isTimerRunning },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Pause Speaking Timer",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { elapsedTimeSeconds = 0 },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Speaking Timer",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Main Teleprompter Text Display Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(vertical = 40.dp)
            ) {
                // Focus marker bar or instruction if scrolling
                if (isAutoScrolling) {
                    Text(
                        "--- Teleprompter Scrolling Active ---",
                        color = Color(0xFFFFB300),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }

                Text(
                    text = talk.content,
                    color = Color(0xFFEEEEEE), // Slightly soft white to prevent glare
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.45).sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("presenter_teleprompter_text")
                )

                Spacer(modifier = Modifier.height(150.dp)) // Extra scrollable spacing at the end
            }

            // Top and Bottom gradient overlays to create a cinema teleprompter fade effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F0F14), Color.Transparent)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF0F0F14))
                        )
                    )
            )
        }

        // Teleprompter Autoscroll Deck Control Board
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { isAutoScrolling = !isAutoScrolling },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (isAutoScrolling) Color(0xFFFFB300) else Color(0xFF6750A4),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause Scrolling",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = if (isAutoScrolling) "Scrolling" else "Paused",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Reset Scroll Button
                    TextButton(
                        onClick = {
                            isAutoScrolling = false
                            coroutineScope.launch {
                                scrollState.animateScrollTo(0)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Scroll", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }

                // Speed Slider Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Speed",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.width(42.dp)
                    )

                    Slider(
                        value = scrollSpeed.toFloat(),
                        onValueChange = { scrollSpeed = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFFFFB300),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                            thumbColor = Color(0xFFFFB300)
                        )
                    )

                    Text(
                        "${scrollSpeed}x",
                        color = Color(0xFFFFB300),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp)
                    )
                }
            }
        }
    }
}
