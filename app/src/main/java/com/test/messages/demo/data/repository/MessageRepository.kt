package com.test.messages.demo.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import android.util.Patterns
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.test.messages.demo.R
import com.test.messages.demo.Util.Constants
import com.test.messages.demo.Util.Constants.GROUP_NAME_KEY
import com.test.messages.demo.Util.Constants.GROUP_SEPARATOR
import com.test.messages.demo.Util.ViewUtils.countryCodes
import com.test.messages.demo.data.Database.Archived.ArchivedConversation
import com.test.messages.demo.data.Database.Block.BlockConversation
import com.test.messages.demo.data.Database.Notification.NotificationSetting
import com.test.messages.demo.data.Database.Pin.PinMessage
import com.test.messages.demo.data.Database.Starred.StarredMessage
import com.test.messages.demo.data.Model.ContactItem
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.ui.SMSend.hasReadContactsPermission
import com.test.messages.demo.ui.SMSend.hasReadSmsPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MessageRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val _messages = MutableLiveData<List<MessageItem>>()
    val messages: LiveData<List<MessageItem>> get() = _messages

    private val _conversation = MutableLiveData<List<ConversationItem>?>()
    val conversation: LiveData<List<ConversationItem>?> get() = _conversation

    fun String.removeCountryCode(): String {
        for (code in countryCodes.sortedByDescending { it.length }) {
            if (this.startsWith(code)) {
                return this.replaceFirst(code, "")
            }
        }
        return this
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getMessages(): List<MessageItem> {
        val startTime = System.currentTimeMillis()
        val sharedPreferences =
            context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        return runBlocking(Dispatchers.IO) {
            val conversationsDeferred = async { getConversations() }
            val recipientDeferred = async { getRecipientAddresses().toMap() }
            val contactDeferred = async {
                val contacts = getContactDetails()
                contacts.associateBy { it.phoneNumber }
            }

            val messageList = conversationsDeferred.await()
            val recipientMap = recipientDeferred.await()
            val contactDetails = contactDeferred.await()

            val newMsgList = ArrayList<MessageItem>(messageList.size)

            for (messageItem in messageList) {
                val reciptids = messageItem.reciptids.trim()
//                Log.d("DEBUG", " Receipt IDs: ${messageItem.threadId} Sender:${messageItem.sender}  Body:${messageItem.body}")

                if (messageItem.body.isNullOrBlank() || messageItem.sender == null) continue
//                Log.d("TAG", "getMessages: " + reciptids)
                val isGroupChat = reciptids.contains(" ")


                val (displayName, rawPhoneNumber, photoUri) = if (isGroupChat) {
//                    Log.d("TAG", "getMessages:11111 ")
                    val receiptIdList = reciptids.split(" ")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
//                    Log.d("DEBUG", "Group detected! Receipt IDs: $receiptIdList")

                    val rawPhoneNumbers = receiptIdList.map { id -> recipientMap[id] ?: id }
//                    Log.d("DEBUG", "Mapped phone numbers: $rawPhoneNumbers")
                    val key = "${GROUP_NAME_KEY}${messageItem.threadId}"

                    val savedGroupName =
                        sharedPreferences.getString(
                            "${GROUP_NAME_KEY}${messageItem.threadId}",
                            null
                        )
//                    Log.d("DEBUG", "Saved group name for thread ${messageItem.threadId}: $savedGroupName")
                    val groupName = savedGroupName ?: rawPhoneNumbers.map { number ->
                        contactDetails[number]?.name
                            ?: contactDetails[number.removeCountryCode()]?.name ?: number
                    }.joinToString(", ")
//                    Log.d("DEBUG", "Final group name: $groupName")

                    Triple(groupName, rawPhoneNumbers.joinToString(GROUP_SEPARATOR), "")
                } else {
                    val rawPhone = (recipientMap[reciptids] ?: reciptids).replace(" ", "")
//                    Log.d("DEBUG", "Processing individual chat. Raw phone number: $rawPhone")
                    var contactInfo =
                        contactDetails[rawPhone] ?: contactDetails[rawPhone.removeCountryCode()]
                    if (contactInfo == null && rawPhone.length > 5) {
                        //remove +___12345
                        //remove +__12345

                        //remove +_12345
                        contactInfo = contactDetails[rawPhone.substring(4)]
                            ?: contactDetails[rawPhone.substring(3)]
                                    ?: contactDetails[rawPhone.substring(2)]
                                    ?: contactDetails[rawPhone.substring(1)]
                                    ?: contactDetails[rawPhone]
                        /* Log.d(
                             "DEBUG",
                             "getMessages: " + contactInfo?.name + "---------number----" + rawPhone
                         )*/
                    }

                    Triple(
                        contactInfo?.name ?: rawPhone,
                        rawPhone,
                        contactInfo?.profileImageUrl ?: ""
                    )
                }
                /* Log.d(
                     "TAG",
                     "getMessages:thread " + messageItem.threadId + "---isgrpoup-----" + isGroupChat
                 )*/
                newMsgList.add(
                    messageItem.copy(
                        sender = displayName,
                        number = rawPhoneNumber,
                        profileImageUrl = photoUri,
                        isRead = messageItem.isRead,
                        isGroupChat = isGroupChat
                    )
                )
            }

            _messages.postValue(newMsgList)
            val endTime = System.currentTimeMillis()
            Log.d("Performance", "getMessages() took ${endTime - startTime} ms")

            newMsgList
        }
    }

    fun emptyConversation() {
        _conversation.postValue(null)
    }

    fun getConversation(threadId: Long): List<ConversationItem> {
        val updatedConversation = getConversationDetails(threadId)
        _conversation.postValue(updatedConversation)
        return updatedConversation
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getConversations(): List<MessageItem> {
        val conversations = mutableListOf<MessageItem>()

        if (!context.hasReadContactsPermission()
        ) {
            return conversations
        }
        val threadProjection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.DATE,
            Telephony.Threads.MESSAGE_COUNT,
            Telephony.Threads.RECIPIENT_IDS,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE

        )

        val threadCursor: Cursor? = context.contentResolver.query(
            Uri.parse("${Telephony.Threads.CONTENT_URI}?simple=true"), threadProjection, null, null,
            "${Telephony.Threads.DATE} DESC"
        )
        val messageMap = getLastLatestMessage()

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
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE))

                val senderName = ""
                val profileImageUrl = ""
