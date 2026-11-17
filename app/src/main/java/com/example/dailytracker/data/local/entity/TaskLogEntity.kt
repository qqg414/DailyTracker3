package com.example.dailytracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 打卡记录。
 * 主键是 (taskId, dateKey)，天然保证「一个任务一天只有一条记录」。
 * 跨天时不需要清理数据：新的一天 dateKey 变了，查询结果自然是空的（未打卡）。
 */
@Entity(
    tableName = "task_log",
    primaryKeys = ["taskId", "dateKey"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dateKey")]
)
data class TaskLogEntity(
    val taskId: Long,
    val dateKey: String,
    val done: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)