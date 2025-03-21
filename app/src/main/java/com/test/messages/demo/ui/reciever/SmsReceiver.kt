package com.test.messages.demo.ui.reciever

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
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
    private val messageCounts = mutableMapOf<Long, Int>()

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
                        return@launch
                    }
                    threadId = getThreadId(context, address)
                    insertMessageIntoSystemDatabase(
                        context,
                        address,
                        subject,
                        body,
                        date,
                        threadId,
                        read,
                        type,
                        subscriptionId,
                        status
                    )
                    threadId = getThreadId(context, address)
                    val isAlreadyBlocked = repository.isBlockedConversation(threadId)
                    if (!isAlreadyBlocked) {
                        repository.blockConversation(threadId, address)
                        repository.removeOldBlockedThreadIds(address, threadId)
                    }

                } else {
                    threadId = getThreadId(context, address)
                    insertMessageIntoSystemDatabase(
                        context,
                        address,
                        subject,
                        body,
                        date,
                        threadId,
                        read,
                        type,
                        subscriptionId,
                        status
                    )
                }

                val sharedPreferences = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                val isGlobalWakeEnabled = sharedPreferences.getBoolean("KEY_WAKE_SCREEN_GLOBAL", false)
                val isThreadWakeEnabled = sharedPreferences.getBoolean("KEY_WAKE_SCREEN_$threadId", isGlobalWakeEnabled)
                if (isThreadWakeEnabled) {
                    wakeUpScreen(context)
                }
                val archivedThreads = repository.getArchivedThreadIds()
                val blockedThreads = repository.getBlockThreadIds()

                val isArchived = archivedThreads.contains(threadId)
                val isBlocked = blockedThreads.contains(threadId)

                if (!isArchived && !isBlocked) {
                    incrementMessageCount(threadId)
                    showNotification(context, address, body, threadId)

                } else {
                }
                repository.getMessages()
                repository.getConversation(threadId)
            }
        }
    }

    private fun wakeUpScreen(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "${context.packageName}:WakeLock"
        )

        wakeLock.acquire(3000)

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val keyguardLock = keyguardManager.newKeyguardLock("MyApp:KeyguardLock")
        keyguardLock.disableKeyguard()
        wakeLock.release()
    }




    private fun incrementMessageCount(threadId: Long) {
        val count = messageCounts[threadId] ?: 0
        messageCounts[threadId] = count + 1
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
            } else {
            }
        } catch (e: Exception) {
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