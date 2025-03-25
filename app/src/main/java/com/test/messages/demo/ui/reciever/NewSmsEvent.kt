package com.test.messages.demo.ui.reciever

import com.test.messages.demo.data.MessageItem

data class RefreshMessagesEvent(val timestamp: Long = System.currentTimeMillis())
data class MessageUnstarredEvent(val threadId: Long)
data class UpdateGroupNameEvent(val threadId: Long, val newName: String)
data class MessageDeletedEvent(
    val threadId: Long,
    val lastMessage: String? // Store the last message text
)

data class ConversationDeletedEvent(
    val threadId: Long)

