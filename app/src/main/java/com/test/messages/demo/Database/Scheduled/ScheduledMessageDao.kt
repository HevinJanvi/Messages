package com.test.messages.demo.Database.Scheduled

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScheduledMessageDao {
    @Insert
    fun insert(message: ScheduledMessage)

    @get:Query("SELECT * FROM scheduled_messages ORDER BY scheduledTime ASC")
    val allScheduledMessages: LiveData<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE threadId = :threadId LIMIT 1")
    fun getMessageById(threadId: String?): ScheduledMessage

    @Delete
    fun delete(message: ScheduledMessage)
}