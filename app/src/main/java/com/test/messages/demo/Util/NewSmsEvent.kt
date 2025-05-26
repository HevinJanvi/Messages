package com.test.messages.demo.Util

data class RefreshMessagesEvent(val timestamp: Long = System.currentTimeMillis())
data class MessageUnstarredEvent(
    val threadId: Long, val lastMessage: String?,
    val lastMessageTime: Long?
)

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
data class MessagesRefreshEvent(val success: Boolean)
data class MarkasreadEvent(val success: Boolean)

data class MessageRestoredEvent(
    val threadId: Long,
    val lastMessage: String,
    val lastMessageTime: Long?
)

data class LanguageChangeEvent(val language: String)
data class ConversationOpenedEvent(val threadId: Long)
data class DraftChangedEvent(val threadId: Long)

data class DeleteSearchMessageEvent(val deletedMessageIds: List<Long>)
data class NewSmsEvent(val threadId: Long)
data class ActivityFinishEvent(val success: Boolean)

class ConversationUpdatedEvent(val threadId: Long)

