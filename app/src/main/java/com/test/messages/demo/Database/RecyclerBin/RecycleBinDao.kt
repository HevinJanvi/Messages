package easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RecycleBinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insertMessage(message: DeletedMessage)

    @Query("SELECT * FROM recycle_bin ORDER BY timestamp DESC")
    fun getAllDeletedMessages(): LiveData<List<DeletedMessage>>

    @Query("DELETE FROM recycle_bin WHERE id = :messageId")
    fun deleteMessage(messageId: Long)

    @Delete
    fun deleteMessages(messages: List<DeletedMessage>)

    @Query("DELETE FROM recycle_bin")
    suspend fun clearRecycleBin()
}
