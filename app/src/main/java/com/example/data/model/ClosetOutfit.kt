package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "closet_outfits")
data class ClosetOutfit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val occasion: String = "Casual",
    val itemIds: String = "", // Comma separated item IDs e.g. "1,4,7"
    val isFavorite: Boolean = false,
    val imagePath: String = "",
    val notes: String = "",
    val timesWorn: Int = 0,
    val lastWornMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
