package com.test.messages.demo.viewmodel


import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.messages.demo.Database.Archived.ArchivedConversation
import com.test.messages.demo.data.ContactItem
import com.test.messages.demo.data.ConversationItem
import com.test.messages.demo.data.MessageItem
import com.test.messages.demo.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repository: MessageRepository
) : ViewModel() {

    val messages: LiveData<List<MessageItem>> = repository.messages
    val conversation: LiveData<List<ConversationItem>> = repository.conversation

    private val _contacts = MutableLiveData<List<ContactItem>>()
    val contacts: LiveData<List<ContactItem>> get() = _contacts

    private val _archivedThreadIds = MutableLiveData<Set<Long>>()
    val archivedThreadIds: LiveData<Set<Long>> get() = _archivedThreadIds

    private val _messages2 = MutableLiveData<List<MessageItem>>()
    val messages2: LiveData<List<MessageItem>> get() = _messages2


    @RequiresApi(Build.VERSION_CODES.Q)
    fun loadMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val updatedMessages = repository.getMessages()
            val unreadMessages = updatedMessages.count { !it.isRead }
            withContext(Dispatchers.Main) {
                (repository.messages as MutableLiveData).value = updatedMessages
                _messages2.value = updatedMessages
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
            Log.d("ArchiveDebug", "Archived Thread IDs: $archivedIds")
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

            val updatedArchivedIds = _archivedThreadIds.value?.filterNot { it in conversationIds }?.toSet()
            _archivedThreadIds.postValue(updatedArchivedIds)

            val updatedMessages = repository.getMessages()
            (repository.messages as MutableLiveData).postValue(updatedMessages)
        }
    }

    fun updateMessages(newList: List<MessageItem>) {
        (repository.messages as MutableLiveData).value = newList
    }

}
