package com.test.messages.demo.data.Model

import androidx.annotation.Keep

@Keep
data class SIMCard(val id: Int, val subscriptionId: Int, val label: String, val number: String)
 