package com.test.messages.demo.ui.reciever

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi
import com.test.messages.demo.repository.MessageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MessageRepository

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            var address = ""
            var body = ""
            var subject = ""
            var date = 0L
            var threadId = 0L
            var status = Telephony.Sms.STATUS_NONE
            val type = Telephony.Sms.MESSAGE_TYPE_INBOX
            val read = 0
            val subscriptionId = intent.getIntExtra("subscription", -1) // Get SIM ID

            messages.forEach {
                address = it.originatingAddress ?: ""
                subject = it.pseudoSubject
                status = it.status
                body += it.messageBody
                date = it.timestampMillis
            }

            CoroutineScope(Dispatchers.IO).launch {
                val prefs = context.getSharedPreferences("block_prefs", Context.MODE_PRIVATE)
                val isDropMessagesEnabled = prefs.getBoolean("drop_messages", false)
                val isDeleted = repository.isDeletedConversation(address)

                if (isDeleted) {
                    if (isDropMessagesEnabled) {
                        Log.d("SmsReceiver", "Drop Messages is ON, ignoring message from: $address")
                        return@launch  // Exit without inserting message
                    }
                    threadId = getThreadId(context,address)
                    insertMessageIntoSystemDatabase(context, address, subject, body, date, threadId, read, type, subscriptionId, status)
                    threadId = getThreadId(context,address)
                    val isAlreadyBlocked = repository.isBlockedConversation(threadId)
                    if (!isAlreadyBlocked) {
                        repository.blockConversation(threadId, address)
                        repository.removeOldBlockedThreadIds(address, threadId)
                        Log.d("TAG", "onReceive: Blocked new thread ID $threadId and removed old ones for $address")
                    } else {
                        Log.d("TAG", "onReceive: Thread ID $threadId is already blocked, skipping...")
                    }

                } else {
                    Log.d("SmsReceiver", "Message from unblocked number: $address")
                    threadId = getThreadId(context, address)
                    insertMessageIntoSystemDatabase(context, address, subject, body, date, threadId, read, type, subscriptionId, status)
                }
                repository.getMessages()
                repository.getConversation(threadId)
            }
        }
    }


    private fun insertMessageIntoSystemDatabase(
        context: Context,
        address: String,
        subject: String,
        body: String,
        date: Long,
        threadId: Long,
        read: Int,
        type: Int,
        subscriptionId: Int,
        status: Int
    ) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, date)
                put(Telephony.Sms.READ, read)
                put(Telephony.Sms.TYPE, type)
                put(Telephony.Sms.STATUS, status)
                put(Telephony.Sms.THREAD_ID, threadId)
                put(Telephony.Sms.SUBSCRIPTION_ID, subscriptionId)
                put(Telephony.Sms.SUBJECT, subject)
            }

            val uri = context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            if (uri != null) {
                Log.d("SmsReceiver", "SMS inserted into system database: $uri")
            } else {
                Log.e("SmsReceiver", "Failed to insert SMS into system database")
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error inserting SMS: ${e.message}")
        }
    }

    private fun getThreadId(context: Context, sender: String): Long {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.THREAD_ID)
        val selection = "${Telephony.Sms.ADDRESS} = ?"
        val selectionArgs = arrayOf(sender)

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                }
            }
        return 0L
    }


}