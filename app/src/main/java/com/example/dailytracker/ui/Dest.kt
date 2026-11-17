package com.example.dailytracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PieChart // 如果没这个图标，可以用 Icons.Filled.List 代替
import androidx.compose.ui.graphics.vector.ImageVector

enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Today("today", "今日打卡", Icons.Filled.DateRange),
    Ledger("ledger", "账本", Icons.Filled.Edit),
    Stats("stats", "统计", Icons.Outlined.PieChart), // ★ 新增
    Settings("settings", "设置", Icons.Filled.Settings)
}