package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "wallet_debts")
data class WalletDebt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val purpose: String,
    val amount: Double,
    val isLent: Boolean, // true for "I lent", false for "I borrowed"
    val timestampMillis: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false
)
