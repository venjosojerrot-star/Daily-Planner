package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "events")
data class PlannerEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val startEpochMillis: Long = 0L,
    val endEpochMillis: Long = 0L,
    val colorHex: String = "#039BE5", // Peacock Blue by default
    val location: String = "",
    val category: String = "General", // Work, Personal, Health, Study, Meeting
    val isAllDay: Boolean = false,
    val recurrence: String = "Does not repeat",
    val isHappened: Boolean = false,
    val seriesId: Long = 0L
)
