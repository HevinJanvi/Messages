package com.test.messages.demo.data.reciever

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.CommanConstants.DROPMSG
import com.test.messages.demo.Util.ConversationOpenedEvent
import com.test.messages.demo.Util.NewSmsEvent
import com.test.messages.demo.Util.RefreshMessagesEvent
import com.test.messages.demo.Util.ViewUtils.getThreadId
import com.test.messages.demo.data.repository.MessageRepository
import com.test.messages.demo.ui.send.notificationHelper
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MessageRepository
    private val messageCounts = mutableMapOf<Long, Int>()
    private var messageId: Long = 0L

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
                val prefs = context.getSharedPreferences(CommanConstants.PREFS_NAME, Context.MODE_PRIVATE)
                val isDropMessagesEnabled = prefs.getBoolean(DROPMSG, true)
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
                    val insertedUri = insertMessageIntoSystemDatabase(
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

                    messageId = insertedUri?.let { uri ->
                        context.contentResolver.query(uri, arrayOf(Telephony.Sms._ID), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
                        }
                    } ?: 0L
                    EventBus.getDefault().post(NewSmsEvent(threadId))
                }

                val database = AppDatabase.getDatabase(context)
                val notificationDao = database.notificationDao()
                val isMuted = notificationDao.getNotificationStatus(threadId) ?: false
                val wakeSetting = notificationDao.getSettings(threadId) ?: notificationDao.getSettings(-1)
                val isWakeScreenEnabled = wakeSetting?.isWakeScreenOn ?: true

                if (!isMuted) {
//                    Log.d("TAG", "Wake Screen DB Value: $isWakeScreenEnabled")
                    if (isWakeScreenEnabled) {
                        wakeUpScreen(context)
                    }
                } else {
                    Log.d("SmsReceiver", "Notifications are muted for thread: $threadId")
                }


                val displayName = repository.getContactName(context, address)

                val archivedThreads = repository.getArchivedThreadIds()
                val blockedThreads = repository.getBlockThreadIds()

                val isArchived = archivedThreads.contains(threadId)
                val isBlocked = blockedThreads.contains(threadId)

                val openEvent = EventBus.getDefault().getStickyEvent(ConversationOpenedEvent::class.java)
                val openThreadId = openEvent?.threadId ?: -1L

                if (!isMuted && !isArchived && !isBlocked && threadId != openThreadId) {
                    incrementMessageCount(threadId)
                    val contactUri = repository.getPhotoUriFromPhoneNumber(address)
                    val bitmap = repository.getNotificationBitmap(context,contactUri)
//                    Log.d("TAG", "onReceive:bitmap "+bitmap)
                    context.notificationHelper.showMessageNotification(messageId, address, body, threadId, bitmap, displayName, alertOnlyOnce = true)
                }
                repository.getMessages()
//                repository.getConversation(threadId)
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
    ): Uri? {
        return try {
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

            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


}