package com.test.messages.demo.repository

import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import com.test.messages.demo.data.MessageItem
import javax.inject.Inject

//class ConversationRepository @Inject constructor(private val context: Context) {
//
//    fun getConversations(): List<MessageItem> {
//        val conversations = mutableListOf<MessageItem>()
//
//        val projection = arrayOf(
//            Telephony.Sms.THREAD_ID,
//            Telephony.Sms.ADDRESS,
//            Telephony.Sms.BODY,
//            Telephony.Sms.DATE,
//            Telephony.Sms.READ
//        )
//
//        val cursor: Cursor? = context.contentResolver.query(
//            Telephony.Sms.CONTENT_URI,
//            projection,
//            null, null,
//            "${Telephony.Sms.DATE} DESC"
//        )
//
//        val seenThreadIds = mutableSetOf<Long>()
//
//        cursor?.use {
//            while (it.moveToNext()) {
//                val threadId = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
//
//                if (!seenThreadIds.contains(threadId)) {
//                    val sender = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
//                    val lastMessage = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
//                    val timestamp = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
//                    val isRead = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
//
//                    conversations.add(MessageItem(sender, lastMessage, timestamp, isRead))
//                    seenThreadIds.add(threadId)
//                }
//            }
//        }
//
//        return conversations
//    }
//
//}
