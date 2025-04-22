//package com.test.messages.demo.data.reciever
//
//import android.annotation.SuppressLint
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.util.Log
//import androidx.core.app.NotificationCompat
//import com.test.messages.demo.R
//import com.test.messages.demo.Util.CommanConstants
//import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
//import com.test.messages.demo.Util.CommanConstants.KEY_SMS_CHANNEL
//import com.test.messages.demo.Util.CommanConstants.NAME
//import com.test.messages.demo.Util.CommanConstants.NUMBER
//import com.test.messages.demo.ui.Activity.ConversationActivity
//import com.test.messages.demo.Util.ViewUtils.updateMessageCount
//
//@SuppressLint("NewApi")
//fun showNotification(context: Context, sender: String, message: String, threadId: Long) {
//    val channelId = "${KEY_SMS_CHANNEL}$sender"
//    val notificationId = threadId.toInt()
//
//    val sharedPreferences =
//        context.getSharedPreferences(CommanConstants.PREFS_NAME, Context.MODE_PRIVATE)
//    val previewOption = sharedPreferences.getInt("preview_$sender", 0)
//    val messageCount = updateMessageCount(context, threadId)
//    val contentTitle: String
//    val contentText: String
//
//    when (previewOption) {
//        0 -> { // Show Sender & Message
//            contentTitle = sender
//            contentText = message
//        }
//
//        1 -> { // Show Only Sender
//            contentTitle = sender
//            contentText = "$messageCount ${context.getString(R.string.new_message)}"
//
//        }
//
//        2 -> { // Hide Contents
//            contentTitle = ""
//            contentText = "$messageCount ${context.getString(R.string.new_messages)}"
//        }
//
//        else -> {
//            contentTitle = sender
//            contentText = message
//        }
//    }
//
//    val intent = Intent(context, ConversationActivity::class.java).apply {
//        putExtra(EXTRA_THREAD_ID, threadId)
//        putExtra(NUMBER, sender)
//        putExtra(NAME, sender)
//        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//    }
//
//    val pendingIntent = PendingIntent.getActivity(
//        context,
//        threadId.toInt(),
//        intent,
//        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//    )
//
//    val notificationManager = context.getSystemService(NotificationManager::class.java)
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        createNotificationChannel(context, sender)
//    }
//
//
//    val notification =
//        NotificationCompat.Builder(context, channelId).setSmallIcon(R.drawable.ic_noti)
//            .setContentTitle(contentTitle).setContentText(contentText)
//            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true)
//            .setContentIntent(pendingIntent).build()
//
//    notificationManager.notify(notificationId, notification)
//}
//
//
