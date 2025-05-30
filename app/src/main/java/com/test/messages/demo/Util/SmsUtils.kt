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
import com.test.messages.demo.Util.ViewUtils.removeCountryCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SmsUtils {

    fun markThreadAsRead(context: Context, threadId: Long, onComplete: (() -> Unit)? = null) {
        if (threadId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val contentValues = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }

            val uri = Telephony.Sms.CONTENT_URI
            val selection = "${Telephony.Sms.THREAD_ID} = ?"
            val selectionArgs = arrayOf(threadId.toString())

            val updatedRows =
                context.contentResolver.update(uri, contentValues, selection, selectionArgs)
            Log.d("MessageRepository", "Marked $updatedRows messages as read in thread $threadId")
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

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

            val updatedRows =
                context.contentResolver.update(uri, contentValues, selection, selectionArgs)
            Log.d("MessageRepository", "Marked $updatedRows messages as read in thread $threadId")
//            withContext(Dispatchers.Main) {
//                onComplete?.invoke()
//            }
        }
    }

    /* fun markThreadAsRead(context: Context, threadId: Long, onComplete: (() -> Unit)? = null) {
         if (threadId == -1L) return

         val contentValues = ContentValues().apply {
             put(Telephony.Sms.READ, 1)
             put(Telephony.Sms.SEEN, 1)
         }

         val uri = Telephony.Sms.CONTENT_URI
         val selection = "${Telephony.Sms.THREAD_ID} = ?"
         val selectionArgs = arrayOf(threadId.toString())

         val updatedRows = context.contentResolver.update(uri, contentValues, selection, selectionArgs)
         Log.d("MessageRepository", "Marked $updatedRows messages as read in thread $threadId")

         onComplete?.let {
             Handler(Looper.getMainLooper()).post {
                 it()
             }
         }
     }*/

    fun createNotificationChannel(context: Context, contactNumber: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "${CommanConstants.KEY_SMS_CHANNEL}${contactNumber.removeCountryCode()}"
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
