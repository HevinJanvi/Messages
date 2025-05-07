package com.test.messages.demo.data.Database.Block

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.errorprone.annotations.Keep

@Keep
@Entity(tableName = "block_conversations")
data class BlockConversation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val isBlocked: Boolean,
    val number: String
)