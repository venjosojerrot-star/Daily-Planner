package com.example.data.db

import androidx.room.*
import com.example.data.model.ClosetItem
import com.example.data.model.ClosetOutfit
import kotlinx.coroutines.flow.Flow

@Dao
interface ClosetDao {
    @Query("SELECT * FROM closet_items ORDER BY id DESC")
    fun getAllItems(): Flow<List<ClosetItem>>

    @Query("SELECT * FROM closet_items WHERE category = :category ORDER BY id DESC")
    fun getItemsByCategory(category: String): Flow<List<ClosetItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ClosetItem): Long

    @Update
    suspend fun updateItem(item: ClosetItem)

    @Delete
    suspend fun deleteItem(item: ClosetItem)

    @Query("DELETE FROM closet_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("UPDATE closet_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateItemFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE closet_items SET status = :status WHERE id = :id")
    suspend fun updateItemStatus(id: Long, status: String)

    @Query("UPDATE closet_items SET timesWorn = timesWorn + 1, lastWornMillis = :wornMillis WHERE id = :id")
    suspend fun logItemWorn(id: Long, wornMillis: Long = System.currentTimeMillis())

    // Outfits
    @Query("SELECT * FROM closet_outfits ORDER BY id DESC")
    fun getAllOutfits(): Flow<List<ClosetOutfit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfit(outfit: ClosetOutfit): Long

    @Update
    suspend fun updateOutfit(outfit: ClosetOutfit)

    @Delete
    suspend fun deleteOutfit(outfit: ClosetOutfit)

    @Query("DELETE FROM closet_outfits WHERE id = :id")
    suspend fun deleteOutfitById(id: Long)

    @Query("UPDATE closet_outfits SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateOutfitFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE closet_outfits SET timesWorn = timesWorn + 1, lastWornMillis = :wornMillis WHERE id = :id")
    suspend fun logOutfitWorn(id: Long, wornMillis: Long = System.currentTimeMillis())
}
