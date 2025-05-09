package com.test.messages.demo.data.viewmodel


import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.messages.demo.data.Database.Archived.ArchivedConversation
import com.test.messages.demo.data.Database.Block.BlockConversation
import com.test.messages.demo.data.Model.ContactItem
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.data.repository.MessageRepository
import dagger.hilt.android.internal.Contexts.getApplication
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

    private val _blockThreadIds = MutableLiveData<Set<Long>>()

    private val _pinnedThreadIds = MutableLiveData<Set<Long>>()

    private val _blockedMessages = MutableLiveData<List<MessageItem>>()
    val blockedMessages: LiveData<List<MessageItem>> get() = _blockedMessages


    @RequiresApi(Build.VERSION_CODES.Q)
    fun loadMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val updatedMessages = repository.getMessages()
//            val unreadMessages = updatedMessages.count { !it.isRead }
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

    private val _conversation = MutableLiveData<List<ConversationItem>>()

    fun getConversation(threadId: Long): List<ConversationItem> {
        val updatedConversation = repository.getConversationDetails(threadId)
        _conversation.postValue(updatedConversation) // Update LiveData for observers
        return updatedConversation // Return the list if needed
    }

    fun getContactNameOrNumber(phoneNumber: String): String {
        return repository.getContactNameOrNumber(phoneNumber)
    }

    fun getContactNameOrNumberLive(phoneNumber: String): LiveData<String> {
        val liveData = MutableLiveData<String>()

        viewModelScope.launch(Dispatchers.IO) {
            val contactName = getContactNameOrNumber(phoneNumber)
            liveData.postValue(contactName)
        }

        return liveData
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


    private val _lastStarredMessages = MutableLiveData<Map<Long, String>>()
    val lastStarredMessages: LiveData<Map<Long, String>> get() = _lastStarredMessages


    //starred
    fun loadLastStarredMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            val starredMessages = repository.getAllStarredMessages()
            val lastStarredMessagesMap = starredMessages
                .groupBy { it.thread_id }
                .mapValues { (_, messages) ->
                    messages.maxByOrNull { it.message_id }?.message ?: ""
                }
            _lastStarredMessages.postValue(lastStarredMessagesMap)
        }
    }

    private val _starredMessageIds = MutableLiveData<Set<Long>>()
    val starredMessageIds: LiveData<Set<Long>> get() = _starredMessageIds

     fun loadStarredMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val starredMessages = repository.getAllStarredMessagesNew()
            withContext(Dispatchers.Main) {
                _starredMessageIds.value = starredMessages
            }
        }
    }

    // Star or unstar selected messages
    fun toggleStarredMessage(messageId: Long, threadId: Long, messageBody: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (_starredMessageIds.value?.contains(messageId) == true) {
               repository.deleteStarredMessageById(messageId)
            } else {
               repository.insertStarredMessage(messageId, threadId, messageBody)
            }
            loadStarredMessages()
        }
    }

    // Star multiple selected messages
    fun starSelectedMessages(selectedItems: List<ConversationItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            selectedItems.forEach { message ->
                if (_starredMessageIds.value?.contains(message.id) == true) {
                    repository.deleteStarredMessageById(message.id)
                } else {
                    repository.insertStarredMessage(
                        message.id,
                        message.threadId,
                        message.body
                    )
                }
            }
            loadStarredMessages()
        }
    }


    //search

    /* private val _fmessages = MutableLiveData<List<MessageItem>>()
     val Filetrmessages: LiveData<List<MessageItem>> get() = _fmessages


     fun setFilteredMessages(newList: List<MessageItem>) {
         _fmessages.value = newList
     }

     fun searchMessages(context: Context,query: String, threadId: String?) {
         viewModelScope.launch(Dispatchers.IO) {
             val filteredMessages = repository.getAllSearchConversation()
             // Group by number (or threadId if needed), but only include groups where ANY message matches the query
             val groupedMessages = filteredMessages
                 .filter { it.number != null }
                 .groupBy { it.number }
                 .mapNotNull { (_, messages) ->
                     // Only pick last message IF it also contains the keyword
                     val lastMatchingMessage = messages.lastOrNull { it.body.contains(query, ignoreCase = true) }
                     lastMatchingMessage
                 }

             withContext(Dispatchers.Main) {
                 _fmessages.value = groupedMessages
             }
         }
     }*/


    //working
    private val _fmessages = MutableLiveData<List<ConversationItem>>()
    val Filetrmessages: LiveData<List<ConversationItem>> get() = _fmessages

    fun setFilteredMessages(newList: List<ConversationItem>) {
        _fmessages.postValue(newList)

    }

    fun getAllMessages(context: Context): List<ConversationItem> {
        return repository.getAllSearchConversation()
    }


    /*fun searchMessages(context: Context, query: String, threadId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val allMessages = repository.getAllSearchConversation(threadId)

            // Filter + last message per address
            val filtered = allMessages
                .asSequence()
                .filter { it.body.contains(query, ignoreCase = true) && it.address.isNotEmpty() }
                .groupBy { it.address }
                .mapNotNull { (_, messages) ->
                    messages.maxByOrNull { it.date }
                }.toList()

            // Get contacts
            val contactMap = repository.getContactDetails().associateBy { it.phoneNumber }

            // Enrich with contact name
            val enriched = filtered.map { msg ->
                val normalized = msg.address.replace(" ", "")
                val contact = contactMap[normalized]
                    ?: contactMap[normalized.removePrefix("+91")]
                    ?: contactMap[normalized.removePrefix("+")] // fallback
                msg.copy(address = contact?.name ?: msg.address)
            }

            withContext(Dispatchers.Main) {
                _fmessages.value = enriched
            }
        }
    }*/

