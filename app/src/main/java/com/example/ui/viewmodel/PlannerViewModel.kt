package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.FocusSession
import com.example.data.model.PlannerEvent
import com.example.data.model.PlannerTask
import com.example.data.model.WalletTransaction
import com.example.data.repository.PlannerRepository
import com.example.util.AmbientSoundGenerator
import com.example.util.AmbientSoundType
import com.example.util.SoundUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.data.model.BackupData

import com.example.data.model.ClosetItem
import com.example.data.model.ClosetOutfit

enum class PlannerViewMode {
    DAY, MONTH, AGENDA, TASKS, WALLET, CLOSET, STATS
}

enum class PlannerEntryType {
    EVENT, TASK
}

class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlannerRepository
    private val ambientSoundGenerator = AmbientSoundGenerator()

    // Active Selected Date
    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    // View Mode & Filtering
    private val _viewMode = MutableStateFlow(PlannerViewMode.DAY)
    val viewMode: StateFlow<PlannerViewMode> = _viewMode.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("₱ PHP")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Batch Check Mode State
    private val _isBatchCheckMode = MutableStateFlow(false)
    val isBatchCheckMode: StateFlow<Boolean> = _isBatchCheckMode.asStateFlow()

    private val _selectedEventIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedEventIds: StateFlow<Set<Long>> = _selectedEventIds.asStateFlow()

    // Creation/Editing State
    private val _isAddSheetOpen = MutableStateFlow(false)
    val isAddSheetOpen: StateFlow<Boolean> = _isAddSheetOpen.asStateFlow()

    private val _addSheetType = MutableStateFlow(PlannerEntryType.EVENT)
    val addSheetType: StateFlow<PlannerEntryType> = _addSheetType.asStateFlow()

    private val _editingEvent = MutableStateFlow<PlannerEvent?>(null)
    val editingEvent: StateFlow<PlannerEvent?> = _editingEvent.asStateFlow()

    private val _editingTask = MutableStateFlow<PlannerTask?>(null)
    val editingTask: StateFlow<PlannerTask?> = _editingTask.asStateFlow()

    private val _preselectedTaskCategory = MutableStateFlow<String?>(null)
    val preselectedTaskCategory: StateFlow<String?> = _preselectedTaskCategory.asStateFlow()

    // Focus Timer State
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(25 * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _totalSeconds = MutableStateFlow(25 * 60)
    val totalSeconds: StateFlow<Int> = _totalSeconds.asStateFlow()

    private val _timerModeName = MutableStateFlow("Focus")
    val timerModeName: StateFlow<String> = _timerModeName.asStateFlow()

    private val _activeFocusTask = MutableStateFlow<PlannerTask?>(null)
    val activeFocusTask: StateFlow<PlannerTask?> = _activeFocusTask.asStateFlow()

    private val _ambientSound = MutableStateFlow(AmbientSoundType.NONE)
    val ambientSound: StateFlow<AmbientSoundType> = _ambientSound.asStateFlow()

    private val _themeColor = MutableStateFlow("Default")
    val themeColor: StateFlow<String> = _themeColor.asStateFlow()

    private val _themeMode = MutableStateFlow("System") // "System", "Light", "Dark"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Wallet security states
    private val sharedPrefs = application.getSharedPreferences("wallet_settings", Context.MODE_PRIVATE)

    private val _isWalletPinEnabled = MutableStateFlow(false)
    val isWalletPinEnabled: StateFlow<Boolean> = _isWalletPinEnabled.asStateFlow()

    private val _walletPin = MutableStateFlow("")
    val walletPin: StateFlow<String> = _walletPin.asStateFlow()

    private val _isWalletAuthenticated = MutableStateFlow(false)
    val isWalletAuthenticated: StateFlow<Boolean> = _isWalletAuthenticated.asStateFlow()

    // Cloud synchronization & Authentication States
    private val firebaseAuth by lazy { com.google.firebase.auth.FirebaseAuth.getInstance() }
    private val firestore by lazy { com.google.firebase.firestore.FirebaseFirestore.getInstance() }

    private val _currentUser = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    // Quote of the Day State
    private val _monthlyQuotes = MutableStateFlow<List<String>>(com.example.util.QuoteUtils.DEFAULT_31_QUOTES)
    val monthlyQuotes: StateFlow<List<String>> = _monthlyQuotes.asStateFlow()

    private val _isQuoteDialogOpen = MutableStateFlow(false)
    val isQuoteDialogOpen: StateFlow<Boolean> = _isQuoteDialogOpen.asStateFlow()

    val quoteOfTheDay: StateFlow<String> = combine(_monthlyQuotes, _selectedDateMillis) { quotes, dateMillis ->
        com.example.util.QuoteUtils.getQuoteForDay(quotes, dateMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.util.QuoteUtils.DEFAULT_31_QUOTES.first())

    // Daily Verse State
    private val _monthlyVerses = MutableStateFlow<List<String>>(com.example.util.VerseUtils.DEFAULT_31_VERSES)
    val monthlyVerses: StateFlow<List<String>> = _monthlyVerses.asStateFlow()

    private val _isVerseDialogOpen = MutableStateFlow(false)
    val isVerseDialogOpen: StateFlow<Boolean> = _isVerseDialogOpen.asStateFlow()

    private val _isNotebookDialogOpen = MutableStateFlow(false)
    val isNotebookDialogOpen: StateFlow<Boolean> = _isNotebookDialogOpen.asStateFlow()

    val dailyVerse: StateFlow<String> = combine(_monthlyVerses, _selectedDateMillis) { verses, dateMillis ->
        com.example.util.VerseUtils.getVerseForDay(verses, dateMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.util.VerseUtils.DEFAULT_31_VERSES.first())

    // Daily Mood Tracking State (DateKey -> Emoji)
    private val _dailyMoods = MutableStateFlow<Map<String, String>>(emptyMap())
    val dailyMoods: StateFlow<Map<String, String>> = _dailyMoods.asStateFlow()

    // Daily Journal Diary State (DateKey -> Entry text)
    private val _dailyJournals = MutableStateFlow<Map<String, String>>(emptyMap())
    val dailyJournals: StateFlow<Map<String, String>> = _dailyJournals.asStateFlow()

    private var timerJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PlannerRepository(
            db.eventDao(),
            db.taskDao(),
            db.focusSessionDao(),
            db.walletDao(),
            db.walletExtraDao(),
            db.closetDao(),
            db.dailyVerseChatDao(),
            db.publicSpeakingTalkDao()
        )

        val appPrefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        _themeColor.value = appPrefs.getString("theme_color", "Default") ?: "Default"
        _themeMode.value = appPrefs.getString("theme_mode", "System") ?: "System"

        _isWalletPinEnabled.value = sharedPrefs.getBoolean("wallet_pin_enabled", false)
        _walletPin.value = sharedPrefs.getString("wallet_pin", "") ?: ""

        _monthlyQuotes.value = com.example.util.QuoteUtils.loadQuotes(application)
        _monthlyVerses.value = com.example.util.VerseUtils.loadVerses(application)
        _dailyMoods.value = loadMoodsFromPrefs()
        _dailyJournals.value = loadJournalsFromPrefs()

        try {
            _currentUser.value = firebaseAuth.currentUser
            firebaseAuth.addAuthStateListener { auth ->
                _currentUser.value = auth.currentUser
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        viewModelScope.launch {
            if (!sharedPrefs.getBoolean("wallet_presets_cleared_v1", false)) {
                repository.clearAllWalletTransactions()
                sharedPrefs.edit().putBoolean("wallet_presets_cleared_v1", true).apply()
            }
            repository.seedInitialDataIfEmpty()

            repository.allWalletAccounts.first().let { accounts ->
                if (accounts.isEmpty()) {
                    repository.insertWalletAccount(com.example.data.model.WalletAccount(name = "Cash", type = "Cash"))
                }
            }
        }
    }

    // Data Streams
    val allEvents: StateFlow<List<PlannerEvent>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<PlannerTask>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFocusSessions: StateFlow<List<FocusSession>> = repository.allFocusSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWalletTransactions: StateFlow<List<WalletTransaction>> = repository.allWalletTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWalletDebts: StateFlow<List<com.example.data.model.WalletDebt>> = repository.allWalletDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWalletBudgets: StateFlow<List<com.example.data.model.WalletBudget>> = repository.allWalletBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWalletGoals: StateFlow<List<com.example.data.model.WalletGoal>> = repository.allWalletGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWalletAccounts: StateFlow<List<com.example.data.model.WalletAccount>> = repository.allWalletAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClosetItems: StateFlow<List<ClosetItem>> = repository.allClosetItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClosetOutfits: StateFlow<List<ClosetOutfit>> = repository.allClosetOutfits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVerseChatMessages: StateFlow<List<com.example.data.model.DailyVerseChatMessage>> = repository.allVerseChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayStartMillis: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val focusTimeTodaySeconds: StateFlow<Int> = repository.getFocusTimeTodaySeconds(todayStartMillis)
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Filtered events for selected date/search/category
    val filteredEventsForSelectedDate: StateFlow<List<PlannerEvent>> = combine(
        allEvents,
        selectedDateMillis,
        selectedCategory,
        searchQuery
    ) { events, dateMillis, category, query ->
        val startOfDay = getStartOfDayMillis(dateMillis)
        val endOfDay = getEndOfDayMillis(dateMillis)

        events.filter { event ->
            val matchesDate = event.startEpochMillis in startOfDay..endOfDay ||
                    (event.startEpochMillis <= startOfDay && event.endEpochMillis >= endOfDay)
            val matchesCategory = category == "All" || event.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    event.title.contains(query, ignoreCase = true) ||
                    event.description.contains(query, ignoreCase = true) ||
                    event.location.contains(query, ignoreCase = true)

            matchesDate && matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered tasks for selected date/search/category
    val filteredTasksForSelectedDate: StateFlow<List<PlannerTask>> = combine(
        allTasks,
        selectedDateMillis,
        selectedCategory,
        searchQuery
    ) { tasks, dateMillis, category, query ->
        tasks.filter { task ->
            val matchesCategory = category == "All" || task.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)

            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation and Selection Actions
    fun setSelectedDate(millis: Long) {
        _selectedDateMillis.value = millis
    }

    fun navigateDateByDays(days: Int) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = _selectedDateMillis.value
        cal.add(Calendar.DAY_OF_YEAR, days)
        _selectedDateMillis.value = cal.timeInMillis
    }

    fun navigateDateByMonths(months: Int) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = _selectedDateMillis.value
        cal.add(Calendar.MONTH, months)
        _selectedDateMillis.value = cal.timeInMillis
    }

    fun setDailyMood(dateMillis: Long, emoji: String) {
        val key = com.example.util.DateTimeUtils.formatDateKey(dateMillis)
        val current = _dailyMoods.value.toMutableMap()
        if (emoji.isBlank()) {
            current.remove(key)
        } else {
            current[key] = emoji.trim()
        }
        _dailyMoods.value = current
        saveMoodsToPrefs(current)
    }

    fun removeDailyMood(dateMillis: Long) {
        val key = com.example.util.DateTimeUtils.formatDateKey(dateMillis)
        val current = _dailyMoods.value.toMutableMap()
        current.remove(key)
        _dailyMoods.value = current
        saveMoodsToPrefs(current)
    }

    fun setDailyJournal(dateMillis: Long, content: String) {
        val key = com.example.util.DateTimeUtils.formatDateKey(dateMillis)
        val current = _dailyJournals.value.toMutableMap()
        if (content.isBlank()) {
            current.remove(key)
        } else {
            current[key] = content.trim()
        }
        _dailyJournals.value = current
        saveJournalsToPrefs(current)
    }

    fun removeDailyJournal(dateMillis: Long) {
        val key = com.example.util.DateTimeUtils.formatDateKey(dateMillis)
        val current = _dailyJournals.value.toMutableMap()
        current.remove(key)
        _dailyJournals.value = current
        saveJournalsToPrefs(current)
    }

    private fun loadMoodsFromPrefs(): Map<String, String> {
        val json = sharedPrefs.getString("daily_moods_json", null) ?: return emptyMap()
        return try {
            val jsonObject = org.json.JSONObject(json)
            val map = mutableMapOf<String, String>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getString(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveMoodsToPrefs(moods: Map<String, String>) {
        val jsonObject = org.json.JSONObject()
        moods.forEach { (k, v) -> jsonObject.put(k, v) }
        sharedPrefs.edit().putString("daily_moods_json", jsonObject.toString()).apply()
    }

    private fun loadJournalsFromPrefs(): Map<String, String> {
        val json = sharedPrefs.getString("daily_journals_json", null) ?: return emptyMap()
        return try {
            val jsonObject = org.json.JSONObject(json)
            val map = mutableMapOf<String, String>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getString(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveJournalsToPrefs(journals: Map<String, String>) {
        val jsonObject = org.json.JSONObject()
        journals.forEach { (k, v) -> jsonObject.put(k, v) }
        sharedPrefs.edit().putString("daily_journals_json", jsonObject.toString()).apply()
    }

    fun setToday() {
        _selectedDateMillis.value = System.currentTimeMillis()
    }

    fun setViewMode(mode: PlannerViewMode) {
        _viewMode.value = mode
        if (mode != PlannerViewMode.WALLET) {
            _isWalletAuthenticated.value = false
        }
    }

    fun saveWalletPinSettings(enabled: Boolean, pin: String) {
        _isWalletPinEnabled.value = enabled
        _walletPin.value = pin
        sharedPrefs.edit().apply {
            putBoolean("wallet_pin_enabled", enabled)
            putString("wallet_pin", pin)
            apply()
        }
    }

    fun authenticateWallet(pin: String): Boolean {
        if (!_isWalletPinEnabled.value || pin == _walletPin.value) {
            _isWalletAuthenticated.value = true
            return true
        }
        return false
    }

    fun lockWallet() {
        _isWalletAuthenticated.value = false
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setCurrency(currency: String) {
        _selectedCurrency.value = currency
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Batch Check Mode Actions
    fun toggleBatchCheckMode() {
        val newMode = !_isBatchCheckMode.value
        _isBatchCheckMode.value = newMode
        if (!newMode) {
            _selectedEventIds.value = emptySet()
        }
    }

    fun toggleEventSelection(eventId: Long) {
        val current = _selectedEventIds.value.toMutableSet()
        if (current.contains(eventId)) {
            current.remove(eventId)
        } else {
            current.add(eventId)
        }
        _selectedEventIds.value = current
    }

    fun markSelectedEventsAsHappened() {
        val selected = _selectedEventIds.value
        if (selected.isEmpty()) return
        SoundUtils.playAccomplishedSound()
        viewModelScope.launch {
            val eventsToUpdate = allEvents.value.filter { it.id in selected }
            eventsToUpdate.forEach { event ->
                repository.updateEvent(event.copy(isHappened = true))
            }
            _selectedEventIds.value = emptySet()
            _isBatchCheckMode.value = false
        }
    }

    fun toggleEventHappened(event: PlannerEvent) {
        viewModelScope.launch {
            val updated = event.copy(isHappened = !event.isHappened)
            if (updated.isHappened) {
                SoundUtils.playAccomplishedSound()
            } else {
                SoundUtils.playClickSound()
            }
            repository.updateEvent(updated)
        }
    }

    // Add / Edit Dialog Controls
    fun openAddSheet(type: PlannerEntryType = PlannerEntryType.EVENT, category: String? = null) {
        _editingEvent.value = null
        _editingTask.value = null
        _addSheetType.value = type
        _preselectedTaskCategory.value = category
        _isAddSheetOpen.value = true
    }

    fun openEditEvent(event: PlannerEvent) {
        _editingEvent.value = event
        _editingTask.value = null
        _preselectedTaskCategory.value = null
        _addSheetType.value = PlannerEntryType.EVENT
        _isAddSheetOpen.value = true
    }

    fun openEditTask(task: PlannerTask) {
        _editingTask.value = task
        _editingEvent.value = null
        _preselectedTaskCategory.value = null
        _addSheetType.value = PlannerEntryType.TASK
        _isAddSheetOpen.value = true
    }

    fun closeAddSheet() {
        _isAddSheetOpen.value = false
        _editingEvent.value = null
        _editingTask.value = null
        _preselectedTaskCategory.value = null
    }

    // Quote of the Day Dialog & Import Controls
    fun openQuoteDialog() {
        _isQuoteDialogOpen.value = true
    }

    fun closeQuoteDialog() {
        _isQuoteDialogOpen.value = false
    }

    fun importQuotesFromText(rawText: String) {
        val parsed = com.example.util.QuoteUtils.parseQuotesFromText(rawText)
        val finalQuotes = if (parsed.isNotEmpty()) parsed else com.example.util.QuoteUtils.DEFAULT_31_QUOTES
        _monthlyQuotes.value = finalQuotes
        com.example.util.QuoteUtils.saveQuotes(getApplication(), finalQuotes)
    }

    fun updateMonthlyQuotes(quotes: List<String>) {
        val finalQuotes = if (quotes.isNotEmpty()) quotes else com.example.util.QuoteUtils.DEFAULT_31_QUOTES
        _monthlyQuotes.value = finalQuotes
        com.example.util.QuoteUtils.saveQuotes(getApplication(), finalQuotes)
    }

    fun resetQuotesToDefault() {
        _monthlyQuotes.value = com.example.util.QuoteUtils.DEFAULT_31_QUOTES
        com.example.util.QuoteUtils.saveQuotes(getApplication(), com.example.util.QuoteUtils.DEFAULT_31_QUOTES)
    }

    // Daily Verse Dialog & Import Controls
    fun openVerseDialog() {
        _isVerseDialogOpen.value = true
    }

    fun closeVerseDialog() {
        _isVerseDialogOpen.value = false
    }

    // Public Speaking Notebook Dialog & Talk Controls
    fun openNotebookDialog() {
        _isNotebookDialogOpen.value = true
    }

    fun closeNotebookDialog() {
        _isNotebookDialogOpen.value = false
    }

    val allTalks: StateFlow<List<com.example.data.model.PublicSpeakingTalk>> = repository.allTalks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun savePublicSpeakingTalk(title: String, content: String, sourceQuote: String = "") {
        viewModelScope.launch {
            repository.insertTalk(
                com.example.data.model.PublicSpeakingTalk(
                    title = title,
                    content = content,
                    sourceQuote = sourceQuote
                )
            )
        }
    }

    fun generateTalkWithAI(
        title: String,
        description: String = "",
        includeInspirationalStory: Boolean = false,
        includeScriptureVerse: Boolean = false,
        includeFunnyStory: Boolean = false,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            val result = com.example.data.api.GeminiVerseService.generateTalkScript(
                title = title,
                description = description,
                includeInspirationalStory = includeInspirationalStory,
                includeScriptureVerse = includeScriptureVerse,
                includeFunnyStory = includeFunnyStory
            )
            result.onSuccess {
                onSuccess(it)
            }
            result.onFailure {
                onFailure(it)
            }
        }
    }

    fun updatePublicSpeakingTalk(talk: com.example.data.model.PublicSpeakingTalk) {
        viewModelScope.launch {
            repository.updateTalk(talk)
        }
    }

    fun deletePublicSpeakingTalk(talk: com.example.data.model.PublicSpeakingTalk) {
        viewModelScope.launch {
            repository.deleteTalk(talk)
        }
    }

    fun deletePublicSpeakingTalkById(id: Long) {
        viewModelScope.launch {
            repository.deleteTalkById(id)
        }
    }

    fun updatePublicSpeakingTalkFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.updateTalkFavorite(id, isFavorite)
        }
    }

    fun importVersesFromText(rawText: String) {
        val parsed = com.example.util.VerseUtils.parseVersesFromText(rawText)
        val finalVerses = if (parsed.isNotEmpty()) parsed else com.example.util.VerseUtils.DEFAULT_31_VERSES
        _monthlyVerses.value = finalVerses
        com.example.util.VerseUtils.saveVerses(getApplication(), finalVerses)
    }

    fun updateMonthlyVerses(verses: List<String>) {
        val finalVerses = if (verses.isNotEmpty()) verses else com.example.util.VerseUtils.DEFAULT_31_VERSES
        _monthlyVerses.value = finalVerses
        com.example.util.VerseUtils.saveVerses(getApplication(), finalVerses)
    }

    fun resetVersesToDefault() {
        _monthlyVerses.value = com.example.util.VerseUtils.DEFAULT_31_VERSES
        com.example.util.VerseUtils.saveVerses(getApplication(), com.example.util.VerseUtils.DEFAULT_31_VERSES)
    }

    // Daily Verse Chat Conversation Persistence
    fun getVerseChatMessages(dateKey: String): Flow<List<com.example.data.model.DailyVerseChatMessage>> =
        repository.getVerseChatMessages(dateKey)

    fun saveVerseChatMessage(dateKey: String, role: String, text: String) {
        viewModelScope.launch {
            repository.insertVerseChatMessage(
                com.example.data.model.DailyVerseChatMessage(
                    dateKey = dateKey,
                    role = role,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun clearVerseChatForDate(dateKey: String) {
        viewModelScope.launch {
            repository.clearVerseChatForDate(dateKey)
        }
    }

    fun deleteVerseChatMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteVerseChatMessageById(id)
        }
    }

    fun updateVerseChatReaction(id: Long, reaction: String?) {
        viewModelScope.launch {
            repository.updateVerseChatReaction(id, reaction)
        }
    }

    // Event DB Actions
    fun moveEvent(event: PlannerEvent, newStartMillis: Long, newEndMillis: Long) {
        viewModelScope.launch {
            repository.updateEvent(
                event.copy(
                    startEpochMillis = newStartMillis,
                    endEpochMillis = newEndMillis
                )
            )
        }
    }

    fun saveEvent(event: PlannerEvent) {
        viewModelScope.launch {
            if (event.id == 0L) {
                val durationMillis = event.endEpochMillis - event.startEpochMillis
                val batchId = if (event.recurrence != "Does not repeat") System.currentTimeMillis() else 0L
                val recurrenceLower = event.recurrence.trim().lowercase()
                when {
                    recurrenceLower.contains("week") -> {
                        // Repeat every week on that selected day for 52 weeks (1 year)
                        for (i in 0 until 52) {
                            val calStart = Calendar.getInstance().apply {
                                timeInMillis = event.startEpochMillis
                                add(Calendar.DAY_OF_YEAR, i * 7)
                            }
                            val calEnd = Calendar.getInstance().apply {
                                timeInMillis = calStart.timeInMillis + durationMillis
                            }
                            val newEvent = event.copy(
                                id = 0L,
                                startEpochMillis = calStart.timeInMillis,
                                endEpochMillis = calEnd.timeInMillis,
                                seriesId = batchId
                            )
                            repository.insertEvent(newEvent)
                        }
                    }
                    recurrenceLower.contains("day") || recurrenceLower.contains("daily") -> {
                        // Repeat every day for 90 days
                        for (i in 0 until 90) {
                            val calStart = Calendar.getInstance().apply {
                                timeInMillis = event.startEpochMillis
                                add(Calendar.DAY_OF_YEAR, i)
                            }
                            val calEnd = Calendar.getInstance().apply {
                                timeInMillis = calStart.timeInMillis + durationMillis
                            }
                            val newEvent = event.copy(
                                id = 0L,
                                startEpochMillis = calStart.timeInMillis,
                                endEpochMillis = calEnd.timeInMillis,
                                seriesId = batchId
                            )
                            repository.insertEvent(newEvent)
                        }
                    }
                    recurrenceLower.contains("month") -> {
                        // Repeat every month for 24 months (2 years)
                        for (i in 0 until 24) {
                            val calStart = Calendar.getInstance().apply {
                                timeInMillis = event.startEpochMillis
                                add(Calendar.MONTH, i)
                            }
                            val calEnd = Calendar.getInstance().apply {
                                timeInMillis = calStart.timeInMillis + durationMillis
                            }
                            val newEvent = event.copy(
                                id = 0L,
                                startEpochMillis = calStart.timeInMillis,
                                endEpochMillis = calEnd.timeInMillis,
                                seriesId = batchId
                            )
                            repository.insertEvent(newEvent)
                        }
                    }
                    else -> {
                        repository.insertEvent(event)
                    }
                }
            } else {
                repository.updateEvent(event)
            }
            closeAddSheet()
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            repository.deleteEventById(eventId)
        }
    }

    fun deleteEventSeries(event: PlannerEvent) {
        viewModelScope.launch {
            repository.deleteEventSeries(event)
            closeAddSheet()
        }
    }

    fun deleteEventsOnDay(dateMillis: Long) {
        viewModelScope.launch {
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = dateMillis
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startMillis = cal.timeInMillis
            val endMillis = startMillis + 24 * 60 * 60 * 1000L
            repository.deleteEventsInRange(startMillis, endMillis)
        }
    }

    // Task DB Actions
    fun saveTask(task: PlannerTask) {
        viewModelScope.launch {
            if (task.id == 0L) {
                repository.insertTask(task)
            } else {
                repository.updateTask(task)
            }
            closeAddSheet()
        }
    }

    fun toggleTaskCompletion(task: PlannerTask) {
        SoundUtils.playClickSound()
        viewModelScope.launch {
            repository.toggleTaskCompletion(task.id, !task.isCompleted)
        }
    }

    fun toggleTaskStar(task: PlannerTask) {
        viewModelScope.launch {
            repository.toggleTaskStar(task.id, !task.isStarred)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTaskById(taskId)
        }
    }

    fun deleteTasksByCategory(category: String) {
        viewModelScope.launch {
            repository.deleteTasksByCategory(category)
        }
    }

    fun renameCategory(oldCategory: String, newCategory: String) {
        viewModelScope.launch {
            repository.renameCategory(oldCategory, newCategory)
        }
    }

    // Focus Timer Actions
    fun setTimerPreset(minutes: Int, modeName: String) {
        pauseTimer()
        _totalSeconds.value = minutes * 60
        _remainingSeconds.value = minutes * 60
        _timerModeName.value = modeName
    }

    fun selectTaskForFocus(task: PlannerTask?) {
        _activeFocusTask.value = task
    }

    fun toggleTimer() {
        if (_isTimerRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        _isTimerRunning.value = true
        ambientSoundGenerator.startSound(_ambientSound.value)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value && _remainingSeconds.value > 0) {
                delay(1000L)
                _remainingSeconds.value -= 1
            }

            if (_remainingSeconds.value == 0) {
                onTimerFinished()
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        timerJob = null
        ambientSoundGenerator.stopSound()
    }

    fun resetTimer() {
        pauseTimer()
        _remainingSeconds.value = _totalSeconds.value
    }

    fun setAmbientSound(type: AmbientSoundType) {
        _ambientSound.value = type
        if (_isTimerRunning.value) {
            ambientSoundGenerator.startSound(type)
        }
    }

    fun setThemeColor(color: String) {
        _themeColor.value = color
        getApplication<Application>().getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().putString("theme_color", color).apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        getApplication<Application>().getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().putString("theme_mode", mode).apply()
    }

    private fun onTimerFinished() {
        pauseTimer()
        val completedSecs = _totalSeconds.value
        val task = _activeFocusTask.value

        viewModelScope.launch {
            repository.insertFocusSession(
                FocusSession(
                    title = task?.title ?: "${_timerModeName.value} Session",
                    durationSeconds = _totalSeconds.value,
                    completedSeconds = completedSecs,
                    category = task?.category ?: "Focus",
                    taskId = task?.id
                )
            )

            // Auto-mark completed task if it was linked
            if (task != null) {
                repository.toggleTaskCompletion(task.id, true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ambientSoundGenerator.stopSound()
    }

    // Wallet Operations
    fun addWalletTransaction(title: String, category: String, amount: Double, isIncome: Boolean, note: String = "", timestampMillis: Long = System.currentTimeMillis(), accountName: String = "Cash") {
        viewModelScope.launch {
            repository.insertWalletTransaction(
                WalletTransaction(
                    title = title,
                    category = category,
                    amount = amount,
                    isIncome = isIncome,
                    timestampMillis = timestampMillis,
                    note = note,
                    accountName = accountName
                )
            )
        }
    }

    fun updateWalletTransaction(transaction: WalletTransaction) {
        viewModelScope.launch {
            repository.insertWalletTransaction(transaction)
        }
    }

    fun deleteWalletTransaction(transaction: WalletTransaction) {
        viewModelScope.launch {
            repository.deleteWalletTransaction(transaction)
        }
    }

    // Account Operations
    fun addWalletAccount(name: String, type: String, initialBalance: Double = 0.0) {
        viewModelScope.launch {
            repository.insertWalletAccount(
                com.example.data.model.WalletAccount(
                    name = name,
                    type = type,
                    initialBalance = initialBalance
                )
            )
        }
    }

    fun deleteWalletAccount(account: com.example.data.model.WalletAccount) {
        viewModelScope.launch {
            repository.deleteWalletAccount(account)
        }
    }

    // Debt Operations
    fun addWalletDebt(personName: String, purpose: String, amount: Double, isLent: Boolean) {
        viewModelScope.launch {
            repository.insertWalletDebt(
                com.example.data.model.WalletDebt(
                    personName = personName,
                    purpose = purpose,
                    amount = amount,
                    isLent = isLent
                )
            )
        }
    }

    fun deleteWalletDebt(debt: com.example.data.model.WalletDebt) {
        viewModelScope.launch {
            repository.deleteWalletDebt(debt)
        }
    }

    fun toggleWalletDebtSettlement(debt: com.example.data.model.WalletDebt) {
        viewModelScope.launch {
            repository.updateWalletDebtSettlement(debt.id, !debt.isSettled)
        }
    }

    // Budget Operations
    fun addWalletBudget(
        category: String,
        limitAmount: Double,
        fromDateMillis: Long = 0L,
        toDateMillis: Long = 0L
    ) {
        viewModelScope.launch {
            repository.insertWalletBudget(
                com.example.data.model.WalletBudget(
                    category = category,
                    limitAmount = limitAmount,
                    fromDateMillis = fromDateMillis,
                    toDateMillis = toDateMillis
                )
            )
        }
    }

    fun updateWalletBudget(budget: com.example.data.model.WalletBudget) {
        viewModelScope.launch {
            repository.insertWalletBudget(budget)
        }
    }

    fun deleteWalletBudget(budget: com.example.data.model.WalletBudget) {
        viewModelScope.launch {
            repository.deleteWalletBudget(budget)
        }
    }

    // Goal Operations
    fun addWalletGoal(name: String, targetAmount: Double, currentAmount: Double = 0.0, deadlineMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertWalletGoal(
                com.example.data.model.WalletGoal(
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    deadlineMillis = deadlineMillis
                )
            )
        }
    }

    fun updateWalletGoal(goal: com.example.data.model.WalletGoal) {
        viewModelScope.launch {
            repository.insertWalletGoal(goal)
        }
    }

    fun deleteWalletGoal(goal: com.example.data.model.WalletGoal) {
        viewModelScope.launch {
            repository.deleteWalletGoal(goal)
        }
    }

    fun addContributionToGoal(goal: com.example.data.model.WalletGoal, contributionAmount: Double) {
        viewModelScope.launch {
            repository.insertWalletGoal(
                goal.copy(currentAmount = goal.currentAmount + contributionAmount)
            )
        }
    }

    // --- My Closet CRUD Methods ---

    fun addClosetItem(
        name: String,
        category: String,
        color: String = "Black",
        season: String = "All Season",
        brand: String = "",
        size: String = "",
        occasion: String = "Casual",
        imagePath: String = "",
        status: String = "In Closet",
        purchasePrice: Double = 0.0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.insertClosetItem(
                ClosetItem(
                    name = name,
                    category = category,
                    color = color,
                    season = season,
                    brand = brand,
                    size = size,
                    occasion = occasion,
                    imagePath = imagePath,
                    status = status,
                    purchasePrice = purchasePrice,
                    notes = notes
                )
            )
        }
    }

    fun updateClosetItem(item: ClosetItem) {
        viewModelScope.launch {
            repository.updateClosetItem(item)
        }
    }

    fun deleteClosetItem(item: ClosetItem) {
        viewModelScope.launch {
            repository.deleteClosetItem(item)
        }
    }

    fun toggleClosetItemFavorite(item: ClosetItem) {
        viewModelScope.launch {
            repository.toggleClosetItemFavorite(item.id, !item.isFavorite)
        }
    }

    fun updateClosetItemStatus(id: Long, status: String) {
        viewModelScope.launch {
            repository.updateClosetItemStatus(id, status)
        }
    }

    fun logClosetItemWorn(id: Long) {
        viewModelScope.launch {
            repository.logClosetItemWorn(id)
        }
    }

    fun addClosetOutfit(
        name: String,
        occasion: String = "Casual",
        itemIds: List<Long> = emptyList(),
        notes: String = "",
        imagePath: String = ""
    ) {
        viewModelScope.launch {
            repository.insertClosetOutfit(
                ClosetOutfit(
                    name = name,
                    occasion = occasion,
                    itemIds = itemIds.joinToString(","),
                    notes = notes,
                    imagePath = imagePath
                )
            )
        }
    }

    fun updateClosetOutfit(outfit: ClosetOutfit) {
        viewModelScope.launch {
            repository.updateClosetOutfit(outfit)
        }
    }

    fun deleteClosetOutfit(outfit: ClosetOutfit) {
        viewModelScope.launch {
            repository.deleteClosetOutfit(outfit)
        }
    }

    fun toggleClosetOutfitFavorite(outfit: ClosetOutfit) {
        viewModelScope.launch {
            repository.toggleClosetOutfitFavorite(outfit.id, !outfit.isFavorite)
        }
    }

    fun logClosetOutfitWorn(id: Long) {
        viewModelScope.launch {
            repository.logClosetOutfitWorn(id)
        }
    }

    // Date Calculation Helpers
    private fun getStartOfDayMillis(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDayMillis(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun exportDataToJson(): String? {
        return try {
            val tasks = allTasks.value
            val events = allEvents.value
            val focusSessions = allFocusSessions.value
            val walletTransactions = allWalletTransactions.value
            val walletAccounts = allWalletAccounts.value
            val walletDebts = allWalletDebts.value
            val walletBudgets = allWalletBudgets.value
            val walletGoals = allWalletGoals.value
            val currency = selectedCurrency.value
            val pinEnabled = isWalletPinEnabled.value
            val walletPin = sharedPrefs.getString("wallet_pin", "")
            
            val tasksListsPrefs = getApplication<Application>().getSharedPreferences("tasks_lists", Context.MODE_PRIVATE)
            val tasksLists = tasksListsPrefs.getStringSet("categories", emptySet())?.toList() ?: emptyList()
            val closetItems = allClosetItems.value
            val closetOutfits = allClosetOutfits.value
            val dailyVerseChats = allVerseChatMessages.value

            val backup = BackupData(
                tasks = tasks,
                events = events,
                focusSessions = focusSessions,
                walletTransactions = walletTransactions,
                walletAccounts = walletAccounts,
                walletDebts = walletDebts,
                walletBudgets = walletBudgets,
                walletGoals = walletGoals,
                currency = currency,
                isWalletPinEnabled = pinEnabled,
                walletPin = walletPin,
                tasksLists = tasksLists,
                dailyMoods = _dailyMoods.value,
                dailyJournals = _dailyJournals.value,
                closetItems = closetItems,
                closetOutfits = closetOutfits,
                dailyVerseChats = dailyVerseChats
            )
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val jsonAdapter = moshi.adapter(BackupData::class.java)
            jsonAdapter.toJson(backup)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importDataFromJson(json: String) {
        viewModelScope.launch {
            try {
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val jsonAdapter = moshi.adapter(BackupData::class.java)
                val backup = jsonAdapter.fromJson(json)

                val existingEvents = repository.allEvents.first()
                val existingTasks = repository.allTasks.first()
                val existingFocusSessions = repository.allFocusSessions.first()
                val existingTransactions = repository.allWalletTransactions.first()
                val existingAccounts = repository.allWalletAccounts.first()
                val existingDebts = repository.allWalletDebts.first()
                val existingBudgets = repository.allWalletBudgets.first()
                val existingGoals = repository.allWalletGoals.first()
                val existingClosetItems = repository.allClosetItems.first()
                val existingClosetOutfits = repository.allClosetOutfits.first()

                backup?.events?.forEach { imported ->
                    val isDuplicate = existingEvents.any { existing ->
                        existing.title.trim().equals(imported.title.trim(), ignoreCase = true) &&
                        existing.startEpochMillis == imported.startEpochMillis &&
                        existing.endEpochMillis == imported.endEpochMillis &&
                        existing.category.trim().equals(imported.category.trim(), ignoreCase = true)
                    }
                    if (!isDuplicate) {
                        repository.insertEvent(imported.copy(id = 0))
                    }
                }

                backup?.tasks?.forEach { imported ->
                    val isDuplicate = existingTasks.any { existing ->
                        existing.title.trim().equals(imported.title.trim(), ignoreCase = true) &&
                        existing.dueDateEpochMillis == imported.dueDateEpochMillis &&
                        existing.priority == imported.priority &&
                        existing.category.trim().equals(imported.category.trim(), ignoreCase = true)
                    }
                    if (!isDuplicate) {
                        repository.insertTask(imported.copy(id = 0))
                    }
                }

                backup?.focusSessions?.forEach { imported ->
                    val isDuplicate = existingFocusSessions.any { existing ->
                        existing.title.trim().equals(imported.title.trim(), ignoreCase = true) &&
                        existing.durationSeconds == imported.durationSeconds &&
                        existing.timestampEpochMillis == imported.timestampEpochMillis
                    }
                    if (!isDuplicate) {
                        repository.insertFocusSession(imported.copy(id = 0))
                    }
                }

                backup?.walletTransactions?.forEach { imported ->
                    val isDuplicate = existingTransactions.any { existing ->
                        existing.title.trim().equals(imported.title.trim(), ignoreCase = true) &&
                        existing.category.trim().equals(imported.category.trim(), ignoreCase = true) &&
                        existing.amount == imported.amount &&
                        existing.isIncome == imported.isIncome &&
                        existing.timestampMillis == imported.timestampMillis
                    }
                    if (!isDuplicate) {
                        repository.insertWalletTransaction(imported.copy(id = 0))
                    }
                }

                backup?.walletAccounts?.forEach { imported ->
                    val isDuplicate = existingAccounts.any { existing ->
                        existing.name.trim().equals(imported.name.trim(), ignoreCase = true)
                    }
                    if (!isDuplicate) {
                        repository.insertWalletAccount(imported.copy(id = 0))
                    }
                }

                backup?.walletDebts?.forEach { imported ->
                    val isDuplicate = existingDebts.any { existing ->
                        existing.personName.trim().equals(imported.personName.trim(), ignoreCase = true) &&
                        existing.amount == imported.amount &&
                        existing.isLent == imported.isLent
                    }
                    if (!isDuplicate) {
                        repository.insertWalletDebt(imported.copy(id = 0))
                    }
                }

                backup?.walletBudgets?.forEach { imported ->
                    val isDuplicate = existingBudgets.any { existing ->
                        existing.category.trim().equals(imported.category.trim(), ignoreCase = true) &&
                        existing.limitAmount == imported.limitAmount
                    }
                    if (!isDuplicate) {
                        repository.insertWalletBudget(imported.copy(id = 0))
                    }
                }

                backup?.walletGoals?.forEach { imported ->
                    val isDuplicate = existingGoals.any { existing ->
                        existing.name.trim().equals(imported.name.trim(), ignoreCase = true) &&
                        existing.targetAmount == imported.targetAmount
                    }
                    if (!isDuplicate) {
                        repository.insertWalletGoal(imported.copy(id = 0))
                    }
                }

                backup?.closetItems?.forEach { imported ->
                    val isDuplicate = existingClosetItems.any { existing ->
                        existing.name.trim().equals(imported.name.trim(), ignoreCase = true) &&
                        existing.category.trim().equals(imported.category.trim(), ignoreCase = true) &&
                        existing.color.trim().equals(imported.color.trim(), ignoreCase = true)
                    }
                    if (!isDuplicate) {
                        repository.insertClosetItem(imported.copy(id = 0))
                    }
                }

                backup?.closetOutfits?.forEach { imported ->
                    val isDuplicate = existingClosetOutfits.any { existing ->
                        existing.name.trim().equals(imported.name.trim(), ignoreCase = true) &&
                        existing.occasion.trim().equals(imported.occasion.trim(), ignoreCase = true)
                    }
                    if (!isDuplicate) {
                        repository.insertClosetOutfit(imported.copy(id = 0))
                    }
                }

                if (backup?.currency != null) {
                    setCurrency(backup.currency)
                }
                if (backup?.isWalletPinEnabled == true) {
                    _isWalletPinEnabled.value = true
                    sharedPrefs.edit().putBoolean("wallet_pin_enabled", true).apply()
                }
                if (backup?.walletPin != null && backup.walletPin.isNotBlank()) {
                    sharedPrefs.edit().putString("wallet_pin", backup.walletPin).apply()
                }
                if (backup?.tasksLists != null && backup.tasksLists.isNotEmpty()) {
                    val tasksListsPrefs = getApplication<Application>().getSharedPreferences("tasks_lists", Context.MODE_PRIVATE)
                    val existingLists = tasksListsPrefs.getStringSet("categories", emptySet()) ?: emptySet()
                    val newLists = existingLists.toMutableSet()
                    newLists.addAll(backup.tasksLists)
                    tasksListsPrefs.edit().putStringSet("categories", newLists).apply()
                }
                if (backup?.dailyMoods != null && backup.dailyMoods.isNotEmpty()) {
                    val mergedMoods = _dailyMoods.value.toMutableMap()
                    mergedMoods.putAll(backup.dailyMoods)
                    _dailyMoods.value = mergedMoods
                    saveMoodsToPrefs(mergedMoods)
                }
                if (backup?.dailyJournals != null && backup.dailyJournals.isNotEmpty()) {
                    val mergedJournals = _dailyJournals.value.toMutableMap()
                    mergedJournals.putAll(backup.dailyJournals)
                    _dailyJournals.value = mergedJournals
                    saveJournalsToPrefs(mergedJournals)
                }
                if (backup?.dailyVerseChats != null && backup.dailyVerseChats.isNotEmpty()) {
                    val existingChats = repository.allVerseChatMessages.first()
                    backup.dailyVerseChats.forEach { imported ->
                        val isDuplicate = existingChats.any { existing ->
                            existing.dateKey == imported.dateKey &&
                            existing.role == imported.role &&
                            existing.text.trim() == imported.text.trim()
                        }
                        if (!isDuplicate) {
                            repository.insertVerseChatMessage(imported.copy(id = 0))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Firebase Sync & Google Sign-In helper methods ---

    fun signInWithGoogleToken(idToken: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(credential).awaitTask()
                onResult(true, "Successfully signed in with Google!")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Authentication failed: ${e.localizedMessage}")
            }
        }
    }

    fun signOut(onResult: (Boolean, String) -> Unit) {
        try {
            firebaseAuth.signOut()
            onResult(true, "Successfully signed out.")
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(false, "Sign out failed: ${e.localizedMessage}")
        }
    }

    fun backupToCloud(onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onResult(false, "Please sign in to back up data.")
            return
        }
        _isCloudSyncing.value = true
        viewModelScope.launch {
            try {
                val userId = user.uid
                val tasks = repository.allTasks.first()
                val events = repository.allEvents.first()
                val focusSessions = repository.allFocusSessions.first()
                val walletTransactions = repository.allWalletTransactions.first()

                val batch = firestore.batch()

                // Save tasks
                tasks.forEach { task ->
                    val ref = firestore.collection("users").document(userId).collection("tasks").document(task.id.toString())
                    batch.set(ref, task)
                }

                // Save events
                events.forEach { event ->
                    val ref = firestore.collection("users").document(userId).collection("events").document(event.id.toString())
                    batch.set(ref, event)
                }

                // Save sessions
                focusSessions.forEach { session ->
                    val ref = firestore.collection("users").document(userId).collection("focus_sessions").document(session.id.toString())
                    batch.set(ref, session)
                }

                // Save transactions
                walletTransactions.forEach { tx ->
                    val ref = firestore.collection("users").document(userId).collection("wallet_transactions").document(tx.id.toString())
                    batch.set(ref, tx)
                }

                // Save metadata
                val metaRef = firestore.collection("users").document(userId)
                batch.set(metaRef, mapOf(
                    "lastBackupTime" to System.currentTimeMillis(),
                    "userEmail" to user.email,
                    "displayName" to user.displayName
                ), com.google.firebase.firestore.SetOptions.merge())

                batch.commit().awaitTask()
                onResult(true, "All data successfully backed up to your Google cloud storage.")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Cloud Backup failed: ${e.localizedMessage}")
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun restoreFromCloud(onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onResult(false, "Please sign in to restore data.")
            return
        }
        _isCloudSyncing.value = true
        viewModelScope.launch {
            try {
                val userId = user.uid

                // Fetch tasks
                val tasksSnap = firestore.collection("users").document(userId).collection("tasks").get().awaitTask()
                val remoteTasks = tasksSnap.documents.mapNotNull { doc ->
                    try {
                        PlannerTask(
                            id = doc.getLong("id") ?: 0L,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            dueDateEpochMillis = doc.getLong("dueDateEpochMillis") ?: 0L,
                            durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 30,
                            isCompleted = doc.getBoolean("isCompleted") ?: false,
                            priority = doc.getString("priority") ?: "MEDIUM",
                            category = doc.getString("category") ?: "Personal",
                            colorHex = doc.getString("colorHex") ?: "#8E24AA",
                            linkedEventId = doc.getLong("linkedEventId"),
                            isStarred = doc.getBoolean("isStarred") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                // Fetch events
                val eventsSnap = firestore.collection("users").document(userId).collection("events").get().awaitTask()
                val remoteEvents = eventsSnap.documents.mapNotNull { doc ->
                    try {
                        PlannerEvent(
                            id = doc.getLong("id") ?: 0L,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            startEpochMillis = doc.getLong("startEpochMillis") ?: 0L,
                            endEpochMillis = doc.getLong("endEpochMillis") ?: 0L,
                            colorHex = doc.getString("colorHex") ?: "#039BE5",
                            location = doc.getString("location") ?: "",
                            category = doc.getString("category") ?: "General",
                            isAllDay = doc.getBoolean("isAllDay") ?: false,
                            recurrence = doc.getString("recurrence") ?: "Does not repeat",
                            isHappened = doc.getBoolean("isHappened") ?: false,
                            seriesId = doc.getLong("seriesId") ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                // Fetch sessions
                val sessionsSnap = firestore.collection("users").document(userId).collection("focus_sessions").get().awaitTask()
                val remoteSessions = sessionsSnap.documents.mapNotNull { doc ->
                    try {
                        FocusSession(
                            id = doc.getLong("id") ?: 0L,
                            title = doc.getString("title") ?: "",
                            durationSeconds = doc.getLong("durationSeconds")?.toInt() ?: 0,
                            completedSeconds = doc.getLong("completedSeconds")?.toInt() ?: 0,
                            timestampEpochMillis = doc.getLong("timestampEpochMillis") ?: System.currentTimeMillis(),
                            isCompleted = doc.getBoolean("isCompleted") ?: true,
                            category = doc.getString("category") ?: "Focus",
                            taskId = doc.getLong("taskId")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                // Fetch transactions
                val txSnap = firestore.collection("users").document(userId).collection("wallet_transactions").get().awaitTask()
                val remoteTransactions = txSnap.documents.mapNotNull { doc ->
                    try {
                        WalletTransaction(
                            id = doc.getLong("id") ?: 0L,
                            title = doc.getString("title") ?: "",
                            category = doc.getString("category") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            isIncome = doc.getBoolean("isIncome") ?: false,
                            timestampMillis = doc.getLong("timestampMillis") ?: System.currentTimeMillis(),
                            note = doc.getString("note") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                if (remoteTasks.isEmpty() && remoteEvents.isEmpty() && remoteSessions.isEmpty() && remoteTransactions.isEmpty()) {
                    onResult(false, "No cloud backup data found for this account.")
                    return@launch
                }

                // Clear and Insert them into Room
                remoteEvents.forEach { repository.insertEvent(it.copy(id = 0)) }
                remoteTasks.forEach { repository.insertTask(it.copy(id = 0)) }
                remoteSessions.forEach { repository.insertFocusSession(it.copy(id = 0)) }
                remoteTransactions.forEach { repository.insertWalletTransaction(it.copy(id = 0)) }

                onResult(true, "Successfully restored all data from your Google cloud storage!")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Cloud Restore failed: ${e.localizedMessage}")
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }
}

suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result, null)
        } else {
            continuation.resumeWith(Result.failure(task.exception ?: RuntimeException("Unknown Task failure")))
        }
    }
}
