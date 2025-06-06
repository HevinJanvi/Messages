package com.test.messages.demo.data.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.data.repository.BackupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupViewModel(application: Application) : AndroidViewModel(application) {


    private val repository = BackupRepository(application)
    private val _restoreProgress = MutableLiveData<Int>()
    val restoreProgress: LiveData<Int> get() = _restoreProgress

    fun backupMessages(uri: Uri, onProgress: (Int) -> Unit, onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.backupMessages(uri, { progress ->
                onProgress.invoke(progress)
            }, onComplete)
        }
    }

    fun restoreMessages(
        uri: Uri,
        onProgressUpdate: (Int) -> Unit,
        onComplete: (List<ConversationItem>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.restoreMessages(uri, { progress ->
                onProgressUpdate.invoke(progress)
            }, onComplete,onFailure)
        }
    }

    fun clearRestoreProgress() {
        _restoreProgress.value = 0
    }

}
