package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PublicSpeakingTalk
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicSpeakingTalkDao {
    @Query("SELECT * FROM public_speaking_talks ORDER BY timestamp DESC")
    fun getAllTalks(): Flow<List<PublicSpeakingTalk>>

    @Query("SELECT * FROM public_speaking_talks WHERE id = :id")
    fun getTalkById(id: Long): Flow<PublicSpeakingTalk?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTalk(talk: PublicSpeakingTalk): Long

    @Update
    suspend fun updateTalk(talk: PublicSpeakingTalk)

    @Delete
    suspend fun deleteTalk(talk: PublicSpeakingTalk)

    @Query("DELETE FROM public_speaking_talks WHERE id = :id")
    suspend fun deleteTalkById(id: Long)

    @Query("UPDATE public_speaking_talks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateTalkFavorite(id: Long, isFavorite: Boolean)
}
