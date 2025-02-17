package easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin

import androidx.lifecycle.LiveData

class RecycleBinRepository(private val recycleBinDao: RecycleBinDao) {

//    val allDeletedMessages: LiveData<List<DeletedMessage>> = recycleBinDao.getAllDeletedMessages()

    suspend fun insertMessage(message: DeletedMessage) {
        recycleBinDao.insertMessage(message)
    }

    suspend fun deleteMessage(messageId: Long) {
        recycleBinDao.deleteMessage(messageId)
    }

    suspend fun clearRecycleBin() {
        recycleBinDao.clearRecycleBin()
    }
}
