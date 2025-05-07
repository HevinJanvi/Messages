package com.test.messages.demo.data.Database.Archived

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.errorprone.annotations.Keep

@Keep
@Entity(tableName = "archived_conversations")
data class ArchivedConversation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val isArchived: Boolean
)