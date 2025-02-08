package com.test.messages.demo.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi
import com.test.messages.demo.data.ContactItem
import com.test.messages.demo.data.ConversationItem
import com.test.messages.demo.data.MessageItem
import javax.inject.Inject

class MessageRepository @Inject constructor(private val context: Context) {


    @RequiresApi(Build.VERSION_CODES.Q)
    fun getConversations(): List<MessageItem> {
        val conversations = mutableListOf<MessageItem>()

        val threadProjection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.DATE,
            Telephony.Threads.MESSAGE_COUNT,
            Telephony.Threads.RECIPIENT_IDS
        )

        val threadCursor: Cursor? = context.contentResolver.query(
            Uri.parse("${Telephony.Threads.CONTENT_URI}?simple=true"), threadProjection, null, null,
            "${Telephony.Threads.DATE} DESC"
        )

        threadCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Threads._ID))
                val lastMessage =
                    cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Threads.SNIPPET))
                val lastMessageTimestamp =
                    cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Threads.DATE))
                val messageCount =
                    cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Threads.MESSAGE_COUNT))
                val reciptid =
                    cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS))
                val senderName = ""
                if (lastMessage != null) {
                    conversations.add(
                        MessageItem(
                            threadId,
                            senderName,
                            lastMessage,
                            lastMessageTimestamp,
                            isRead = messageCount > 0,
                            reciptid
                        )
                    )

                }
            }

        }
        return conversations
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getRecipientAddresses(): HashMap<String, String> {
        val recipientList = mutableListOf<String>()

        val threadProjection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.RECIPIENT_IDS
        )

        val threadCursor: Cursor? = context.contentResolver.query(
            Uri.parse("${Telephony.Threads.CONTENT_URI}?simple=true"), threadProjection, null, null,
            "${Telephony.Threads.DATE} DESC"
        )

        threadCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val recipientIds =
                    cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS))
                recipientIds.split(",").map { it.trim() }
                    .filter { it.isNotEmpty() } // Filter out blank recipient IDs
                    .let { recipientList.addAll(it) }
            }
        }
        val recipientMap = getRecipientAddressesForIds(recipientList) // Get as HashMap
        return recipientMap
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getRecipientAddressesForIds(recipientIds: List<String>): HashMap<String, String> {
        val recipientMap = HashMap<String, String>()

        val projection = arrayOf(Telephony.Mms.Addr._ID, Telephony.Mms.Addr.ADDRESS)
        val uri = Uri.withAppendedPath(Telephony.MmsSms.CONTENT_URI, "canonical-addresses")
        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val recipientId = it.getString(it.getColumnIndexOrThrow(Telephony.Mms.Addr._ID))
                val recipientPhoneNumber =
                    it.getString(it.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS))
                recipientMap[recipientId] = recipientPhoneNumber

            }
        }
        Log.d("TAG", "getRecipientAddressesForIds:- $recipientMap")
        return recipientMap
    }

    fun getContactDetails(): List<ContactItem> {
        val contactList = mutableListOf<ContactItem>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val phoneNumber =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val displayName =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                contactList.add(ContactItem(displayName, phoneNumber))
            }
        }

        return contactList
    }

    fun updateRecipientWithContactNames(recipientMap: HashMap<String, String>) {
        val contactDetails = getContactDetails()

        // Map phone numbers to contact names
        contactDetails.forEach { contact ->
            recipientMap.entries.find { it.value == contact.phoneNumber }?.apply {
                this.setValue(contact.name!!) // Update recipient map with contact name
            }
        }

        Log.d("UpdatedRecipientMap", "Recipient map after contact name update: $recipientMap")
    }


    fun getConversationDetails(threadId: Long): List<ConversationItem> {
        val conversationList = mutableListOf<ConversationItem>()

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

        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} ASC" // Order messages by date
        )

        cursor?.use {
            Log.e("TAG", "getConversationDetails: "+cursor.count )
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                val read = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
                val subscriptionId = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID))

                // Add conversation item
                conversationList.add(ConversationItem(id, threadId, date, body, address, type, read, subscriptionId))
            }
        }

        return conversationList
    }
}