//    fun searchMessages(context: Context, query: String, threadId: String?, isExpanded: Boolean) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val allMessages = repository.getAllSearchConversation(threadId)
//
//            // Filter by query first
//            val filtered = allMessages
//                .asSequence()
//                .filter { it.body.contains(query, ignoreCase = true) && it.address.isNotEmpty() }
//                .groupBy { it.threadId } // Faster grouping if threads are your UI basis
//                .mapNotNull { (_, messages) -> messages.maxByOrNull { it.date } }
//                .let { if (isExpanded) it else it.take(3) }
//                .toList()
//
//            withContext(Dispatchers.Main) {
//                _fmessages.value = filtered
//            }
//        }
//    }


    /* fun searchMessages(context: Context, query: String, threadId: String?) {
         viewModelScope.launch(Dispatchers.IO) {
             if (query.isBlank()) {
                 withContext(Dispatchers.Main) {
                     _fmessages.value = emptyList()
                 }
                 return@launch
             }

             val allMessages = repository.getAllSearchConversation(threadId)

             // Filter messages by query and non-empty address
             val filtered = allMessages
                 .asSequence()
                 .filter { it.body.contains(query, ignoreCase = true) && it.address.isNotEmpty() }
                 .groupBy { it.address }
                 .mapNotNull { (_, messages) ->
                     messages.maxByOrNull { it.date } // Get latest message per address
                 }.toList()

             // Fetch contacts
             val contactMap = repository.getContactDetails()
                 .associateBy { it.phoneNumber.replace(" ", "") }

             // Enrich messages with contact name (fallback if not found)
             val enriched = filtered.map { msg ->
                 val normalized = msg.address.replace(" ", "")
                 val contact = contactMap[normalized]
                     ?: contactMap[normalized.removePrefix("+91")]
                     ?: contactMap[normalized.removePrefix("+")]
                 msg.copy(address = contact?.name ?: msg.address)
             }

             withContext(Dispatchers.Main) {
                 _fmessages.value = enriched
             }
         }
     }*/


    private val _filteredContacts = MutableLiveData<List<ContactItem>>()
    val filteredContacts: LiveData<List<ContactItem>> get() = _filteredContacts

    fun setFilteredContacts(filtered: List<ContactItem>) {
        _filteredContacts.postValue(filtered)
    }

    fun getAllContacts(context: Context): List<ContactItem> {
        return repository.getAllContacts(context)
    }

    fun searchContacts(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val allContacts = repository.getContactDetails()
            val filteredContacts = allContacts.filter {
                it.name?.contains(query, ignoreCase = true) == true ||
                        it.phoneNumber.contains(query, ignoreCase = true)
            }
            withContext(Dispatchers.Main) {
                _filteredContacts.value = filteredContacts
            }
        }
    }


    /*fun searchContacts(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val allContacts = repository.getContactDetails()
            val filteredContacts = allContacts.filter {
                it.name!!.contains(query, ignoreCase = true) || it.phoneNumber.contains(query, ignoreCase = true)
            }

            withContext(Dispatchers.Main) {
                _filteredContacts.value = filteredContacts
            }
        }
    }*/


    fun insertMissingThreadIds(threadIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMissingThreadIds(threadIds)
        }
    }

    fun updateOrInsertThread(threadId: Long) {
        viewModelScope.launch {
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

    fun getMutedThreadIds(): List<Long> {
        return runBlocking { repository.getMutedThreadIds() }
    }


    private val _conversations22 = MutableLiveData<List<MessageItem>>()
    val conversations22: LiveData<List<MessageItem>> get() = _conversations22

    fun getConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            val fetchedConversations = repository.getConversations()
            _conversations22.postValue(fetchedConversations)
        }
    }

    fun getContactName(context: Context, phoneNumber: String): String {
        return repository.getContactName(context, phoneNumber)
    }

    fun getContactNumber(context: Context, address: String): String {
        return repository.getPhoneNumber(context, address)
    }


}
