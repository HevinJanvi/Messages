package com.test.messages.demo.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.JsonWriter
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.test.messages.demo.R
import com.test.messages.demo.Util.ViewUtils.getThreadId
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.data.Model.MessageItem
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.OutputStreamWriter

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


    suspend fun backupMessages(
        uri: Uri,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        try {
            val messages = fetchMessagesFromCursor()
            if (messages.isEmpty()) {
                withContext(Dispatchers.Main) { onComplete(false) }
                return
            }

            val outputStream = context.contentResolver.openOutputStream(uri)
            if (outputStream == null) {
                withContext(Dispatchers.Main) { onComplete(false) }
                return
            }

            // Using BufferedWriter instead of JsonWriter
            val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))

            // Start writing the JSON array directly
            writer.write("[\n")

            val gson = Gson()

            // Serialize each message as JSON and write it to the BufferedWriter
            for ((index, message) in messages.withIndex()) {
                val jsonMessage = gson.toJson(message)  // Direct JSON string serialization
                writer.write("  $jsonMessage")

                if (index < messages.size - 1) {
                    writer.write(",\n")  // Add comma between JSON objects
                }

                val progress = ((index + 1) * 100) / messages.size
                withContext(Dispatchers.Main) {
                    onProgress(progress)
                }
            }

            // End of JSON array
            writer.write("\n]")

            writer.flush()
            writer.close()

            withContext(Dispatchers.Main) {
                onComplete(true)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onComplete(false)
            }
        }
    }



   /* suspend fun restoreMessages(
        uri: Uri,
        onProgressUpdate: (Int) -> Unit,
        onComplete: (List<ConversationItem>) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            onProgressUpdate.invoke(0)
        }

        val inputStream = context.contentResolver.openInputStream(uri)
        val json = inputStream?.bufferedReader().use { it?.readText() }

        val restoredList: List<ConversationItem> =
            Gson().fromJson(json, object : TypeToken<List<ConversationItem>>() {}.type)
        onProgressUpdate.invoke(10)
        val sortedList = restoredList.sortedBy { it.date }

        val existingIds = getExistingMessageIds()
        val existingThreadIds = getExistingThreadIds()
        onProgressUpdate.invoke(15)
        val insertedMessages = mutableListOf<ConversationItem>()
        val totalMessages = sortedList.size.coerceAtLeast(1)

        try {
            for ((index, conversation) in sortedList.withIndex()) {
                val originalThreadId = conversation.threadId

                if (!existingIds.contains(conversation.id)) {
                    var threadId = originalThreadId

                    if (!existingThreadIds.contains(threadId)) {
                        threadId = insertThreadIfNotExist(conversation)
                        Log.d("TAG", "restoreMessages:if ")
                    } else {
                        Log.d("TAG", "restoreMessages:else ")
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

                    withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(context).recycleBinDao().deleteMessagesByThreadId(originalThreadId)
                    }

                    val progress = ((index + 1) * 100) / totalMessages
                    withContext(Dispatchers.Main) {
                        onProgressUpdate(progress)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                onProgressUpdate(100)
            }
        } finally {
            withContext(Dispatchers.Main) {
                onProgressUpdate(100)
                onComplete(insertedMessages)
            }
        }
    }*/

    suspend fun restoreMessages(
        uri: Uri,
        onProgressUpdate: (Int) -> Unit,
        onComplete: (List<ConversationItem>) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            onProgressUpdate.invoke(0)
        }

        val inputStream = context.contentResolver.openInputStream(uri)
        val json = inputStream?.bufferedReader().use { it?.readText() }

        val restoredList: List<ConversationItem> =
            Gson().fromJson(json, object : TypeToken<List<ConversationItem>>() {}.type)

        onProgressUpdate.invoke(10)

        val sortedList = restoredList.sortedBy { it.date }
        val existingIds = getExistingMessageIds()
        val insertedMessages = mutableListOf<ConversationItem>()
        val totalMessages = sortedList.size.coerceAtLeast(1)

        try {
            for ((index, message) in sortedList.withIndex()) {
                if (existingIds.contains(message.id)) continue

                val isGroup = message.address.contains(",")
                val originalThreadId = message.threadId
                var threadId = originalThreadId

                val resolvedAddress = if (isGroup) {
                    message.address.split(",")
                        .map { it.trim() }
                        .map { resolveContactToNumber(it) }
                        .joinToString(",")
                } else {
                    resolveContactToNumber(message.address.trim())
                }

                if (!isThreadExists(threadId)) {
                    threadId = if (isGroup) {
                        val random = System.currentTimeMillis().toString().takeLast(5)
                        val dummyEmail = "dummy$random@group.com"
                        val modifiedAddress = "$resolvedAddress"
                        getThreadId(modifiedAddress.split(",").toSet())
                    } else {
                        getThreadId(setOf(resolvedAddress))
                    }
                }

                val readStatus = if (message.read) 1 else 0

                insertMessageIntoSystemDatabase(
                    context = context,
                    address = resolvedAddress,
                    body = message.body,
                    date = message.date,
                    threadId = threadId,
                    read = readStatus,
                    type = message.type,
                    subscriptionId = message.subscriptionId
                )

                insertedMessages.add(message)

                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(context).recycleBinDao().deleteMessagesByThreadId(originalThreadId)
                }

                val progress = ((index + 1) * 100) / totalMessages
                withContext(Dispatchers.Main) {
                    onProgressUpdate(progress)
                }
            }

            withContext(Dispatchers.Main) {
                onProgressUpdate(100)
            }

        } finally {
            withContext(Dispatchers.Main) {
                onProgressUpdate(100)
                onComplete(insertedMessages)
            }
        }
    }

    fun getThreadId(addresses: Set<String>): Long {
        return Telephony.Threads.getOrCreateThreadId(context, addresses)
    }
    fun resolveContactToNumber(contact: String): String {
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(contact)

        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex != -1) {
                    return it.getString(numberIndex)
                }
            }
        }

        // If no matching name found, assume it's already a number and return as is
        return contact
    }


    fun isThreadExists(threadId: Long): Boolean {
        val uri = Telephony.Threads.CONTENT_URI
        val cursor = context.contentResolver.query(uri, null, "_id = ?", arrayOf(threadId.toString()), null)
        cursor?.use {
            return it.moveToFirst()
        }
        return false
    }

    private fun insertThreadIfNotExist(conversation: ConversationItem): Long {
        val threadId = getThreadId(context,conversation.address)
        Log.d("TAG", "insertThreadIfNotExist: "+conversation.body)
        val values = ContentValues().apply {
//            put(Telephony.Sms.THREAD_ID, threadId)
            put(Telephony.Sms.ADDRESS, conversation.address)
            put(Telephony.Sms.BODY, conversation.body)
            put(Telephony.Sms.DATE, conversation.date)
            put(Telephony.Sms.READ, conversation.read)
            put(Telephony.Sms.TYPE, conversation.type)
            put(Telephony.Sms.SUBSCRIPTION_ID, conversation.subscriptionId)
        }

        val uri = context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        if (uri != null) {
            Log.d("TAG", "Message inserted successfully." + uri)
        } else {
            Log.d("TAG", "Failed to insert message.")
        }
        return threadId
    }

//    fun getThreadId(address: String): Long {
//        return try {
//            Telephony.Threads.getOrCreateThreadId(context, address)
//        } catch (e: Exception) {
//            0L
//        }
//    }

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
