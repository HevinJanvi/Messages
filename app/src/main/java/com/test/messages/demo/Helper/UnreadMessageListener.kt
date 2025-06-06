package com.test.messages.demo.Helper

interface UnreadMessageListener {
    fun onUnreadMessagesCountUpdated(count: Int)
}