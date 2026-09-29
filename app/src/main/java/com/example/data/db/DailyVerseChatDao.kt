package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyVerseChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyVerseChatDao {
    @Query("SELECT * FROM daily_verse_chat_messages WHERE dateKey = :dateKey ORDER BY timestamp ASC, id ASC")
    fun getMessagesForDate(dateKey: String): Flow<List<DailyVerseChatMessage>>

    @Query("SELECT * FROM daily_verse_chat_messages ORDER BY timestamp ASC, id ASC")
    fun getAllMessages(): Flow<List<DailyVerseChatMessage>>

    @Query("SELECT * FROM daily_verse_chat_messages WHERE dateKey = :dateKey ORDER BY timestamp ASC, id ASC")
    suspend fun getMessagesForDateSync(dateKey: String): List<DailyVerseChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DailyVerseChatMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<DailyVerseChatMessage>)

    @Query("DELETE FROM daily_verse_chat_messages WHERE dateKey = :dateKey")
    suspend fun clearMessagesForDate(dateKey: String)

    @Query("DELETE FROM daily_verse_chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("UPDATE daily_verse_chat_messages SET reaction = :reaction WHERE id = :id")
    suspend fun updateReaction(id: Long, reaction: String?)
}
