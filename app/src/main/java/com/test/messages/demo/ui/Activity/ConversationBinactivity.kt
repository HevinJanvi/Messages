package com.test.messages.demo.ui.Activity

import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.Util.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.Constants.GROUP_SEPARATOR
import com.test.messages.demo.Util.Constants.ISCONTACT_SAVE
import com.test.messages.demo.Util.Constants.ISDELETED
import com.test.messages.demo.Util.Constants.ISGROUP
import com.test.messages.demo.Util.Constants.NAME
import com.test.messages.demo.Util.Constants.NUMBER
import com.test.messages.demo.Util.MessageRestoredEvent
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.TimeUtils
import com.test.messages.demo.Util.ViewUtils.getorcreateThreadId
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.data.viewmodel.DraftViewModel
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.databinding.ActivityConversationBinBinding
import com.test.messages.demo.ui.Adapter.ConversationBinAdapter
import com.test.messages.demo.ui.Dialogs.DeleteProgressDialog
import com.test.messages.demo.ui.SMSend.getThreadId
import com.test.messages.demo.ui.SMSend.hasReadContactsPermission
import com.test.messages.demo.ui.SMSend.hasReadSmsPermission
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.RecycleBinDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.util.Calendar

@AndroidEntryPoint
class ConversationBinactivity : BaseActivity() {

