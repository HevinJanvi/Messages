package com.test.messages.demo.data.Model

import com.google.errorprone.annotations.Keep

@Keep
data class SearchableContact(
    val original: ContactItem,
    val searchText: String
)
