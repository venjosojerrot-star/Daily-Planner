package com.example.util

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    fun formatMonthYear(millis: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDayMonth(millis: Long): String {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDayHeader(millis: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatFullDate(millis: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
        return sdf.format(Date(millis))
    }

    fun formatTime12(millis: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.US)
        return sdf.format(Date(millis)).lowercase(Locale.US)
    }

    fun formatShortDayName(millis: Long): String {
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDayOfMonth(millis: Long): String {
        val sdf = SimpleDateFormat("d", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatTime24(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        val sdf = SimpleDateFormat("h a", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun formatDateKey(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(millis))
    }

    fun isToday(millis: Long): Boolean {
        return isSameDay(millis, System.currentTimeMillis())
    }

    fun isFutureDay(millis: Long, referenceMillis: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val ref = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        val calYear = cal.get(Calendar.YEAR)
        val refYear = ref.get(Calendar.YEAR)
        return if (calYear != refYear) {
            calYear > refYear
        } else {
            cal.get(Calendar.DAY_OF_YEAR) > ref.get(Calendar.DAY_OF_YEAR)
        }
    }

    fun isTomorrow(millis: Long, referenceMillis: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val ref = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.get(Calendar.YEAR) == ref.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == ref.get(Calendar.DAY_OF_YEAR)
    }

    fun getWeekDays(centerMillis: Long): List<Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = centerMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        cal.add(Calendar.DATE, - (dayOfWeek - 1)) // Go to Sunday of current week

        val days = mutableListOf<Long>()
        for (i in 0 until 7) {
            days.add(cal.timeInMillis)
            cal.add(Calendar.DATE, 1)
        }
        return days
    }

    fun formatMediumDate(millis: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun getStartOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun parseColor(hex: String): Color {
        return try {
            val cleanHex = hex.replace("#", "")
            val colorInt = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color(colorInt or 0xFF000000)
            } else {
                Color(colorInt)
            }
        } catch (_: Exception) {
            Color(0xFF039BE5) // Fallback Peacock Blue
        }
    }
}
