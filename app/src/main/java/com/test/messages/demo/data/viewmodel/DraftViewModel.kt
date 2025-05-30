package com.test.messages.demo.data.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.test.messages.demo.data.Model.DraftModel
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

    fun getDraft(threadId: Long): LiveData<DraftModel?> = liveData(Dispatchers.IO) {
        val draft = repository.getDraft(threadId)
        if (draft != null) {
            emit(draft)
        } else {
            emit(null)
        }
    }

    fun saveDraft(threadId: Long, draftText: String) {
        repository.saveDraft(threadId, draftText)
    }

    fun deleteDraft(messageId: Int) {
        repository.deleteDraft(messageId)
    }
}
