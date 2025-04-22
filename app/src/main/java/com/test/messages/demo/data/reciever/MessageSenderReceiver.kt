package com.test.messages.demo.data.reciever

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.test.messages.demo.Util.MessagesRestoredEvent
import com.test.messages.demo.ui.send.MessageUtils
import com.test.messages.demo.Util.SmsSender
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import org.greenrobot.eventbus.EventBus

class MessageSenderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getStringExtra("threadId") ?: return

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

                    AppDatabase.getDatabase(context).scheduledMessageDao().delete(it)
                    EventBus.getDefault().post(MessagesRestoredEvent(true))

                } catch (e: Exception) {
                }
            }
        }.start()
    }
}