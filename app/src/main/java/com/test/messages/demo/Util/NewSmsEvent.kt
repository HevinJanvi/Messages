package com.test.messages.demo.Util

data class RefreshMessagesEvent(val timestamp: Long = System.currentTimeMillis())
data class MessageUnstarredEvent(val threadId: Long)
data class UpdateGroupNameEvent(val threadId: Long, val newName: String)
data class MessageDeletedEvent(
    val threadId: Long,
    val lastMessage: String?,
    val lastMessageTime: Long?
)

data class CategoryVisibilityEvent(val isEnabled: Boolean)

data class CategoryUpdateEvent(val updatedCategories: List<String>)

data class SwipeActionEvent(val action: Int, val isRightSwipe: Boolean)

data class MessagesRestoredEvent(val success: Boolean)

data class MessageRestoredEvent(val threadId: Long, val lastMessage: String,val lastMessageTime: Long?)


