package com.example.dailytracker.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytracker.data.local.AppDatabase
import com.example.dailytracker.data.local.entity.*
import com.example.dailytracker.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val expenseDao = db.expenseDao()
    private val taskDao = db.taskDao()
    private val prefs = app.getSharedPreferences("dailytracker_prefs", Context.MODE_PRIVATE)

    // ==================== 业务数据 ====================
    private val _todayKey = MutableStateFlow(DateUtils.todayKey())
    val todayKey: StateFlow<String> = _todayKey.asStateFlow()

    private val _ledgerDate = MutableStateFlow(DateUtils.todayKey())
    val ledgerDate: StateFlow<String> = _ledgerDate.asStateFlow()

    val tasks: StateFlow<List<TaskEntity>> = taskDao.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayLogs: StateFlow<List<TaskLogEntity>> = _todayKey
        .flatMapLatest { taskDao.observeLogs(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ledgerItems: StateFlow<List<ExpenseEntity>> = _ledgerDate
        .flatMapLatest { expenseDao.observeByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ledgerDayExpense: StateFlow<Double> = _ledgerDate
        .flatMapLatest { expenseDao.observeDayExpense(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // 分类与历史记录
    private val _expenseCategories = MutableStateFlow<List<String>>(emptyList())
    val expenseCategories: StateFlow<List<String>> = _expenseCategories.asStateFlow()
    private val _incomeCategories = MutableStateFlow<List<String>>(emptyList())
    val incomeCategories: StateFlow<List<String>> = _incomeCategories.asStateFlow()
    private val _exportHistory = MutableStateFlow<List<String>>(emptyList())
    val exportHistory: StateFlow<List<String>> = _exportHistory.asStateFlow()

    enum class LedgerViewMode { MONTH, YEAR }
    private val _viewMode = MutableStateFlow(LedgerViewMode.MONTH)
    val viewMode: StateFlow<LedgerViewMode> = _viewMode.asStateFlow()

    private val _currentYear = MutableStateFlow(LocalDate.now().year)
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(LocalDate.now().monthValue)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    val monthlyBalance: StateFlow<Double> = combine(_currentYear, _currentMonth) { y, m ->
        String.format("%04d-%02d", y, m)
    }.flatMapLatest { ym -> expenseDao.observeMonthlyNet(ym) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val yearlyBalance: StateFlow<Double> = _currentYear
        .flatMapLatest { y -> expenseDao.observeYearlyNet(y.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val monthlyDailyExpense: StateFlow<Map<String, Double>> = combine(_currentYear, _currentMonth) { y, m ->
        String.format("%04d-%02d", y, m)
    }.flatMapLatest { ym -> expenseDao.observeMonthlyDailyExpense(ym) }
        .map { list -> list.associate { it.dateKey to it.total } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val yearlyMonthlyExpense: StateFlow<Map<String, Double>> = _currentYear
        .flatMapLatest { y -> expenseDao.observeYearlyMonthlyExpense(y.toString()) }
        .map { list -> list.associate { it.monthKey to it.total } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        // 恢复分类与导出历史
        val expSaved = prefs.getString("expense_categories", null)
        _expenseCategories.value = expSaved?.split("|||")?.filter { it.isNotBlank() } ?: listOf("餐饮", "交通", "购物", "娱乐", "居住", "医疗", "其他")
        val incSaved = prefs.getString("income_categories", null)
        _incomeCategories.value = incSaved?.split("|||")?.filter { it.isNotBlank() } ?: listOf("工资", "兼职", "投资", "其他")
        val historySaved = prefs.getString("export_history", null)
        _exportHistory.value = historySaved?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()

        viewModelScope.launch {
            while (isActive) {
                delay(DateUtils.millisUntilNextMidnight() + 800)
                refreshToday()
            }
        }
    }

    private fun saveCategories(isIncome: Boolean, list: List<String>) {
        prefs.edit().putString(if (isIncome) "income_categories" else "expense_categories", list.joinToString("|||")).apply()
        if (isIncome) _incomeCategories.value = list else _expenseCategories.value = list
    }

    fun addCategory(isIncome: Boolean, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val current = if (isIncome) _incomeCategories.value else _expenseCategories.value
        if (!current.contains(trimmed)) saveCategories(isIncome, current + trimmed)
    }

    fun deleteCategory(isIncome: Boolean, name: String) {
        val current = if (isIncome) _incomeCategories.value else _expenseCategories.value
        saveCategories(isIncome, current - name)
    }

    fun moveCategory(isIncome: Boolean, index: Int, delta: Int) {
        val current = (if (isIncome) _incomeCategories.value else _expenseCategories.value).toMutableList()
        val target = index + delta
        if (index in current.indices && target in current.indices) {
            val item = current.removeAt(index)
            current.add(target, item)
            saveCategories(isIncome, current)
        }
    }

    fun setViewMode(mode: LedgerViewMode) { _viewMode.value = mode }
    fun setYear(year: Int) { _currentYear.value = year }
    fun setMonth(month: Int) { _currentMonth.value = month }

    fun prevMonth() { if (_currentMonth.value == 1) { _currentMonth.value = 12; _currentYear.value -= 1 } else { _currentMonth.value -= 1 } }
    fun nextMonth() { if (_currentMonth.value == 12) { _currentMonth.value = 1; _currentYear.value += 1 } else { _currentMonth.value += 1 } }
    fun prevYear() { _currentYear.value -= 1 }
    fun nextYear() { _currentYear.value += 1 }

    fun jumpToMonth(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month
        _viewMode.value = LedgerViewMode.MONTH
    }

    fun jumpToToday() {
        val today = LocalDate.now()
        _currentYear.value = today.year
        _currentMonth.value = today.monthValue
        _viewMode.value = LedgerViewMode.MONTH
        _ledgerDate.value = DateUtils.todayKey()
    }

    fun checkHasData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(expenseDao.getAllExpensesOnce().isNotEmpty()) }
    }

    fun exportData(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val allExpenses = expenseDao.getAllExpensesOnce()
                val csv = StringBuilder("\uFEFF日期,金额,类型,分类,备注\n")
                allExpenses.forEach {
                    val type = if (it.isIncome) "收入" else "支出"
                    val safeNote = if (it.note.contains(",")) "\"${it.note}\"" else it.note
                    csv.append("${it.dateKey},${it.amount},${type},${it.category},${safeNote}\n")
                }
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toString().toByteArray(Charsets.UTF_8))
                }
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                val current = _exportHistory.value.toMutableList()
                current.add(0, now)
                val newList = current.take(10)
                prefs.edit().putString("export_history", newList.joinToString("|||")).apply()
                _exportHistory.value = newList
                onResult(true)
            } catch (e: Exception) { e.printStackTrace(); onResult(false) }
        }
    }

    fun importData(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val lines = getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.readLines() ?: emptyList()
                val newExpenses = mutableListOf<ExpenseEntity>()
                val importedExpenseCats = mutableSetOf<String>()
                val importedIncomeCats = mutableSetOf<String>()

                lines.drop(1).forEach { line ->
                    if (line.isBlank()) return@forEach
                    val parts = mutableListOf<String>()
                    var inQuotes = false
                    var currentPart = StringBuilder()
                    for (char in line) {
                        when {
                            char == '"' -> inQuotes = !inQuotes
                            char == ',' && !inQuotes -> { parts.add(currentPart.toString()); currentPart = StringBuilder() }
                            else -> currentPart.append(char)
                        }
                    }
                    parts.add(currentPart.toString())
                    if (parts.size >= 5) {
                        val date = parts[0].trim()
                        val amount = parts[1].trim().toDoubleOrNull() ?: 0.0
                        val isIncome = parts[2].trim() == "收入"
                        val category = parts[3].trim()
                        val note = parts[4].trim()
                        newExpenses.add(ExpenseEntity(amount = amount, isIncome = isIncome, category = category, note = note, dateKey = date))
                        if (isIncome) importedIncomeCats.add(category) else importedExpenseCats.add(category)
                    }
                }
                if (importedExpenseCats.isNotEmpty()) saveCategories(false, importedExpenseCats.toList())
                if (importedIncomeCats.isNotEmpty()) saveCategories(true, importedIncomeCats.toList())
                expenseDao.clearAllExpenses()
                if (newExpenses.isNotEmpty()) expenseDao.insertAll(newExpenses)
                onResult(true)
            } catch (e: Exception) { e.printStackTrace(); onResult(false) }
        }
    }

    fun refreshToday() {
        val now = DateUtils.todayKey()
        if (_todayKey.value != now) {
            _todayKey.value = now
            if (_ledgerDate.value == now) _ledgerDate.value = now
        }
    }

    fun toggleTask(taskId: Long) {
        viewModelScope.launch {
            val key = _todayKey.value
            val existing = taskDao.getLog(taskId, key)
            if (existing?.done == true) taskDao.deleteLog(taskId, key)
            else taskDao.upsertLog(TaskLogEntity(taskId = taskId, dateKey = key, done = true))
        }
    }

    fun addTask(title: String) {
        val t = title.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            val next = (tasks.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            taskDao.insert(TaskEntity(title = t, sortOrder = next))
        }
    }

    fun deleteTask(task: TaskEntity) { viewModelScope.launch { taskDao.delete(task) } }

    fun moveTask(index: Int, delta: Int) {
        val list = tasks.value.toMutableList()
        val target = index + delta
        if (index !in list.indices || target !in list.indices) return
        list.add(target, list.removeAt(index))
        viewModelScope.launch {
            list.forEachIndexed { i, t -> if (t.sortOrder != i) taskDao.update(t.copy(sortOrder = i)) }
        }
    }

    fun shiftLedgerDate(days: Long) {
        val next = DateUtils.shift(_ledgerDate.value, days)
        if (next > DateUtils.todayKey()) return
        _ledgerDate.value = next
    }

    fun goToToday() { _ledgerDate.value = DateUtils.todayKey() }
    fun setLedgerDate(dateKey: String) { _ledgerDate.value = dateKey }

    fun addExpense(amount: Double, isIncome: Boolean, category: String, note: String, dateKey: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            expenseDao.insert(ExpenseEntity(amount = amount, isIncome = isIncome, category = category, note = note.trim(), dateKey = dateKey))
        }
    }

    fun updateExpense(item: ExpenseEntity) { viewModelScope.launch { expenseDao.update(item) } }
    fun deleteExpense(item: ExpenseEntity) { viewModelScope.launch { expenseDao.delete(item) } }

    // ==================== 统计相关 ====================
    enum class StatsMode { WEEK, MONTH, YEAR }
    private val _statsMode = MutableStateFlow(StatsMode.WEEK)
    val statsMode: StateFlow<StatsMode> = _statsMode.asStateFlow()

    private val _statsBaseDate = MutableStateFlow(LocalDate.now())
    val statsBaseDate: StateFlow<LocalDate> = _statsBaseDate.asStateFlow()

    fun setStatsMode(mode: StatsMode) { _statsMode.value = mode }
    fun prevStatsPeriod() {
        _statsBaseDate.value = when (_statsMode.value) {
            StatsMode.WEEK -> _statsBaseDate.value.minusWeeks(1)
            StatsMode.MONTH -> _statsBaseDate.value.minusMonths(1)
            StatsMode.YEAR -> _statsBaseDate.value.minusYears(1)
        }
    }
    fun nextStatsPeriod() {
        _statsBaseDate.value = when (_statsMode.value) {
            StatsMode.WEEK -> _statsBaseDate.value.plusWeeks(1)
            StatsMode.MONTH -> _statsBaseDate.value.plusMonths(1)
            StatsMode.YEAR -> _statsBaseDate.value.plusYears(1)
        }
    }

    val statsRange: StateFlow<Pair<LocalDate, LocalDate>> = combine(_statsMode, _statsBaseDate) { mode, date ->
        when (mode) {
            StatsMode.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            StatsMode.MONTH -> YearMonth.from(date).atDay(1) to YearMonth.from(date).atEndOfMonth()
            StatsMode.YEAR -> LocalDate.of(date.year, 1, 1) to LocalDate.of(date.year, 12, 31)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now() to LocalDate.now())

    val statsExpenses: StateFlow<List<ExpenseEntity>> = statsRange
        .flatMapLatest { (start, end) -> expenseDao.getExpensesBetween(start.toString(), end.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun jumpStatsToToday() { _statsBaseDate.value = LocalDate.now() }
}