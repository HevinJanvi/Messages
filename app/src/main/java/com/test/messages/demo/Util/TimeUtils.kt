package com.test.messages.demo.Util

import android.content.Context
import android.graphics.Color
import com.test.messages.demo.R
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {


    fun formatTimestamp(cntxt: Context, timestamp: Long): String {
        val locale = Locale.getDefault()
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        val is24Hour = android.text.format.DateFormat.is24HourFormat(cntxt)
        val timePattern = if (is24Hour) "HH:mm" else "h:mm a"

        return when {
            isSameDay(now, messageTime) -> {
                SimpleDateFormat(timePattern, locale).format(messageTime.time)
            }
            isYesterday(now, messageTime) -> {
                cntxt.getString(R.string.yesterday)
            }
            isSameYear(now, messageTime) -> {
                SimpleDateFormat("d MMM", locale).format(messageTime.time)
            }
            else -> {
                SimpleDateFormat("d MMM yy", locale).format(messageTime.time)
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


    private fun isSameYear(now: Calendar, messageTime: Calendar): Boolean {
        return now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)
    }

    fun getInitials(name: String): String {
        return name.trim().split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(1).uppercase()
    }


    fun getFormattedHeaderTimestamp(context: Context, timestamp: Long): String {
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        val timeFormat = if (is24Hour) "HH:mm" else "h:mm a"

        return when {
            isSameDay(now, messageTime) -> {
               context.getString(R.string.today) + " " + SimpleDateFormat(timeFormat, Locale.getDefault()).format(messageTime.time)
            }
            isYesterday(now.clone() as Calendar, messageTime) -> {
                context.getString(R.string.yesterday) + " " + SimpleDateFormat(timeFormat, Locale.getDefault()).format(messageTime.time)
            }
            isSameYear(now, messageTime) -> {
                SimpleDateFormat("d MMM", Locale.getDefault()).format(messageTime.time)
            }
            else -> {
                SimpleDateFormat("d MMM yy", Locale.getDefault()).format(messageTime.time)
            }
        }
    }

    fun getRandomColor(input: String): Int {
        val colors = listOf(
            Color.parseColor("#0CA3AE"),
            Color.parseColor("#FF564D"),
            Color.parseColor("#6955D2"),
            Color.parseColor("#4FA784"),
            Color.parseColor("#1A73E8")
        )
        val index = (input.hashCode() and 0x7FFFFFFF) % colors.size
        return colors[index]
    }


}
