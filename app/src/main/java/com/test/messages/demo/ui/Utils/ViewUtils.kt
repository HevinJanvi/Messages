package com.test.messages.demo.ui.Utils

import android.content.Context
import androidx.appcompat.app.AppCompatActivity


object ViewUtils {

    val PREF_NAME = "notification_prefs"
    val PREF_KEY = "notification_preview"
    private const val KEY_NOTIFICATION_OPTION = "notification_option"

    fun isOfferSender(sender: String): Boolean {
        return sender.matches(Regex("^[A-Z-]+$"))
    }

     fun isServiceNumber(number: String): Boolean {
         return number.any { it.isLetter() }
     }

     fun getNotificationPreference(context: Context,contactNumber: String): Int {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        return sharedPreferences.getInt("preview_$contactNumber", 0) // Default: Show Sender & Message
    }
     fun saveNotificationPreference(context: Context,contactNumber: String, option: Int) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        sharedPreferences.edit().putInt("preview_$contactNumber", option).apply()
    }

    fun updateMessageCount(context: Context, threadId: Long): Int {
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("msg_count_$threadId", 0) + 1
        prefs.edit().putInt("msg_count_$threadId", count).apply()
        return count
    }

    fun resetMessageCount(context: Context, threadId: Long) {
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("msg_count_$threadId", 0).apply() // Reset count to 0
    }

    fun saveNotificationOption(context: Context, option: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_NOTIFICATION_OPTION, option).apply()
    }

    fun getNotificationOption(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NOTIFICATION_OPTION, 0) // Default: Show sender & message
    }


}

