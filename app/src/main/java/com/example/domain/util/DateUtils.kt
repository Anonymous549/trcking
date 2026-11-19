package com.example.domain.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun todayEpochDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // Compute local epoch day accurately without timezone off-by-one
        val utcOffset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)
        return (cal.timeInMillis + utcOffset) / MILLIS_PER_DAY
    }

    fun epochDayToMillis(epochDay: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochDay * MILLIS_PER_DAY
        val utcOffset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)
        return epochDay * MILLIS_PER_DAY - utcOffset
    }

    fun formatFullDate(epochDay: Long): String {
        val sdf = SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault())
        return sdf.format(Date(epochDayToMillis(epochDay)))
    }

    fun formatShortDate(epochDay: Long): String {
        val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
        return sdf.format(Date(epochDayToMillis(epochDay)))
    }

    fun formatDayOfWeekShort(epochDay: Long): String {
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        return sdf.format(Date(epochDayToMillis(epochDay)))
    }

    /** Returns 1 (Mon) .. 7 (Sun) */
    fun isoDayOfWeek(epochDay: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochDayToMillis(epochDay)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun currentYearMonth(epochDay: Long = todayEpochDay()): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochDayToMillis(epochDay)
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    fun monthStartEpochDay(year: Int, month1Based: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month1Based - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val utcOffset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)
        return (cal.timeInMillis + utcOffset) / MILLIS_PER_DAY
    }

    fun monthLength(year: Int, month1Based: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month1Based - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun formatMinuteOfDay(minuteOfDay: Int): String {
        val clamped = minuteOfDay.coerceIn(0, 1439)
        val h = clamped / 60
        val m = clamped % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun currentMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun daysBetween(fromEpochDay: Long, toEpochDay: Long): Long = toEpochDay - fromEpochDay

    fun formatTimerSeconds(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val mins = safe / 60
        val secs = safe % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    fun formatMinutesReadable(minutes: Int): String {
        if (minutes < 60) return "${minutes}m"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h}h" else "${h}h ${m}m"
    }

    fun formatCurrency(amount: Double, symbol: String, code: String = "INR"): String {
        return try {
            val nf = NumberFormat.getNumberInstance(if (code == "INR") Locale("en", "IN") else Locale.getDefault())
            nf.maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            nf.minimumFractionDigits = 0
            "$symbol${nf.format(amount)}"
        } catch (_: Exception) {
            "$symbol${String.format(Locale.US, "%.0f", amount)}"
        }
    }

    fun millisToDays(millis: Long): Long = TimeUnit.MILLISECONDS.toDays(millis)
}
