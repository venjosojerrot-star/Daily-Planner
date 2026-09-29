package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "closet_items")
data class ClosetItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String, // Tops, Bottoms, Dresses, Outerwear, Shoes, Bags & Accessories, Activewear, Sleepwear
    val color: String = "Black",
    val season: String = "All Season", // All Season, Summer, Winter, Spring/Fall
    val brand: String = "",
    val size: String = "",
    val occasion: String = "Casual", // Casual, Work/Formal, Party, Sport, Lounge
    val imagePath: String = "", // File path or content URI
    val status: String = "In Closet", // In Closet, In Laundry, Dry Clean, Borrowed, Stored
    val isFavorite: Boolean = false,
    val purchasePrice: Double = 0.0,
    val timesWorn: Int = 0,
    val lastWornMillis: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
