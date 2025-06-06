package com.test.messages.demo.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.test.messages.demo.R
import com.test.messages.demo.Helper.Constants.GROUP_SEPARATOR
import com.test.messages.demo.data.Model.ConversationItem
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStream
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

    private var messages: List<ConversationItem> = emptyList()
    private var outputStream: OutputStream? = null
    private var writer: BufferedWriter? = null

    suspend fun backupMessages(
        uri: Uri,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        try {
             messages = fetchMessagesFromCursor()
            if (messages.isEmpty()) {
                withContext(Dispatchers.Main) { onComplete(false) }
                return
            }

             outputStream = context.contentResolver.openOutputStream(uri)
            if (outputStream == null) {
                withContext(Dispatchers.Main) { onComplete(false) }
                return
            }

             writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
            writer?.write("[\n")
            val gson = Gson()
            for ((index, message) in messages.withIndex()) {
                val jsonMessage = gson.toJson(message)
                writer?.write("  $jsonMessage")

                if (index < messages.size - 1) {
                    writer?.write(",\n")
                }

                val progress = ((index + 1) * 100) / messages.size
                withContext(Dispatchers.Main) {
                    onProgress(progress)
                }
            }
            writer?.write("\n]")

            writer?.flush()
            writer?.close()

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

    suspend fun restoreMessages(
        uri: Uri,
        onProgressUpdate: (Int) -> Unit,
        onComplete: (List<ConversationItem>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            onProgressUpdate.invoke(0)
        }

        val inputStream = context.contentResolver.openInputStream(uri)
        val json = inputStream?.bufferedReader().use { it?.readText() }

        if (json.isNullOrEmpty()) {
            withContext(Dispatchers.Main) {
                onFailure(IllegalArgumentException(context.getString(R.string.restore_failed)))
            }
            return
        }

        val restoredList: List<ConversationItem> =
            Gson().fromJson(json, object : TypeToken<List<ConversationItem>>() {}.type)
        val existingIds = getExistingMessageIds()
        val sortedList = restoredList.sortedBy { it.date }.filterNot { existingIds.contains(it.id) }
        val insertedMessages = mutableListOf<ConversationItem>()
        val totalMessages = sortedList.size.coerceAtLeast(1)

        try {
            for ((index, message) in sortedList.withIndex()) {
                if (existingIds.contains(message.id)) continue

                val address = message.address
                if (address.isNullOrEmpty()) {
                    continue
                }

                val isGroup = message.address.contains(GROUP_SEPARATOR)
                val originalThreadId = message.threadId
                var threadId = originalThreadId

                val resolvedAddress = if (isGroup) {
                    message.address.split(GROUP_SEPARATOR)
                        .map { it.trim() }
                        .map { resolveContactToNumber(it) }
                        .joinToString(GROUP_SEPARATOR)
                } else {
                    resolveContactToNumber(message.address.trim())
                }

                if (!isThreadExists(threadId)) {
                    threadId = if (isGroup) {
                        val random = System.currentTimeMillis().toString().takeLast(5)
                        val modifiedAddress = "$resolvedAddress"
                        getThreadId(modifiedAddress.split(GROUP_SEPARATOR).toSet())
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
                    AppDatabase.getDatabase(context).recycleBinDao()
                        .deleteMessagesByThreadId(originalThreadId)
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
        return contact
    }


    fun isThreadExists(threadId: Long): Boolean {
        if (threadId <= 0) return false
        val uri = Uri.parse("${Telephony.Threads.CONTENT_URI}?simple=true")
        val cursor = context.contentResolver.query(uri, null, "_id = $threadId", null, null)
        cursor?.use {
            return it.moveToFirst()
        }
        return false
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
            context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)

        } catch (e: Exception) {
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
