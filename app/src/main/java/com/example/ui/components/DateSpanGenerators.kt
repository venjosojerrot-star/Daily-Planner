package com.example.ui.components

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.api.GeminiVerseService
import com.example.util.DateTimeUtils
import com.example.util.QuoteUtils
import com.example.util.VerseUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDateSpanGeneratorDialog(
    currentQuotes: List<String> = emptyList(),
    initialStartDateMillis: Long = System.currentTimeMillis(),
    initialEndDateMillis: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    onDismiss: () -> Unit,
    onApplyQuotes: (List<String>) -> Unit,
    onResetDefault: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    var startDateMillis by remember { mutableStateOf(initialStartDateMillis) }
    var endDateMillis by remember { mutableStateOf(initialEndDateMillis) }
    var includeDatesInText by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf("All / Balanced Inspiration") }
    var isGeneratingAi by remember { mutableStateOf(false) }

    // Number of days in span
    val daysCount = remember(startDateMillis, endDateMillis) {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = endDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = ((endCal.timeInMillis - startCal.timeInMillis) / (1000L * 60 * 60 * 24)).toInt() + 1
        if (diff > 0) diff else 1
    }

    var generatedQuotes by remember {
        mutableStateOf(
            if (currentQuotes.isNotEmpty()) currentQuotes
            else QuoteUtils.generateQuotesForDateSpan(startDateMillis, endDateMillis, includeDatesInText)
        )
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Preview List, 1: Raw Text Editor
    var rawText by remember(generatedQuotes) {
        mutableStateOf(
            generatedQuotes.mapIndexed { index, q -> "${index + 1}. $q" }.joinToString("\n")
        )
    }

    fun showStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                }
                startDateMillis = newCal.timeInMillis
                if (endDateMillis < startDateMillis) {
                    endDateMillis = startDateMillis + (30L * 24 * 60 * 60 * 1000)
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showEndDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = endDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                }
                if (newCal.timeInMillis >= startDateMillis) {
                    endDateMillis = newCal.timeInMillis
                } else {
                    Toast.makeText(context, "End date must be on or after start date", Toast.LENGTH_SHORT).show()
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun generateInstantQuotes() {
        val quotes = QuoteUtils.generateQuotesForDateSpan(startDateMillis, endDateMillis, includeDatesInText)
        generatedQuotes = quotes
        rawText = quotes.mapIndexed { index, q -> "${index + 1}. $q" }.joinToString("\n")
        Toast.makeText(context, "Generated ${quotes.size} daily quotes for date span", Toast.LENGTH_SHORT).show()
    }

    fun generateAiQuotes() {
        isGeneratingAi = true
        coroutineScope.launch {
            val result = GeminiVerseService.generateQuotesWithAI(daysCount.coerceIn(1, 40), selectedTheme)
            isGeneratingAi = false
            result.onSuccess { aiList ->
                if (aiList.isNotEmpty()) {
                    generatedQuotes = aiList
                    rawText = aiList.mapIndexed { index, q -> "${index + 1}. $q" }.joinToString("\n")
                    Toast.makeText(context, "Gemini AI generated ${aiList.size} custom quotes!", Toast.LENGTH_SHORT).show()
                } else {
                    generateInstantQuotes()
                }
            }.onFailure {
                generateInstantQuotes()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
                .testTag("quote_date_span_generator_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Daily Quotes Settings",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Generate daily quotes for a selected date span",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date Span Picker Section Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SELECT DATE SPAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "🗓️ $daysCount Days",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Date From & To Pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date From
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showStartDatePicker() }
                                    .testTag("quote_date_from_btn")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("From Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sdf.format(Date(startDateMillis)),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Date To
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showEndDatePicker() }
                                    .testTag("quote_date_to_btn")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("To Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sdf.format(Date(endDateMillis)),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Range Presets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Next 7 Days" to 7,
                                "Next 14 Days" to 14,
                                "30 Days (1 Mo)" to 30,
                                "60 Days (2 Mos)" to 60,
                                "90 Days (Quarter)" to 90,
                                "365 Days (1 Yr)" to 365
                            ).forEach { (label, days) ->
                                SuggestionChip(
                                    onClick = {
                                        val now = System.currentTimeMillis()
                                        startDateMillis = now
                                        endDateMillis = now + ((days - 1).toLong() * 24 * 60 * 60 * 1000)
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }

                        // Generate Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { generateInstantQuotes() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_quotes_span_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate ($daysCount Days)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { generateAiQuotes() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_quotes_ai_btn"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isGeneratingAi
                            ) {
                                if (isGeneratingAi) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Generate", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Preview List vs Text Editor
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Quotes Preview (${generatedQuotes.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Edit / Raw Text", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content View
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        // Preview List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(generatedQuotes) { index, quote ->
                                val dayCal = Calendar.getInstance().apply {
                                    timeInMillis = startDateMillis
                                    add(Calendar.DAY_OF_YEAR, index)
                                }
                                val dayDateStr = sdf.format(dayCal.time)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = "Day ${index + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = dayDateStr,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = quote,
                                                fontSize = 12.sp,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Raw Text Editor
                        OutlinedTextField(
                            value = rawText,
                            onValueChange = {
                                rawText = it
                                val parsed = QuoteUtils.parseQuotesFromText(it)
                                if (parsed.isNotEmpty()) {
                                    generatedQuotes = parsed
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("quote_span_raw_editor"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            placeholder = { Text("1. Quote 1\n2. Quote 2...") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Actions Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Quotes", rawText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Quotes copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Quotes", modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = {
                                onResetDefault()
                                Toast.makeText(context, "Reset to default 31 quotes", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = "Reset Defaults", modifier = Modifier.size(20.dp))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val finalList = if (selectedTab == 1) {
                                    QuoteUtils.parseQuotesFromText(rawText)
                                } else {
                                    generatedQuotes
                                }
                                onApplyQuotes(if (finalList.isNotEmpty()) finalList else QuoteUtils.DEFAULT_31_QUOTES)
                                Toast.makeText(context, "Applied ${finalList.size} daily quotes!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier.testTag("apply_quotes_span_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply Quotes (${generatedQuotes.size})")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseDateSpanGeneratorDialog(
    currentVerses: List<String> = emptyList(),
    initialStartDateMillis: Long = System.currentTimeMillis(),
    initialEndDateMillis: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    onDismiss: () -> Unit,
    onApplyVerses: (List<String>) -> Unit,
    onResetDefault: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    var startDateMillis by remember { mutableStateOf(initialStartDateMillis) }
    var endDateMillis by remember { mutableStateOf(initialEndDateMillis) }
    var selectedTheme by remember { mutableStateOf("Faith, Peace & Hope") }
    var isGeneratingAi by remember { mutableStateOf(false) }

    // Number of days in span
    val daysCount = remember(startDateMillis, endDateMillis) {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = endDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = ((endCal.timeInMillis - startCal.timeInMillis) / (1000L * 60 * 60 * 24)).toInt() + 1
        if (diff > 0) diff else 1
    }

    var generatedVerses by remember {
        mutableStateOf(
            if (currentVerses.isNotEmpty()) currentVerses
            else VerseUtils.generateVersesForDateSpan(startDateMillis, endDateMillis)
        )
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Preview List, 1: Raw Text Editor
    var rawText by remember(generatedVerses) {
        mutableStateOf(
            generatedVerses.mapIndexed { index, v -> "Day ${index + 1}:\n$v" }.joinToString("\n\n---\n\n")
        )
    }

    fun showStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                }
                startDateMillis = newCal.timeInMillis
                if (endDateMillis < startDateMillis) {
                    endDateMillis = startDateMillis + (30L * 24 * 60 * 60 * 1000)
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showEndDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = endDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                }
                if (newCal.timeInMillis >= startDateMillis) {
                    endDateMillis = newCal.timeInMillis
                } else {
                    Toast.makeText(context, "End date must be on or after start date", Toast.LENGTH_SHORT).show()
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun generateInstantVerses() {
        val verses = VerseUtils.generateVersesForDateSpan(startDateMillis, endDateMillis)
        generatedVerses = verses
        rawText = verses.mapIndexed { index, v -> "Day ${index + 1}:\n$v" }.joinToString("\n\n---\n\n")
        Toast.makeText(context, "Generated ${verses.size} daily scripture verses for date span", Toast.LENGTH_SHORT).show()
    }

    fun generateAiVerses() {
        isGeneratingAi = true
        coroutineScope.launch {
            val result = GeminiVerseService.generateVersesWithAI(daysCount.coerceIn(1, 31), selectedTheme)
            isGeneratingAi = false
            result.onSuccess { aiList ->
                if (aiList.isNotEmpty()) {
                    generatedVerses = aiList
                    rawText = aiList.mapIndexed { index, v -> "Day ${index + 1}:\n$v" }.joinToString("\n\n---\n\n")
                    Toast.makeText(context, "Gemini AI generated ${aiList.size} daily devotional verses!", Toast.LENGTH_SHORT).show()
                } else {
                    generateInstantVerses()
                }
            }.onFailure {
                generateInstantVerses()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
                .testTag("verse_date_span_generator_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Daily Verse Settings",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Generate daily scripture verses for a date span",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date Span Picker Section Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SELECT DATE SPAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "📖 $daysCount Days",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Date From & To Pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date From
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showStartDatePicker() }
                                    .testTag("verse_date_from_btn")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("From Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sdf.format(Date(startDateMillis)),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Date To
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showEndDatePicker() }
                                    .testTag("verse_date_to_btn")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("To Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sdf.format(Date(endDateMillis)),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Range Presets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Next 7 Days" to 7,
                                "Next 14 Days" to 14,
                                "30 Days (1 Mo)" to 30,
                                "60 Days (2 Mos)" to 60,
                                "90 Days (Quarter)" to 90,
                                "365 Days (1 Yr)" to 365
                            ).forEach { (label, days) ->
                                SuggestionChip(
                                    onClick = {
                                        val now = System.currentTimeMillis()
                                        startDateMillis = now
                                        endDateMillis = now + ((days - 1).toLong() * 24 * 60 * 60 * 1000)
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }

                        // Generate Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { generateInstantVerses() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_verses_span_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate ($daysCount Days)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { generateAiVerses() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_verses_ai_btn"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isGeneratingAi
                            ) {
                                if (isGeneratingAi) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Generate", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Preview List vs Text Editor
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Verses Preview (${generatedVerses.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Edit / Raw Text", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content View
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        // Preview List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(generatedVerses) { index, verse ->
                                val dayCal = Calendar.getInstance().apply {
                                    timeInMillis = startDateMillis
                                    add(Calendar.DAY_OF_YEAR, index)
                                }
                                val dayDateStr = sdf.format(dayCal.time)

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "Day ${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = dayDateStr,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = verse,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Raw Text Editor
                        OutlinedTextField(
                            value = rawText,
                            onValueChange = {
                                rawText = it
                                val parsed = VerseUtils.parseVersesFromText(it)
                                if (parsed.isNotEmpty()) {
                                    generatedVerses = parsed
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("verse_span_raw_editor"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            placeholder = { Text("Day 1:\nVerse...\n\n---\n\nDay 2:\nVerse...") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Actions Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Daily Verses", rawText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Verses copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Verses", modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = {
                                onResetDefault()
                                Toast.makeText(context, "Reset to default 31 scripture verses", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = "Reset Defaults", modifier = Modifier.size(20.dp))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val finalList = if (selectedTab == 1) {
                                    VerseUtils.parseVersesFromText(rawText)
                                } else {
                                    generatedVerses
                                }
                                onApplyVerses(if (finalList.isNotEmpty()) finalList else VerseUtils.DEFAULT_31_VERSES)
                                Toast.makeText(context, "Applied ${finalList.size} daily verses!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.testTag("apply_verses_span_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply Verses (${generatedVerses.size})")
                        }
                    }
                }
            }
        }
    }
}
