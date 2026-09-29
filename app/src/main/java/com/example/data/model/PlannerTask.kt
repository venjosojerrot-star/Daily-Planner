package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "tasks")
data class PlannerTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val dueDateEpochMillis: Long = 0L,
    val durationMinutes: Int = 30,
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val category: String = "Personal", // Work, Personal, Health, Study
    val colorHex: String = "#8E24AA", // Purple by default
    val linkedEventId: Long? = null,
    val isStarred: Boolean = false
)
