package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val durationSeconds: Int,
    val completedSeconds: Int,
    val timestampEpochMillis: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = true,
    val category: String = "Focus",
    val taskId: Long? = null
)
