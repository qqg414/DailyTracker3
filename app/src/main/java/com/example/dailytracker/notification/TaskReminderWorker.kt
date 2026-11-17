package com.example.dailytracker.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dailytracker.data.local.AppDatabase
import com.example.dailytracker.util.DateUtils
import kotlinx.coroutines.flow.first

class TaskReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.get(applicationContext)
        val taskDao = db.taskDao()

        val todayKey = DateUtils.todayKey()
        val tasks = taskDao.observeTasks().first()
        val logs = taskDao.observeLogs(todayKey).first()

        // 没有任何任务就不提醒
        if (tasks.isEmpty()) return Result.success()

        val doneIds = logs.filter { it.done }.map { it.taskId }.toSet()
        val doneCount = tasks.count { it.id in doneIds }

        // 全部完成就不打扰
        if (doneCount < tasks.size) {
            NotificationHelper.showUnfinished(applicationContext, doneCount, tasks.size)
        }

        return Result.success()
    }
}