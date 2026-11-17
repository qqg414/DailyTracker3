package com.example.dailytracker.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dailytracker.data.local.entity.ExpenseEntity
import com.example.dailytracker.ui.MainViewModel
import com.example.dailytracker.util.formatMoney
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow

private val ChartColors = listOf(
    Color(0xFF5C7BD9), Color(0xFFD9B05C), Color(0xFFE57373),
    Color(0xFF81C784), Color(0xFFBA68C8), Color(0xFF4DD0E1), Color(0xFFFFB74D)
)

@Composable
fun StatsScreen(vm: MainViewModel) {
    val mode by vm.statsMode.collectAsStateWithLifecycle()
    val range by vm.statsRange.collectAsStateWithLifecycle()
    val expenses by vm.statsExpenses.collectAsStateWithLifecycle()

    var selectedCategoryExpenses by remember { mutableStateOf<List<ExpenseEntity>?>(null) }
    var selectedCategoryName by remember { mutableStateOf("") }

    val (start, end) = range
    val daysCount = ChronoUnit.DAYS.between(start, end).toInt() + 1

    val trendData = remember(expenses, start, end, mode) {
        val expList = DoubleArray(daysCount)
        val incList = DoubleArray(daysCount)
        expenses.forEach { e ->
            val d = LocalDate.parse(e.dateKey)
            val index = ChronoUnit.DAYS.between(start, d).toInt()
            if (index in 0 until daysCount) {
                if (!e.isIncome) expList[index] += e.amount
                else incList[index] += e.amount
            }
        }
        expList to incList
    }

    val expenseGroups = remember(expenses) {
        expenses.filter { !it.isIncome }.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }
    }
    val incomeGroups = remember(expenses) {
        expenses.filter { it.isIncome }.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }
    }

    val totalExpense = expenseGroups.sumOf { it.second }
    val avgExpense = if (daysCount > 0) totalExpense / daysCount else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部模式切换
        item {
            TabRow(selectedTabIndex = mode.ordinal) {
                MainViewModel.StatsMode.entries.forEach { m ->
                    Tab(
                        selected = mode == m,
                        onClick = { vm.setStatsMode(m) },
                        text = { Text(when(m) { MainViewModel.StatsMode.WEEK -> "周"; MainViewModel.StatsMode.MONTH -> "月"; MainViewModel.StatsMode.YEAR -> "年" }) }
                    )
                }
            }
        }

        // 时间导航
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.prevStatsPeriod() }) { Text("<", style = MaterialTheme.typography.titleLarge) }
                Text(
                    text = getRangeTitle(mode, start, end),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { vm.nextStatsPeriod() }) { Text(">", style = MaterialTheme.typography.titleLarge) }
            }
        }

        // 收支趋势折线图卡片
        item {
            TrendChartCard(
                expenses = trendData.first,
                incomes = trendData.second,
                labels = getChartLabels(mode, start, daysCount)
            )
        }

        // 支出构成
        if (expenseGroups.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("支出构成", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        DoughnutChart(
                            data = expenseGroups,
                            total = totalExpense,
                            onLegendClick = { name, items ->
                                selectedCategoryName = name
                                selectedCategoryExpenses = items
                            },
                            allExpenses = expenses.filter { !it.isIncome }
                        )
                    }
                }
            }
        }

        // 收入构成
        if (incomeGroups.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("收入构成", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        DoughnutChart(
                            data = incomeGroups,
                            total = incomeGroups.sumOf { it.second },
                            onLegendClick = { name, items ->
                                selectedCategoryName = name
                                selectedCategoryExpenses = items
                            },
                            allExpenses = expenses.filter { it.isIncome }
                        )
                    }
                }
            }
        }

        // 总结
        if (expenseGroups.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("总结", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("共计消费 ${formatMoney(totalExpense)} 元，日均消费 ${formatMoney(avgExpense)} 元。", fontSize = 14.sp)
                        if (expenseGroups.isNotEmpty()) {
                            val top1 = expenseGroups[0]
                            val percent1 = if (totalExpense > 0) (top1.second / totalExpense * 100) else 0.0
                            Text("最多是${top1.first}，占比${String.format("%.1f", percent1)}%，消费${formatMoney(top1.second)}元", fontSize = 14.sp)
                        }
                        if (expenseGroups.size > 1) {
                            val top2 = expenseGroups[1]
                            val percent2 = if (totalExpense > 0) (top2.second / totalExpense * 100) else 0.0
                            Text("其次是${top2.first}，占比${String.format("%.1f", percent2)}%，消费${formatMoney(top2.second)}元", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // 分类消费清单弹窗
    selectedCategoryExpenses?.let { list ->
        AlertDialog(
            onDismissRequest = { selectedCategoryExpenses = null },
            title = { Text("$selectedCategoryName - 消费清单") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(list) { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(item.note.ifBlank { item.category }, fontWeight = FontWeight.Medium)
                                Text(item.dateKey, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("-${formatMoney(item.amount)}", color = MaterialTheme.colorScheme.error)
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedCategoryExpenses = null }) { Text("关闭") } }
        )
    }
}

@Composable
private fun TrendChartCard(expenses: DoubleArray, incomes: DoubleArray, labels: List<String>) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("收支趋势", fontWeight = FontWeight.Bold)
                if (scale > 1f || offsetX != 0f) {
                    TextButton(
                        onClick = { scale = 1f; offsetX = 0f },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("复位", fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(color = Color(0xFF81C784), label = "支出")
                LegendItem(color = Color(0xFF5C7BD9), label = "收入")
            }
            Spacer(Modifier.height(12.dp))

            TrendChart(
                expenses = expenses,
                incomes = incomes,
                labels = labels,
                scale = scale,
                offsetX = offsetX,
                onTransform = { newScale, newOffset ->
                    scale = newScale
                    offsetX = newOffset
                }
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(12.dp)) { drawRect(color = color) }
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun TrendChart(
    expenses: DoubleArray,
    incomes: DoubleArray,
    labels: List<String>,
    scale: Float,
    offsetX: Float,
    onTransform: (Float, Float) -> Unit
) {
    if (expenses.isEmpty()) return

    // 获取当前数据的最大值，并向上取整，用于确定 Y 轴刻度
    val rawMax = (expenses.maxOrNull() ?: 0.0).coerceAtLeast(incomes.maxOrNull() ?: 0.0)
    val maxVal = if (rawMax == 0.0) 10.0 else calculateNiceMax(rawMax)
    val valueRange = maxVal

    val currentScale by rememberUpdatedState(scale)
    val currentOffsetX by rememberUpdatedState(offsetX)

    val density = LocalDensity.current

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(220.dp) // 稍微增加高度，给 Y 轴文字更好的间距
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (currentScale * zoom).coerceIn(1f, 5f)
                    val width = size.width.toFloat()
                    val maxOffset = -(width * newScale - width)

                    val focusX = centroid.x
                    val realChartX = (focusX - currentOffsetX) / currentScale

                    val newOffsetX = if (newScale == 1f) 0f
                    else (focusX - realChartX * newScale + pan.x).coerceIn(maxOffset, 0f)

                    onTransform(newScale, newOffsetX)
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val bottomPaddingPx = with(density) { 30.dp.toPx() }
        val chartHeight = constraints.maxHeight.toFloat() - bottomPaddingPx
        val leftPaddingPx = with(density) { 45.dp.toPx() } // 左侧 Y 轴宽度

        val chartWidth = width - leftPaddingPx
        val baseStepX = chartWidth / (expenses.size - 1).coerceAtLeast(1)

        val textPaintY = android.graphics.Paint().apply {
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.RIGHT
            color = android.graphics.Color.GRAY
        }

        val textPaintX = android.graphics.Paint().apply {
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.GRAY
        }

        // 1. 绘制 Y 轴刻度和水平辅助线
        Canvas(Modifier.fillMaxSize()) {
            val ySteps = 4 // 分 4 等份
            for (i in 0..ySteps) {
                val ratio = i.toFloat() / ySteps
                val y = chartHeight - (ratio * chartHeight)
                val value = maxVal * ratio

                // 绘制水平线
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(leftPaddingPx, y),
                    end = Offset(width, y),
                    strokeWidth = 1f,
                    pathEffect = if (i == 0) null else androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )

                // 绘制左侧 Y 轴文字
                val labelText = formatYAxisValue(value)
                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    leftPaddingPx - with(density) { 8.dp.toPx() },
                    y + with(density) { 4.dp.toPx() }, // 文字垂直居中对齐
                    textPaintY
                )
            }
        }

        // 2. 绘制折线和 X 轴文字（限制在 chartWidth 范围内）
        Canvas(Modifier.fillMaxSize()) {
            clipRect(
                left = leftPaddingPx,
                top = 0f,
                right = width,
                bottom = constraints.maxHeight.toFloat()
            ) {
                val expPath = Path()
                val incPath = Path()
                val pointsExp = mutableListOf<Offset>()
                val pointsInc = mutableListOf<Offset>()

                val labelStep = if (labels.size > 12) labels.size / 7 else 1

                for (i in expenses.indices) {
                    // 基于 leftPaddingPx 偏移
                    val x = leftPaddingPx + i * baseStepX * scale + offsetX

                    val ratioExp = (expenses[i] / valueRange).toFloat().coerceIn(0f, 1f)
                    val yExp = chartHeight - (ratioExp * chartHeight)

                    val ratioInc = (incomes[i] / valueRange).toFloat().coerceIn(0f, 1f)
                    val yInc = chartHeight - (ratioInc * chartHeight)

                    pointsExp.add(Offset(x, yExp))
                    pointsInc.add(Offset(x, yInc))

                    if (i == 0) {
                        expPath.moveTo(x, yExp)
                        incPath.moveTo(x, yInc)
                    } else {
                        expPath.lineTo(x, yExp)
                        incPath.lineTo(x, yInc)
                    }

                    // 绘制底部标签
                    if (i < labels.size && (i % labelStep == 0 || i == 0 || i == labels.size - 1)) {
                        val edgePadding = with(density) { 10.dp.toPx() }
                        // 留出左右边缘检测，防止文字被切
                        if (x >= leftPaddingPx - edgePadding && x <= width + edgePadding) {
                            drawContext.canvas.nativeCanvas.drawText(
                                labels[i],
                                x,
                                chartHeight + with(density) { 20.dp.toPx() },
                                textPaintX
                            )
                        }
                    }
                }

                drawPath(expPath, color = Color(0xFF81C784), style = Stroke(width = 3.dp.toPx()))
                drawPath(incPath, color = Color(0xFF5C7BD9), style = Stroke(width = 3.dp.toPx()))

                pointsExp.forEach { p ->
                    if (p.x >= leftPaddingPx && p.x <= width) {
                        drawCircle(Color.White, radius = 4.dp.toPx(), center = p)
                        drawCircle(Color(0xFF81C784), radius = 4.dp.toPx(), center = p, style = Stroke(width = 2.dp.toPx()))
                    }
                }
                pointsInc.forEach { p ->
                    if (p.x >= leftPaddingPx && p.x <= width) {
                        drawCircle(Color.White, radius = 4.dp.toPx(), center = p)
                        drawCircle(Color(0xFF5C7BD9), radius = 4.dp.toPx(), center = p, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }
        }
    }
}

/**
 * 将原始最大值向上取整为“好看”的整数，例如 830 -> 1000, 45 -> 50
 */
private fun calculateNiceMax(value: Double): Double {
    if (value == 0.0) return 10.0
    val exponent = log10(value).toInt()
    val fraction = value / 10.0.pow(exponent)
    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    return niceFraction * 10.0.pow(exponent)
}

/**
 * 格式化 Y 轴数值，过大时显示 k
 */
private fun formatYAxisValue(value: Double): String {
    return if (value >= 10000) {
        "${String.format("%.1f", value / 1000)}k"
    } else if (value >= 1000) {
        "${String.format("%.1f", value / 1000)}k"
    } else {
        value.toInt().toString()
    }
}

@Composable
private fun DoughnutChart(
    data: List<Pair<String, Double>>,
    total: Double,
    allExpenses: List<ExpenseEntity>,
    onLegendClick: (String, List<ExpenseEntity>) -> Unit
) {
    if (total == 0.0) return

    Row(
        Modifier.fillMaxWidth().heightIn(min = 180.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .aspectRatio(1f)
                .padding(8.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val diameter = minOf(size.width, size.height) * 0.85f
                val chartRadius = diameter / 2
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                val arcSize = Size(diameter, diameter)
                var startAngle = -90f

                data.forEachIndexed { index, (name, amount) ->
                    val sweepAngle = (amount / total * 360).toFloat()
                    val color = ChartColors[index % ChartColors.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = chartRadius * 0.4f)
                    )
                    if (sweepAngle > 20) {
                        val middleAngle = startAngle + sweepAngle / 2
                        val rad = Math.toRadians(middleAngle.toDouble())
                        val labelRadius = chartRadius * 0.8f
                        val x = center.x + labelRadius * kotlin.math.cos(rad)
                        val y = center.y + labelRadius * kotlin.math.sin(rad)
                        drawContext.canvas.nativeCanvas.apply {
                            val text = "${String.format("%.1f", amount/total*100)}%"
                            val paint = android.graphics.Paint().apply {
                                textSize = 12.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                setColor(android.graphics.Color.WHITE)
                                isFakeBoldText = true
                            }
                            drawText(text, x.toFloat(), y.toFloat() + paint.textSize/3, paint)
                        }
                    }
                    startAngle += sweepAngle
                }
            }
        }

        Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            data.forEachIndexed { index, (name, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val items = allExpenses.filter { it.category == name }
                            onLegendClick(name, items)
                        }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(Modifier.size(10.dp)) { drawCircle(ChartColors[index % ChartColors.size]) }
                    Spacer(Modifier.width(6.dp))
                    Text("$name ${String.format("%.1f", amount/total*100)}%", fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

private fun getRangeTitle(mode: MainViewModel.StatsMode, start: LocalDate, end: LocalDate): String {
    return when (mode) {
        MainViewModel.StatsMode.WEEK -> "${start.year}年${start.monthValue}月 第${(start.dayOfMonth - 1) / 7 + 1}周\n${start.monthValue}月${start.dayOfMonth}日 - ${end.monthValue}月${end.dayOfMonth}日"
        MainViewModel.StatsMode.MONTH -> "${start.year}年${start.monthValue}月"
        MainViewModel.StatsMode.YEAR -> "${start.year}年"
    }
}

private fun getChartLabels(mode: MainViewModel.StatsMode, start: LocalDate, days: Int): List<String> {
    return when (mode) {
        MainViewModel.StatsMode.WEEK -> listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        MainViewModel.StatsMode.MONTH -> (1..days).map { "${it}日" }
        MainViewModel.StatsMode.YEAR -> (1..12).map { "${it}月" }
    }
}