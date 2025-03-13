package com.test.messages.demo.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.test.messages.demo.Database.Archived.ArchivedConversation
import com.test.messages.demo.Database.Block.BlockConversation
import com.test.messages.demo.Database.Pin.PinMessage
import com.test.messages.demo.Database.Starred.StarredMessage
import com.test.messages.demo.data.ContactItem
import com.test.messages.demo.data.ConversationItem
import com.test.messages.demo.data.MessageItem
import dagger.hilt.android.qualifiers.ApplicationContext
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class MessageRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val _messages = MutableLiveData<List<MessageItem>>()
    val messages: LiveData<List<MessageItem>> get() = _messages

    private val _conversation = MutableLiveData<List<ConversationItem>>()
    val conversation: LiveData<List<ConversationItem>> get() = _conversation


   /* @RequiresApi(Build.VERSION_CODES.Q)
    fun getMessages(): List<MessageItem> {
        val messageList = getConversations()
        val recipientMap = getRecipientAddresses().toMap()
        val contactMap = getContactDetails().associateBy({ it.normalizeNumber }, { it.name })
        val contactPhotoMap =
            getContactDetails().associateBy({ it.normalizeNumber }, { it.profileImageUrl })

        val sharedPreferences = context.getSharedPreferences("GroupPrefs", Context.MODE_PRIVATE)

        val newMsgList = messageList
            .filter {
                it.body != null && it.body?.trim()?.isNotEmpty() == true && it.sender != null
            }
            .map { messageItem ->
                val reciptids = messageItem.reciptids.trim()
                val displayName: String
                val rawPhoneNumber: String
                val photoUri: String
                val isGroupChat = reciptids.contains(" ")

                if (reciptids.contains(" ")) {  // ✅ Only split if space exists
                    val receiptIdList = reciptids.split(" ")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    val rawPhoneNumbers =
                        receiptIdList.map { id -> recipientMap[id] ?: "Unknown Number" }

                    val contactNames = rawPhoneNumbers.map { number ->
                        val normalizedNumber = normalizePhoneNumber(number)
                        contactMap[normalizedNumber]
                            ?: number  // Use contact name if available, else show number
                    }

                    displayName = contactNames.joinToString(", ")
                    rawPhoneNumber = rawPhoneNumbers.joinToString(", ")  // Group numbers
                    photoUri = ""
                } else {
                    rawPhoneNumber = recipientMap[reciptids] ?: "Unknown Number"
                    val normalizedNumber = normalizePhoneNumber(rawPhoneNumber)
                    displayName =
                        contactMap[normalizedNumber] ?: rawPhoneNumber  // Single name or number
                    photoUri = contactPhotoMap[normalizedNumber].toString()
                }
                val isRead = messageItem.isRead
//                Log.d("MessageRepository", "Message from: $displayName, isRead: $isRead")

                messageItem.copy(
                    sender = displayName,
                    number = rawPhoneNumber,
                    profileImageUrl = photoUri,
                    isRead = isRead,
                    isGroupChat = isGroupChat

                )
            }
        Log.d("ObserverDebug", "getMessages: " + newMsgList.size)
        _messages.postValue(newMsgList)
        return newMsgList
    }*/


    @RequiresApi(Build.VERSION_CODES.Q)
    fun getMessages(): List<MessageItem> {
        val messageList = getConversations()
        val recipientMap = getRecipientAddresses().toMap()
        val contactMap = getContactDetails().associateBy({ normalizePhoneNumber(it.normalizeNumber) }, { it.name })
        val contactPhotoMap = getContactDetails().associateBy({ normalizePhoneNumber(it.normalizeNumber) }, { it.profileImageUrl })

        val sharedPreferences = context.getSharedPreferences("GroupPrefs", Context.MODE_PRIVATE)

        val newMsgList = messageList
            .filter {
                it.body != null && it.body?.trim()?.isNotEmpty() == true && it.sender != null
            }
            .map { messageItem ->
                val reciptids = messageItem.reciptids.trim()
                val displayName: String
                val rawPhoneNumber: String
                val photoUri: String
                val isGroupChat = reciptids.contains(" ")

                if (isGroupChat) {
                    val receiptIdList = reciptids.split(" ")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    val rawPhoneNumbers = receiptIdList.map { id ->
                        recipientMap[id] ?: id
                    }

                    // 🟢 Get the group name from SharedPreferences using `threadId`
                    val savedGroupName = sharedPreferences.getString("group_name_${messageItem.threadId}", null)

                    // If a custom group name exists, use it; otherwise, generate from contacts
                    displayName = savedGroupName ?: rawPhoneNumbers.map { number ->
                        val normalizedNumber = normalizePhoneNumber(number)
                        contactMap[normalizedNumber] ?: number
                    }.joinToString(", ")

                    rawPhoneNumber = rawPhoneNumbers.joinToString(", ")
                    photoUri = ""
                } else {
                    rawPhoneNumber = recipientMap[reciptids] ?: reciptids
                    val normalizedNumber = normalizePhoneNumber(rawPhoneNumber)
                    displayName = contactMap[normalizedNumber] ?: rawPhoneNumber
                    photoUri = contactPhotoMap[normalizedNumber].toString()
                }

                messageItem.copy(
                    sender = displayName,
                    number = rawPhoneNumber,
                    profileImageUrl = photoUri,
                    isRead = messageItem.isRead,
                    isGroupChat = isGroupChat
                )
            }

        Log.d("ObserverDebug", "getMessages: ${newMsgList.size}")
        _messages.postValue(newMsgList)
        return newMsgList
    }


    fun emptyConversation() {
        _conversation.value = emptyList()
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
            Telephony.Threads.RECIPIENT_IDS,
            Telephony.Sms.READ

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
                val reciptids =
                    cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS))
                val isRead = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1

