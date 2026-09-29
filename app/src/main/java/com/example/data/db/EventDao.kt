package com.example.data.db

import androidx.room.*
import com.example.data.model.PlannerEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startEpochMillis ASC")
    fun getAllEvents(): Flow<List<PlannerEvent>>

    @Query("SELECT * FROM events WHERE startEpochMillis >= :startMillis AND startEpochMillis < :endMillis ORDER BY startEpochMillis ASC")
    fun getEventsInRange(startMillis: Long, endMillis: Long): Flow<List<PlannerEvent>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Long): PlannerEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PlannerEvent): Long

    @Update
    suspend fun updateEvent(event: PlannerEvent)

    @Delete
    suspend fun deleteEvent(event: PlannerEvent)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM events WHERE startEpochMillis >= :startMillis AND startEpochMillis < :endMillis")
    suspend fun deleteEventsInRange(startMillis: Long, endMillis: Long)

    @Query("DELETE FROM events WHERE seriesId = :seriesId")
    suspend fun deleteEventsBySeriesId(seriesId: Long)

    @Query("DELETE FROM events WHERE title = :title AND recurrence = :recurrence")
    suspend fun deleteEventsByTitleAndRecurrence(title: String, recurrence: String)

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()
}
