package com.test.messages.demo.data.Database.Pin

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PinDao {

    @Query("SELECT COUNT(*) FROM pinned_messages WHERE thread_id IN (:threadIds)")
    suspend fun isPinned(threadIds: List<Long>): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
     fun insertPinnedMessages(pinnedMessages: List<PinMessage>)

    @Query("UPDATE pinned_messages SET thread_id = :newThreadId WHERE thread_id = :oldThreadId")
    fun updateThreadId(oldThreadId: Long, newThreadId: Long)

    @Query("DELETE FROM pinned_messages WHERE thread_id IN (:threadIds)")
    suspend fun removePinnedMessages(threadIds: List<Long>)

     @Query("DELETE FROM pinned_messages WHERE thread_id IN (:threadIds)")
     fun deletePinnedMessage(threadIds: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun pinMessage(pinEntity: PinMessage)

    @Query("DELETE FROM pinned_messages WHERE thread_id = :threadId")
    suspend fun unpinMessage(threadId: Long)

    @Query("SELECT * FROM pinned_messages")
    fun getPinnedMessages(): List<PinMessage>

    @Query("SELECT * FROM pinned_messages WHERE thread_id = :threadId")
    suspend fun isMessagePinned(threadId: Long): PinMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun pinThread(pinnedMessage: PinMessage)

    @Query("DELETE FROM pinned_messages WHERE thread_id = :threadId")
    suspend fun unpinThread(threadId: Long)

    @Query("SELECT thread_id FROM pinned_messages")
    fun getAllPinnedThreadIds(): List<Long>

    @Query("SELECT * FROM pinned_messages WHERE thread_id = :threadId")
    suspend fun isThreadPinned(threadId: List<Long>): PinMessage?
}
