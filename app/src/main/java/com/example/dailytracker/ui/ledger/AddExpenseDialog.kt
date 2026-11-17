package com.example.dailytracker.ui.ledger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dailytracker.data.local.entity.ExpenseEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    defaultDateKey: String,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    onAddCategory: (isIncome: Boolean, name: String) -> Unit,
    initialItem: ExpenseEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, isIncome: Boolean, category: String, note: String, dateKey: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var amountText by remember { mutableStateOf(initialItem?.amount?.toString() ?: "") }
    var isIncome by remember { mutableStateOf(initialItem?.isIncome ?: false) }

    val currentCategories = if (isIncome) incomeCategories else expenseCategories
    var category by remember { mutableStateOf(initialItem?.category ?: currentCategories.firstOrNull() ?: "其他") }

    var note by remember { mutableStateOf(initialItem?.note ?: "") }
    var dateKey by remember { mutableStateOf(initialItem?.dateKey ?: defaultDateKey) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull()
    val isEdit = initialItem != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "修改记录" else if (isIncome) "记一笔收入" else "记一笔支出") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isIncome, onClick = {
                        isIncome = false
                        if (!expenseCategories.contains(category)) category = expenseCategories.firstOrNull() ?: "其他"
                    }, label = { Text("支出") })
                    FilterChip(selected = isIncome, onClick = {
                        isIncome = true
                        if (!incomeCategories.contains(category)) category = incomeCategories.firstOrNull() ?: "其他"
                    }, label = { Text("收入") })
                }
                OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("日期", style = MaterialTheme.typography.bodyMedium)
                        Text(dateKey, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { s -> amountText = s.filter { it.isDigit() || it == '.' }.take(10) },
                    label = { Text("金额") },
                    prefix = { Text("¥") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    currentCategories.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                    }
                    FilterChip(
                        selected = false,
                        onClick = { showAddCategoryDialog = true },
                        label = { Text("+ 添加") }
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount?.let { onConfirm(it, isIncome, category, note, dateKey) } },
                enabled = amount != null && amount > 0
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (isEdit && onDelete != null) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("删除") }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )

    if (showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("添加分类") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("分类名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        onAddCategory(isIncome, newCategoryName)
                        category = newCategoryName.trim()
                        showAddCategoryDialog = false
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text("取消") } }
        )
    }

    if (showDatePicker) {
        val initialMillis = runCatching {
            LocalDate.parse(dateKey).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val ld = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        dateKey = ld.toString()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除？") },
            text = { Text("删除后无法恢复，确定要删除这条记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onDelete?.invoke() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("确定删除") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}