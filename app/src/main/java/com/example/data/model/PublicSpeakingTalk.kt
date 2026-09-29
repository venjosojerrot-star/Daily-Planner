package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * Entity to persist Public Speaking Talks inside the Notebook.
 */
@JsonClass(generateAdapter = true)
@Entity(tableName = "public_speaking_talks")
data class PublicSpeakingTalk(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val sourceQuote: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
