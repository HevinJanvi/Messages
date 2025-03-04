package com.test.messages.demo.Database.Archived

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArchivedDao {

    @Query("SELECT * FROM archived_conversations")
    fun getAllArchivedConversations(): List<ArchivedConversation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchivedConversations(archivedConversations: List<ArchivedConversation>)

    @Query("SELECT conversationId FROM archived_conversations WHERE isArchived = 1")
    fun getArchivedThreadIds(): List<Long>

    @Query("DELETE FROM archived_conversations WHERE conversationId = :conversationId")
    suspend fun unarchiveConversation(conversationId: Long)

}
