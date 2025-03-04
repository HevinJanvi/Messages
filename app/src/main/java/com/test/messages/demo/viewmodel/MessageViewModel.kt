package com.test.messages.demo.viewmodel


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.messages.demo.Database.Archived.ArchivedConversation
import com.test.messages.demo.Database.Block.BlockConversation
import com.test.messages.demo.data.ContactItem
import com.test.messages.demo.data.ConversationItem
import com.test.messages.demo.data.MessageItem
import com.test.messages.demo.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.Q)
class MessageViewModel @Inject constructor(
    private val repository: MessageRepository
) : ViewModel() {

    val messages: LiveData<List<MessageItem>> = repository.messages
    val conversation: LiveData<List<ConversationItem>> = repository.conversation

    private val _contacts = MutableLiveData<List<ContactItem>>()
    val contacts: LiveData<List<ContactItem>> get() = _contacts

    private val _archivedThreadIds = MutableLiveData<Set<Long>>()
    val archivedThreadIds: LiveData<Set<Long>> get() = _archivedThreadIds

    private val _blockThreadIds = MutableLiveData<Set<Long>>()
    val blockThreadIds: LiveData<Set<Long>> get() = _blockThreadIds

    private val _pinnedThreadIds = MutableLiveData<Set<Long>>()
    val pinnedThreadIds: LiveData<Set<Long>> get() = _pinnedThreadIds

    init {
        _pinnedThreadIds.value = emptySet() // or load from database/storage
    }

    private val _blockedMessages = MutableLiveData<List<MessageItem>>()
    val blockedMessages: LiveData<List<MessageItem>> get() = _blockedMessages


    @RequiresApi(Build.VERSION_CODES.Q)
    fun loadMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val updatedMessages = repository.getMessages()
            val unreadMessages = updatedMessages.count { !it.isRead }
            withContext(Dispatchers.Main) {
                (repository.messages as MutableLiveData).value = updatedMessages
            }
        }
    }

    fun emptyConversation() {
        repository.emptyConversation()
    }

    fun loadConversation(threadId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val conversationList = repository.getConversation(threadId)
            withContext(Dispatchers.Main) {
                (repository.conversation as MutableLiveData).value = conversationList
            }
        }
    }

    fun getContactNameOrNumber(phoneNumber: String): String {
        return repository.getContactNameOrNumber(phoneNumber)
    }

    fun findGroupThreadId(addresses: Set<String>): Long? {
        return repository.findGroupThreadId(addresses)
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
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun unarchiveConversations(conversationIds: List<Long>) {
        viewModelScope.launch {
            repository.unarchiveConversations(conversationIds)
            val updatedArchivedIds =
                _archivedThreadIds.value?.filterNot { it in conversationIds }?.toSet()
            _archivedThreadIds.postValue(updatedArchivedIds)
            val updatedMessages = repository.getMessages()
            (repository.messages as MutableLiveData).postValue(updatedMessages)
        }
    }

    fun updateMessages(newList: List<MessageItem>) {
        (repository.messages as MutableLiveData).value = newList
    }

    fun getPinnedThreadIds(): List<Long> {
//        return repository.getPinnedThreadIds()
        return runBlocking(Dispatchers.IO) { repository.getPinnedThreadIds() }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun togglePin(selectedIds: List<Long>) {

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
            refreshPinnedMessages()
        }
    }

    fun isPinned(threadId: Long): Boolean {
        return _pinnedThreadIds.value?.contains(threadId) == true
    }

    private suspend fun refreshPinnedMessages() {
        val updatedMessages = repository.getMessages()
        val pinnedThreadIds = repository.getPinnedThreadIds()
        val sortedMessages =
            updatedMessages.sortedByDescending { pinnedThreadIds.contains(it.threadId) }

        withContext(Dispatchers.Main) {
            (repository.messages as MutableLiveData).value = sortedMessages
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

    @RequiresApi(Build.VERSION_CODES.Q)
    fun blockSelectedConversations(conversationIds: List<Long>) {
        viewModelScope.launch {
            repository.blockConversations(conversationIds)

        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun unblockConversations(conversationIds: List<Long>) {
        viewModelScope.launch {
            repository.unblockConversations(conversationIds)
            val updatedBlockIds =
                _blockThreadIds.value?.filterNot { it in conversationIds }?.toSet()
            _blockThreadIds.postValue(updatedBlockIds)
            val updatedMessages = repository.getMessages()
            (repository.messages as MutableLiveData).postValue(updatedMessages)
        }
    }
}
