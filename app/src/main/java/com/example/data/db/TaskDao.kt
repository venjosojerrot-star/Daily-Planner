package com.example.data.db

import androidx.room.*
import com.example.data.model.PlannerTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, id ASC")
    fun getAllTasks(): Flow<List<PlannerTask>>

    @Query("SELECT * FROM tasks WHERE dueDateEpochMillis >= :startMillis AND dueDateEpochMillis < :endMillis ORDER BY isCompleted ASC, id ASC")
    fun getTasksInRange(startMillis: Long, endMillis: Long): Flow<List<PlannerTask>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): PlannerTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PlannerTask): Long

    @Update
    suspend fun updateTask(task: PlannerTask)

    @Delete
    suspend fun deleteTask(task: PlannerTask)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskCompletion(id: Long, isCompleted: Boolean)

    @Query("UPDATE tasks SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateTaskStar(id: Long, isStarred: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks WHERE LOWER(category) = LOWER(:category)")
    suspend fun deleteTasksByCategory(category: String)

    @Query("UPDATE tasks SET category = :newCategory WHERE LOWER(category) = LOWER(:oldCategory)")
    suspend fun renameCategory(oldCategory: String, newCategory: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
