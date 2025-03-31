package com.test.messages.demo.data.Database.Pin

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_messages")
data class PinMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "thread_id") val threadId: Long,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean
)
