package com.example.dailytracker.ui.ledger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dailytracker.data.local.entity.ExpenseEntity
import com.example.dailytracker.ui.MainViewModel
import com.example.dailytracker.util.formatMoney
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

private val Green = Color(0xFF2E7D32)

@Composable
fun LedgerScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val monthBal by vm.monthlyBalance.collectAsStateWithLifecycle()
    val yearBal by vm.yearlyBalance.collectAsStateWithLifecycle()
    val mode by vm.viewMode.collectAsStateWithLifecycle()
    val year by vm.currentYear.collectAsStateWithLifecycle()
    val month by vm.currentMonth.collectAsStateWithLifecycle()
    val dailyMap by vm.monthlyDailyExpense.collectAsStateWithLifecycle()
    val monthlyMap by vm.yearlyMonthlyExpense.collectAsStateWithLifecycle()
    val items by vm.ledgerItems.collectAsStateWithLifecycle()

    // ★ 获取分类预设数据
    val expenseCats by vm.expenseCategories.collectAsStateWithLifecycle()
    val incomeCats by vm.incomeCategories.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var detailDate by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<ExpenseEntity?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }

    // ★ 核心修复：记录你点的是哪一天，添加时就存哪一天
    var addDate by remember { mutableStateOf<String?>(null) }

    val isMonth = mode == MainViewModel.LedgerViewMode.MONTH
    val title = if (isMonth) "${year}年${month}月" else "${year}年"
    val balTitle = if (isMonth) "${year}年${month}月余额" else "${year}年余额"
    val balValue = if (isMonth) monthBal else yearBal

    val defaultDate = remember(year, month) {
        val t = LocalDate.now()
        if (year == t.year && month == t.monthValue) t.toString()
        else YearMonth.of(year, month)
            .atDay(minOf(t.dayOfMonth, YearMonth.of(year, month).lengthOfMonth()))
            .toString()
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            // 余额卡片
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(balTitle, style = MaterialTheme.typography.labelLarge)
                    Text("¥ ${formatMoney(balValue)}", style = MaterialTheme.typography.headlineMedium)
                }
            }

            // 顶部栏
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(mode == MainViewModel.LedgerViewMode.MONTH, { vm.setViewMode(MainViewModel.LedgerViewMode.MONTH) }, { Text("月") })
                    FilterChip(mode == MainViewModel.LedgerViewMode.YEAR, { vm.setViewMode(MainViewModel.LedgerViewMode.YEAR) }, { Text("年") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton({
                        pickerOpen = false
                        if (isMonth) vm.prevMonth() else vm.prevYear()
                    }) { Text("<", style = MaterialTheme.typography.titleLarge) }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.width(120.dp).clickable { pickerOpen = !pickerOpen }.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton({
                        pickerOpen = false
                        if (isMonth) vm.nextMonth() else vm.nextYear()
                    }) { Text(">", style = MaterialTheme.typography.titleLarge) }
                }
            }

            AnimatedVisibility(pickerOpen) {
                if (isMonth) {
                    MonthPicker(year, month, { vm.setYear(it) }, { vm.setMonth(it); pickerOpen = false })
                } else {
                    YearPicker(year, { vm.setYear(it); pickerOpen = false })
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isMonth) {
                MonthCalendar(year, month, dailyMap) { d ->
                    vm.setLedgerDate(d)
                    detailDate = d
                }
            } else {
                YearCalendar(year, monthlyMap) { y, m -> vm.jumpToMonth(y, m) }
            }
        }

        // 右下角“今”字按钮
        FloatingActionButton(
            onClick = { pickerOpen = false; vm.jumpToToday() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Text("今", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }

    // ============ 弹窗区 ============

    if (showAdd) {
        AddExpenseDialog(
            defaultDateKey = addDate ?: defaultDate,   // ★ 优先使用你点选的具体日期
            expenseCategories = expenseCats,           // ★ 传入分类数据
            incomeCategories = incomeCats,             // ★ 传入分类数据
            onAddCategory = { isIncome, name -> vm.addCategory(isIncome, name) }, // ★ 快捷添加回调
            onDismiss = { showAdd = false; addDate = null },
            onConfirm = { a, i, c, n, d ->
                vm.addExpense(a, i, c, n, d)
                showAdd = false
                addDate = null
            }
        )
    }

    detailDate?.let { d ->
        DailyDetailDialog(
            dateKey = d,
            items = items,
            onDismiss = { detailDate = null },
            onAddClick = {
                addDate = d          // ★ 记录点击的日期并传给添加弹窗
                detailDate = null
                showAdd = true
            },
            onEditItem = { editing = it }
        )
    }

    editing?.let { it ->
        AddExpenseDialog(
            defaultDateKey = it.dateKey,
            expenseCategories = expenseCats,
            incomeCategories = incomeCats,
            onAddCategory = { isIncome, name -> vm.addCategory(isIncome, name) },
            initialItem = it,
            onDismiss = { editing = null },
            onConfirm = { a, i, c, n, d ->
                vm.updateExpense(it.copy(amount = a, isIncome = i, category = c, note = n, dateKey = d))
                editing = null
            },
            onDelete = { vm.deleteExpense(it); editing = null }
        )
    }
}

@Composable
private fun MonthPicker(year: Int, selected: Int, onYear: (Int) -> Unit, onMonth: (Int) -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                IconButton({ onYear(year - 1) }) { Text("<", style = MaterialTheme.typography.titleLarge) }
                Text("${year}年", style = MaterialTheme.typography.titleMedium)
                IconButton({ onYear(year + 1) }) { Text(">", style = MaterialTheme.typography.titleLarge) }
            }
            Spacer(Modifier.height(4.dp))
            (1..12).chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { m ->
                        val sel = m == selected
                        Surface(
                            onClick = { onMonth(m) },
                            shape = MaterialTheme.shapes.small,
                            color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "${m}月",
                                    color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearPicker(current: Int, onSelect: (Int) -> Unit) {
    var showJumpDialog by remember { mutableStateOf(false) }
    val years = remember(current) { ((current + 10) downTo (current - 10)).toList() }
    val state = rememberLazyListState()
    LaunchedEffect(current) { years.indexOf(current).takeIf { it >= 0 }?.let { state.scrollToItem(it) } }

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            TextButton(
                onClick = { showJumpDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) { Text("跳到指定年份...", style = MaterialTheme.typography.bodyMedium) }
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(years) { y ->
                    val sel = y == current
                    Surface(
                        onClick = { onSelect(y) },
                        shape = MaterialTheme.shapes.small,
                        color = if (sel) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "${y}年",
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
    if (showJumpDialog) {
        JumpToYearDialog(
            initial = current,
            onDismiss = { showJumpDialog = false },
            onConfirm = { y -> onSelect(y); showJumpDialog = false }
        )
    }
}

@Composable
private fun JumpToYearDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(initial.toString()) }
    val value = text.toIntOrNull()
    val valid = value != null && value in 1900..2100
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳到指定年份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("请输入 1900 ~ 2100 之间的年份", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("年份") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { value?.let(onConfirm) }, enabled = valid) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun MonthCalendar(year: Int, month: Int, data: Map<String, Double>, onClick: (String) -> Unit) {
    val ym = YearMonth.of(year, month)
    val firstDow = ym.atDay(1).dayOfWeek.value
    val cells: List<String?> = buildList {
        repeat(firstDow - 1) { add(null) }
        for (i in 1..ym.lengthOfMonth()) add(String.format("%04d-%02d-%02d", year, month, i))
        repeat((7 - size % 7) % 7) { add(null) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(cells) { key ->
                if (key == null) {
                    Spacer(Modifier.height(56.dp))
                } else {
                    DayCell(key, data[key] ?: 0.0, onClick)
                }
            }
        }
    }
}

@Composable
private fun DayCell(key: String, net: Double, onClick: (String) -> Unit) {
    val day = key.substring(8).toInt()
    val today = key == LocalDate.now().toString()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(if (today) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, MaterialTheme.shapes.small)
            .clickable { onClick(key) }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
            color = if (today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (net != 0.0) {
            val pos = net > 0
            Text(
                text = (if (pos) "+" else "-") + formatMoney(abs(net)),
                fontSize = 9.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (pos) Green else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun YearCalendar(year: Int, data: Map<String, Double>, onClick: (Int, Int) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        items(12) { i ->
            val m = i + 1
            val net = data[String.format("%04d-%02d", year, m)] ?: 0.0
            Card(
                Modifier.fillMaxWidth().clickable { onClick(year, m) },
                colors = CardDefaults.cardColors(
                    containerColor = if (net != 0.0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${m} 月", style = MaterialTheme.typography.titleMedium)
                    if (net != 0.0) {
                        val pos = net > 0
                        Text(
                            (if (pos) "+¥" else "-¥") + formatMoney(abs(net)),
                            color = if (pos) Green else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}