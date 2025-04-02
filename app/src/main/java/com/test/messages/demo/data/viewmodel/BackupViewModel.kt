package com.test.messages.demo.data.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.data.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupViewModel(application: Application) : AndroidViewModel(application) {


    private val repository = BackupRepository(application)
    private val _restoreProgress = MutableLiveData<Int>()
    val restoreProgress: LiveData<Int> get() = _restoreProgress

    fun backupMessages(uri: Uri) {
        repository.backupMessages(uri)
    }
    fun restoreMessages(uri: Uri, onComplete: (List<ConversationItem>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreMessages(uri, { progress ->
                _restoreProgress.postValue(progress) // Update LiveData on UI thread
            }, onComplete)
        }
    }



    /*fun restoreMessages(uri: Uri, onComplete: (List<ConversationItem>) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            repository.restoreMessages(uri, onComplete)
        }
    }*/


}
