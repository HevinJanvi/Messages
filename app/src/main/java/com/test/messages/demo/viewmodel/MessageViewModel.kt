package com.test.messages.demo.viewmodel


import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.messages.demo.data.ConversationItem
import com.test.messages.demo.data.MessageItem
import com.test.messages.demo.data.Recipient
import com.test.messages.demo.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repository: MessageRepository
) : ViewModel() {

    val messages: LiveData<List<MessageItem>> = repository.messages
    val conversation: LiveData<List<ConversationItem>> = repository.conversation




    @RequiresApi(Build.VERSION_CODES.Q)
    fun loadMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedMessages = repository.getMessages()
            withContext(Dispatchers.Main) {
                (repository.messages as MutableLiveData).value = updatedMessages
            }
        }
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




}