//                Log.d("MessageRepository", "Message from: isRead: $isRead")
                val senderName = ""
                val profileImageUrl = ""

//                Log.d("TAG", "getConversations:reciptid " + reciptid)
                if (lastMessage != null) {
                    conversations.add(
                        MessageItem(
                            threadId,
                            senderName, "",
                            lastMessage,
                            lastMessageTimestamp,
                            isRead = isRead,
                            reciptid,
                            reciptids,
                            profileImageUrl,
                            false,
                            false

                        )
                    )

                }
            }

        }
        Log.d("ObserverDebug", "getConversations: " + conversations.size)
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

//                Log.d("TAG", "getRecipientid---- " + recipientIds)
                recipientIds.split(",").map { it.trim() }
                    .filter { it.isNotEmpty() } // Filter out blank recipient IDs
                    .let { recipientList.addAll(it) }
            }
        }
        val recipientMap = getRecipientAddressesForIds(recipientList) // Get as HashMap
//        Log.d("TAG", "getRecipientAddresses: " + recipientMap)
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
//                Log.d(
//                    "TAG",
//                    "getRecipientAddressesFor:---" + recipientId + "-------number-------" + recipientPhoneNumber
//                )

                recipientMap[recipientId] = recipientPhoneNumber

            }
        }
//        Log.d("TAG", "getRecipientAddressesForIds:- $recipientMap")
        return recipientMap
    }

    fun getContactDetails(): List<ContactItem> {
        val contactList = mutableListOf<ContactItem>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI

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
                val cid =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                val phoneNumber =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val normalizePhoneNumber =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER))
                val displayName =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                var profileImageUrl =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))
                        ?: ""
                if (phoneNumber != null && normalizePhoneNumber != null) {
                    contactList.add(
                        ContactItem(
                            cid,
                            displayName,
                            phoneNumber,
                            normalizePhoneNumber,
                            profileImageUrl
                        )
                    )
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
            phoneNumber
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
            "${Telephony.Sms.DATE} DESC"
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
                Log.d("SMS", "Message: $body | Type: $type | Address: $address | date: $date")

                conversationList.add(
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
        return conversationList
    }

    private val contactCache = mutableMapOf<String, String>()

    fun getContactNameOrNumber(phoneNumber: String): String {

        contactCache[phoneNumber]?.let { return it }

        val contactMap = getContactDetails().associateBy(
            { it.phoneNumber },
            { it.name }
        )
        contactMap[phoneNumber]?.let {
            contactCache[phoneNumber] = it  // Save to cache
            return it
        }
//        contactMap[phoneNumber]?.let { return it }

        val contentResolver: ContentResolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

       /* contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                return cursor.getString(nameIndex)
            }
        }*/
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val contactName = cursor.getString(nameIndex)
                contactCache[phoneNumber] = contactName  // Save to cache
                return contactName
            }
        }
        return phoneNumber
    }

    fun findGroupThreadId(addresses: Set<String>): Long? {
        val mergedAddresses = addresses.joinToString("|")
        val cursor = context.contentResolver.query(
            Uri.parse("content://sms/threads"),
            arrayOf("thread_id"),
            "address LIKE ?",
            arrayOf("%$mergedAddresses%"),
            null
        )

        return cursor?.use {
            if (it.moveToFirst()) {
                val threadId = it.getLong(it.getColumnIndexOrThrow("thread_id"))
                threadId
            } else {
                null // No group thread found
            }
        }
    }

    suspend fun getArchivedConversations(): List<ArchivedConversation> {
        return withContext(Dispatchers.IO) {
             AppDatabase.getDatabase(context).archivedDao().getAllArchivedConversations()
        }
    }

    suspend fun getArchivedThreadIds(): List<Long> {
        return withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).archivedDao().getArchivedThreadIds()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun archiveConversations(conversationIds: List<Long>) {
        val archivedConversations = conversationIds.map { conversationId ->
            ArchivedConversation(id = 0, conversationId = conversationId, isArchived = true)
        }
        AppDatabase.getDatabase(context).archivedDao()
            .insertArchivedConversations(archivedConversations)
    }

    suspend fun unarchiveConversations(conversationIds: List<Long>) {
        conversationIds.forEach {
            AppDatabase.getDatabase(context).archivedDao().unarchiveConversation(it)
        }
    }

    fun getPinnedThreadIds(): List<Long> {
        return AppDatabase.getDatabase(context).pinDao().getAllPinnedThreadIds()
    }

    fun addPinnedMessage(threadId: Long) {
        AppDatabase.getDatabase(context).pinDao()
            .insertPinnedMessages(listOf(PinMessage(0, threadId, true)))
    }

    fun removePinnedMessage(threadId: Long) {
        AppDatabase.getDatabase(context).pinDao().deletePinnedMessage(listOf(threadId))
    }

    fun addPinnedMessages(threadIds: List<Long>) {
        threadIds.forEach { addPinnedMessage(it) }
    }

    fun removePinnedMessages(threadIds: List<Long>) {
        threadIds.forEach { removePinnedMessage(it) }
    }

    suspend fun getBlockConversations(): List<BlockConversation> {
        return AppDatabase.getDatabase(context).blockDao().getAllBlockConversations()
    }

    suspend fun getBlockedThreadId(phoneNumber: String): Long? {
        return withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).blockDao().getBlockedThreadId(phoneNumber)
        }
    }

    suspend fun getBlockThreadIds(): List<Long> {
        return withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).blockDao().getBlockThreadIds()
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun blockConversations(conversationIds: List<Long>) {
        val blockConversations = conversationIds.map { conversationId ->
            val phoneNumber = getPhoneNumberForThread(conversationId) ?: ""
            BlockConversation(id = 0, conversationId = conversationId, isBlocked = true,phoneNumber)
        }
        AppDatabase.getDatabase(context).blockDao()
            .insertBlockConversations(blockConversations)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun blockConversation(conversationId: Long, phoneNumber: String) {
        blockConversations(listOf(conversationId))
    }
    suspend fun unblockConversations(conversationIds: List<Long>) {
        conversationIds.forEach {
            AppDatabase.getDatabase(context).blockDao().unblockConversation(it)
        }
    }

    suspend fun removeOldBlockedThreadIds(phoneNumber: String, newThreadId: Long) {
        AppDatabase.getDatabase(context).blockDao().deleteOldBlockedThreads(phoneNumber, newThreadId)
    }

    suspend fun isDeletedConversation(number: String): Boolean {
        return  AppDatabase.getDatabase(context).blockDao().isDeleted(number)
    }

    suspend fun isBlockedConversation(threadId: Long): Boolean {
        return AppDatabase.getDatabase(context).blockDao().isThreadBlocked(threadId) > 0
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getPhoneNumberForThread(threadId: Long): String? {
        val projection = arrayOf(Telephony.Sms.ADDRESS)
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        val cursor: Cursor? = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, projection, selection, selectionArgs, null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
            }
        }
        return null
    }


    //blocked contacts
    fun getBlockedContacts(): LiveData<List<MessageItem>> {
        val liveData = MutableLiveData<List<MessageItem>>()

        CoroutineScope(Dispatchers.IO).launch {
            val blockedMessagesList = mutableListOf<MessageItem>()
            val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
            val projection = arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)

            val cursor = context.contentResolver.query(uri, projection, null, null, null)

            cursor?.use {
                while (it.moveToNext()) {
                    val number =
                        it.getString(it.getColumnIndexOrThrow(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER))
                    val displayName = getContactNameOrNumber(number)
                    val threadId = getThreadIdForNumber(number)
                    if (threadId == -1L) continue // Skip if threadId not found
                    val messages = getConversationDetails(threadId)

                    if (messages.isNotEmpty()) {
                        val latestMessage = messages.last()

                        blockedMessagesList.add(
                            MessageItem(
                                threadId = latestMessage.threadId,
                                sender = displayName,
                                number = latestMessage.address,
                                body = latestMessage.body,
                                timestamp = latestMessage.date,
                                isRead = latestMessage.read,
                                reciptid = 0,
                                reciptids = "",
                                profileImageUrl = "",
                                isPinned = false,
                                isGroupChat = false
                            )
                        )
                    }
                }
            }

            liveData.postValue(blockedMessagesList)
        }
        return liveData
    }

    fun getThreadIdForNumber(phoneNumber: String): Long {
        val uri = Uri.parse("content://sms/")
        val projection = arrayOf("thread_id")
        val selection = "address = ?"
        val selectionArgs = arrayOf(phoneNumber)

        val cursor: Cursor? =
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(it.getColumnIndexOrThrow("thread_id"))
            }
        }

        return -1
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun blockContacts(messagesToBlock: List<MessageItem>) {
        val contentResolver = context.contentResolver

        for (message in messagesToBlock) {
            val values = ContentValues().apply {
                put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, message.number)
            }

            try {
                val uri =
                    contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values)
                if (uri != null) {
                    Log.d("BlockMessages", "Successfully blocked number: ${message.number}")
                } else {
                    Log.d("BlockMessages", "Failed to block number: ${message.number}")
                }
            } catch (e: Exception) {
                Log.d("BlockMessages", "Error blocking number: ${message.number}", e)
            }
        }
    }


    fun getBlockedNumbers(): List<String> {
        val blockedNumbers = mutableListOf<String>()
        val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI

        val cursor: Cursor? = context.contentResolver.query(
            uri,
            arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
            null, null, null
        )

        cursor?.use {
            val numberIndex =
                it.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
            while (it.moveToNext()) {
                val number = it.getString(numberIndex)
                if (!number.isNullOrEmpty()) {
                    blockedNumbers.add(number)
                }
            }
        }

        Log.d("BlockMessages", "Blocked numbers: $blockedNumbers")
        return blockedNumbers
    }

    fun getAllStarredMessages(): List<StarredMessage> {
        return AppDatabase.getDatabase(context).starredMessageDao().getAllStarredMessages()
    }

}
