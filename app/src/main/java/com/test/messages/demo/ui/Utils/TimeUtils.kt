package com.test.messages.demo.ui.Utils

import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object TimeUtils {

    fun formatTimestamp(timestamp: Long): String {
        val context = Locale.getDefault()
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            // Today: Show time (e.g., 9:30 AM)
            isSameDay(now, messageTime) -> {
                SimpleDateFormat("h:mm a", context).format(messageTime.time)
            }
            // Yesterday: Show "Yesterday"
            isYesterday(now, messageTime) -> {
                "Yesterday"
            }
            // Within last 7 days (excluding today and yesterday): Show weekday name (e.g., "Thursday")
//            isWithinLast7Days(now, messageTime) -> {
//                SimpleDateFormat("EEEE", context).format(messageTime.time)
//            }
            // This year but older than a week: Show "12 Feb"
            isSameYear(now, messageTime) -> {
                SimpleDateFormat("d MMM", context).format(messageTime.time)
            }
            // Older than a year: Show "12 Feb 24"
            else -> {
                SimpleDateFormat("d MMM yy", context).format(messageTime.time)
            }
        }
    }
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, messageTime: Calendar): Boolean {
        now.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(now, messageTime)
    }
    fun isWithinLast7Days(now: Calendar, messageTime: Calendar): Boolean {
        val diff = now.timeInMillis - messageTime.timeInMillis
        val daysDiff = TimeUnit.MILLISECONDS.toDays(diff)

        return daysDiff in 2..6 // Exclude today (0) and yesterday (1)
    }


    private fun isSameYear(now: Calendar, messageTime: Calendar): Boolean {
        return now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)
    }

    fun getInitials(name: String): String {
        return name.trim().split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(1).uppercase()
    }

    fun getRandomColor(input: String): Int {
        val colors = listOf(
            Color.parseColor("#0CA3AE"),
            Color.parseColor("#FF564D"),
            Color.parseColor("#6955D2"),
            Color.parseColor("#4FA784"),
            Color.parseColor("#1A73E8")
//            Color.parseColor("#9C27B0")  // Purple
        )
        val index = (input.hashCode() and 0x7FFFFFFF) % colors.size
        return colors[index]
    }

}
