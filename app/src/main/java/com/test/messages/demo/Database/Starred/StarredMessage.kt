package com.test.messages.demo.Database.Starred

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "starred_messages")
data class StarredMessage(
    @PrimaryKey val message_id: Long,
    val thread_id: Long,
    val message: String
)
