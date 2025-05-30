package com.test.messages.demo.data.Model

import com.google.errorprone.annotations.Keep

@Keep
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
    val isMuted: Boolean,
    val lastMsgDate: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageItem) return false
        return threadId == other.threadId && sender == other.sender &&
                body == other.body && isMuted == other.isMuted && isPinned == other.isPinned
                && isRead == other.isRead && profileImageUrl == other.profileImageUrl
    }

    override fun hashCode(): Int {
        return threadId.hashCode() * 31 + body.hashCode()
    }
}
