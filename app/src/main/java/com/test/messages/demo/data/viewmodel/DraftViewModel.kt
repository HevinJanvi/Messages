package com.test.messages.demo.data.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.test.messages.demo.data.repository.DraftRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class DraftViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DraftRepository(application)

    val draftsLiveData: LiveData<Map<Long, Pair<String, Long>>> = repository.draftsLiveData

    fun loadAllDrafts() {
        CoroutineScope(Dispatchers.IO).launch{
            repository.getAllDrafts()
        }

    }

    fun getDraft(threadId: Long): LiveData<String> = liveData(Dispatchers.IO) {
        val draft = repository.getDraft(threadId) // suspend function
        emit(draft ?: "")
    }

    fun saveDraft(threadId: Long, draftText: String) {
        repository.saveDraft(threadId, draftText)
    }

    fun deleteDraft(threadId: Long) {
        repository.deleteDraft(threadId)
    }
}
