package com.test.messages.demo.data.Database.Starred

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.errorprone.annotations.Keep

@Keep
//@Entity(tableName = "starred_messages")
//data class StarredMessage(
//    @PrimaryKey val message_id: Long,
//    val thread_id: Long,
//    val message: String
//)
@Entity(tableName = "starred_messages")
data class StarredMessage(
    @PrimaryKey val message_id: Long,
    val thread_id: Long,
    val body: String,
    val sender: String,
    val number: String,
    val timestamp: Long,
    val is_group_chat: Boolean,
    val profile_image: String? = null,
    val starred: Boolean = true
)