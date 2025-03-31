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

    @Query("SELECT message_id FROM starred_messages")
    suspend fun getStarredMessageIds(): List<Long>

    @Query("SELECT * FROM starred_messages WHERE message_id = :messageId")
    suspend fun getStarredMessage(messageId: Long): StarredMessage?

    @Query("SELECT * FROM starred_messages")
    fun getAllStarredMessagesLiveData(): LiveData<List<StarredMessage>>

    @Query("SELECT * FROM starred_messages")
    fun getAllStarredMessages(): List<StarredMessage>
    @Query("DELETE FROM starred_messages WHERE message_id = :messageId")
    suspend fun deleteStarredMessageById(messageId: Long)
}
