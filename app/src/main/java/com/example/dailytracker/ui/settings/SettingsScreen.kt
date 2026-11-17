package com.example.dailytracker.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dailytracker.ui.MainViewModel
import java.time.LocalDate

enum class SettingsPage { MAIN, DATA, CATEGORY }

@Composable
fun SettingsScreen(vm: MainViewModel) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }

    BackHandler(enabled = currentPage != SettingsPage.MAIN) {
        currentPage = SettingsPage.MAIN
    }

    when (currentPage) {
        SettingsPage.MAIN -> SettingsMainContent(
            onDataManageClick = { currentPage = SettingsPage.DATA },
            onCategoryManageClick = { currentPage = SettingsPage.CATEGORY }
        )
        SettingsPage.DATA -> DataManageScreen(vm, onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.CATEGORY -> CategoryManagerScreen(vm, onBack = { currentPage = SettingsPage.MAIN })
    }
}

@Composable
fun SettingsMainContent(
    onDataManageClick: () -> Unit,
    onCategoryManageClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SettingsCard(title = "数据管理", subtitle = "导入/导出账本数据及查看导出记录", onClick = onDataManageClick) }
        item { SettingsCard(title = "分类预设管理", subtitle = "管理记账的支出与收入分类", onClick = onCategoryManageClick) }
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(">", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManageScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val exportHistory by vm.exportHistory.collectAsStateWithLifecycle()

    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val todayDateStr = remember { LocalDate.now().toString().replace("-", "") }
    val defaultFileName = "DailyTracker_Backup_${todayDateStr}.csv"

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            vm.exportData(uri) { success ->
                Toast.makeText(context, if (success) "导出成功" else "导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            vm.checkHasData { hasData ->
                if (hasData) { pendingImportUri = uri; showImportConfirm = true }
                else {
                    vm.importData(uri) { success -> Toast.makeText(context, if (success) "导入成功" else "导入失败", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("数据管理") }, navigationIcon = { IconButton(onClick = onBack) { Text("<", style = MaterialTheme.typography.titleLarge) } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(onClick = { exportLauncher.launch(defaultFileName) }, modifier = Modifier.fillMaxWidth()) { Text("导出账本数据 (CSV)") }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { importLauncher.launch("text/*") }, modifier = Modifier.fillMaxWidth()) { Text("导入账本数据 (CSV)") }
            Spacer(modifier = Modifier.height(24.dp))
            Text("最近导出记录 (最多10次)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (exportHistory.isEmpty()) {
                Text("暂无导出记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(exportHistory) { time ->
                        Card(modifier = Modifier.fillMaxWidth()) { Text(time, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("覆盖警告") },
            text = { Text("检测到当前应用内已有数据。\n\n导入操作将**完全覆盖**您原来的所有账本数据和分类预设！\n\n确定要继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    pendingImportUri?.let { uri ->
                        vm.importData(uri) { success -> Toast.makeText(context, if (success) "覆盖导入成功" else "导入失败", Toast.LENGTH_SHORT).show() }
                    }
                    pendingImportUri = null
                }) { Text("确定覆盖", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showImportConfirm = false; pendingImportUri = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(vm: MainViewModel, onBack: () -> Unit) {
    var isIncomeTab by remember { mutableStateOf(false) }
    val categories by (if (isIncomeTab) vm.incomeCategories else vm.expenseCategories).collectAsStateWithLifecycle()
    var newCategoryText by remember { mutableStateOf("") }
    var pendingDeleteName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("分类管理") }, navigationIcon = { IconButton(onClick = onBack) { Text("<", style = MaterialTheme.typography.titleLarge) } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            TabRow(selectedTabIndex = if (isIncomeTab) 1 else 0) {
                Tab(selected = !isIncomeTab, onClick = { isIncomeTab = false }, text = { Text("支出分类") })
                Tab(selected = isIncomeTab, onClick = { isIncomeTab = true }, text = { Text("收入分类") })
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCategoryText,
                    onValueChange = { newCategoryText = it },
                    label = { Text("新分类名称") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (newCategoryText.isNotBlank()) {
                        vm.addCategory(isIncomeTab, newCategoryText)
                        newCategoryText = ""
                    }
                }) { Text("添加") }
            }
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(categories) { index, name ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                        Row {
                            IconButton(onClick = { vm.moveCategory(isIncomeTab, index, -1) }, enabled = index > 0) { Text("↑", style = MaterialTheme.typography.titleLarge) }
                            IconButton(onClick = { vm.moveCategory(isIncomeTab, index, 1) }, enabled = index < categories.size - 1) { Text("↓", style = MaterialTheme.typography.titleLarge) }
                            IconButton(onClick = { pendingDeleteName = name }) { Text("×", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }

    if (pendingDeleteName != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteName = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除分类“${pendingDeleteName}”吗？\n(该分类下的历史账单不会受到影响)") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteName?.let { vm.deleteCategory(isIncomeTab, it) }
                    pendingDeleteName = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteName = null }) { Text("取消") } }
        )
    }
}