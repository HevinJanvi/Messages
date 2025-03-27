package com.test.messages.demo.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DraftRepository(private val contentResolver: ContentResolver) {

    private val _draftsLiveData = MutableLiveData<Map<Long, Pair<String, Long>>>()
    val draftsLiveData: LiveData<Map<Long, Pair<String, Long>>> = _draftsLiveData

    fun getAllDrafts() {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        val selection = "type = ?"
        val selectionArgs = arrayOf(Telephony.Sms.MESSAGE_TYPE_DRAFT.toString())

        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            Telephony.Sms.DATE + " DESC"
        )
        val draftMap = mutableMapOf<Long, Pair<String, Long>>()

        cursor?.use {
            while (it.moveToNext()) {
                val threadId = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val timestamp = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))

                if (!body.isNullOrEmpty()) {
                    draftMap[threadId] = Pair(body, timestamp)
                }
            }
        }
        cursor?.close()

        _draftsLiveData.postValue(draftMap)
    }

    fun getDraft(threadId: Long): LiveData<String> {
        val draftLiveData = MutableLiveData<String>()
        val uri = Telephony.Sms.CONTENT_URI
        val selection = "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.TYPE} = ?"
        val selectionArgs =
            arrayOf(threadId.toString(), Telephony.Sms.MESSAGE_TYPE_DRAFT.toString())

        val cursor =
            contentResolver.query(uri, arrayOf(Telephony.Sms.BODY), selection, selectionArgs, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val draftText = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                draftLiveData.postValue(draftText)
            }
        }
        cursor?.close()

        return draftLiveData
    }

    fun saveDraft(threadId: Long, draftText: String) {
        if (threadId == -1L) return
        if (draftText.isBlank()) {
            deleteDraft(threadId)
            return
        }
        val contentValues = ContentValues().apply {
            put("body", draftText)
            put("type", Telephony.Sms.MESSAGE_TYPE_DRAFT)
        }
        val uri = Telephony.Sms.CONTENT_URI
        val selection = "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.TYPE} = ?"
        val selectionArgs = arrayOf(threadId.toString(), Telephony.Sms.MESSAGE_TYPE_DRAFT.toString())

        val cursor = contentResolver.query(uri, null, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val messageId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))
            val updateUri = ContentUris.withAppendedId(uri, messageId)
            contentResolver.update(updateUri, contentValues, null, null)
        } else {
            contentValues.put(Telephony.Sms.THREAD_ID, threadId)
            contentResolver.insert(uri, contentValues)
        }
        cursor?.close()
        getAllDrafts()
    }


    fun deleteDraft(threadId: Long) {
        val uri = Telephony.Sms.CONTENT_URI
        val selection = "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.TYPE} = ?"
        val selectionArgs =
            arrayOf(threadId.toString(), Telephony.Sms.MESSAGE_TYPE_DRAFT.toString())

        contentResolver.delete(uri, selection, selectionArgs)
        getAllDrafts()
    }
}
