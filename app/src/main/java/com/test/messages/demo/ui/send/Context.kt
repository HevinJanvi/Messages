package com.test.messages.demo.ui.send

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.test.messages.demo.Util.NotificationHelper
import com.test.messages.demo.Util.SmsSender


val Context.smsSender get() = SmsSender.getInstance(applicationContext as Application)
val Context.notificationHelper get() = NotificationHelper(this)

val Context.notificationManager: NotificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
fun Context.subscriptionManagerCompat(): SubscriptionManager {
    return getSystemService(SubscriptionManager::class.java)
}

fun Context.hasReadSmsPermission(): Boolean {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
}

fun Context.hasReadContactsPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}
fun Context.hasReadStatePermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this, Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("NewApi")
fun Context.getThreadId(addresses: Set<String>): Long {
    return try {
        Telephony.Threads.getOrCreateThreadId(this, addresses)
    } catch (e: Exception) {
        0L
    }
}
