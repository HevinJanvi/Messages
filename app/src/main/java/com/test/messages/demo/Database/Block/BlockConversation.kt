package com.test.messages.demo.Database.Block

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_conversations")
data class BlockConversation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val isBlocked: Boolean,
    val number: String
)