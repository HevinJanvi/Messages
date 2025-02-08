package com.test.messages.demo.viewmodel


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.messages.demo.data.ConversationItem
import com.test.messages.demo.data.MessageItem
import com.test.messages.demo.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repository: MessageRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<MessageItem>>()
    val messages: LiveData<List<MessageItem>> get() = _messages

    private val _conversation = MutableLiveData<List<ConversationItem>>()
    val conversation: LiveData<List<ConversationItem>> = _conversation

    fun loadConversation(threadId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val conversationList = repository.getConversationDetails(threadId) // Fetch SMS details for the thread
            _conversation.postValue(conversationList)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun loadMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            val messageList = repository.getConversations()
            val recipientMap = repository.getRecipientAddresses().toMap()
            val contactMap =
                repository.getContactDetails().associateBy({ it.phoneNumber }, { it.name })
            repository.updateRecipientWithContactNames(HashMap(recipientMap))

            val updatedMessages = messageList
                .filter { it.body != null && it.body?.trim()?.isNotEmpty() == true }
                .map { messageItem ->
                    val phoneNumber =
                        recipientMap[messageItem.reciptid.toString()] ?: "Unknown Number"
                    val contactName = contactMap[phoneNumber] ?: phoneNumber
                    messageItem.copy(sender = contactName.toString()) // Update sender name
                }

            _messages.postValue(updatedMessages)
        }
    }


}
