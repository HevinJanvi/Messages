package com.test.messages.demo.ui.Utils

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.test.messages.demo.ui.Activity.SmsPermissionActivity

object SmsPermissionUtils {

    fun isDefaultSmsApp(context: Context): Boolean {
        return Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    }

    fun checkAndRedirectIfNotDefault(context: Context): Boolean {
        return if (!isDefaultSmsApp(context)) {
            val intent = Intent(context, SmsPermissionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            false
        } else {
            true
        }
    }
}
