package com.example.dailytracker.ui.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dailytracker.ui.MainViewModel
import com.example.dailytracker.util.DateUtils
import kotlin.math.roundToInt

@Composable
fun TodayScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val todayKey by vm.todayKey.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val logs by vm.todayLogs.collectAsStateWithLifecycle()

    val doneIds = remember(logs) { logs.filter { it.done }.map { it.taskId }.toSet() }
    val doneCount = tasks.count { it.id in doneIds }
    val progress = if (tasks.isEmpty()) 0f else doneCount.toFloat() / tasks.size

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ProgressHeader(
                dateText = DateUtils.pretty(todayKey),
                done = doneCount,
                total = tasks.size,
                progress = progress
            )
        }
        item { Text("今日任务", style = MaterialTheme.typography.titleMedium) }

        if (tasks.isEmpty()) {
            item {
                Text(
                    "还没有任务，去「设置 → 任务管理」添加吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                TaskRow(
                    title = task.title,
                    checked = task.id in doneIds,
                    onToggle = { vm.toggleTask(task.id) }
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "打卡记录会在每天 0 点自动切换到新的一天，历史记录会保留。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProgressHeader(dateText: String, done: Int, total: Int, progress: Float) {
    val animated by animateFloatAsState(targetValue = progress, label = "progress")
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animated },
                    modifier = Modifier.size(84.dp),
                    strokeWidth = 8.dp
                )
                Text(
                    "${(animated * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(dateText, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text("已完成 $done / $total", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (total > 0 && done == total) "今天全部完成，太棒了！" else "加油，继续保持～",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TaskRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    Card(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (checked) TextDecoration.LineThrough else null,
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )
        }
    }
}