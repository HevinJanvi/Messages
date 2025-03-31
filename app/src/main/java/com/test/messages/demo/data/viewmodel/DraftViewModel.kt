package com.test.messages.demo.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.test.messages.demo.data.repository.DraftRepository


class DraftViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DraftRepository(application.contentResolver)

    val draftsLiveData: LiveData<Map<Long, Pair<String, Long>>> = repository.draftsLiveData

    fun loadAllDrafts() {
        repository.getAllDrafts()
    }

    fun getDraft(threadId: Long): LiveData<String> {
        return repository.getDraft(threadId)
    }

    fun saveDraft(threadId: Long, draftText: String) {
        repository.saveDraft(threadId, draftText)
    }

    fun deleteDraft(threadId: Long) {
        repository.deleteDraft(threadId)
    }
}
