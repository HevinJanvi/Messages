package com.test.messages.demo.Helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.test.messages.demo.data.Database.Scheduled.ScheduledMessage
import com.test.messages.demo.data.reciever.MessageSenderReceiver

object MessageScheduler {
    @RequiresApi(Build.VERSION_CODES.S)
    fun scheduleMessage(context: Context, message: ScheduledMessage) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, MessageSenderReceiver::class.java).apply {
            putExtra("threadId", message.threadId)
            putExtra("messageId", message.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            message.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            message.scheduledTime,
            pendingIntent
        )
    }
}
