package com.test.messages.demo.data.Database.Starred

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StarredMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStarredMessage(starredMessage: StarredMessage)

    @Query("SELECT * FROM starred_messages WHERE message_id = :messageId")
    suspend fun getStarredMessage(messageId: Long): StarredMessage?

    @Query("SELECT * FROM starred_messages")
    fun getAllStarredMessagesLiveData(): LiveData<List<StarredMessage>>

    @Query("SELECT * FROM starred_messages")
    fun getAllStarredMessages(): List<StarredMessage>

    @Query("DELETE FROM starred_messages WHERE message_id = :messageId")
    suspend fun deleteStarredMessageById(messageId: Long)

    @Query("DELETE FROM starred_messages WHERE message_id IN (:messageIds)")
    suspend fun deleteStarredMessagesByIds(messageIds: List<Long>)

    @Query("DELETE FROM starred_messages WHERE thread_id = :threadId")
    suspend fun deleteStarredMessagesByThreadId(threadId: Long)

    @Query("UPDATE starred_messages SET sender = :newName, profile_image = :newUrl WHERE thread_id = :threadId")
    suspend fun updateNameAndProfile(threadId: Long, newName: String, newUrl: String)


}
