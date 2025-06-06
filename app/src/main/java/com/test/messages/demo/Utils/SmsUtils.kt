package com.test.messages.demo.Utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.Telephony
import com.test.messages.demo.Helper.Constants
import com.test.messages.demo.Utils.ViewUtils.removeCountryCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SmsUtils {

    fun markThreadAsRead(context: Context, threadId: Long) {
        if (threadId == -1L) return
        CoroutineScope(Dispatchers.IO).launch {
            val contentValues = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }

            val uri = Telephony.Sms.CONTENT_URI
            val selection = "${Telephony.Sms.THREAD_ID} = ?"
            val selectionArgs = arrayOf(threadId.toString())
            context.contentResolver.update(uri, contentValues, selection, selectionArgs)

        }
    }


    fun createNotificationChannel(context: Context, contactNumber: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "${Constants.KEY_SMS_CHANNEL}${contactNumber.removeCountryCode()}"
            val channelName = "$contactNumber"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotificationChannelGlobal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = Constants.KEY_SMS_CHANNEL
            val channelName = "Default"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
