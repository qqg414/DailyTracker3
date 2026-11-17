package com.example.dailytracker.data.local.dao

import androidx.room.*
import com.example.dailytracker.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense WHERE dateKey = :dateKey ORDER BY createdAt DESC")
    fun observeByDate(dateKey: String): Flow<List<ExpenseEntity>>

    @Query("SELECT IFNULL(SUM(CASE WHEN isIncome = 0 THEN -amount ELSE amount END), 0.0) FROM expense")
    fun observeNetAmount(): Flow<Double>

    @Query("""
        SELECT IFNULL(SUM(CASE WHEN isIncome = 0 THEN -amount ELSE amount END), 0.0) FROM expense
        WHERE dateKey LIKE :yearMonth || '%'
    """)
    fun observeMonthlyNet(yearMonth: String): Flow<Double>

    @Query("""
        SELECT IFNULL(SUM(CASE WHEN isIncome = 0 THEN -amount ELSE amount END), 0.0) FROM expense
        WHERE dateKey LIKE :year || '%'
    """)
    fun observeYearlyNet(year: String): Flow<Double>

    @Query("SELECT IFNULL(SUM(amount), 0.0) FROM expense WHERE dateKey = :dateKey AND isIncome = 0")
    fun observeDayExpense(dateKey: String): Flow<Double>

    @Query("""
        SELECT dateKey, IFNULL(SUM(CASE WHEN isIncome = 0 THEN -amount ELSE amount END), 0.0) as total
        FROM expense WHERE dateKey LIKE :yearMonth || '%' GROUP BY dateKey
    """)
    fun observeMonthlyDailyExpense(yearMonth: String): Flow<List<DailyExpenseSum>>

    @Query("""
        SELECT substr(dateKey, 1, 7) as monthKey,
               IFNULL(SUM(CASE WHEN isIncome = 0 THEN -amount ELSE amount END), 0.0) as total
        FROM expense WHERE dateKey LIKE :year || '%' GROUP BY monthKey
    """)
    fun observeYearlyMonthlyExpense(year: String): Flow<List<MonthlyExpenseSum>>

    // ★ 新增：查询一段时间内的所有账单（用于统计页）
    @Query("SELECT * FROM expense WHERE dateKey BETWEEN :startDate AND :endDate ORDER BY dateKey ASC, createdAt ASC")
    fun getExpensesBetween(startDate: String, endDate: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense ORDER BY dateKey DESC")
    suspend fun getAllExpensesOnce(): List<ExpenseEntity>

    @Query("DELETE FROM expense")
    suspend fun clearAllExpenses()

    @Insert suspend fun insert(entity: ExpenseEntity): Long
    @Insert suspend fun insertAll(entities: List<ExpenseEntity>)
    @Update suspend fun update(entity: ExpenseEntity)
    @Delete suspend fun delete(entity: ExpenseEntity)
}

data class DailyExpenseSum(val dateKey: String, val total: Double)
data class MonthlyExpenseSum(val monthKey: String, val total: Double)