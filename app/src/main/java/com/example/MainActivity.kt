package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyanFAB
import com.example.ui.theme.DailyPlannerTheme
import com.example.ui.viewmodel.PlannerEntryType
import com.example.ui.viewmodel.PlannerViewModel
import com.example.ui.viewmodel.PlannerViewMode
import com.example.data.model.WalletAccount

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.io.OutputStreamWriter
import java.io.InputStreamReader
import android.widget.Toast

import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PlannerViewModel = viewModel()
            val themeColor by viewModel.themeColor.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            
            val isDarkTheme = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            DailyPlannerTheme(darkTheme = isDarkTheme, themeColor = themeColor) {
                PlannerMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PlannerMainApp(viewModel: PlannerViewModel) {
    val selectedDateMillis by viewModel.selectedDateMillis.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()

    val filteredEvents by viewModel.filteredEventsForSelectedDate.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasksForSelectedDate.collectAsStateWithLifecycle()
    val allEvents by viewModel.allEvents.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val allWalletTransactions by viewModel.allWalletTransactions.collectAsStateWithLifecycle()
    val allWalletDebts by viewModel.allWalletDebts.collectAsStateWithLifecycle()
    val allWalletBudgets by viewModel.allWalletBudgets.collectAsStateWithLifecycle()
    val allWalletGoals by viewModel.allWalletGoals.collectAsStateWithLifecycle()
    val allWalletAccounts by viewModel.allWalletAccounts.collectAsStateWithLifecycle()

    val isAddSheetOpen by viewModel.isAddSheetOpen.collectAsStateWithLifecycle()
    val addSheetType by viewModel.addSheetType.collectAsStateWithLifecycle()
    val editingEvent by viewModel.editingEvent.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()
    val preselectedTaskCategory by viewModel.preselectedTaskCategory.collectAsStateWithLifecycle()

    val isBatchCheckMode by viewModel.isBatchCheckMode.collectAsStateWithLifecycle()
    val selectedEventIds by viewModel.selectedEventIds.collectAsStateWithLifecycle()

    val isWalletPinEnabled by viewModel.isWalletPinEnabled.collectAsStateWithLifecycle()
    val isWalletAuthenticated by viewModel.isWalletAuthenticated.collectAsStateWithLifecycle()
    val walletPin by viewModel.walletPin.collectAsStateWithLifecycle()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()

    val monthlyQuotes by viewModel.monthlyQuotes.collectAsStateWithLifecycle()
    val isQuoteDialogOpen by viewModel.isQuoteDialogOpen.collectAsStateWithLifecycle()
    val monthlyVerses by viewModel.monthlyVerses.collectAsStateWithLifecycle()
    val isVerseDialogOpen by viewModel.isVerseDialogOpen.collectAsStateWithLifecycle()
    val isNotebookDialogOpen by viewModel.isNotebookDialogOpen.collectAsStateWithLifecycle()
    val dailyMoods by viewModel.dailyMoods.collectAsStateWithLifecycle()
    val dailyJournals by viewModel.dailyJournals.collectAsStateWithLifecycle()
    val allClosetItems by viewModel.allClosetItems.collectAsStateWithLifecycle()
    val allClosetOutfits by viewModel.allClosetOutfits.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var activeSettingsSection by remember { mutableStateOf("all") }
    var showDayDeleteDialogDate by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            val json = viewModel.exportDataToJson()
            if (json != null) {
                try {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        OutputStreamWriter(os).use { writer ->
                            writer.write(json)
                        }
                    }
                    Toast.makeText(context, "Data exported successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val json = reader.readText()
                        viewModel.importDataFromJson(json)
                        Toast.makeText(context, "Data imported successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProfileDrawerContent(
                currentViewMode = viewMode,
                onViewModeSelect = { viewModel.setViewMode(it) },
                onOpenSettings = { section ->
                    activeSettingsSection = section
                    showSettingsDialog = true
                },
                onOpenQuoteOfTheDay = { viewModel.openQuoteDialog() },
                onOpenDailyVerse = { viewModel.openVerseDialog() },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopCalendarBar(
                    selectedDateMillis = selectedDateMillis,
                    currentViewMode = viewMode,
                    selectedCategory = selectedCategory,
                    searchQuery = searchQuery,
                    onTodayClick = { viewModel.setToday() },
                    onPreviousClick = {
                        if (viewMode == PlannerViewMode.MONTH) {
                            viewModel.navigateDateByMonths(-1)
                        } else {
                            viewModel.navigateDateByDays(-1)
                        }
                    },
                    onNextClick = {
                        if (viewMode == PlannerViewMode.MONTH) {
                            viewModel.navigateDateByMonths(1)
                        } else {
                            viewModel.navigateDateByDays(1)
                        }
                    },
                    onViewModeChange = { viewModel.setViewMode(it) },
                    onCategoryChange = { viewModel.setCategory(it) },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onMenuClick = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    isBatchCheckMode = isBatchCheckMode,
                    onToggleBatchMode = { viewModel.toggleBatchCheckMode() },
                    onExportData = { exportLauncher.launch("planner_backup.json") },
                    onImportData = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    onQuoteClick = { viewModel.openQuoteDialog() },
                    onVerseClick = { viewModel.openVerseDialog() },
                    onNotebookClick = { viewModel.openNotebookDialog() }
                )
            },
        bottomBar = {
            BottomNavigationBar(
                currentViewMode = viewMode,
                onViewModeSelect = { viewModel.setViewMode(it) }
            )
        },
        floatingActionButton = {
            if (viewMode == PlannerViewMode.DAY || viewMode == PlannerViewMode.AGENDA) {
                val hasSelection = isBatchCheckMode && selectedEventIds.isNotEmpty()
                FloatingActionButton(
                    onClick = {
                        if (hasSelection) {
                            viewModel.markSelectedEventsAsHappened()
                        } else {
                            viewModel.openAddSheet(PlannerEntryType.EVENT)
                        }
                    },
                    containerColor = if (hasSelection) Color(0xFF00E676) else CyanFAB,
                    contentColor = if (hasSelection) Color.Black else Color.White,
                    modifier = Modifier.testTag("main_add_fab")
                ) {
                    if (hasSelection) {
                        Icon(Icons.Default.Save, contentDescription = "Save Selected as Happened")
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "Add Schedule Item")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Strip Calendar for Day view
            if (viewMode == PlannerViewMode.DAY) {
                MiniCalendarStrip(
                    selectedDateMillis = selectedDateMillis,
                    onDateSelect = { viewModel.setSelectedDate(it) },
                    onDateLongClick = { showDayDeleteDialogDate = it }
                )
            }

            // Main Active View Content
            when (viewMode) {
                PlannerViewMode.DAY -> {
                    DayScheduleView(
                        selectedDateMillis = selectedDateMillis,
                        events = filteredEvents,
                        tasks = filteredTasks,
                        onEventClick = { viewModel.openEditEvent(it) },
                        onEventMove = { event, newStart, newEnd ->
                            viewModel.moveEvent(event, newStart, newEnd)
                        },
                        onTaskClick = { viewModel.openEditTask(it) },
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onAddClick = { viewModel.openAddSheet(PlannerEntryType.EVENT) },
                        onNavigateDate = { days -> viewModel.navigateDateByDays(days) },
                        isBatchCheckMode = isBatchCheckMode,
                        selectedEventIds = selectedEventIds,
                        onToggleEventSelection = { viewModel.toggleEventSelection(it) },
                        onToggleHappened = { viewModel.toggleEventHappened(it) }
                    )
                }
                PlannerViewMode.MONTH -> {
                    MonthGridView(
                        selectedDateMillis = selectedDateMillis,
                        allEvents = allEvents,
                        allTasks = allTasks,
                        dailyMoods = dailyMoods,
                        dailyJournals = dailyJournals,
                        onDateSelect = {
                            viewModel.setSelectedDate(it)
                            viewModel.setViewMode(PlannerViewMode.DAY)
                        },
                        onNavigateMonth = { viewModel.navigateDateByMonths(it) },
                        onSaveMood = { millis, emoji -> viewModel.setDailyMood(millis, emoji) },
                        onRemoveMood = { millis -> viewModel.removeDailyMood(millis) },
                        onSaveJournal = { millis, text -> viewModel.setDailyJournal(millis, text) },
                        onRemoveJournal = { millis -> viewModel.removeDailyJournal(millis) }
                    )
                }
                PlannerViewMode.AGENDA -> {
                    AgendaView(
                        events = filteredEvents,
                        tasks = filteredTasks,
                        onEventClick = { viewModel.openEditEvent(it) },
                        onTaskClick = { viewModel.openEditTask(it) },
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onToggleHappened = { viewModel.toggleEventHappened(it) },
                        isBatchCheckMode = isBatchCheckMode,
                        selectedEventIds = selectedEventIds,
                        onToggleEventSelection = { viewModel.toggleEventSelection(it) }
                    )
                }
                PlannerViewMode.TASKS -> {
                    TasksScreen(
                        tasks = allTasks,
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onToggleStar = { viewModel.toggleTaskStar(it) },
                        onEditTask = { viewModel.openEditTask(it) },
                        onAddNewTask = { category -> viewModel.openAddSheet(PlannerEntryType.TASK, category) },
                        onDeleteCategory = { category -> viewModel.deleteTasksByCategory(category) },
                        onRenameCategory = { oldCat, newCat -> viewModel.renameCategory(oldCat, newCat) }
                    )
                }
                PlannerViewMode.STATS -> {
                    com.example.ui.components.EventsStatsScreen(
                        selectedDateMillis = selectedDateMillis,
                        allEvents = allEvents,
                        onToggleHappened = { viewModel.toggleEventHappened(it) }
                    )
                }
                PlannerViewMode.WALLET -> {
                    if (isWalletPinEnabled && !isWalletAuthenticated) {
                        WalletPasscodeScreen(
                            expectedPinLength = if (walletPin.isNotEmpty()) walletPin.length else 4,
                            onAuthenticate = { pin ->
                                viewModel.authenticateWallet(pin)
                            }
                        )
                    } else {
                        WalletScreen(
                            transactions = allWalletTransactions,
                            debts = allWalletDebts,
                            budgets = allWalletBudgets,
                            goals = allWalletGoals,
                            accounts = allWalletAccounts,
                            selectedCurrency = selectedCurrency,
                            onAddTransaction = { title, category, amount, isIncome, note, timestampMillis, accountName ->
                                viewModel.addWalletTransaction(title, category, amount, isIncome, note, timestampMillis, accountName)
                            },
                            onUpdateTransaction = { transaction ->
                                viewModel.updateWalletTransaction(transaction)
                            },
                            onDeleteTransaction = { transaction ->
                                viewModel.deleteWalletTransaction(transaction)
                            },
                            onAddDebt = { personName, purpose, amount, isLent ->
                                viewModel.addWalletDebt(personName, purpose, amount, isLent)
                            },
                            onDeleteDebt = { debt ->
                                viewModel.deleteWalletDebt(debt)
                            },
                            onToggleDebtSettlement = { debt ->
                                viewModel.toggleWalletDebtSettlement(debt)
                            },
                            onAddBudget = { category, limitAmount, fromDateMillis, toDateMillis ->
                                viewModel.addWalletBudget(category, limitAmount, fromDateMillis, toDateMillis)
                            },
                            onUpdateBudget = { budget ->
                                viewModel.updateWalletBudget(budget)
                            },
                            onDeleteBudget = { budget ->
                                viewModel.deleteWalletBudget(budget)
                            },
                            onAddGoal = { name, targetAmount, currentAmount, deadlineMillis ->
                                viewModel.addWalletGoal(name, targetAmount, currentAmount, deadlineMillis)
                            },
                            onUpdateGoal = { goal ->
                                viewModel.updateWalletGoal(goal)
                            },
                            onDeleteGoal = { goal ->
                                viewModel.deleteWalletGoal(goal)
                            },
                            onAddContributionToGoal = { goal, amount ->
                                viewModel.addContributionToGoal(goal, amount)
                            },
                            onAddAccount = { name, type, initialBalance ->
                                viewModel.addWalletAccount(name, type, initialBalance)
                            },
                            onDeleteAccount = { account ->
                                viewModel.deleteWalletAccount(account)
                            }
                        )
                    }
                }
                PlannerViewMode.CLOSET -> {
                    ClosetScreen(
                        closetItems = allClosetItems,
                        closetOutfits = allClosetOutfits,
                        selectedCurrency = selectedCurrency,
                        onAddItem = { name, category, color, season, brand, size, occasion, imagePath, status, price, notes ->
                            viewModel.addClosetItem(name, category, color, season, brand, size, occasion, imagePath, status, price, notes)
                        },
                        onUpdateItem = { item ->
                            viewModel.updateClosetItem(item)
                        },
                        onDeleteItem = { item ->
                            viewModel.deleteClosetItem(item)
                        },
                        onToggleFavorite = { item ->
                            viewModel.toggleClosetItemFavorite(item)
                        },
                        onUpdateStatus = { id, status ->
                            viewModel.updateClosetItemStatus(id, status)
                        },
                        onLogWorn = { id ->
                            viewModel.logClosetItemWorn(id)
                        },
                        onAddOutfit = { name, occasion, itemIds, notes, imagePath ->
                            viewModel.addClosetOutfit(name, occasion, itemIds, notes, imagePath)
                        },
                        onUpdateOutfit = { outfit ->
                            viewModel.updateClosetOutfit(outfit)
                        },
                        onDeleteOutfit = { outfit ->
                            viewModel.deleteClosetOutfit(outfit)
                        },
                        onToggleOutfitFavorite = { outfit ->
                            viewModel.toggleClosetOutfitFavorite(outfit)
                        },
                        onLogOutfitWorn = { id ->
                            viewModel.logClosetOutfitWorn(id)
                        }
                    )
                }
            }
        }

        // Add/Edit Bottom Sheet Modal
        if (isAddSheetOpen) {
            AddEditPlannerSheet(
                initialType = addSheetType,
                selectedDateMillis = selectedDateMillis,
                editingEvent = editingEvent,
                editingTask = editingTask,
                onSaveEvent = { viewModel.saveEvent(it) },
                onSaveTask = { viewModel.saveTask(it) },
                onDeleteEvent = { viewModel.deleteEvent(it) ; viewModel.closeAddSheet() },
                onDeleteTask = { viewModel.deleteTask(it) ; viewModel.closeAddSheet() },
                onDeleteEventSeries = { viewModel.deleteEventSeries(it) },
                onDismiss = { viewModel.closeAddSheet() },
                existingEvents = allEvents,
                preselectedCategory = preselectedTaskCategory
            )
        }
        }
    }

    if (showSettingsDialog) {
        val currentThemeColor by viewModel.themeColor.collectAsState()
        val currentThemeMode by viewModel.themeMode.collectAsState()

        SettingsDialog(
            initialCurrency = selectedCurrency,
            isWalletPinEnabled = isWalletPinEnabled,
            walletPin = walletPin,
            activeSection = activeSettingsSection,
            themeColor = currentThemeColor,
            themeMode = currentThemeMode,
            monthlyQuotes = monthlyQuotes,
            monthlyVerses = monthlyVerses,
            onDismiss = { showSettingsDialog = false },
            onSaveCurrency = { viewModel.setCurrency(it) },
            onSaveWalletPinSettings = { enabled, pin -> viewModel.saveWalletPinSettings(enabled, pin) },
            onSaveThemeSettings = { color, mode -> 
                viewModel.setThemeColor(color)
                viewModel.setThemeMode(mode)
            },
            onSaveQuotesText = { viewModel.importQuotesFromText(it) },
            onSaveQuotesList = { viewModel.updateMonthlyQuotes(it) },
            onResetQuotesDefault = { viewModel.resetQuotesToDefault() },
            onSaveVersesText = { viewModel.importVersesFromText(it) },
            onSaveVersesList = { viewModel.updateMonthlyVerses(it) },
            onResetVersesDefault = { viewModel.resetVersesToDefault() },
            onExportData = {
                exportLauncher.launch("DailyPlanner_Backup_${System.currentTimeMillis()}.json")
                showSettingsDialog = false
            },
            onImportData = {
                importLauncher.launch(arrayOf("application/json", "*/*"))
                showSettingsDialog = false
            }
        )
    }

    if (isQuoteDialogOpen) {
        QuoteOfTheDayDialog(
            selectedDateMillis = selectedDateMillis,
            monthlyQuotes = monthlyQuotes,
            onDismiss = { viewModel.closeQuoteDialog() },
            onOpenImport = {
                activeSettingsSection = "quotes"
                showSettingsDialog = true
            },
            viewModel = viewModel
        )
    }

    if (isVerseDialogOpen) {
        com.example.ui.components.DailyVerseDialog(
            selectedDateMillis = selectedDateMillis,
            monthlyVerses = monthlyVerses,
            onDismiss = { viewModel.closeVerseDialog() },
            onOpenImport = {
                activeSettingsSection = "verses"
                showSettingsDialog = true
            },
            viewModel = viewModel
        )
    }

    if (isNotebookDialogOpen) {
        NotebookDialog(
            onDismiss = { viewModel.closeNotebookDialog() },
            viewModel = viewModel
        )
    }

    showDayDeleteDialogDate?.let { dateMillis ->
        val formattedDate = com.example.util.DateTimeUtils.formatDayHeader(dateMillis)
        AlertDialog(
            onDismissRequest = { showDayDeleteDialogDate = null },
            title = { Text("Delete Day Schedules?") },
            text = { Text("Are you sure you want to delete all scheduled events on $formattedDate?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEventsOnDay(dateMillis)
                        showDayDeleteDialogDate = null
                        Toast.makeText(context, "All events on $formattedDate deleted", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_delete_day_btn")
                ) {
                    Text("Yes", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDayDeleteDialogDate = null },
                    modifier = Modifier.testTag("dismiss_delete_day_btn")
                ) {
                    Text("No")
                }
            },
            modifier = Modifier.testTag("delete_day_dialog")
        )
    }
}
