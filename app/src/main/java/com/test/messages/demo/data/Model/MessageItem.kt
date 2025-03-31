package com.test.messages.demo.data.Model


data class MessageItem(
    val threadId: Long,
    val sender: String,
    val number: String,
    val body: String,
    val timestamp: Long,
    var isRead: Boolean,
    val reciptid: Int,
    val reciptids: String,
    val profileImageUrl: String,
    var isPinned: Boolean,
    val isGroupChat: Boolean,
    val isMuted: Boolean
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageItem) return false
        return threadId == other.threadId && body == other.body
    }

    override fun hashCode(): Int {
        return threadId.hashCode() * 31 + body.hashCode()
    }
}
