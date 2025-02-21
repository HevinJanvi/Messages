package com.test.messages.demo.Database.Archived

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "archived_conversations")
data class ArchivedConversation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val isArchived: Boolean
)