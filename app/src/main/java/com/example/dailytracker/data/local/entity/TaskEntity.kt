package com.example.dailytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val sortOrder: Int = 0,
    /** 停用的任务不在打卡页显示，但历史记录保留 */
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)