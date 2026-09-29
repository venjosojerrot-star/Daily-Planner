package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.PlannerViewMode
import com.example.util.DateTimeUtils

import com.example.ui.theme.BurgundyTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopCalendarBar(
    selectedDateMillis: Long,
    currentViewMode: PlannerViewMode,
    selectedCategory: String,
    searchQuery: String,
    onTodayClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onViewModeChange: (PlannerViewMode) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    isBatchCheckMode: Boolean = false,
    onToggleBatchMode: () -> Unit = {},
    onExportData: () -> Unit = {},
    onImportData: () -> Unit = {},
    onQuoteClick: () -> Unit = {},
    onVerseClick: () -> Unit = {},
    onNotebookClick: () -> Unit = {}
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurgundyTopBar)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                if (isSearchExpanded) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search...", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input")
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { 
                            if (currentViewMode == PlannerViewMode.DAY || currentViewMode == PlannerViewMode.MONTH || currentViewMode == PlannerViewMode.AGENDA || currentViewMode == PlannerViewMode.TASKS) {
                                onTodayClick()
                            }
                        }
                    ) {
                        Text(
                            text = when (currentViewMode) {
                                PlannerViewMode.WALLET -> "My Wallet"
                                PlannerViewMode.CLOSET -> "My Closet"
                                PlannerViewMode.STATS -> "Statistics"
                                else -> DateTimeUtils.formatDayMonth(selectedDateMillis)
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                        if (currentViewMode == PlannerViewMode.DAY || currentViewMode == PlannerViewMode.MONTH || currentViewMode == PlannerViewMode.AGENDA || currentViewMode == PlannerViewMode.TASKS) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Date",
                                tint = Color.White
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (isSearchExpanded) {
                        onSearchQueryChange("")
                        isSearchExpanded = false
                    } else {
                        onMenuClick()
                    }
                }) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }
            },
            actions = {
                if (!isSearchExpanded) {
                    IconButton(
                        onClick = onQuoteClick,
                        modifier = Modifier.testTag("top_bar_quote_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Quote of the Day",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onVerseClick,
                        modifier = Modifier.testTag("top_bar_verse_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Daily Verse",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onNotebookClick,
                        modifier = Modifier.testTag("top_bar_notebook_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Public Speaking Notebook",
                            tint = Color.White
                        )
                    }
                    var showMoreMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMoreMenu, 
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export", fontSize = 13.sp) },
                                onClick = {
                                    showMoreMenu = false
                                    onExportData()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.FileUpload, contentDescription = "Export", modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.height(36.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("Import", fontSize = 13.sp) },
                                onClick = {
                                    showMoreMenu = false
                                    onImportData()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.FileDownload, contentDescription = "Import", modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BurgundyTopBar,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
    }
}
