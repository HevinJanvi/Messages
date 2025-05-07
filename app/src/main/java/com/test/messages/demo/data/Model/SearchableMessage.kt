package com.test.messages.demo.data.Model

import com.google.errorprone.annotations.Keep

@Keep
data class SearchableMessage(
    val original: ConversationItem,
    val searchText: String
)
