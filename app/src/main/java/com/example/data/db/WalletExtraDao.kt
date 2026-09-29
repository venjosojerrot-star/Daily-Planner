package com.example.data.db

import androidx.room.*
import com.example.data.model.WalletBudget
import com.example.data.model.WalletDebt
import com.example.data.model.WalletGoal
import com.example.data.model.WalletAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletExtraDao {
    // Debts
    @Query("SELECT * FROM wallet_debts ORDER BY timestampMillis DESC")
    fun getAllDebts(): Flow<List<WalletDebt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: WalletDebt): Long

    @Delete
    suspend fun deleteDebt(debt: WalletDebt)

    @Query("UPDATE wallet_debts SET isSettled = :isSettled WHERE id = :id")
    suspend fun updateDebtSettlement(id: Long, isSettled: Boolean)

    // Budgets
    @Query("SELECT * FROM wallet_budgets")
    fun getAllBudgets(): Flow<List<WalletBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: WalletBudget): Long

    @Delete
    suspend fun deleteBudget(budget: WalletBudget)

    // Goals
    @Query("SELECT * FROM wallet_goals ORDER BY deadlineMillis ASC")
    fun getAllGoals(): Flow<List<WalletGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: WalletGoal): Long

    @Delete
    suspend fun deleteGoal(goal: WalletGoal)

    // Accounts
    @Query("SELECT * FROM wallet_accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<WalletAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: WalletAccount): Long

    @Delete
    suspend fun deleteAccount(account: WalletAccount)
}
