package com.test.messages.demo.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.test.messages.demo.ui.send.hasReadContactsPermission
import com.test.messages.demo.ui.send.hasReadSmsPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

//class DraftRepository(private val contentResolver: ContentResolver) {
class DraftRepository @Inject constructor(@ApplicationContext private val context: Context) {


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

        val cursor = context.contentResolver.query(
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

  /*  fun getDraft(threadId: Long): LiveData<String> {
        val draftLiveData = MutableLiveData<String>()

        if (!context.hasReadSmsPermission()
        ) {
            return draftLiveData
        }
        val uri = Telephony.Sms.CONTENT_URI
        val selection = "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.TYPE} = ?"
        val selectionArgs =
            arrayOf(threadId.toString(), Telephony.Sms.MESSAGE_TYPE_DRAFT.toString())

        val cursor =
            context.contentResolver.query(
                uri,
                arrayOf(Telephony.Sms.BODY),
                selection,
                selectionArgs,
                null
            )
        cursor?.use {
            if (it.moveToFirst()) {
                val draftText = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                draftLiveData.postValue(draftText)
            }
        }
        cursor?.close()

        return draftLiveData
    }*/
  suspend fun getDraft(threadId: Long): String? {
      if (!context.hasReadSmsPermission()) {
          return null
      }

      return withContext(Dispatchers.IO) {
          val uri = Telephony.Sms.CONTENT_URI
          val selection = "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.TYPE} = ?"
          val selectionArgs = arrayOf(threadId.toString(), Telephony.Sms.MESSAGE_TYPE_DRAFT.toString())

          var draftText: String? = null
          val cursor = context.contentResolver.query(
              uri,
              arrayOf(Telephony.Sms.BODY),
              selection,
              selectionArgs,
              null
          )
          cursor?.use {
              if (it.moveToFirst()) {
                  draftText = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
              }
          }
          draftText
      }
  }

    fun saveDraft(threadId: Long, draftText: String) {
        if (threadId == -1L) return
        if (!context.hasReadSmsPermission()) return

        if (draftText.isBlank() ) {
            deleteDraft(threadId)
            return
        }
        val contentValues = ContentValues().apply {
            put("body", draftText)
            put("type", Telephony.Sms.MESSAGE_TYPE_DRAFT)
            put("date", System.currentTimeMillis())
        }
        val uri = Telephony.Sms.CONTENT_URI
        val selection = "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.TYPE} = ?"
        val selectionArgs =
            arrayOf(threadId.toString(), Telephony.Sms.MESSAGE_TYPE_DRAFT.toString())

        val cursor = context.contentResolver.query(uri, null, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val messageId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))
            val updateUri = ContentUris.withAppendedId(uri, messageId)
            context.contentResolver.update(updateUri, contentValues, null, null)
        } else {
            contentValues.put(Telephony.Sms.THREAD_ID, threadId)
            context.contentResolver.insert(uri, contentValues)
        }
        cursor?.close()
        getAllDrafts()
    }


    fun deleteDraft(threadId: Long) {
        val uri = Telephony.Sms.CONTENT_URI
        val selection = "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.TYPE} = ?"
        val selectionArgs =
            arrayOf(threadId.toString(), Telephony.Sms.MESSAGE_TYPE_DRAFT.toString())

        context.contentResolver.delete(uri, selection, selectionArgs)
        getAllDrafts()
    }
}
