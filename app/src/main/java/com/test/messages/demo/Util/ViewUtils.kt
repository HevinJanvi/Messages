package com.test.messages.demo.Util

import android.content.Context
import android.preference.PreferenceManager
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.KEY_INTRO_SHOWN
import com.test.messages.demo.Util.CommanConstants.KEY_LANGUAGE_SELECTED
import com.test.messages.demo.Util.CommanConstants.KEY_LEFT_SWIPE_ACTION
import com.test.messages.demo.Util.CommanConstants.KEY_NOTIFICATION_OPTION
import com.test.messages.demo.Util.CommanConstants.KEY_RIGHT_SWIPE_ACTION
import com.test.messages.demo.Util.CommanConstants.KEY_SELECTED_LANGUAGE
import com.test.messages.demo.Util.CommanConstants.PREFS_NAME
import org.json.JSONArray


object ViewUtils {


    fun isOfferSender(sender: String): Boolean {
        return sender.matches(Regex("^[A-Z-]+$"))
    }

     fun isServiceNumber(number: String): Boolean {
         return number.any { it.isLetter() }
     }

    fun String.extractOtp(): String? {
        val otpRegex = Regex("\\b\\d{4,6}\\b")
        val matchResult = otpRegex.find(this)

        return matchResult?.value?.let { otp ->
            if (isLikelyOtp(this, otp)) otp else null
        }
    }

    fun isLikelyOtp(message: String, otp: String): Boolean {
        val nonOtpKeywords = listOf("offer", "best", "balance", "validity", "tariff", "recharge")
        val otpKeywords = listOf("OTP", "code", "login", "verify", "confirmation", "password")

        val containsOtpKeyword = otpKeywords.any { message.contains(it, ignoreCase = true) }
        val containsNonOtpKeyword = nonOtpKeywords.any { message.contains(it, ignoreCase = true) }

        return containsOtpKeyword && !containsNonOtpKeyword
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

    fun saveCategoriesToPrefs(context: Context, categories: List<String>) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val json = JSONArray(categories).toString()
        sharedPrefs.edit().putString("CATEGORY_ORDER", json).apply()
    }

    fun getCategoriesFromPrefs(context: Context): List<String> {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val json = sharedPreferences.getString("CATEGORY_ORDER", null)

        return if (json != null) {
            val jsonArray = JSONArray(json)
            List(jsonArray.length()) { jsonArray.getString(it) }
        } else {
            listOf(
                context.getString(R.string.inbox),
                context.getString(R.string.personal),
                context.getString(R.string.transactions),
                context.getString(R.string.otps),
                context.getString(R.string.offers)
            )
        }
    }

    fun saveNotificationOption(context: Context, option: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_NOTIFICATION_OPTION, option).apply()
    }


    fun isLanguageSelected(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LANGUAGE_SELECTED, false)
    }

    fun setLanguageSelected(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LANGUAGE_SELECTED, true).apply()
    }

    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_LANGUAGE, "en") ?: "en"
    }
    fun setSelectedLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, language).apply()
    }

    fun isIntroShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_INTRO_SHOWN, false)
    }

    fun setIntroShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_INTRO_SHOWN, true).apply()
    }

    fun isCategoryEnabled(context: Context): Boolean {
        val sharedPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean("SHOW_CATEGORIES", true)
    }

    fun setCategoryEnabled(context: Context, isEnabled: Boolean) {
        val sharedPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("SHOW_CATEGORIES", isEnabled).apply()
    }



    fun getSwipeAction(context: Context, isRightSwipe: Boolean): Int {
        val key = if (isRightSwipe) KEY_RIGHT_SWIPE_ACTION else KEY_LEFT_SWIPE_ACTION
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(key, CommanConstants.SWIPE_ARCHIVE)
    }

    fun saveSwipeAction(context: Context, action: Int, isRightSwipe: Boolean) {
        val key = if (isRightSwipe) KEY_RIGHT_SWIPE_ACTION else KEY_LEFT_SWIPE_ACTION
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(key, action)
            .apply()
    }


}

