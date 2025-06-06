package com.test.messages.demo.SMSHelper

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.PhoneNumberUtils
import androidx.annotation.ChecksSdkIntAtLeast
import com.test.messages.demo.data.SmsException
import com.test.messages.demo.data.SmsException.Companion.EMPTY_DESTINATION_ADDRESS
import com.test.messages.demo.data.SmsException.Companion.ERROR_SENDING_MESSAGE
import com.test.messages.demo.data.reciever.SendStatusReceiver
import com.test.messages.demo.data.reciever.SmsStatusSentReceiver

class SmsSender(val app: Application) {

    private val sendMultipartSmsAsSeparateMessages = false
    fun sendMessage(
        subId: Int, destination: String, body: String, serviceCenter: String?,
        requireDeliveryReport: Boolean, messageUri: Uri,groupId:Long=-1L,groupUri:Uri?=null
    ) {
        var dest = destination
        if (body.isEmpty()) {
            throw IllegalArgumentException("SmsSender: empty text message")
        }
        dest = PhoneNumberUtils.stripSeparators(dest)
        if (dest.isEmpty()) {
            throw SmsException(EMPTY_DESTINATION_ADDRESS)
        }
        val smsManager = getSmsManager(subId)
        val messages = smsManager.divideMessage(body)
        if (messages == null || messages.size < 1) {
            throw SmsException(ERROR_SENDING_MESSAGE)
        }
        sendInternal(
            subId, dest, messages, serviceCenter, requireDeliveryReport, messageUri,groupId,groupUri
        )
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun isSPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private fun sendInternal(
        subId: Int, dest: String,
        messages: ArrayList<String>, serviceCenter: String?,
        requireDeliveryReport: Boolean, messageUri: Uri,groupId:Long=-1L,groupUri:Uri?=null
    ) {
        val smsManager = getSmsManager(subId)
        val messageCount = messages.size
        val deliveryIntents = ArrayList<PendingIntent?>(messageCount)
        val sentIntents = ArrayList<PendingIntent>(messageCount)

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (isSPlus()) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }

        for (i in 0 until messageCount) {

            val partId = if (messageCount <= 1) 0 else i + 1
            if (requireDeliveryReport && i == messageCount - 1) {

            } else {
                deliveryIntents.add(null)
            }
            sentIntents.add(
                PendingIntent.getBroadcast(
                    app,
                    partId,
                    getSendStatusIntent(messageUri, subId,groupId,groupUri),
                    flags
                )
            )
        }
        try {
            if (sendMultipartSmsAsSeparateMessages) {
                for (i in 0 until messageCount) {
                    smsManager.sendTextMessage(
                        dest,
                        serviceCenter,
                        messages[i],
                        sentIntents[i],
                        deliveryIntents[i]
                    )
                }
            } else {
                smsManager.sendMultipartTextMessage(
                    dest, serviceCenter, messages, sentIntents, deliveryIntents
                )
            }
        } catch (e: Exception) {
            throw SmsException(ERROR_SENDING_MESSAGE, e)
        }
    }

    private fun getSendStatusIntent(requestUri: Uri, subId: Int,groupId:Long=-1L,groupUri:Uri?=null): Intent {
        val intent = Intent(SendStatusReceiver.SMS_SENT_ACTION, requestUri, app, SmsStatusSentReceiver::class.java)
        intent.putExtra(SendStatusReceiver.EXTRA_SUB_ID, subId)
        intent.putExtra(SendStatusReceiver.EXTRA_GROUP_ID, groupId)
        if(groupUri!=null)
            intent.putExtra(SendStatusReceiver.EXTRA_GROUP_URI, groupUri.toString())
        return intent
    }

    companion object {
        private var instance: SmsSender? = null
        fun getInstance(app: Application): SmsSender {
            if (instance == null) {
                instance = SmsSender(app)
            }
            return instance!!
        }
    }
}
