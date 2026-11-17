package com.example.dailytracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.dailytracker.notification.NotificationHelper
import com.example.dailytracker.notification.ReminderScheduler
import com.example.dailytracker.ui.AppRoot
import com.example.dailytracker.ui.theme.DailyTrackerTheme

class MainActivity : ComponentActivity() {

    // 通知权限请求器（自动申请）
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 允许或拒绝都继续运行，不阻塞 App */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. 创建通知渠道（Android 8.0+ 必须）
        NotificationHelper.createChannel(this)

        // 2. 自动申请通知权限（Android 13+ 才需要）
        ensureNotificationPermission()

        // 3. 启动后台提醒（每小时检查一次未完成任务）
        ReminderScheduler.schedule(this)

        setContent {
            DailyTrackerTheme {
                AppRoot()
            }
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}