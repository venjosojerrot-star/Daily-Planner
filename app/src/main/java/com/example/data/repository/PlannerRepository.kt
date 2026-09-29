package com.example.data.repository

import com.example.data.db.ClosetDao
import com.example.data.db.DailyVerseChatDao
import com.example.data.db.EventDao
import com.example.data.db.PublicSpeakingTalkDao
import com.example.data.model.PublicSpeakingTalk
import com.example.data.db.FocusSessionDao
import com.example.data.db.TaskDao
import com.example.data.db.WalletDao
import com.example.data.db.WalletExtraDao
import com.example.data.model.ClosetItem
import com.example.data.model.ClosetOutfit
import com.example.data.model.DailyVerseChatMessage
import com.example.data.model.FocusSession
import com.example.data.model.PlannerEvent
import com.example.data.model.PlannerTask
import com.example.data.model.WalletAccount
import com.example.data.model.WalletBudget
import com.example.data.model.WalletDebt
import com.example.data.model.WalletGoal
import com.example.data.model.WalletTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class PlannerRepository(
    private val eventDao: EventDao,
    private val taskDao: TaskDao,
    private val focusSessionDao: FocusSessionDao,
    private val walletDao: WalletDao,
    private val walletExtraDao: WalletExtraDao,
    private val closetDao: ClosetDao,
    private val dailyVerseChatDao: DailyVerseChatDao,
    private val publicSpeakingTalkDao: PublicSpeakingTalkDao
) {
    val allEvents: Flow<List<PlannerEvent>> = eventDao.getAllEvents()
    val allTasks: Flow<List<PlannerTask>> = taskDao.getAllTasks()
    val allFocusSessions: Flow<List<FocusSession>> = focusSessionDao.getAllFocusSessions()
    val allWalletTransactions: Flow<List<WalletTransaction>> = walletDao.getAllTransactions()
    val allWalletDebts: Flow<List<WalletDebt>> = walletExtraDao.getAllDebts()
    val allWalletBudgets: Flow<List<WalletBudget>> = walletExtraDao.getAllBudgets()
    val allWalletGoals: Flow<List<WalletGoal>> = walletExtraDao.getAllGoals()
    val allWalletAccounts: Flow<List<WalletAccount>> = walletExtraDao.getAllAccounts()
    val allClosetItems: Flow<List<ClosetItem>> = closetDao.getAllItems()
    val allClosetOutfits: Flow<List<ClosetOutfit>> = closetDao.getAllOutfits()
    val allVerseChatMessages: Flow<List<DailyVerseChatMessage>> = dailyVerseChatDao.getAllMessages()
    val allTalks: Flow<List<PublicSpeakingTalk>> = publicSpeakingTalkDao.getAllTalks()

    fun getTalkById(id: Long): Flow<PublicSpeakingTalk?> = publicSpeakingTalkDao.getTalkById(id)
    suspend fun insertTalk(talk: PublicSpeakingTalk): Long = publicSpeakingTalkDao.insertTalk(talk)
    suspend fun updateTalk(talk: PublicSpeakingTalk) = publicSpeakingTalkDao.updateTalk(talk)
    suspend fun deleteTalk(talk: PublicSpeakingTalk) = publicSpeakingTalkDao.deleteTalk(talk)
    suspend fun deleteTalkById(id: Long) = publicSpeakingTalkDao.deleteTalkById(id)
    suspend fun updateTalkFavorite(id: Long, isFavorite: Boolean) = publicSpeakingTalkDao.updateTalkFavorite(id, isFavorite)

    fun getVerseChatMessages(dateKey: String): Flow<List<DailyVerseChatMessage>> =
        dailyVerseChatDao.getMessagesForDate(dateKey)

    suspend fun getVerseChatMessagesSync(dateKey: String): List<DailyVerseChatMessage> =
        dailyVerseChatDao.getMessagesForDateSync(dateKey)

    suspend fun insertVerseChatMessage(message: DailyVerseChatMessage): Long =
        dailyVerseChatDao.insertMessage(message)

    suspend fun insertVerseChatMessages(messages: List<DailyVerseChatMessage>) =
        dailyVerseChatDao.insertMessages(messages)

    suspend fun clearVerseChatForDate(dateKey: String) =
        dailyVerseChatDao.clearMessagesForDate(dateKey)

    suspend fun deleteVerseChatMessageById(id: Long) =
        dailyVerseChatDao.deleteMessageById(id)

    suspend fun updateVerseChatReaction(id: Long, reaction: String?) =
        dailyVerseChatDao.updateReaction(id, reaction)

    fun getEventsInRange(startMillis: Long, endMillis: Long): Flow<List<PlannerEvent>> {
        return eventDao.getEventsInRange(startMillis, endMillis)
    }

    fun getTasksInRange(startMillis: Long, endMillis: Long): Flow<List<PlannerTask>> {
        return taskDao.getTasksInRange(startMillis, endMillis)
    }

    suspend fun insertEvent(event: PlannerEvent): Long = eventDao.insertEvent(event)
    suspend fun updateEvent(event: PlannerEvent) = eventDao.updateEvent(event)
    suspend fun deleteEvent(event: PlannerEvent) = eventDao.deleteEvent(event)
    suspend fun deleteEventById(id: Long) = eventDao.deleteEventById(id)
    suspend fun deleteEventsInRange(startMillis: Long, endMillis: Long) = eventDao.deleteEventsInRange(startMillis, endMillis)
    suspend fun deleteEventSeries(event: PlannerEvent) {
        if (event.seriesId != 0L) {
            eventDao.deleteEventsBySeriesId(event.seriesId)
        } else {
            eventDao.deleteEventsByTitleAndRecurrence(event.title, event.recurrence)
        }
    }

    suspend fun insertTask(task: PlannerTask): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: PlannerTask) = taskDao.updateTask(task)
    suspend fun deleteTask(task: PlannerTask) = taskDao.deleteTask(task)
    suspend fun toggleTaskCompletion(id: Long, isCompleted: Boolean) = taskDao.updateTaskCompletion(id, isCompleted)
    suspend fun toggleTaskStar(id: Long, isStarred: Boolean) = taskDao.updateTaskStar(id, isStarred)
    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)
    suspend fun deleteTasksByCategory(category: String) = taskDao.deleteTasksByCategory(category)
    suspend fun renameCategory(oldCategory: String, newCategory: String) = taskDao.renameCategory(oldCategory, newCategory)

    suspend fun insertFocusSession(session: FocusSession): Long = focusSessionDao.insertSession(session)
    fun getFocusTimeTodaySeconds(startOfDayMillis: Long): Flow<Int?> = focusSessionDao.getFocusTimeTodaySeconds(startOfDayMillis)

    suspend fun insertWalletTransaction(transaction: WalletTransaction): Long = walletDao.insertTransaction(transaction)
    suspend fun deleteWalletTransaction(transaction: WalletTransaction) = walletDao.deleteTransaction(transaction)
    suspend fun deleteWalletTransactionById(id: Long) = walletDao.deleteTransactionById(id)
    suspend fun clearAllWalletTransactions() = walletDao.deleteAllTransactions()

    // Extra Wallet CRUDs
    suspend fun insertWalletDebt(debt: WalletDebt) = walletExtraDao.insertDebt(debt)
    suspend fun deleteWalletDebt(debt: WalletDebt) = walletExtraDao.deleteDebt(debt)
    suspend fun updateWalletDebtSettlement(id: Long, isSettled: Boolean) = walletExtraDao.updateDebtSettlement(id, isSettled)

    suspend fun insertWalletBudget(budget: WalletBudget) = walletExtraDao.insertBudget(budget)
    suspend fun deleteWalletBudget(budget: WalletBudget) = walletExtraDao.deleteBudget(budget)

    suspend fun insertWalletGoal(goal: WalletGoal) = walletExtraDao.insertGoal(goal)
    suspend fun deleteWalletGoal(goal: WalletGoal) = walletExtraDao.deleteGoal(goal)

    suspend fun insertWalletAccount(account: WalletAccount) = walletExtraDao.insertAccount(account)
    suspend fun deleteWalletAccount(account: WalletAccount) = walletExtraDao.deleteAccount(account)

    // Closet CRUDs
    suspend fun insertClosetItem(item: ClosetItem): Long = closetDao.insertItem(item)
    suspend fun updateClosetItem(item: ClosetItem) = closetDao.updateItem(item)
    suspend fun deleteClosetItem(item: ClosetItem) = closetDao.deleteItem(item)
    suspend fun deleteClosetItemById(id: Long) = closetDao.deleteItemById(id)
    suspend fun toggleClosetItemFavorite(id: Long, isFavorite: Boolean) = closetDao.updateItemFavorite(id, isFavorite)
    suspend fun updateClosetItemStatus(id: Long, status: String) = closetDao.updateItemStatus(id, status)
    suspend fun logClosetItemWorn(id: Long) = closetDao.logItemWorn(id)

    suspend fun insertClosetOutfit(outfit: ClosetOutfit): Long = closetDao.insertOutfit(outfit)
    suspend fun updateClosetOutfit(outfit: ClosetOutfit) = closetDao.updateOutfit(outfit)
    suspend fun deleteClosetOutfit(outfit: ClosetOutfit) = closetDao.deleteOutfit(outfit)
    suspend fun deleteClosetOutfitById(id: Long) = closetDao.deleteOutfitById(id)
    suspend fun toggleClosetOutfitFavorite(id: Long, isFavorite: Boolean) = closetDao.updateOutfitFavorite(id, isFavorite)
    suspend fun logClosetOutfitWorn(id: Long) = closetDao.logOutfitWorn(id)

    suspend fun seedInitialDataIfEmpty() {
        // Preset recent transactions removed as requested
    }
}
