package com.test.messages.demo.data

data class Message(
    val id: String,
    var sender: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean,

)
