package com.test.messages.demo.data.Database.Starred

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.errorprone.annotations.Keep

@Keep
@Entity(tableName = "starred_messages")
data class StarredMessage(
    @PrimaryKey val message_id: Long,
    val thread_id: Long,
    val message: String
)
