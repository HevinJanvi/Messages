package com.test.messages.demo.ui.send

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import com.test.messages.demo.Util.SmsSender
import com.test.messages.demo.data.Model.ConversationItem

class SmsSend(
    private val context: Context,
    private val messagingUtils: MessageUtils
) {
    private val application = context.applicationContext as Application

    fun sendSmsMessage(
        text: String,
        addresses: Set<String>,
        subId: Int,
        requireDeliveryReport: Boolean,
        messageId: Long? = null
    ) {
        if (addresses.isEmpty()) return

        for (address in addresses) {
            val personalThreadId = getThreadId(context,setOf(address))
            val messageUri = messagingUtils.insertSmsMessage(
                subId = subId,
                dest = address,
                text = text,
                timestamp = System.currentTimeMillis(),
                threadId = personalThreadId,
                messageId = messageId
            )

            try {
                SmsSender.getInstance(application).sendMessage(
                    subId = subId,
                    destination = address,
                    body = text,
                    serviceCenter = null,
                    requireDeliveryReport = requireDeliveryReport,
                    messageUri = messageUri
                )
            } catch (e: Exception) {
                Log.d("SmsMessageSender", "Failed to send message to $address", e)
            }
        }
    }

    fun resendMessage(message: ConversationItem, numbers: String) {
        val addresses = numbers.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val subId = SmsManager.getDefaultSmsSubscriptionId()

        sendSmsMessage(
            text = message.body ?: return,
            addresses = addresses,
            subId = subId,
            requireDeliveryReport = false,
            messageId = message.id
        )
    }

    @SuppressLint("NewApi")
    fun getThreadId(context: Context,addresses: Set<String>): Long {
        return try {
            Telephony.Threads.getOrCreateThreadId(context, addresses)
        } catch (e: Exception) {
            0L
        }
    }


}
