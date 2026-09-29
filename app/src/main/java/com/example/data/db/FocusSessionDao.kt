package com.example.data.db

import androidx.room.*
import com.example.data.model.FocusSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY timestampEpochMillis DESC")
    fun getAllFocusSessions(): Flow<List<FocusSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession): Long

    @Query("SELECT SUM(completedSeconds) FROM focus_sessions WHERE timestampEpochMillis >= :startOfDayMillis")
    fun getFocusTimeTodaySeconds(startOfDayMillis: Long): Flow<Int?>

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
