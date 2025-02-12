package com.test.messages.demo.ui.Utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import com.klinker.android.send_message.Settings
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


    /** Send an SMS message given [text] and [addresses]. A [SmsException] is thrown in case any errors occur. */
//    fun sendSmsMessage(
//        text: String, addresses: Set<String>, subId: Int, requireDeliveryReport: Boolean, messageId: Long? = null
//    ) {
//        if (addresses.size > 1) {
//            // insert a dummy message for this thread if it is a group message
//            val broadCastThreadId = context.getThreadId(addresses.toSet())
//            val mergedAddresses = addresses.joinToString(ADDRESS_SEPARATOR)
//            insertSmsMessage(
//                subId = subId, dest = mergedAddresses, text = text,
//                timestamp = System.currentTimeMillis(), threadId = broadCastThreadId,
//                status = Sms.Sent.STATUS_COMPLETE, type = Sms.Sent.MESSAGE_TYPE_SENT,
//                messageId = messageId
//            )
//        }
//
//        for (address in addresses) {
//            val threadId = context.getThreadId(address)
//            val messageUri = insertSmsMessage(
//                subId = subId, dest = address, text = text,
//                timestamp = System.currentTimeMillis(), threadId = threadId,
//                messageId = messageId
//            )
//            try {
//                context.smsSender.sendMessage(
//                    subId = subId, destination = address, body = text, serviceCenter = null,
//                    requireDeliveryReport = requireDeliveryReport, messageUri = messageUri
//                )
//            } catch (e: Exception) {
//                updateSmsMessageSendingStatus(messageUri, Sms.Outbox.MESSAGE_TYPE_FAILED)
//                throw e // propagate error to caller
//            }
//        }
//    }

    fun updateSmsMessageSendingStatus(messageUri: Uri?, type: Int) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(Sms.Outbox.TYPE, type)
        }

        try {
            if (messageUri != null) {
                resolver.update(messageUri, values, null, null)
            } else {
                // mark latest sms as sent, need to check if this is still necessary (or reliable)
                // as this was taken from android-smsmms. The messageUri shouldn't be null anyway
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

//    @Deprecated("TODO: Move/rewrite MMS code into the app.")
//    fun sendMmsMessage(text: String, addresses: List<String>, attachment: Attachment?, settings: Settings, messageId: Long? = null) {
//        val transaction = Transaction(context, settings)
//        val message = Message(text, addresses.toTypedArray())
//
//        if (attachment != null) {
//            try {
//                val uri = attachment.getUri()
//                context.contentResolver.openInputStream(uri)?.use {
//                    val bytes = it.readBytes()
//                    val mimeType = if (attachment.mimetype.isPlainTextMimeType()) {
//                        "application/txt"
//                    } else {
//                        attachment.mimetype
//                    }
//                    val name = attachment.filename
//                    message.addMedia(bytes, mimeType, name, name)
//                }
//            } catch (e: Exception) {
////                context.showErrorToast(e)
//            } catch (e: Error) {
////                context.showErrorToast(e.localizedMessage ?: context.getString(com.simplemobiletools.commons.R.string.unknown_error_occurred))
//            }
//        }
//
//        val mmsSentIntent = Intent(context, MmsSentReceiver::class.java)
//        mmsSentIntent.putExtra(MmsSentReceiver.EXTRA_ORIGINAL_RESENT_MESSAGE_ID, messageId)
//        transaction.setExplicitBroadcastForSentMms(mmsSentIntent)
//
//        try {
//            transaction.sendNewMessage(message)
//        } catch (e: Exception) {
////            context.showErrorToast(e)
//        }
//    }



    companion object {
        const val ADDRESS_SEPARATOR = "|"
    }
}
