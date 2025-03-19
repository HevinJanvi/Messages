package com.test.messages.demo.ui.reciever

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.test.messages.demo.ui.Utils.MessageUtils
import com.test.messages.demo.ui.Utils.SmsSender
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase

class MessageSenderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getStringExtra("threadId") ?: return
        Log.d("MessageSenderReceiver", "Received threadId: $threadId")

        val messagingUtils = MessageUtils(context)

        Thread {
            val message = AppDatabase.getDatabase(context).scheduledMessageDao().getMessageById(threadId)
            message?.let {
                val subId = SmsManager.getDefaultSmsSubscriptionId()
                val personalThreadId = it.threadId.toLongOrNull() ?: -1L

                val messageUri = messagingUtils.insertSmsMessage(
                    subId = subId,
                    dest = it.recipient,
                    text = it.message,
                    timestamp = System.currentTimeMillis(),
                    threadId = personalThreadId,
                )

                try {
                    SmsSender.getInstance(context.applicationContext as Application).sendMessage(
                        subId = subId,
                        destination = it.recipient,
                        body = it.message,
                        serviceCenter = null,
                        requireDeliveryReport = false,
                        messageUri = messageUri
                    )
                    Log.d("TAG", "Scheduled message sent to: ${it.recipient} at ${System.currentTimeMillis()}")

                    AppDatabase.getDatabase(context).scheduledMessageDao().delete(it)
                } catch (e: Exception) {
                    Log.d("TAG", "Failed to send scheduled message to ${it.recipient}", e)
                }
            }
        }.start()
    }
}