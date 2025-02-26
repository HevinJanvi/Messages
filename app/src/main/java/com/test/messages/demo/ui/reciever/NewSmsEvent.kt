package com.test.messages.demo.ui.reciever

data class NewSmsEvent(val threadId: Long)
data class SelectedMessagesEvent(val selectedIds: List<Long>)
data class PinEvent(val selectedIds: List<Long>, val isPin: Boolean)
