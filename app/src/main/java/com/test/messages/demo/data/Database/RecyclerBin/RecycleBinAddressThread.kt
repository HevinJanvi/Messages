package com.test.messages.demo.data.Database.RecyclerBin

import androidx.annotation.Keep
import androidx.room.ColumnInfo

@Keep
data class RecycleBinAddressThread(
    @ColumnInfo(name = "address")
    val address: String,
    @ColumnInfo(name = "thread_id")
    val threadId: Long
)