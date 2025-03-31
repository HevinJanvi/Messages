package com.test.messages.demo.data.reciever

interface UnreadMessageListener {
    fun onUnreadMessagesCountUpdated(count: Int)
}