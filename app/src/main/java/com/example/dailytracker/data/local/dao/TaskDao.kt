package com.example.dailytracker.data.local.dao

import androidx.room.*
import com.example.dailytracker.data.local.entity.TaskEntity
import com.example.dailytracker.data.local.entity.TaskLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ---------- 任务 ----------
    @Query("SELECT * FROM task WHERE enabled = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    // ---------- 打卡 ----------
    @Query("SELECT * FROM task_log WHERE dateKey = :dateKey")
    fun observeLogs(dateKey: String): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_log WHERE taskId = :taskId AND dateKey = :dateKey LIMIT 1")
    suspend fun getLog(taskId: Long, dateKey: String): TaskLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: TaskLogEntity)

    @Query("DELETE FROM task_log WHERE taskId = :taskId AND dateKey = :dateKey")
    suspend fun deleteLog(taskId: Long, dateKey: String)
}