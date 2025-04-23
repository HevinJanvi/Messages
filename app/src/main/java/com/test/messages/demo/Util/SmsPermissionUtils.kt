package com.test.messages.demo.Util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import com.test.messages.demo.ui.Activity.SmsPermissionActivity

object SmsPermissionUtils {

    fun isDefaultSmsApp(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                roleManager.isRoleHeld(RoleManager.ROLE_SMS)

            } else {
                false
            }
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
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
