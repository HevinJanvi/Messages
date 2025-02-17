package easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RecycleBinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DeletedMessage)

//    @Query("SELECT * FROM recycle_bin ORDER BY date DESC")
//    fun getAllDeletedMessages(): LiveData<List<DeletedMessage>>

    @Query("DELETE FROM recycle_bin WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)

    @Query("DELETE FROM recycle_bin")
    suspend fun clearRecycleBin()
}
