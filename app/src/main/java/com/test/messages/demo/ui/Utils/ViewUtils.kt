package com.test.messages.demo.ui.Utils

import android.content.Context
import androidx.appcompat.app.AppCompatActivity


object ViewUtils {

    val PREF_NAME = "notification_prefs"
    private const val KEY_NOTIFICATION_OPTION = "notification_option"
    private const val KEY_LANGUAGE_SELECTED = "language_selected"
    private const val KEY_SELECTED_LANGUAGE = "selected_language"
    private const val KEY_INTRO_SHOWN = "intro_shown"
    fun isOfferSender(sender: String): Boolean {
        return sender.matches(Regex("^[A-Z-]+$"))
    }

     fun isServiceNumber(number: String): Boolean {
         return number.any { it.isLetter() }
     }

    fun updateMessageCount(context: Context, threadId: Long): Int {
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("msg_count_$threadId", 0) + 1
        prefs.edit().putInt("msg_count_$threadId", count).apply()
        return count
    }

    fun resetMessageCount(context: Context, threadId: Long) {
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("msg_count_$threadId", 0).apply()
    }

    fun saveNotificationOption(context: Context, option: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_NOTIFICATION_OPTION, option).apply()
    }

    fun getNotificationOption(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NOTIFICATION_OPTION, 0)
    }

    fun isLanguageSelected(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LANGUAGE_SELECTED, false)
    }

    fun setLanguageSelected(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LANGUAGE_SELECTED, true).apply()
    }

    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_LANGUAGE, "en") ?: "en"
    }
    fun setSelectedLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, language).apply()
    }

    fun isIntroShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_INTRO_SHOWN, false)
    }

    fun setIntroShown(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_INTRO_SHOWN, true).apply()
    }
}

