package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupData(
    val tasks: List<PlannerTask>? = emptyList(),
    val events: List<PlannerEvent>? = emptyList(),
    val focusSessions: List<FocusSession>? = emptyList(),
    val walletTransactions: List<WalletTransaction>? = emptyList(),
    val walletAccounts: List<WalletAccount>? = emptyList(),
    val walletDebts: List<WalletDebt>? = emptyList(),
    val walletBudgets: List<WalletBudget>? = emptyList(),
    val walletGoals: List<WalletGoal>? = emptyList(),
    val currency: String? = "₱ PHP",
    val isWalletPinEnabled: Boolean? = false,
    val walletPin: String? = "",
    val tasksLists: List<String>? = emptyList(),
    val dailyMoods: Map<String, String>? = emptyMap(),
    val dailyJournals: Map<String, String>? = emptyMap(),
    val closetItems: List<ClosetItem>? = emptyList(),
    val closetOutfits: List<ClosetOutfit>? = emptyList(),
    val dailyVerseChats: List<DailyVerseChatMessage>? = emptyList()
)
