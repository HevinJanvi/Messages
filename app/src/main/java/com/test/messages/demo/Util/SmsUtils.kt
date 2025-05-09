package com.test.messages.demo.Util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log

object SmsUtils {

    fun markThreadAsRead(context: Context, threadId: Long, onComplete: (() -> Unit)? = null) {
        if (threadId == -1L) return

        val contentValues = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }

        val uri = Telephony.Sms.CONTENT_URI
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        val updatedRows = context.contentResolver.update(uri, contentValues, selection, selectionArgs)

        Log.d("SmsUtils", "Marked $updatedRows messages as read in thread $threadId")

        onComplete?.let {
            Handler(Looper.getMainLooper()).post {
                it()
            }
        }
    }

    fun createNotificationChannel(context: Context, contactNumber: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "${CommanConstants.KEY_SMS_CHANNEL}$contactNumber"
            val channelName = "$contactNumber"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotificationChannelGlobal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = CommanConstants.KEY_SMS_CHANNEL
            val channelName = "Default"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
