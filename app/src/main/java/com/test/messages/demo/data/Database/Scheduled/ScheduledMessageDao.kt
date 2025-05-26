package com.test.messages.demo.data.Database.Scheduled

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScheduledMessageDao {
//    @Insert
//    fun insert(message: ScheduledMessage)

    @Insert
    fun insert(message: ScheduledMessage): Long

    @get:Query("SELECT * FROM scheduled_messages ORDER BY scheduledTime ASC")
    val allScheduledMessages: LiveData<List<ScheduledMessage>>

//    @Query("SELECT * FROM scheduled_messages WHERE threadId = :threadId LIMIT 1")
//    fun getMessageById(threadId: String?): ScheduledMessage

    @Query("SELECT * FROM scheduled_messages WHERE id = :id")
    fun getMessageById1(id: Int): ScheduledMessage?

    @Delete
    fun delete(message: ScheduledMessage)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    fun deleteById(id: Int)
//    @Query("DELETE FROM scheduled_messages WHERE threadId = :threadId")
//    fun deleteByThreadId(threadId: Long)
}