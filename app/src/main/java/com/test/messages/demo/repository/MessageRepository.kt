package com.test.messages.demo.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.test.messages.demo.data.ContactItem
import com.test.messages.demo.data.ConversationItem
import com.test.messages.demo.data.MessageItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

class MessageRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val _messages = MutableLiveData<List<MessageItem>>()
    val messages: LiveData<List<MessageItem>> get() = _messages

    private val _conversation = MutableLiveData<List<ConversationItem>>()
    val conversation: LiveData<List<ConversationItem>> get() = _conversation

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getMessages(): List<MessageItem> {
        val messageList = getConversations()
        val recipientMap = getRecipientAddresses().toMap()
        val contactMap =
            getContactDetails().associateBy({ it.normalizeNumber }, { it.name })
        val newmsgList = messageList
            .filter {
                it.body != null && it.body?.trim()?.isNotEmpty() == true && it.sender != null
            }
            .map { messageItem ->
                val rawPhoneNumber = recipientMap[messageItem.reciptid.toString()] ?: "Unknown Number"
                val normalizedMessageNumber = normalizePhoneNumber(rawPhoneNumber)
                val contactName = contactMap[normalizedMessageNumber] ?: rawPhoneNumber
                messageItem.copy(sender = contactName, number = rawPhoneNumber)
            }
        _messages.postValue(newmsgList)
        return newmsgList

    }

     fun getConversation(threadId: Long): List<ConversationItem> {
         val updatedConversation = getConversationDetails(threadId)
         _conversation.postValue(updatedConversation)
        return updatedConversation
    }

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
                            senderName, "",
                            lastMessage,
                            lastMessageTimestamp,
                            isRead = messageCount > 0,
                            reciptid
                        )
                    )

                }
            }

        }
        _messages.postValue(conversations)
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
        Log.d("TAG", "getRecipientAddresses: " + recipientMap)
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
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
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
                val normalizePhoneNumber =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER))
                val displayName =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))

                if (phoneNumber != null && normalizePhoneNumber!=null) {
                    contactList.add(ContactItem(displayName, phoneNumber, normalizePhoneNumber))
                }
            }
        }

        return contactList
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        return try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val number = phoneUtil.parse(phoneNumber, Locale.getDefault().country)
            phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: Exception) {
            phoneNumber // Return original if parsing fails
        }
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
            "${Telephony.Sms.DATE} ASC"
        )

        cursor?.use {
            Log.d("SMS", "Conversation loaded. Messages found: ${it.count}")

            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                val read = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
                val subscriptionId =
                    it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID))
                Log.d("SMS", "Message: $body | Type: $type | Address: $address")

                // Add conversation item
                conversationList.add(
                    ConversationItem(
                        id,
                        threadId,
                        date,
                        body,
                        address,
                        type,
                        read,
                        subscriptionId
                    )
                )
            }
        }

        return conversationList
    }

    fun getContactNameOrNumber(phoneNumber: String): String {
        val contactMap = getContactDetails().associateBy(
            { it.phoneNumber },
            { it.name }
        )
        contactMap[phoneNumber]?.let { return it }

        val contentResolver: ContentResolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                return cursor.getString(nameIndex)
            }
        }
        return phoneNumber
    }



}