//                Log.d("TAG", "getConversations:snnipet " + lastMessage + "---thread----" + threadId)
                if (lastMessage != null && messageCount > 0) {
                    conversations.add(
                        MessageItem(
                            threadId,
                            senderName, "",
                            messageMap[threadId]?.body ?: lastMessage,
                            lastMessageTimestamp,
                            isRead = isRead,
                            reciptid,
                            reciptids,
                            profileImageUrl,
                            false,
                            false,
                            false,
                            messageMap[threadId]?.date ?: lastMessageTimestamp


                        )
                    )

                }
            }

        }
//        Log.d("ObserverDebug", "getConversations: " + conversations.size)
//        val conversationList = getLastLatestMessage()
//        Log.d("ObserverDebug", "Unique threads count: ${conversationList.size}")
        return conversations
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getRecipientAddresses(): HashMap<String, String> {
        val recipientList = mutableListOf<String>()

        if (!context.hasReadSmsPermission()
        ) {
            return hashMapOf()
        }
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
                recipientIds.split(GROUP_SEPARATOR).map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .let { recipientList.addAll(it) }
            }
        }
        val recipientMap = getRecipientAddressesForIds(recipientList)
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
                /* Log.d(
                     "DEBUG",
                     "getRecipientAddressesFor:---" + recipientId + "-------number-------" + recipientPhoneNumber
                 )*/

                recipientMap[recipientId] = recipientPhoneNumber

            }
        }