    private lateinit var binding: ActivityConversationBinBinding
    private lateinit var adapter: ConversationBinAdapter
    private var isDeletedThread: Boolean = false
    private var isGroupThread: Boolean = false
    private var threadId: Long = -1L
    private lateinit var name: String
    private var isContactSaved: Boolean = false
    private lateinit var db: RecycleBinDao
    private val selectedMessages = mutableSetOf<ConversationItem>()
    private var isMultiSelectEnabled = false
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val viewModel: MessageViewModel by viewModels()
    private val draftViewModel: DraftViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this).recycleBinDao()
        isGroupThread = intent.getBooleanExtra(ISGROUP, false)
        isDeletedThread = intent.getBooleanExtra(ISDELETED, false)
        threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1L)
        name = intent.getStringExtra(NAME) ?: ""
        isContactSaved = intent.getBooleanExtra(ISCONTACT_SAVE,false)
        binding.address.text = name


        adapter = ConversationBinAdapter(
            context = this,
            isContactSaved,
            onSelectionChanged = { count ->
                if (count == 0) {
                    disableMultiSelection()
                } else {
                    if (!isMultiSelectEnabled) {
                        isMultiSelectEnabled = true
                        notifySelectionModeChanged(true)
                    }
                    binding.countText.text = "$count ${getString(R.string.selected)}"
                }
            }
        )

        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        binding.recycleConversationView.layoutManager = linearLayoutManager
        binding.recycleConversationView.adapter = adapter


        loadDeletedMessages(threadId)
        binding.icClose.setOnClickListener {
            disableMultiSelection()
        }
        binding.btnDelete.setOnClickListener {
            binding.actionSelectItem.visibility = View.GONE
            deleteSelectedMessages()
            disableMultiSelection()
        }
        binding.btnRestore.setOnClickListener {
            binding.actionSelectItem.visibility = View.GONE
            restoreSelectedMessages()
            disableMultiSelection()
        }
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadDeletedMessages(threadId: Long) {
        val db = AppDatabase.getDatabase(this).recycleBinDao()
        Thread {
            val deletedMessages = db.getMessagesByThreadId(threadId)
            val messages = deletedMessages.map {
                ConversationItem(
                    id = it.messageId,
                    threadId = it.threadId,
                    address = it.address,
                    body = it.body,
                    date = it.date,
                    type = it.type,
                    read = it.read,
                    subscriptionId = it.subscriptionId,
                    profileImageUrl = "",
                    isHeader = false
                )
            }.sortedBy { it.date }

            runOnUiThread {
                val processedList = addDateHeadersToMessages(messages)
                adapter.submitList(processedList)
                binding.emptyText.visibility =
                    if (processedList.isEmpty()) View.VISIBLE else View.GONE
            }
        }.start()
    }

    private fun addDateHeadersToMessages(messages: List<ConversationItem>): List<ConversationItem> {
        val groupedList = mutableListOf<ConversationItem>()
        val addedHeaders = mutableSetOf<Long>()
        for (message in messages) {
            val headerKey = getStartOfDayTimestamp(message.date)
            if (headerKey !in addedHeaders) {
                val formattedHeaderText =
                    TimeUtils.getFormattedHeaderTimestamp(
                        this@ConversationBinactivity,
                        message.date
                    )
                groupedList.add(
                    ConversationItem.createHeader(formattedHeaderText, headerKey)
                )
                addedHeaders.add(headerKey)
            }
            groupedList.add(message)
        }

        return groupedList
    }


    private fun getStartOfDayTimestamp(time: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }


    private fun disableMultiSelection() {
        if (!isMultiSelectEnabled) return

        isMultiSelectEnabled = false
        selectedMessages.clear()
        binding.actionbar.visibility = View.VISIBLE
        binding.actionSelectItem.visibility = View.GONE
        adapter.clearSelection()
    }


    private fun deleteSelectedMessages() {
        val selected = adapter.getSelectedItems().filter { !it.isHeader }
        if (selected.isEmpty()) return

        val deleteDialog = DeleteProgressDialog(this)
        val handler = Handler(Looper.getMainLooper())
        val showDialogRunnable = Runnable {
            deleteDialog.show(getString(R.string.moving_messages_to_recycle_bin))
        }
        handler.postDelayed(showDialogRunnable, 400)

        Thread {
            try {
                selected.forEach { db.deleteMessageById(it.id) }

                runOnUiThread {

                    loadDeletedMessages(threadId)
                    handler.removeCallbacks(showDialogRunnable)
                    deleteDialog.dismiss()
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (adapter.itemCount == 0) {
                            setResult(RESULT_OK)
                            finish()
                        }
                    }, 100)

                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    handler.removeCallbacks(showDialogRunnable)
                    deleteDialog.dismiss()
                }
            }
        }.start()

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun restoreSelectedMessages() {
        val selected = adapter.getSelectedItems().filter { !it.isHeader }

        val pinDao = AppDatabase.getDatabase(this).pinDao()
        val notificationDao = AppDatabase.getDatabase(this).notificationDao()
        val db = AppDatabase.getDatabase(this).recycleBinDao()

        val dialogView = layoutInflater.inflate(R.layout.dialog_progress, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvProgressMessage)
        tvMessage.text = getString(R.string.restoring_messages)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.apply {
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val restoredThreadMap = mutableMapOf<Long, Pair<String, Long>>()
                val updatedMutedThreads = mutableSetOf<Long>()
                val updatedPinnedThreads = mutableSetOf<Long>()

                for (item in selected) {
                    val originalThreadId = item.threadId
                    var threadId = if (item.address.contains(GROUP_SEPARATOR)) {
                        val normalizedAddress = item.address
                            .split(GROUP_SEPARATOR)
                            .map { addr ->
                                viewModel.getContactNumber(this@ConversationBinactivity, addr.trim())
                            }
                            .joinToString(GROUP_SEPARATOR)
                        val tempThreadId = getThreadId(setOf(normalizedAddress))
                        if (isThreadExists(tempThreadId)) tempThreadId
                        else createNewGroupThread(normalizedAddress)
                    } else {
                        getorcreateThreadId(this@ConversationBinactivity, item.address)
                    }

                    // Handle pinned thread
                    if (!updatedPinnedThreads.contains(originalThreadId) && pinDao.isPinned(listOf(originalThreadId)) == 1) {
                        pinDao.updateThreadId(
                            oldThreadId = originalThreadId,
                            newThreadId = threadId
                        )
                        updatedPinnedThreads.add(originalThreadId)
                    }

                    // Handle muted thread
                    if (!updatedMutedThreads.contains(originalThreadId) && notificationDao.isThreadExists(originalThreadId) == 1) {
                        val oldSetting = notificationDao.getNotificationSetting(originalThreadId)
                        if (oldSetting != null) {
                            val newSetting = oldSetting.copy(threadId = threadId)
                            notificationDao.insertOrUpdate(newSetting)
                            notificationDao.deleteByThreadId(originalThreadId)
                            updatedMutedThreads.add(originalThreadId)
                        }
                    }

                    // Insert SMS
                    val values = ContentValues().apply {
                        put(Telephony.Sms.THREAD_ID, threadId)
                        put(Telephony.Sms.DATE, item.date)
                        put(Telephony.Sms.BODY, item.body)
                        put(Telephony.Sms.ADDRESS, item.address)
                        put(Telephony.Sms.TYPE, item.type)
                        put(Telephony.Sms.READ, if (item.read) 1 else 0)
                        put(Telephony.Sms.SUBSCRIPTION_ID, item.subscriptionId)
                    }

                    contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
                    db.deleteMessageById(item.id)

                    val current = restoredThreadMap[threadId]
                    if (current == null || item.date > current.second) {
                        restoredThreadMap[threadId] = Pair(item.body, item.date)
                    }
                }

                fetchInsertAndDeleteDraft(threadId)

                withContext(Dispatchers.Main) {
                    for ((threadId, pair) in restoredThreadMap) {
                        val (lastMessage, lastTime) = pair
                        EventBus.getDefault().post(MessageRestoredEvent(threadId, lastMessage, lastTime))
                    }
                    loadDeletedMessages(threadId)

                    Handler(Looper.getMainLooper()).postDelayed({
                        if (adapter.itemCount == 0) {
                            setResult(RESULT_OK)
                            finish()
                        }
                    }, 100)

                    dialog.dismiss()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                }
            }
        }
    }

   /* @RequiresApi(Build.VERSION_CODES.Q)
    private fun restoreSelectedMessages() {
        val selected = adapter.getSelectedItems().filter { !it.isHeader }
        Thread {
            try {
                selected.forEach {
                    val newThreadId = if (it.address.contains(GROUP_SEPARATOR)) {
                        val normalizedAddress = it.address
                            .split(GROUP_SEPARATOR)
                            .map { addr ->
                                viewModel.getContactNumber(
                                    this@ConversationBinactivity,
                                    addr.trim()
                                )
                            }
                            .joinToString(GROUP_SEPARATOR)
                        val tempThreadId = getThreadId(setOf(normalizedAddress))
                        if (isThreadExists(tempThreadId)) {
                            tempThreadId
                        } else {
                            createNewGroupThread(normalizedAddress)
                        }
                    } else {
                        getorcreateThreadId(this@ConversationBinactivity, it.address)
                    }

                    val values = ContentValues().apply {
                        put(Telephony.Sms.THREAD_ID, newThreadId)
                        put(Telephony.Sms.DATE, it.date)
                        put(Telephony.Sms.BODY, it.body)
                        put(Telephony.Sms.ADDRESS, it.address)
                        put(Telephony.Sms.TYPE, it.type)
                        put(Telephony.Sms.READ, if (it.read) 1 else 0)
                        put(Telephony.Sms.SUBSCRIPTION_ID, it.subscriptionId)
                    }

                    contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
                    db.deleteMessageById(it.id)

                }
                fetchInsertAndDeleteDraft(threadId)

                EventBus.getDefault().post(MessagesRestoredEvent(true))

                runOnUiThread {
                    loadDeletedMessages(threadId)
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (adapter.itemCount == 0) {
                            setResult(RESULT_OK)
                            finish()
                        }
                    }, 100)
                }

            } catch (e: Exception) {
            }
        }.start()
    }*/

    fun fetchInsertAndDeleteDraft(threadId: Long): Boolean {
        val contentResolver = contentResolver
        var Date = System.currentTimeMillis()
        var latestBody: String? = ""

        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE)
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT 1"

        contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            if (cursor.moveToFirst()) {
                Date = cursor.getLong(
                    cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                )
                latestBody = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY))
            } else null
        }

        if (latestBody.isNullOrEmpty()) return false
        val contentValues = ContentValues().apply {
            put("body", latestBody)
            put("type", Telephony.Sms.MESSAGE_TYPE_SENT)
            put("date", Date)
        }
        contentValues.put(Telephony.Sms.THREAD_ID, threadId)
        val insertedUri: Uri? = contentResolver.insert(Telephony.Sms.CONTENT_URI, contentValues)
        draftViewModel.deleteDraft(insertedUri!!.lastPathSegment!!.toInt())
        return false
    }

    private fun notifySelectionModeChanged(enabled: Boolean) {
        binding.actionbar.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.actionSelectItem.visibility = if (enabled) View.VISIBLE else View.GONE
    }


    override fun onBackPressed() {
        if (isMultiSelectEnabled) {
            adapter.clearSelection()
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                setResult(RESULT_OK)
                finish()
                super.onBackPressed()
            }, 200)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this) && hasReadSmsPermission() && hasReadContactsPermission()) {
            return
        }
    }

    private fun createNewGroupThread(addresses: String): Long {
        val uri = Uri.parse("content://mms-sms/threadID")
        val addressList = addresses.split(GROUP_SEPARATOR).map { it.trim() }

        val uriBuilder = Uri.Builder().apply {
            scheme("content")
            authority("mms-sms")
            appendPath("threadID")
            for (address in addressList) {
                appendQueryParameter("recipient", address)
            }
        }

        val cursor = contentResolver.query(uriBuilder.build(), arrayOf("_id"), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        return -1
    }

    private fun isThreadExists(threadId: Long): Boolean {
        val uri = Telephony.Threads.CONTENT_URI
//        val uri =  Uri.parse("${Telephony.Threads.CONTENT_URI}?simple=true")
        val projection = arrayOf(Telephony.Threads._ID)
        val selection = "${Telephony.Threads._ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        contentResolver.query(uri, projection, selection, selectionArgs, null).use { cursor ->
            return cursor != null && cursor.moveToFirst()
        }
    }
}