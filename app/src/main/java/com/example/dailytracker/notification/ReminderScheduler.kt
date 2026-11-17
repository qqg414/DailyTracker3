package com.example.dailytracker.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val WORK_NAME = "task_reminder_work"

    /**
     * 每隔 1 小时检查一次未完成任务。
     * WorkManager 最小周期是 15 分钟。
     */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<TaskReminderWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}