package com.example.dailytracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense",
    indices = [Index("dateKey")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 金额，恒为正数；收入/支出由 isIncome 决定 */
    val amount: Double,
    /** false = 支出，true = 收入 */
    val isIncome: Boolean = false,
    val category: String = "其他",
    val note: String = "",
    /** yyyy-MM-dd，按天聚合用 */
    val dateKey: String,
    val createdAt: Long = System.currentTimeMillis()
)