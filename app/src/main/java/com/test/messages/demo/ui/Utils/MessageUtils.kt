package com.test.messages.demo.ui.Utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony.Sms
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import com.klinker.android.send_message.Settings
import com.test.messages.demo.R
import com.test.messages.demo.data.SmsException
import com.test.messages.demo.data.SmsException.Companion.ERROR_PERSISTING_MESSAGE

class MessageUtils(val context: Context) {

    fun insertSmsMessage(
        subId: Int, dest: String, text: String, timestamp: Long, threadId: Long,
        status: Int = Sms.STATUS_NONE, type: Int = Sms.MESSAGE_TYPE_OUTBOX, messageId: Long? = null
    ): Uri {
        val response: Uri?
        val values = ContentValues().apply {
            put(Sms.ADDRESS, dest)
            put(Sms.DATE, timestamp)
            put(Sms.READ, 1)
            put(Sms.SEEN, 1)
            put(Sms.BODY, text)

            if (subId != Settings.DEFAULT_SUBSCRIPTION_ID) {
                put(Sms.SUBSCRIPTION_ID, subId)
            }

            if (status != Sms.STATUS_NONE) {
                put(Sms.STATUS, status)
            }
            if (type != Sms.MESSAGE_TYPE_ALL) {
                put(Sms.TYPE, type)
            }
            if (threadId != -1L) {
                put(Sms.THREAD_ID, threadId)
            }
        }

        try {
            if (messageId != null) {
                val selection = "${Sms._ID} = ?"
                val selectionArgs = arrayOf(messageId.toString())
                val count = context.contentResolver.update(Sms.CONTENT_URI, values, selection, selectionArgs)
                 if (count > 0) {
                    response = Uri.parse("${Sms.CONTENT_URI}/${messageId}")
                } else {
                    response = null
                }
            } else {
                response = context.contentResolver.insert(Sms.CONTENT_URI, values)
            }
        } catch (e: Exception) {
            throw SmsException(ERROR_PERSISTING_MESSAGE, e)

        }
        return response ?: throw SmsException(ERROR_PERSISTING_MESSAGE)
    }




    fun updateSmsMessageSendingStatus(messageUri: Uri?, type: Int) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(Sms.Outbox.TYPE, type)
        }
        Log.d("TAG", "updateSmsMessageSendingStatus:- " +messageUri)

        try {
            if (messageUri != null) {
                resolver.update(messageUri, values, null, null)
            } else {

                val cursor = resolver.query(Sms.Outbox.CONTENT_URI, null, null, null, null)
                cursor?.use {
                    if (cursor.moveToFirst()) {
                        @SuppressLint("Range")
                        val id = cursor.getString(cursor.getColumnIndex(Sms.Outbox._ID))
                        val selection = "${Sms._ID} = ?"
                        val selectionArgs = arrayOf(id.toString())
                        resolver.update(Sms.Outbox.CONTENT_URI, values, selection, selectionArgs)
                    }
                }
            }
        } catch (e: Exception) {
//            context.showErrorToast(e)
        }
    }

    fun getSmsMessageFromDeliveryReport(intent: Intent): SmsMessage? {
        val pdu = intent.getByteArrayExtra("pdu")
        val format = intent.getStringExtra("format")
        return SmsMessage.createFromPdu(pdu, format)
    }

    fun maybeShowErrorToast(resultCode: Int, errorCode: Int) {
        if (resultCode != Activity.RESULT_OK) {
            val msg = if (errorCode != SendStatusReceiver.NO_ERROR_CODE) {
//                context.getString(R.string.carrier_error)
            } else {
                when (resultCode) {
                    SmsManager.RESULT_ERROR_NO_SERVICE -> context.getString(R.string.error_service_unavailable)
                    SmsManager.RESULT_ERROR_RADIO_OFF -> context.getString(R.string.error_radio_turn_off)
                    SmsManager.RESULT_NO_DEFAULT_SMS_APP -> context.getString(R.string.simcard_not_available)
                    else -> context.getString(R.string.unknown_error_sending_message, resultCode)
                }
            }
//            Handler(Looper.getMainLooper()).post {
//                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
//            }
        } else {
        }
    }


    companion object {
        const val ADDRESS_SEPARATOR = "|"
    }
}
