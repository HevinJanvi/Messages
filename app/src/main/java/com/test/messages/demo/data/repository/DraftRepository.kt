package com.test.messages.demo.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.Telephony
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.test.messages.demo.data.Model.DraftModel
import com.test.messages.demo.ui.SMSend.hasReadSmsPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

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

    suspend fun getDraft(threadId: Long): DraftModel? {
        if (!context.hasReadSmsPermission()) {
            return null
        }

        return withContext(Dispatchers.IO) {
            val uri = Telephony.Sms.CONTENT_URI
            val selection = "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.TYPE} = ?"
            val selectionArgs =
                arrayOf(threadId.toString(), Telephony.Sms.MESSAGE_TYPE_DRAFT.toString())

            var draftModel: DraftModel? = null

            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.THREAD_ID,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                ),
                selection,
                selectionArgs,
                "${Telephony.Sms.DATE} DESC"
            )
            cursor?.use {

                if (it.moveToFirst()) {
                    val msgId = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                    val thread = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                    val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))

                    draftModel = DraftModel(
                        msg_id = msgId,
                        thraed_id = thread,
                        draft_label = body,
                        draft_time = date
                    )
                }

            }
            draftModel
        }
    }

    fun saveDraft(threadId: Long, draftText: String) {
        if (threadId == -1L || !context.hasReadSmsPermission()) return

        CoroutineScope(Dispatchers.IO).launch {
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
            withContext(Dispatchers.Main) {
                getAllDrafts()
            }
        }
    }

    fun deleteDraft(messageId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val selection = "${Telephony.Sms._ID} = ?"
            val selectionArgs = arrayOf(messageId.toString())
            context.contentResolver.delete(Telephony.Sms.CONTENT_URI, selection, selectionArgs)
        }
    }

}
