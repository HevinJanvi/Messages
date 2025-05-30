package easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin

import androidx.lifecycle.LiveData
import androidx.room.*
import com.test.messages.demo.data.Database.RecyclerBin.RecycleBinAddressThread

@Dao
interface RecycleBinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: DeletedMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(messages: List<DeletedMessage>)

    @Query("SELECT * FROM recycle_bin WHERE thread_id = :threadId")
    fun getMessagesByThreadId(threadId: Long): List<DeletedMessage>

    @Query("SELECT * FROM recycle_bin WHERE thread_id = :threadId")
    fun getAllMessagesByThread(threadId: Long): List<DeletedMessage>

    @Query("SELECT * FROM recycle_bin")
    fun getAllDeletedMessages(): List<DeletedMessage>

    @Query("DELETE FROM recycle_bin WHERE id = :messageId")
    fun deleteMessage(messageId: Long)

    @Query("DELETE FROM recycle_bin WHERE message_id = :id")
    fun deleteMessageById(id: Long)


    @Query("SELECT * FROM recycle_bin WHERE thread_id = :threadId")
    fun getDeletedMessages(threadId: Long): DeletedMessage?

    @Query(
        """
    SELECT DISTINCT d.* FROM recycle_bin d
    JOIN (
        SELECT thread_id, MAX(date) AS max_date
        FROM recycle_bin
        GROUP BY thread_id
    ) grouped 
    ON d.thread_id = grouped.thread_id AND d.date = grouped.max_date
    ORDER BY d.date DESC
"""
    )
    fun getGroupedDeletedMessages(): List<DeletedMessage>

    @Query("DELETE FROM recycle_bin WHERE deletedTime < :cutoffTime")
    fun deleteMessagesOlderThan(cutoffTime: Long)

    @Query("DELETE FROM recycle_bin WHERE thread_id = :threadId")
    fun deleteMessagesByThreadId(threadId: Long)

    @Delete
    fun deleteMessage(deletedMessage: DeletedMessage)

    @Delete
    fun deleteMessages(messages: List<DeletedMessage>)

    @Query("DELETE FROM recycle_bin")
    suspend fun clearRecycleBin()

    @Query("SELECT DISTINCT address, thread_id FROM recycle_bin")
    fun getAllRecycleBinAddresses(): List<RecycleBinAddressThread>

}
