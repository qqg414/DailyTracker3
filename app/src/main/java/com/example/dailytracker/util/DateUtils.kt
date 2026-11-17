package com.example.dailytracker.util

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    private val KEY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val PRETTY_FMT = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)

    /** 今天的 key，例如 2025-03-08 */
    fun todayKey(): String = LocalDate.now().format(KEY_FMT)

    fun keyOf(date: LocalDate): String = date.format(KEY_FMT)

    fun parse(key: String): LocalDate = LocalDate.parse(key, KEY_FMT)

    /** 在某个 key 上加减天数 */
    fun shift(key: String, days: Long): String =
        parse(key).plusDays(days).format(KEY_FMT)

    fun pretty(key: String): String =
        runCatching { parse(key).format(PRETTY_FMT) }.getOrDefault(key)

    fun isToday(key: String): Boolean = key == todayKey()

    /** 距离下一个 0 点的毫秒数（用于跨天自动刷新） */
    fun millisUntilNextMidnight(): Long {
        val now = LocalDateTime.now()
        val next = now.toLocalDate().plusDays(1).atStartOfDay()
        return Duration.between(now, next).toMillis().coerceAtLeast(1_000L)
    }
}

/** 金额格式化：1234.5 -> "1,234.50" */
fun formatMoney(value: Double): String =
    String.format(Locale.CHINA, "%,.2f", value)