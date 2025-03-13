package com.test.messages.demo.ui.reciever

data class RefreshMessagesEvent(val timestamp: Long = System.currentTimeMillis())
data class MessageUnstarredEvent(val threadId: Long)
data class UpdateGroupNameEvent(val threadId: Long, val newName: String)
