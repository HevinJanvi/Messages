package com.test.messages.demo.data.Model

import android.provider.Telephony

data class ConversationItem(
    val id: Long,
    val threadId: Long,
    val date: Long,
    val body: String,
    val address: String,
    val type: Int,
    val read: Boolean,
    val subscriptionId: Int,
    val profileImageUrl: String,
    val isHeader: Boolean
) {
    companion object {
        fun createHeader(headerText: String, date: Long): ConversationItem {
            return ConversationItem(
                id = -date,
                threadId = -1,
                date = date,
                body = headerText,
                address = "",
                type = -1,
                read = true,
                subscriptionId = -1,
                profileImageUrl = "",
                isHeader = true
            )
        }
    }

    fun isIncoming(): Boolean {
        return type == Telephony.Sms.MESSAGE_TYPE_INBOX
    }
    fun extractLink(): String? {
        val linkRegex = "(https?://[\\w./?=#%&_-]+)".toRegex()
        return linkRegex.find(body)?.value
    }
}