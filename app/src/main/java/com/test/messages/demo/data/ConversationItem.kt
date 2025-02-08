package com.test.messages.demo.data

import android.provider.Telephony

data class ConversationItem(
    val id: Long,
    val threadId: Long,
    val date: Long,
    val body: String,
    val address: String,
    val type: Int,
    val read: Boolean,
    val subscriptionId: Int
) {
    fun isIncoming(): Boolean {
        return type == Telephony.Sms.MESSAGE_TYPE_INBOX
    }
}