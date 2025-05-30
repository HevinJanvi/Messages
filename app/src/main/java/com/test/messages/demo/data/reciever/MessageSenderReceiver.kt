package com.test.messages.demo.data.reciever

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.test.messages.demo.Util.Constants.GROUP_SEPARATOR
import com.test.messages.demo.Util.MessagesRestoredEvent
import com.test.messages.demo.ui.send.MessageUtils
import com.test.messages.demo.ui.send.SmsSender
import com.test.messages.demo.ui.send.queryThreadIdForSingleAddress
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import org.greenrobot.eventbus.EventBus

class MessageSenderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("messageId", -1)
        val threadId = intent.getIntExtra("threadId", -1)
        if (id == -1) {
            return
        }

        Thread {
            try {
                val db = AppDatabase.getDatabase(context)
                val message = db.scheduledMessageDao().getMessageById1(id)

                if (message == null) {
                    return@Thread
                }

                val messagingUtils = MessageUtils(context)
                val subId = message.subscriptionId
                val recipients =
                    message.recipientNumber.split(GROUP_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }

                for (recipient in recipients) {
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

                val groupRecipientSet = recipients.toSet()
                val groupThreadIdActual = Telephony.Threads.getOrCreateThreadId(context, groupRecipientSet)
                val groupMessageUri = messagingUtils.insertSmsMessage(
                    subId = subId,
                    dest = recipients.joinToString(GROUP_SEPARATOR),
                    text = message.message,
                    timestamp = System.currentTimeMillis(),
                    threadId = groupThreadIdActual,
                    status = Telephony.Sms.Sent.STATUS_COMPLETE,
                    type = Telephony.Sms.Sent.MESSAGE_TYPE_SENT,
                    messageId = null
                )

                SmsSender.getInstance(context.applicationContext as Application).sendMessage(
                    subId = subId,
                    destination = recipients.joinToString(GROUP_SEPARATOR),
                    body = message.message,
                    serviceCenter = null,
                    requireDeliveryReport = false,
                    messageUri = groupMessageUri
                )

                db.scheduledMessageDao().delete(message)
                EventBus.getDefault().post(MessagesRestoredEvent(true))

            } catch (e: Exception) {
            }
        }.start()
    }



}
