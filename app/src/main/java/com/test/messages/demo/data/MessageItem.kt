package com.test.messages.demo.data

data class MessageItem(
    val threadId: Long,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean,
    val reciptid: Int
)