package com.test.messages.demo.data.viewmodel


import android.content.Context
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.messages.demo.Util.Constants.GROUP_SEPARATOR
import com.test.messages.demo.data.Database.Archived.ArchivedConversation
import com.test.messages.demo.data.Database.Block.BlockConversation
import com.test.messages.demo.data.Database.Starred.StarredMessage
import com.test.messages.demo.data.Model.ContactItem
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.Q)
class MessageViewModel @Inject constructor(
    val repository: MessageRepository

) : ViewModel() {

    val messages: LiveData<List<MessageItem>> = repository.messages
    val conversation: LiveData<List<ConversationItem>?> = repository.conversation

    private val _contacts = MutableLiveData<List<ContactItem>>()
    val contacts: LiveData<List<ContactItem>> get() = _contacts

    private val _archivedThreadIds = MutableLiveData<Set<Long>>()

    private val _blockThreadIds = MutableLiveData<Set<Long>>()

    private val _pinnedThreadIds = MutableLiveData<Set<Long>>()
    private val _mutedThreadIds = MutableLiveData<Set<Long>>()

    private val _blockedMessages = MutableLiveData<List<MessageItem>>()
    val blockedMessages: LiveData<List<MessageItem>> get() = _blockedMessages


    @RequiresApi(Build.VERSION_CODES.Q)
    fun loadMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val updatedMessages = repository.getMessages()
            withContext(Dispatchers.Main) {
                (repository.messages as MutableLiveData).value = updatedMessages
            }
        }
    }

    val isLoading = MutableLiveData<Boolean>()

    @RequiresApi(Build.VERSION_CODES.Q)
    fun loadRestoreMessages(showLoading: Boolean = false) {
        viewModelScope.launch {
            isLoading.postValue(true)
            val updatedMessages = withContext(Dispatchers.IO) {
                repository.getMessages()
            }
            (repository.messages as MutableLiveData).postValue(updatedMessages)
            isLoading.postValue(false)
        }
    }

    fun emptyConversation() {
        repository.emptyConversation()
    }

    private var job: Job? = null
    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }


    fun loadConversation(threadId: Long) {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            repository.getConversation(threadId)
        }
    }

    fun getContactNameOrNumber(phoneNumber: String): String {
        return repository.getContactNameOrNumber(phoneNumber)
    }
    fun loadContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val contactList = repository.getContactDetails()
            withContext(Dispatchers.Main) {
                _contacts.value = contactList
            }
        }
    }

    fun loadArchivedThreads() {
        viewModelScope.launch {
            val archivedIds = repository.getArchivedThreadIds().toSet()
            _archivedThreadIds.postValue(archivedIds)
        }
    }

    suspend fun getArchivedConversations(): List<ArchivedConversation> {
        return repository.getArchivedConversations()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun archiveSelectedConversations(conversationIds: List<Long>) {
        viewModelScope.launch {
            repository.archiveConversations(conversationIds)
            val updatedMessages = repository.getMessages()
            (repository.messages as MutableLiveData).postValue(updatedMessages)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteArchiveConversations(conversationIds: List<Long>) {
        viewModelScope.launch {
            repository.unarchiveConversations(conversationIds)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun unarchiveConversations(conversationIds: List<Long>) {
        viewModelScope.launch {
            repository.unarchiveConversations(conversationIds)
            val updatedArchivedIds =
                _archivedThreadIds.value?.filterNot { it in conversationIds }?.toSet()
            _archivedThreadIds.postValue(
                updatedArchivedIds
            )
            val updatedMessages = repository.getMessages()
            (repository.messages as MutableLiveData).postValue(updatedMessages)
        }
    }

    fun updateMessages(newList: List<MessageItem>) {
        (repository.messages as MutableLiveData).postValue(newList)
    }

    fun getPinnedThreadIds(): List<Long> {
        return runBlocking(Dispatchers.IO) { repository.getPinnedThreadIds() }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun togglePin(selectedIds: List<Long>, callback: () -> Unit) {

        CoroutineScope(Dispatchers.IO).launch {
            val pinnedThreadIds = repository.getPinnedThreadIds().toMutableSet()

            val pinnedMessages = selectedIds.filter { it in pinnedThreadIds }  // Already pinned
            val unpinnedMessages = selectedIds.filter { it !in pinnedThreadIds } // Not pinned yet
            when {
                pinnedMessages.isNotEmpty() && unpinnedMessages.isNotEmpty() -> {
                    when {
                        pinnedMessages.size >= unpinnedMessages.size -> {
                            // If pinned >= unpinned, we unpin all
                            repository.removePinnedMessages(pinnedMessages)
                        }
                        else -> {
                            // More unpinned → Pin only unpinned ones
                            repository.addPinnedMessages(unpinnedMessages)
                        }
                    }
                }

                pinnedMessages.isNotEmpty() -> {
                    // Only pinned messages selected → Unpin them
                    repository.removePinnedMessages(pinnedMessages)
                }

                unpinnedMessages.isNotEmpty() -> {
                    // Only unpinned messages selected → Pin them
                    repository.addPinnedMessages(unpinnedMessages)
                }
            }
            withContext(Dispatchers.Main) {
                callback.invoke()
            }
            refreshPinnedMessages()
        }
    }

    fun isPinned(threadId: Long): Boolean {
        return _pinnedThreadIds.value?.contains(threadId) == true
    }


    fun isMuted(threadId: Long): Boolean {
        return _mutedThreadIds.value?.contains(threadId) == true
    }

    fun getMutedThreadIds(): List<Long> {
        return runBlocking { repository.getMutedThreadIds() }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun toggleMute(selectedIds: List<Long>, callback: () -> Unit) {

        CoroutineScope(Dispatchers.IO).launch {
            val muteThreadIds = repository.getMutedThreadIds().toMutableSet()

            val mutedMessages = selectedIds.filter { it in muteThreadIds }  // Already muted
            val unmutedMessages = selectedIds.filter { it !in muteThreadIds } // Not unmuted yet
            when {
                mutedMessages.isNotEmpty() && unmutedMessages.isNotEmpty() -> {
                    when {
                        mutedMessages.size >= unmutedMessages.size -> {
                            // Ifmuted >= unmuted, we unmute all
                            repository.removeMutedMessages(mutedMessages)
                        }
                        else -> {
                            // More unmuted → mute only unmute ones
                            repository.addMutedMessages(unmutedMessages)
                        }
                    }
                }
                mutedMessages.isNotEmpty() -> {
                    // Only muted messages selected → Unmute them
                    repository.removeMutedMessages(mutedMessages)
                }
                unmutedMessages.isNotEmpty() -> {
                    // Only unmuted messages selected → mute them
                    repository.addMutedMessages(unmutedMessages)
                }
            }
            withContext(Dispatchers.Main) {
                callback.invoke()
            }
        }
    }

    private suspend fun refreshPinnedMessages() = withContext(Dispatchers.IO) {
        val updatedMessages = repository.getMessages()
        val pinnedThreadIds = repository.getPinnedThreadIds()
        val sortedMessages =
            updatedMessages.sortedByDescending { pinnedThreadIds.contains(it.threadId) }
        withContext(Dispatchers.Main) {
            (repository.messages as MutableLiveData).postValue(sortedMessages)
        }
    }

    suspend fun getBlockedNumbers(): List<String> {
        return repository.getBlockedNumbers()
    }

    fun blockContacts(contactToBlock: List<MessageItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.blockContacts(contactToBlock)
            val updatedMessages = repository.getMessages()
            withContext(Dispatchers.Main) {
                (repository.messages as MutableLiveData).value = updatedMessages
            }
        }
    }

    fun loadBlockedMessages() {
        repository.getBlockedContacts().observeForever { messages ->
            _blockedMessages.postValue(messages)
        }
    }

    fun loadBlockThreads() {
        viewModelScope.launch {
            val blockIds = repository.getBlockThreadIds().toSet()
            _blockThreadIds.postValue(blockIds)
        }
    }

    suspend fun getBlockedThreadIdForNumber(phoneNumber: String): Long? {
        return repository.getBlockedThreadId(phoneNumber)
    }

    suspend fun getBlockedConversations(): List<BlockConversation> {
        return repository.getBlockConversations()
    }

    fun deleteblockConversations(conversationIds: List<Long>) {
        viewModelScope.launch {
            repository.unblockConversations(conversationIds)
        }
    }

    fun blockSelectedConversations(
        conversationIds: List<Long>,
        callback: () -> Unit
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.blockConversations(conversationIds)
                val updatedBlockIds =
                    repository.getBlockConversations().map { it.conversationId }.toSet()
                _blockThreadIds.postValue(updatedBlockIds.toSet())

                conversationIds.forEach {
                   deleteScheduledForThread(it)
                }
                val updatedMessages = repository.getMessages()
                (repository.messages as MutableLiveData).postValue(updatedMessages)
                withContext(Dispatchers.Main) {
                    callback.invoke()
                }
            }

        }
    }

    fun blockSelectedConversations(conversationIds: List<Long>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.blockConversations(conversationIds)
                val updatedBlockIds =
                    repository.getBlockConversations().map { it.conversationId }.toSet()
                _blockThreadIds.postValue(updatedBlockIds.toSet())
                conversationIds.forEach {
                    deleteScheduledForThread(it)
                }
                val updatedMessages = repository.getMessages()
                (repository.messages as MutableLiveData).postValue(updatedMessages)
            }

        }
    }


    fun unblockConversations(conversationIds: List<Long>) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.unblockConversations(conversationIds)
            val updatedBlockIds =
                _blockThreadIds.value?.filterNot { it in conversationIds }?.toSet()
            _blockThreadIds.postValue(updatedBlockIds)
            val updatedMessages = repository.getMessages()
            (repository.messages as MutableLiveData).postValue(updatedMessages)
        }
    }

    private val _starredMessageIds = MutableLiveData<Set<Long>>()
    val starredMessageIds: LiveData<Set<Long>> get() = _starredMessageIds
    fun getAllStarredMessages(): LiveData<List<StarredMessage>> {
        return repository.getAllStarredMessages()
    }

    fun loadStarredMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val starredMessages = repository.getAllStarredMessagesNew()
            withContext(Dispatchers.Main) {
                _starredMessageIds.value = starredMessages
            }
        }
    }

    fun deleteStarredMessagesForThread(threadId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStarredMessagesByThreadId(threadId)
        }
    }


    fun toggleStarredMessage(
        messageId: Long,
        threadId: Long,
        messageBody: String,
        timestamp: Long,
        currentStarred: Boolean,
        isGroupChat: Boolean,
        profileImageUrl: String?,
        senderNumber: String,
        senderName: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (_starredMessageIds.value?.contains(messageId) == true || currentStarred) {
                repository.deleteStarredMessageById(messageId)
            } else {
                val starredMessage = StarredMessage(
                    message_id = messageId,
                    thread_id = threadId,
                    body = messageBody,
                    timestamp = timestamp,
                    starred = true,
                    is_group_chat = isGroupChat,
                    profile_image = profileImageUrl,
                    number = senderNumber,
                    sender = senderName
                )
                repository.insertStarredMessage(starredMessage)
            }
            loadStarredMessages()
            /*val starredMessages = repository.getAllStarredMessagesNew()
            withContext(Dispatchers.Main) {
                _starredMessageIds.value = starredMessages
            }*/
        }
    }


    fun starSelectedMessages(selectedItems: List<ConversationItem>, name: String, number: String) {
        CoroutineScope(Dispatchers.IO).launch {
            selectedItems.forEach { message ->
                if (_starredMessageIds.value?.contains(message.id) == true) {
                    repository.deleteStarredMessageById(message.id)
                    message.starred = false
                } else {
                    val isGroup = message.address.contains(GROUP_SEPARATOR)
                    Log.d("TAG", "starSelectedMessages: "+message.profileImageUrl)
                    repository.insertStarredMessage(
                        StarredMessage(
                            message_id = message.id,
                            thread_id = message.threadId,
                            body = message.body,
                            sender = name ?: message.address,
                            number = number ?: message.address,
                            timestamp = message.date,
                            is_group_chat =isGroup,
                            profile_image = message.profileImageUrl
                        )
                    )
                    message.starred = true
                }
            }
            val starredMessages =
                repository.getAllStarredMessagesNew()
            withContext(Dispatchers.Main) {
                _starredMessageIds.value = starredMessages
            }
        }
    }

    private val _fmessages = MutableLiveData<List<ConversationItem>>()
    val Filetrmessages: LiveData<List<ConversationItem>> get() = _fmessages

    fun setFilteredMessages(newList: List<ConversationItem>) {
        _fmessages.postValue(newList)
    }

    fun getAllMessages(context: Context): List<ConversationItem> {
        return repository.getAllSearchConversation()
    }

    private val _filteredContacts = MutableLiveData<List<ContactItem>>()
    val filteredContacts: LiveData<List<ContactItem>> get() = _filteredContacts

    fun setFilteredContacts(filtered: List<ContactItem>) {
        _filteredContacts.postValue(filtered)
    }

    fun getAllContacts(context: Context): List<ContactItem> {
        return repository.getAllContacts(context)
    }

    fun insertMissingThreadIds(threadIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMissingThreadIds(threadIds)
        }
    }

    fun updateOrInsertThread(threadId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.updateOrInsertThread(threadId)
        }
    }


    private val _previewOption = MutableLiveData<Int>()
    fun updatePreviewOption(threadId: Long, option: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePreviewOption(threadId, option)
            withContext(Dispatchers.Main) {
                _previewOption.value = option
            }
        }
    }

    fun getContactName(context: Context, phoneNumber: String): String {
        return repository.getContactName(context, phoneNumber)
    }

    fun getContactNumber(context: Context, address: String): String {
        return repository.getPhoneNumber(context, address)
    }

    fun deleteScheduledForThread(threadId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteScheduledByThreadId(threadId)
        }
    }

    fun deleteSelectedMessages(
        context: Context,
        selectedMessages: List<ConversationItem>,
        isFromSearch: Boolean,
        onComplete: (List<Long>?) -> Unit
    ) {
        viewModelScope.launch {
            val deletedIds = withContext(Dispatchers.IO) {
                val messageIds = selectedMessages.map { it.id }
                val deletedMessages = selectedMessages.map { message ->
                    val isGroup = message.address.contains(GROUP_SEPARATOR)
                    DeletedMessage(
                        messageId = message.id,
                        threadId = message.threadId,
                        address = message.address,
                        date = message.date,
                        body = message.body,
                        type = message.type,
                        read = message.read,
                        subscriptionId = message.subscriptionId,
                        deletedTime = System.currentTimeMillis(),
                        isGroupChat = isGroup,
                        profileImageUrl = message.profileImageUrl
                    )
                }

                val dao = AppDatabase.getDatabase(context).recycleBinDao()
                dao.insertMessages(deletedMessages)

                if (messageIds.isNotEmpty()) {
                    val placeholders = messageIds.joinToString(GROUP_SEPARATOR) { "?" }
                    val selectionArgs = messageIds.map { it.toString() }.toTypedArray()

                    context.contentResolver.delete(
                        Telephony.Sms.CONTENT_URI,
                        "_id IN ($placeholders)",
                        selectionArgs
                    )
                }
                if (messageIds.isNotEmpty()) {
                    val starredMessageDao = AppDatabase.getDatabase(context).starredMessageDao()
                    starredMessageDao.deleteStarredMessagesByIds(messageIds)
                }
                if (isFromSearch && selectedMessages.isNotEmpty()) {
                    selectedMessages.map { it.id }
                } else null
            }
            onComplete(deletedIds)
        }
    }


}
