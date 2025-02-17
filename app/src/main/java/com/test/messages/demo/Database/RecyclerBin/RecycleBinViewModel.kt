package easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class RecycleBinViewModel(private val repository: RecycleBinRepository) : ViewModel() {

//    val allDeletedMessages: LiveData<List<DeletedMessage>> = repository.allDeletedMessages

    fun insertMessage(message: DeletedMessage) {
        viewModelScope.launch {
            repository.insertMessage(message)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun clearRecycleBin() {
        viewModelScope.launch {
            repository.clearRecycleBin()
        }
    }
}
