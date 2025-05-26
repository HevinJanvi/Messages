package com.test.messages.demo.data.Model

import androidx.annotation.Keep

@Keep
data class DraftModel
    (val msg_id: Int, val thraed_id: Int, val draft_label: String, val draft_time: Long)
 