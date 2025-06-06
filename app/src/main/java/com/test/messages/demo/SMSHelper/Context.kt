package com.test.messages.demo.SMSHelper

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.test.messages.demo.Helper.NotificationHelper


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

@SuppressLint("NewApi")
fun Context.getThreadIdSingle(addresses: String): Long {
    return try {
        Telephony.Threads.getOrCreateThreadId(this, addresses)
    } catch (e: Exception) {
        0L
    }
}

fun Context.queryThreadIdForSingleAddress(address: String): Long {
    val uri = Uri.parse("content://mms-sms/threadID")
    val encodedAddress = Uri.encode(address)
    val fullUri = Uri.withAppendedPath(uri, encodedAddress)

    return try {
        contentResolver.query(fullUri, arrayOf("_id"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
            } else {
                Telephony.Threads.getOrCreateThreadId(this, address)
            }
        } ?: 0L
    } catch (e: Exception) {
        e.printStackTrace()
        0L
    }
}



