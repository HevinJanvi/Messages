package com.test.messages.demo.ui.reciever

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.test.messages.demo.R
import com.test.messages.demo.ui.Activity.ConversationActivity
import com.test.messages.demo.ui.Utils.ViewUtils.updateMessageCount


/*
fun showNotification(context: Context, sender: String, message: String, threadId: Long) {
    val channelId = "sms_channel_$sender"
    val notificationId = threadId.toInt()

    val sharedPreferences = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    val previewOption = sharedPreferences.getInt("preview_$sender", 0)

    val messageCount = updateMessageCount(context, threadId)

    val contentTitle = sender
    val contentText = when (previewOption) {
        1 -> "New message" // Show only sender, no message content
        2 -> " new messages" // Hide content but show count
        else -> if (messageCount > 1) "($messageCount) new messages: $message" else message // Show sender & message
    }

    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setClassName(context.packageName, ".ConversationActivity")
        putExtra("thread_id", threadId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        threadId.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notificationManager = context.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(context, sender)
    }

    val inboxStyle = NotificationCompat.InboxStyle()
        .setBigContentTitle(sender)
        .addLine(message)
        .setSummaryText("($messageCount) new messages")

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_noti)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setStyle(inboxStyle)
        .setGroup("sms_group_$sender")
        .setContentIntent(pendingIntent)
        .build()

    notificationManager.notify(notificationId, notification)
}
*/

@SuppressLint("NewApi")
fun showNotification(context: Context, sender: String, message: String, threadId: Long) {
    val channelId = "sms_channel_$sender"
    val notificationId = threadId.toInt()

    val sharedPreferences = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    val previewOption = sharedPreferences.getInt("preview_$sender", 0)

    // Get updated message count
    val messageCount = updateMessageCount(context, threadId)

    // Adjust content based on preview settings
    val contentTitle: String
    val contentText: String

    when (previewOption) {
        0 -> { // Show Sender & Message
            contentTitle = sender
            contentText =  message
        }
        1 -> { // Show Only Sender
            contentTitle = sender
            contentText = "$messageCount New message"
        }
        2 -> { // Hide Contents
            contentTitle = ""
            contentText = "$messageCount new messages"
        }
        else -> {
            contentTitle = sender
            contentText = message
        }
    }

    /*val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setClassName(context.packageName, ".ConversationActivity")
        putExtra("thread_id", threadId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }*/
    val intent = Intent(context, ConversationActivity::class.java).apply {
        putExtra("EXTRA_THREAD_ID", threadId)
        putExtra("NUMBER", sender)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        threadId.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notificationManager = context.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(context, sender)
    }


    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_noti)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    notificationManager.notify(notificationId, notification)
}

fun createNotificationChannel(context: Context, contactNumber: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "sms_channel_$contactNumber"  // Unique channel ID per contact
        val channelName = "$contactNumber"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, channelName, importance)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
