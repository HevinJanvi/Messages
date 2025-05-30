package com.test.messages.demo.Util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.Util.Constants.CATEGORY_ORDER
import com.test.messages.demo.Util.Constants.FONT_SIZE_KEY
import com.test.messages.demo.Util.Constants.KEY_INTRO_SHOWN
import com.test.messages.demo.Util.Constants.KEY_LANGUAGE_SELECTED
import com.test.messages.demo.Util.Constants.KEY_LEFT_SWIPE_ACTION
import com.test.messages.demo.Util.Constants.KEY_NOTIFICATION_OPTION
import com.test.messages.demo.Util.Constants.KEY_RIGHT_SWIPE_ACTION
import com.test.messages.demo.Util.Constants.KEY_SELECTED_LANGUAGE
import com.test.messages.demo.Util.Constants.PREFS_NAME
import com.test.messages.demo.Util.Constants.SHOW_CATEGORIES
import com.test.messages.demo.Util.Constants.SIM_SELECT
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import org.json.JSONArray
import java.time.Instant
import kotlin.math.abs
import kotlin.random.Random


object ViewUtils {
    private const val LOCK_SCREEN_VISIBILITY = "lock_screen_visibility"
    private const val LOCK_SCREEN_SENDER_MESSAGE = 1

    fun RecyclerView.Adapter<*>.autoScrollToStart(
        recyclerView: RecyclerView,
        callBack: () -> Unit
    ) {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return

                if (layoutManager.stackFromEnd) {

                    if (positionStart > 0) {
                        notifyItemChanged(positionStart - 1)
                    }

                    val lastPosition = layoutManager.findLastVisibleItemPosition()
                    if (positionStart >= getItemCount() - 1 && lastPosition == positionStart - 1) {


                        recyclerView.scrollToPosition(positionStart)
                    }
                } else {
                    val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()

                    if (firstVisiblePosition == 0) {
                        recyclerView.scrollToPosition(firstVisiblePosition)
                    }
                }
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return

                if (!layoutManager.stackFromEnd) {
                    onItemRangeInserted(positionStart, itemCount)
                }
            }
        })
    }


    fun generateRandomId(length: Int = 9): Long {
        val millis = Instant.now().toEpochMilli()
        val random = abs(Random(millis).nextLong())
        return random.toString().takeLast(length).toLong()
    }

    fun getLockScreenVisibilitySetting(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(LOCK_SCREEN_VISIBILITY, LOCK_SCREEN_SENDER_MESSAGE)
    }

    fun setLockScreenVisibilitySetting(context: Context, setting: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(LOCK_SCREEN_VISIBILITY, setting).apply()
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    fun isNougatPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    fun getorcreateThreadId(context: Context, address: String): Long {
        return try {
            Telephony.Threads.getOrCreateThreadId(context, address)
        } catch (e: Exception) {
            0L
        }
    }


    fun isShortCodeWithLetters(address: String): Boolean {
        return address.any { it.isLetter() || it == '-' }
    }

//    fun getThreadId(context: Context, sender: String): Long {
//        val uri = Telephony.Sms.CONTENT_URI
//        val projection = arrayOf(Telephony.Sms.THREAD_ID)
//        val selection = "${Telephony.Sms.ADDRESS} = ?"
//        val selectionArgs = arrayOf(sender)
//
//        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
//            ?.use { cursor ->
//                if (cursor.moveToFirst()) {
//                    return cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
//                }
//            }
//        return 0L
//    }

    /* fun isOfferSender(sender: String): Boolean {
         return sender.matches(Regex("^[A-Z-]+$"))
     }*/

    fun isOfferSender(sender: String): Boolean {
        val cleanedSender =
            sender.replace("[^\\d]".toRegex(), "") // Remove everything except digits
        // Check if the cleaned sender matches a phone number pattern (10-15 digits, optional + at the start)
        val isPhoneNumber = cleanedSender.matches(Regex("^\\+?\\d{10,15}$"))
        return !isPhoneNumber
    }



    fun isServiceNumber(number: String): Boolean {
        return number.any { it.isLetter() }
    }

    fun String.extractOtp(): String? {
        val otpRegex = Regex("\\b\\d{4,6}\\b")
        val matchResults = otpRegex.findAll(this)

        return matchResults.mapNotNull { matchResult ->
            val otp = matchResult.value
            if (!isProbablyYear(otp) && isLikelyOtp(this, otp)) {
                otp
            } else null
        }.firstOrNull()
    }

    fun isLikelyOtp(message: String, otp: String): Boolean {
        val nonOtpKeywords = listOf("offer", "best", "balance", "validity", "tariff", "recharge")
        val otpKeywords = listOf(
            "OTP",
            "code",
            "login",
            "verify",
            "confirmation",
            "password",
            "authentication",
            "secure",
            "token"
        )
        val lowerMessage = message.lowercase()
        val containsOtpKeyword = otpKeywords.any { lowerMessage.contains(it, ignoreCase = true) }
        val containsNonOtpKeyword =
            nonOtpKeywords.any { lowerMessage.contains(it, ignoreCase = true) }

        return containsOtpKeyword && !containsNonOtpKeyword
    }

    // Dynamically detect years and avoid them
    fun isProbablyYear(value: String): Boolean {
        val num = value.toIntOrNull() ?: return false
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return num in 1900..(currentYear + 5)
    }


    /*fun String.extractOtp(): String? {
        val otpRegex = Regex("\\b\\d{4,6}\\b")
        val matchResult = otpRegex.find(this)

        return matchResult?.value?.let { otp ->
            if (isLikelyOtp(this, otp)) otp else null
        }
    }*/
    /*  fun isLikelyOtp(message: String, otp: String): Boolean {
            val nonOtpKeywords = listOf("offer", "best", "balance", "validity", "tariff", "recharge")
            val otpKeywords = listOf("OTP", "code", "login", "verify", "confirmation", "password")

            val containsOtpKeyword = otpKeywords.any { message.contains(it, ignoreCase = true) }
            val containsNonOtpKeyword = nonOtpKeywords.any { message.contains(it, ignoreCase = true) }

            return containsOtpKeyword && !containsNonOtpKeyword
        }
    */

    fun View.blinkThen(action: () -> Unit) {
        val anim = AnimationUtils.loadAnimation(context, R.anim.blink)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                action()
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
        this.startAnimation(anim)
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OTP", text)
        clipboard.setPrimaryClip(clip)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Optional: Add logic here if you want to conditionally show the toast
//            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        } else {
            // Always show on Android 12 and below
            Toast.makeText(context, context.getString(R.string.otp_copied), Toast.LENGTH_SHORT).show()
        }
    }


    fun updateMessageCount(context: Context, threadId: Long): Int {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt("msg_count_$threadId", 0) + 1
        prefs.edit().putInt("msg_count_$threadId", count).apply()
        return count
    }


    fun resetMessageCount(context: Context, threadId: Long) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("msg_count_$threadId", 0).apply()
    }

    fun getPreviewOptionActivity(context: Context, threadId: Long): Int {
        val dao = AppDatabase.getDatabase(context).notificationDao()
        return dao.getPreviewOptionNow(threadId) ?: dao.getPreviewOptionforGlobal() ?: 0
    }

    suspend fun getPreviewOption(context: Context, threadId: Long): Int {
        return AppDatabase.getDatabase(context).notificationDao().getPreviewOption(threadId) ?: 0
    }


    fun getUseSIMIdAtNumber(context: Context, number: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("USE_SIM_ID_$number", 0)
    }

    fun saveCategoriesToPrefs(context: Context, categories: List<String>) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val json = JSONArray(categories).toString()
        sharedPrefs.edit().putString(CATEGORY_ORDER, json).apply()
    }

    fun getCategoriesFromPrefs(context: Context): List<String> {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val json = sharedPreferences.getString(CATEGORY_ORDER, null)

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
        return prefs.getString(KEY_SELECTED_LANGUAGE, "") ?: ""
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
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean(SHOW_CATEGORIES, true)
    }

    fun setCategoryEnabled(context: Context, isEnabled: Boolean) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(SHOW_CATEGORIES, isEnabled).apply()
    }

    fun saveSelectedSimId(context: Context,subscriptionId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(SIM_SELECT, subscriptionId)
            .apply()
    }
     fun getSavedSimId(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(SIM_SELECT, SubscriptionManager.getDefaultSmsSubscriptionId())
    }

    fun getSwipeAction(context: Context, isRightSwipe: Boolean): Int {
        val key = if (isRightSwipe) KEY_RIGHT_SWIPE_ACTION else KEY_LEFT_SWIPE_ACTION
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(key, Constants.SWIPE_ARCHIVE)
    }

    fun saveSwipeAction(context: Context, action: Int, isRightSwipe: Boolean) {
        val key = if (isRightSwipe) KEY_RIGHT_SWIPE_ACTION else KEY_LEFT_SWIPE_ACTION
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(key, action)
            .apply()
    }


    fun saveFontSize(context: Context, fontSize: Int) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putInt(FONT_SIZE_KEY, fontSize).apply()
    }

    fun getFontSize(context: Context): Int {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getInt(FONT_SIZE_KEY, Constants.ACTION_NORMAL)
    }

    fun getLanguageName(context: Context, languageCode: String?): String {
        return when (languageCode) {
            "en" -> context.getString(R.string.subtext_english)
            "af" -> context.getString(R.string.subtext_afrikaans)
            "ar" -> context.getString(R.string.subtext_arabic)
            "bn" -> context.getString(R.string.subtext_bangla)
            "fil" -> context.getString(R.string.subtext_fillipino)
            "fr" -> context.getString(R.string.subtext_French)
            "de" -> context.getString(R.string.subtext_German)
            "hi" -> context.getString(R.string.subtext_Indian)
            "in" -> context.getString(R.string.subtext_Indonesia)
            "it" -> context.getString(R.string.subtext_italian)
            "ja" -> context.getString(R.string.subtext_Japanese)
            "ko" -> context.getString(R.string.subtext_Korean)
            "pl" -> context.getString(R.string.subtext_polish)
            "pt" -> context.getString(R.string.subtext_portugal)
            "ru" -> context.getString(R.string.subtext_Russian)
            "es" -> context.getString(R.string.subtext_Spanish)
            "th" -> context.getString(R.string.subtext_thai)
            "tr" -> context.getString(R.string.subtext_turkish)
            "uk" -> context.getString(R.string.subtext_ukrainian)
            "vi" -> context.getString(R.string.subtext_Vietnamese)
            "zh" -> context.getString(R.string.subtext_chinese)
            else -> languageCode ?: ""
        }
    }

    val countryCodes = listOf(
        "0", "+1", "+7", "+20", "+27", "+30", "+31", "+32", "+33", "+34", "+36", "+39",
        "+40", "+41", "+43", "+44", "+45", "+46", "+47", "+48", "+49", "+51", "+52",
        "+53", "+54", "+55", "+56", "+57", "+58", "+60", "+61", "+62", "+63", "+64",
        "+65", "+66", "+81", "+82", "+84", "+86", "+90", "+91", "+92", "+93", "+94",
        "+95", "+98", "+211", "+212", "+213", "+216", "+218", "+220", "+221", "+222",
        "+223", "+224", "+225", "+226", "+227", "+228", "+229", "+230", "+231", "+232",
        "+233", "+234", "+235", "+236", "+237", "+238", "+239", "+240", "+241", "+242",
        "+243", "+244", "+245", "+246", "+247", "+248", "+249", "+250", "+251", "+252",
        "+253", "+254", "+255", "+256", "+257", "+258", "+260", "+261", "+262", "+263",
        "+264", "+265", "+266", "+267", "+268", "+269", "+290", "+291", "+297", "+298",
        "+299", "+350", "+351", "+352", "+353", "+354", "+355", "+356", "+357", "+358",
        "+359", "+370", "+371", "+372", "+373", "+374", "+375", "+376", "+377", "+378",
        "+379", "+380", "+381", "+382", "+383", "+385", "+386", "+387", "+389", "+420",
        "+421", "+423", "+500", "+501", "+502", "+503", "+504", "+505", "+506", "+507",
        "+508", "+509", "+590", "+591", "+592", "+593", "+594", "+595", "+596", "+597",
        "+598", "+599", "+670", "+672", "+673", "+674", "+675", "+676", "+677", "+678",
        "+679", "+680", "+681", "+682", "+683", "+685", "+686", "+687", "+688", "+689",
        "+690", "+691", "+692", "+850", "+852", "+853", "+855", "+856", "+880", "+886",
        "+960", "+961", "+962", "+963", "+964", "+965", "+966", "+967", "+968", "+970",
        "+971", "+972", "+973", "+974", "+975", "+976", "+977", "+992", "+993", "+994",
        "+995", "+996", "+998"
    )

    fun String.removeCountryCode(): String {
        for (code in countryCodes.sortedByDescending { it.length }) {
            if (this.startsWith(code)) {
                return this.replaceFirst(code, "")
            }
        }
        return this
    }


}

