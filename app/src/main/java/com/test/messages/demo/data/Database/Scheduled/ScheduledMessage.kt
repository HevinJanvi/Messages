package com.test.messages.demo.data.Database.Scheduled

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.errorprone.annotations.Keep

@Keep
@Entity(tableName = "scheduled_messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recipient: String,
    val message: String,
    val scheduledTime: Long,
    val threadId: String,
    val profileUrl: String
)