//        Log.d("TAG", "getRecipientAddressesForIds:- $recipientMap")
        return recipientMap
    }

    fun getContactDetails(): List<ContactItem> {
        val contactList = mutableListOf<ContactItem>()
        if (!context.hasReadContactsPermission()
        ) {
            return contactList
        }
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
                    (it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)))
                val normalizePhoneNumber =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER))
                val displayName =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                var profileImageUrl =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))
                        ?: ""
                if (phoneNumber != null) {
                    var number = phoneNumber.replace(
                        " ",
                        ""
                    ).replace("(", "").replace(")", "").replace("-", "")
//                    Log.d("DEBUG", "getContactDetails: "+phoneNumber+"-----name----"+displayName+"-----normalize numbr----"+normalizePhoneNumber )
                    contactList.add(
                        ContactItem(
                            cid,
                            displayName,
                            number.removeCountryCode(),
                            normalizePhoneNumber ?: number,
                            profileImageUrl
                        )
                    )
                }
            }
        }
        return contactList
    }

    fun getConversationDetails(threadId: Long): List<ConversationItem> {
        val conversationList = mutableListOf<ConversationItem>()
//        Log.d("TAG", "getConversationDetails:- " + threadId)
        if (!context.hasReadContactsPermission()
        ) {
//            Log.d("MessageRepository", "READ_CONTACTS permission not granted")
            return conversationList
        }
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
//            Log.d("TAG", "Conversation loaded. Messages found: ${it.count}")

            while (it.moveToNext()) {
//                Log.d("TAG", "Conversation loaded.")

                val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                val read = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
                val subscriptionId =
                    it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID))
//                Log.d("MessageRepository", "Message: $body | Type: $type | Address: $address | date: $date")

                conversationList.add(
                    ConversationItem(
                        id,
                        threadId,
                        date,
                        body,
                        address ?: "",
                        type,
                        read,
                        subscriptionId,
                        "",
                        false,
                        false
                    )
                )
            }
        }
