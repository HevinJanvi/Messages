package com.test.messages.demo.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.test.messages.demo.data.Model.ConversationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class BackupRepository(private val context: Context) {


    fun fetchMessagesFromCursor(): List<ConversationItem> {
        val messageList = mutableListOf<ConversationItem>()
        val uri: Uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.SUBSCRIPTION_ID
        )

        val cursor =
            context.contentResolver.query(uri, projection, null, null, Telephony.Sms.DATE + " DESC")

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                val threadId = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                val read = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
                val subscriptionId =
                    it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID))

                if (address != null) {
                    messageList.add(
                        ConversationItem(
                            id,
                            threadId,
                            date,
                            body,
                            address,
                            type,
                            read,
                            subscriptionId,
                            "",
                            false
                        )
                    )
                }

            }
        }
        return messageList
    }

    fun backupMessages(uri: Uri) {
        try {
            val messages = fetchMessagesFromCursor()
            if (messages.isEmpty()) {
                Toast.makeText(context, "No messages found to backup", Toast.LENGTH_SHORT).show()
                return
            }
            val json = Gson().toJson(messages)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    suspend fun restoreMessages(
        uri: Uri,
        onProgressUpdate: (Int) -> Unit,
        onComplete: (List<ConversationItem>) -> Unit
    ) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val json = inputStream?.bufferedReader().use { it?.readText() }

        val restoredList: List<ConversationItem> =
            Gson().fromJson(json, object : TypeToken<List<ConversationItem>>() {}.type)



        val existingIds = getExistingMessageIds()
        val existingThreadIds = getExistingThreadIds()

        val insertedMessages = mutableListOf<ConversationItem>()
        val totalMessages = restoredList.size.coerceAtLeast(1) // Avoid division by zero


        try {
            for ((index, conversation) in restoredList.withIndex()) {
//            for (conversation in restoredList) {

                if (!existingIds.contains(conversation.id)) {
                    var threadId = conversation.threadId
                    if (!existingThreadIds.contains(threadId)) {
                        threadId = insertThreadIfNotExist(conversation)

                    } else {
                        val readStatus = if (conversation.read) 1 else 0
                        insertMessageIntoSystemDatabase(
                            context,
                            conversation.address,
                            conversation.body,
                            conversation.date,
                            threadId,
                            readStatus,
                            conversation.type,
                            conversation.subscriptionId
                        )
                    }

                    insertedMessages.add(conversation)
                    val progress = ((index + 1) * 100) / totalMessages
                    withContext(Dispatchers.Main) {
                        onProgressUpdate(progress)
                    }
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                onComplete(insertedMessages)
            }
        }
    }

    private fun insertThreadIfNotExist(conversation: ConversationItem): Long {
        val threadId = getThreadId(conversation.address)
        val values = ContentValues().apply {
            put(Telephony.Sms.THREAD_ID, threadId)
            put(Telephony.Sms.ADDRESS, conversation.address)
            put(Telephony.Sms.BODY, conversation.body)
            put(Telephony.Sms.DATE, conversation.date)
            put(Telephony.Sms.READ, conversation.read)
            put(Telephony.Sms.TYPE, conversation.type)
            put(Telephony.Sms.SUBSCRIPTION_ID, conversation.subscriptionId)
        }
        Log.d("TAG", "insertThreadIfNotExist: ")

        val uri = context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        return threadId
    }

    fun getThreadId(address: String): Long {
        return try {
            Telephony.Threads.getOrCreateThreadId(context, address)
        } catch (e: Exception) {
            0L
        }
    }

    private fun getExistingMessageIds(): Set<Long> {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms._ID)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        val existingIds = mutableSetOf<Long>()

        cursor?.use {
            while (it.moveToNext()) {
                existingIds.add(it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID)))
            }
        }

        return existingIds
    }


    private fun getExistingThreadIds(): Set<Long> {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.THREAD_ID)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        val existingThreadIds = mutableSetOf<Long>()

        cursor?.use {
            while (it.moveToNext()) {
                existingThreadIds.add(it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)))
            }
        }

        return existingThreadIds
    }

    private fun insertMessageIntoSystemDatabase(
        context: Context,
        address: String,
        body: String,
        date: Long,
        threadId: Long,
        read: Int,
        type: Int,
        subscriptionId: Int
    ) {
        try {

            val existingMessageId = checkIfMessageExists(body, date, threadId)
            if (existingMessageId != 0L) {
                Log.d("SmsReceiver", "Message already exists, skipping insert.")
                return
            }


            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, date)
                put(Telephony.Sms.READ, read)
                put(Telephony.Sms.TYPE, type)
                put(Telephony.Sms.THREAD_ID, threadId)
                put(Telephony.Sms.SUBSCRIPTION_ID, subscriptionId)
                put(Telephony.Sms.SUBJECT, "")
            }

            val uri = context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            if (uri != null) {
                Log.d("SmsReceiver", "Message inserted successfully." + uri)
            } else {
                Log.d("SmsReceiver", "Failed to insert message.")
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error inserting message: ${e.message}")
        }
    }


    private fun checkIfMessageExists(body: String, date: Long, threadId: Long): Long {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms._ID)
        val selection =
            "${Telephony.Sms.BODY} = ? AND ${Telephony.Sms.DATE} = ? AND ${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(body, date.toString(), threadId.toString())

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))
                }
            }
        return 0L
    }


}
