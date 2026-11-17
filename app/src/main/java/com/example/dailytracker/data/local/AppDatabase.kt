package com.example.dailytracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dailytracker.data.local.dao.ExpenseDao
import com.example.dailytracker.data.local.dao.TaskDao
import com.example.dailytracker.data.local.entity.ExpenseEntity
import com.example.dailytracker.data.local.entity.TaskEntity
import com.example.dailytracker.data.local.entity.TaskLogEntity

@Database(
    entities = [ExpenseEntity::class, TaskEntity::class, TaskLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_tracker.db"
                )
                    // 注意：正式发布前请改成 addMigrations(...)，否则升级会清空数据
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}