package com.example.data.db

import androidx.room.*
import com.example.data.model.WalletTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet_transactions ORDER BY timestampMillis DESC")
    fun getAllTransactions(): Flow<List<WalletTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WalletTransaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: WalletTransaction)

    @Query("DELETE FROM wallet_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM wallet_transactions")
    suspend fun deleteAllTransactions()
}
