package com.test.messages.demo.data.reciever

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import com.test.messages.demo.Util.MessagesRestoredEvent
import com.test.messages.demo.ui.send.MessageUtils
import com.test.messages.demo.Util.SmsSender
import com.test.messages.demo.ui.send.getThreadId
import com.test.messages.demo.ui.send.queryThreadIdForSingleAddress
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import org.greenrobot.eventbus.EventBus

class MessageSenderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("messageId", -1)
        val threadId = intent.getIntExtra("threadId", -1)
        Log.d("ScheduleDebug", "Receiver triggered with ID: $id")
        Log.d("ScheduleDebug", "Receiver triggered with threadId: $threadId")

        if (id == -1) {
            Log.e("ScheduleDebug", "Invalid ID in intent")
            return
        }

        Thread {
            try {
                val db = AppDatabase.getDatabase(context)
                val message = db.scheduledMessageDao().getMessageById1(id)

                if (message == null) {
                    Log.e("ScheduleDebug", "Message not found for ID: $id")
                    return@Thread
                }

                Log.d(
                    "ScheduleDebug",
                    "Message found: ${message.message} to ${message.recipientNumber}"
                )

                val messagingUtils = MessageUtils(context)
                val subId = message.subscriptionId

                // Split recipients by comma and send individually
                val recipients =
                    message.recipientNumber.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                for (recipient in recipients) {
//                    val recipientThreadId = context.getThreadId(setOf(message.recipientNumber))
                    val recipientThreadId = context.queryThreadIdForSingleAddress(recipient)

                    val messageUri = messagingUtils.insertSmsMessage(
                        subId = subId,
                        dest = recipient,
                        text = message.message,
                        timestamp = System.currentTimeMillis(),
                        threadId = recipientThreadId,
                        status = Telephony.Sms.Sent.STATUS_COMPLETE,
                        type = Telephony.Sms.Sent.MESSAGE_TYPE_SENT,
                        messageId = null
                    )

                    SmsSender.getInstance(context.applicationContext as Application).sendMessage(
                        subId = subId,
                        destination = recipient,
                        body = message.message,
                        serviceCenter = null,
                        requireDeliveryReport = false,
                        messageUri = messageUri
                    )
                }


//                val groupThreadId = message.threadId  // Make sure this was stored when scheduling
                val groupRecipientSet = recipients.toSet()
                val groupThreadIdActual = Telephony.Threads.getOrCreateThreadId(context, groupRecipientSet)
                val groupMessageUri = messagingUtils.insertSmsMessage(
                    subId = subId,
                    dest = recipients.joinToString(","), // comma-separated
                    text = message.message,
                    timestamp = System.currentTimeMillis(),
                    threadId = groupThreadIdActual,
                    status = Telephony.Sms.Sent.STATUS_COMPLETE,
                    type = Telephony.Sms.Sent.MESSAGE_TYPE_SENT,
                    messageId = null
                )

                SmsSender.getInstance(context.applicationContext as Application).sendMessage(
                    subId = subId,
                    destination = recipients.joinToString(","),
                    body = message.message,
                    serviceCenter = null,
                    requireDeliveryReport = false,
                    messageUri = groupMessageUri
                )







                // After sending to all recipients, delete the scheduled message from DB
                db.scheduledMessageDao().delete(message)
                EventBus.getDefault().post(MessagesRestoredEvent(true))

                Log.d("ScheduleDebug", "All messages sent & scheduled message deleted from DB")

            } catch (e: Exception) {
                Log.e("ScheduleDebug", "Failed to send scheduled message", e)
            }
        }.start()
    }



}

/*
class MessageSenderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("messageId", -1)
        Log.d("ScheduleDebug", "Receiver triggered with ID: $id")

        if (id == -1) {
            Log.e("ScheduleDebug", "Invalid ID in intent")
            return
        }

        Thread {
            try {
                val db = AppDatabase.getDatabase(context)
                val message = db.scheduledMessageDao().getMessageById1(id)

                if (message == null) {
                    Log.e("ScheduleDebug", "Message not found for ID: $id")
                    return@Thread
                }

                Log.d("ScheduleDebug", "Message found: ${message.message} to ${message.recipientNumber}")

                val messagingUtils = MessageUtils(context)
                val subId = message.subscriptionId
                val messageUri = messagingUtils.insertSmsMessage(
                    subId = subId,
                    dest = message.recipientNumber,
                    text = message.message,
                    timestamp = System.currentTimeMillis(),
                    threadId = message.threadId.toLongOrNull() ?: -1L,
                    status = Telephony.Sms.Sent.STATUS_COMPLETE,
                    type = Telephony.Sms.Sent.MESSAGE_TYPE_SENT,
                    messageId = null
                )

                SmsSender.getInstance(context.applicationContext as Application).sendMessage(
                    subId = subId,
                    destination = message.recipientNumber,
                    body = message.message,
                    serviceCenter = null,
                    requireDeliveryReport = false,
                    messageUri = messageUri
                )

                db.scheduledMessageDao().delete(message)
                EventBus.getDefault().post(MessagesRestoredEvent(true))

                Log.d("ScheduleDebug", "Message sent & deleted from DB")

            } catch (e: Exception) {
                Log.e("ScheduleDebug", "Failed to send scheduled message", e)
            }
        }.start()
    }
}
*/

/*
class MessageSenderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getStringExtra("threadId") ?: return

        val messagingUtils = MessageUtils(context)

        Thread {
            val message = AppDatabase.getDatabase(context).scheduledMessageDao().getMessageById(threadId)
            message?.let {
                val subId = message.subscriptionId
                val personalThreadId = it.threadId.toLongOrNull() ?: -1L

                val messageUri = messagingUtils.insertSmsMessage(
                    subId = subId,
                    dest = it.recipientNumber,
                    text = it.message,
                    timestamp = System.currentTimeMillis(),
                    threadId = personalThreadId,
                    status = Telephony.Sms.Sent.STATUS_COMPLETE,
                    type = Telephony.Sms.Sent.MESSAGE_TYPE_SENT,
                    messageId = null
                )

                try {
                    SmsSender.getInstance(context.applicationContext as Application).sendMessage(
                        subId = subId,
                        destination = it.recipientNumber,
                        body = it.message,
                        serviceCenter = null,
                        requireDeliveryReport = false,
                        messageUri = messageUri
                    )
                    Log.d("TAG", "onReceive:delete db ")
                    AppDatabase.getDatabase(context).scheduledMessageDao().delete(it)
                    EventBus.getDefault().post(MessagesRestoredEvent(true))

                } catch (e: Exception) {
                }
            }
        }.start()
    }
}*/
