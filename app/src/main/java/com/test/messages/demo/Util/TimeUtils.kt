package com.test.messages.demo.Util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import com.test.messages.demo.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object TimeUtils {

    fun formatTimestamp(cntxt: Context,timestamp: Long): String {
        val context = Locale.getDefault()
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            isSameDay(now, messageTime) -> {
                SimpleDateFormat("h:mm a", context).format(messageTime.time)
            }
            isYesterday(now, messageTime) -> {
                cntxt.getString(R.string.yesterday)
            }
            isSameYear(now, messageTime) -> {
                SimpleDateFormat("d MMM", context).format(messageTime.time)
            }
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

        return daysDiff in 2..6
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
        )
        val index = (input.hashCode() and 0x7FFFFFFF) % colors.size
        return colors[index]
    }

    fun formatHeaderDate(context: Context,timestamp: Long): String {
        val date = Date(timestamp)
        val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
        return dateTimeFormat.format(date)
    }

//    fun formatHeaderDate(context: Activity, timestamp: Long): String {
//        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
//        val today = Calendar.getInstance().apply {
//            set(Calendar.HOUR_OF_DAY, 0)
//            set(Calendar.MINUTE, 0)
//            set(Calendar.SECOND, 0)
//            set(Calendar.MILLISECOND, 0)
//        }
//        val yesterday = Calendar.getInstance().apply {
//            add(Calendar.DAY_OF_YEAR, -1)
//            set(Calendar.HOUR_OF_DAY, 0)
//            set(Calendar.MINUTE, 0)
//            set(Calendar.SECOND, 0)
//            set(Calendar.MILLISECOND, 0)
//        }
//
//        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
//        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
//
//        return when {
//            timestamp >= today.timeInMillis -> context.getString(
//                R.string.today,
//                timeFormat.format(messageDate.time)
//            )
//            timestamp >= yesterday.timeInMillis -> context.getString(
//                R.string.yesterday,
//                timeFormat.format(messageDate.time)
//            )
//            else -> "${dateFormat.format(messageDate.time)}, ${timeFormat.format(messageDate.time)}"
//        }
//    }

}