//        Log.d("TAG", "Conversation loaded. Messages found:1 ${conversationList.size}")

        return conversationList
    }

    fun getLastLatestMessage(): MutableMap<Long, ConversationItem> {
        val threadMessageMap = mutableMapOf<Long, ConversationItem>()
        if (!context.hasReadContactsPermission() || (!Build.MANUFACTURER.equals("motorola") && !Build.MANUFACTURER.equals(
                "Google"
            ) && !Build.MANUFACTURER.equals("vivo"))
        ) {
            return threadMessageMap
        }
        val uri = Telephony.Sms.CONTENT_URI
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

        val sortOrder = "${Telephony.Sms.DATE} DESC"
        val cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)


        cursor?.use {
            while (it.moveToNext()) {
                val threadId = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))

                if (threadMessageMap.containsKey(threadId)) continue

                val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                val read = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
                val subscriptionId =
                    it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID))

                val conversationItem =
                    ConversationItem(
                        id,
                        threadId,
                        date,
                        body,
                        address ?: "",
                        type,
                        read,
                        subscriptionId,
                        "",
                        false,
                        false
                    )

                threadMessageMap[threadId] = conversationItem
            }
        }

        return threadMessageMap
    }

    private val contactCache = mutableMapOf<String, String>()


    fun getContactNameOrNumber(phoneNumbers: String): String {
        val numberList = phoneNumbers.split(GROUP_SEPARATOR).map { it.trim() }

        val contactMap = getContactDetails().associateBy(
            { it.phoneNumber.removeCountryCode() },
            { it.name }
        )

        val resolvedNames = numberList.map { number ->
            contactCache[number]?.let { return@map it }
            contactCache[number.removeCountryCode()]?.let { return@map it }

            contactMap[number]?.let {
                contactCache[number] = it
                return@map it
            }
            contactMap[number.removeCountryCode()]?.let {
                contactCache[number] = it
                return@map it
            }

            val contentResolver: ContentResolver = context.contentResolver
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            var name: String? = null
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    name = cursor.getString(nameIndex)
                }
            }

            val finalName = name ?: number
            contactCache[number] = finalName
            return@map finalName
        }

        return resolvedNames.joinToString(GROUP_SEPARATOR)
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

    suspend fun getMutedThreadIds(): List<Long> {
        return notificationDao.getAllMutedThreads()
    }

    fun addMutedMessages(threadIds: List<Long>) {
        threadIds.forEach { addMutedMessage(it) }
    }

    fun addMutedMessage(threadId: Long) {
        val dao = AppDatabase.getDatabase(context).notificationDao()
        dao.updateNotificationSetting(threadId, 1)
    }


    fun removeMutedMessages(threadIds: List<Long>) {
        threadIds.forEach { removeMutedMessage(it) }
    }

    fun removeMutedMessage(threadId: Long) {
        val dao = AppDatabase.getDatabase(context).notificationDao()
        dao.updateNotificationSetting(threadId, 0)
    }

    suspend fun getBlockConversations(): List<BlockConversation> = withContext(Dispatchers.IO) {
        AppDatabase.getDatabase(context).blockDao().getAllBlockConversations()
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
            BlockConversation(
                id = 0,
                conversationId = conversationId,
                isBlocked = true,
                phoneNumber
            )
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
        AppDatabase.getDatabase(context).blockDao()
            .deleteOldBlockedThreads(phoneNumber, newThreadId)
    }

    suspend fun isDeletedConversation(number: String): Boolean {
        return AppDatabase.getDatabase(context).blockDao().isDeleted(number)
    }

    suspend fun isBlockedConversation(threadId: Long): Boolean {
        return AppDatabase.getDatabase(context).blockDao().isThreadBlocked(threadId) > 0
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun getPhoneNumberForThread(threadId: Long): String? = withContext(Dispatchers.IO) {
        val projection = arrayOf(Telephony.Sms.ADDRESS)
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, projection, selection, selectionArgs, null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return@withContext it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
            }
        }
        return@withContext null
    }


    //blocked contacts
    fun getBlockedContacts(): LiveData<List<MessageItem>> {
        val liveData = MutableLiveData<List<MessageItem>>()

        CoroutineScope(Dispatchers.IO).launch {
            val blockedMessagesList = mutableListOf<MessageItem>()
            val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
            val projection = arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
            val messageMap = mutableMapOf<Long, MessageItem>()

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
                                isGroupChat = false,
                                isMuted = false,
                                messageMap[threadId]?.lastMsgDate ?: latestMessage.date
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
                } else {
                }
            } catch (e: Exception) {
            }
        }
    }


    fun getBlockedNumbers(): List<String> {
        val blockedNumbers = mutableListOf<String>()
        val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
        if (!context.hasReadSmsPermission() || context.hasReadContactsPermission()) {
            return blockedNumbers
        }

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
        return blockedNumbers
    }

    fun getAllStarredMessages(): LiveData<List<StarredMessage>> {
        return AppDatabase.getDatabase(context).starredMessageDao().getAllStarredMessagesLiveData()
    }

    suspend fun getAllStarredMessagesNew(): Set<Long> {
        return AppDatabase.getDatabase(context).starredMessageDao().getAllStarredMessages()
            .map { it.message_id }.toSet()
    }

    suspend fun deleteStarredMessagesByThreadId(threadId: Long) {
        AppDatabase.getDatabase(context).starredMessageDao()
            .deleteStarredMessagesByThreadId(threadId)
    }

    suspend fun insertStarredMessage(starredMessage: StarredMessage) {
        AppDatabase.getDatabase(context).starredMessageDao().insertStarredMessage(starredMessage)
    }

    suspend fun deleteStarredMessageById(messageId: Long) {
        AppDatabase.getDatabase(context).starredMessageDao().deleteStarredMessageById(messageId)
    }

    suspend fun deleteScheduledByThreadId(threadId: Long) {
        AppDatabase.getDatabase(context).scheduledMessageDao()
            .deleteByThreadId(threadId.toString())
    }

    fun getAllSearchConversation(): List<ConversationItem> {
        val messages = mutableListOf<ConversationItem>()

        if (!context.hasReadSmsPermission()) {
            return messages
        }

        val uri = Telephony.Sms.CONTENT_URI
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

        val sortOrder = "${Telephony.Sms.DATE} DESC"

        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdIndex = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val readIndex = it.getColumnIndexOrThrow(Telephony.Sms.READ)
            val subIdIndex = it.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val threadId = it.getLong(threadIdIndex)
                val date = it.getLong(dateIndex)
                val body = it.getString(bodyIndex) ?: ""
                val address = it.getString(addressIndex) ?: "Unknown"
                val type = it.getInt(typeIndex)
                val read = it.getInt(readIndex) == 1
                val subscriptionId = if (!it.isNull(subIdIndex)) it.getInt(subIdIndex) else -1

                val conversationItem = ConversationItem(
                    id = id,
                    threadId = threadId,
                    date = date,
                    body = body,
                    address = address,
                    type = type,
                    read = read,
                    subscriptionId = subscriptionId,
                    profileImageUrl = "",
                    isHeader = false
                )

                messages.add(conversationItem)
            }
        }

        return messages
    }


    fun getAllContacts(context: Context): List<ContactItem> {
        val contactList = mutableListOf<ContactItem>()
        if (!context.hasReadContactsPermission()) {
            return contactList
        }
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val normalizedIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            val photoIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            val seenNumbers = mutableSetOf<String>()

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)?.replace("\\s".toRegex(), "") ?: continue
                val normalized = it.getString(normalizedIndex) ?: number
                val photoUri = it.getString(photoIndex) ?: ""

                if (seenNumbers.contains(number)) continue
                seenNumbers.add(number)
                Log.d("TAG", "getAllContacts:normalizeNumber " + normalized)

                var number2 = number.replace(
                    " ",
                    ""
                ).replace("(", "").replace(")", "").replace("-", "")
                Log.d("TAG", "getAllContacts:Number " + number2)
                val contact = ContactItem(
                    cid = id,
                    name = name,
                    phoneNumber = number2,
                    normalizeNumber = normalized,
                    profileImageUrl = photoUri
                )

                contactList.add(contact)
            }
        }

        return contactList
    }


    private val notificationDao = AppDatabase.getDatabase(context).notificationDao()
    suspend fun updateOrInsertThread(threadId: Long) {
        if (threadId == -1L) return
        val existingSetting = notificationDao.getNotificationSetting(threadId)
        if (existingSetting == null) {
            val newSetting = NotificationSetting(threadId = threadId)
            notificationDao.insertNotificationSetting(newSetting)
        }
    }

    suspend fun updatePreviewOption(threadId: Long, previewOption: Int) {
        if (threadId == -1L) {
            notificationDao.updateGlobalPreviewOption(previewOption)
        } else {
            notificationDao.updatePreviewOption(threadId, previewOption, true)
        }
    }

    suspend fun insertMissingThreadIds(newThreadIds: List<Long>) {
        if (!context.hasReadSmsPermission()) {
            return
        }
        val existingThreadIds = notificationDao.getAllThreadIds()
        val missingThreadIds = newThreadIds.filter { it !in existingThreadIds }

        if (missingThreadIds.isNotEmpty()) {
            val defaultSettings = missingThreadIds.map { threadId ->
                NotificationSetting(
                    threadId,
                    isNotificationOn = 0,
                    isWakeScreenOn = true,
                    isCustom = false,
                    previewOption = 0
                )
            }
            notificationDao.insertNotificationSettings(defaultSettings)
        }
    }


    fun getContactName(context: Context, phoneNumber: String): String {
        if (!context.hasReadContactsPermission()) {
            return phoneNumber
        }
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return phoneNumber
    }


    fun getPhotoUriFromPhoneNumber(number: String): String {

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup.PHOTO_URI
        )

        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor.use {
                if (cursor?.moveToFirst() == true) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
                        ?: ""
                }
            }
        } catch (ignored: Exception) {
        }

        return ""
    }


    fun getNotificationBitmap(context: Context, photoUri: String): Bitmap? {
        val size = context.resources.getDimension(R.dimen.notification_large_icon_size).toInt()
        if (photoUri.isEmpty()) {
            return null
        }

        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .centerCrop()

        return try {
            Glide.with(context)
                .asBitmap()
                .load(photoUri)
                .apply(options)
                .apply(RequestOptions.circleCropTransform())
                .into(size, size)
                .get()
        } catch (e: Exception) {
            null
        }
    }

    fun getPhoneNumber(context: Context, address: String): String {
        if (Patterns.PHONE.matcher(address).matches()) {
            return address
        }
        val uri = ContactsContract.Contacts.CONTENT_URI
        val projection = arrayOf(ContactsContract.Contacts._ID)
        val selection = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} = ?"
        val selectionArgs = arrayOf(address)

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val contactId = cursor.getString(0)

                    val phoneCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    phoneCursor?.use {
                        if (it.moveToFirst()) {
                            return it.getString(0)
                        }
                    }
                }
            }
        return address
    }


}
