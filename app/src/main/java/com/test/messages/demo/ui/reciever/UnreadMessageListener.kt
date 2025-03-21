package com.test.messages.demo.ui.reciever

interface UnreadMessageListener {
    fun onUnreadMessagesCountUpdated(count: Int)
}