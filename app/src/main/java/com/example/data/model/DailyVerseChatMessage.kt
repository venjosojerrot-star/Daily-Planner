package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * Entity to persist Daily Verse chat conversations on a per-day basis.
 */
@JsonClass(generateAdapter = true)
@Entity(tableName = "daily_verse_chat_messages")
data class DailyVerseChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String, // e.g. "2026-09-20"
    val role: String, // "user" or "model"
    val text: String,
    val reaction: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
