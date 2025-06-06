package com.test.messages.demo.data.Database.Block

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlockDao {

    @Query("SELECT * FROM block_conversations")
    fun getAllBlockConversations(): List<BlockConversation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockConversations(blockConversations: List<BlockConversation>)

    @Query("SELECT conversationId FROM block_conversations WHERE isBlocked = 1")
    fun getBlockThreadIds(): List<Long>

    @Query("SELECT COUNT(*) FROM block_conversations WHERE number = :number")
    suspend fun isDeleted(number: String): Boolean

    @Query("DELETE FROM block_conversations WHERE number = :phoneNumber AND conversationId != :newThreadId")
    suspend fun deleteOldBlockedThreads(phoneNumber: String, newThreadId: Long)

    @Query("SELECT COUNT(*) FROM block_conversations WHERE conversationId = :threadId")
    suspend fun isThreadBlocked(threadId: Long): Int

    @Query("DELETE FROM block_conversations WHERE conversationId = :conversationId")
    suspend fun unblockConversation(conversationId: Long)

}
