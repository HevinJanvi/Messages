package com.test.messages.demo.ui.Activity

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.ISDELETED
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.MessagesRestoredEvent
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.TimeUtils
import com.test.messages.demo.Util.TimeUtils.formatHeaderDate
import com.test.messages.demo.Util.ViewUtils.getThreadId
import com.test.messages.demo.Util.ViewUtils.getorcreateThreadId
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.databinding.ActivityConversationBinBinding
import com.test.messages.demo.ui.Adapter.ConversationBinAdapter
import com.test.messages.demo.ui.Dialogs.DeleteProgressDialog
import com.test.messages.demo.ui.send.hasReadContactsPermission
import com.test.messages.demo.ui.send.hasReadSmsPermission
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.RecycleBinDao
import org.greenrobot.eventbus.EventBus
import java.util.Calendar

class ConversationBinactivity : BaseActivity() {

    private lateinit var binding: ActivityConversationBinBinding
    private lateinit var adapter: ConversationBinAdapter
    private var isDeletedThread: Boolean = false
    private var threadId: Long = -1L
    private lateinit var name: String
    private lateinit var db: RecycleBinDao
    private val selectedMessages = mutableSetOf<ConversationItem>()
    private var isMultiSelectEnabled = false
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this).recycleBinDao()

        adapter = ConversationBinAdapter(
            context = this,
            isContactSaved = false,
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

        isDeletedThread = intent.getBooleanExtra(ISDELETED, false)
        threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1L)
        name = intent.getStringExtra(NAME) ?: ""
        binding.address.text = name
        loadDeletedMessages(threadId)
        binding.icClose.setOnClickListener {
            disableMultiSelection()
        }

        binding.btnDelete.setOnClickListener {
            deleteSelectedMessages()
            disableMultiSelection()
        }

        binding.btnRestore.setOnClickListener {
            restoreSelectedMessages()
            disableMultiSelection()
        }
        binding.icBack.setOnClickListener {
            onBackPressed() }
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
                binding.emptyText.visibility = if (processedList.isEmpty()) View.VISIBLE else View.GONE
            }
        }.start()
    }

    private fun addDateHeadersToMessages(messages: List<ConversationItem>): List<ConversationItem> {
        val groupedList = mutableListOf<ConversationItem>()
        val addedHeaders = mutableSetOf<Long>()

        for (message in messages) {
            val headerTimestamp = getStartOfDayTimestamp(message.date)
            if (headerTimestamp !in addedHeaders) {
                val formattedDate = formatHeaderDate(this, message.date)
                groupedList.add(
                    ConversationItem.createHeader(formattedDate, headerTimestamp)
                )
                addedHeaders.add(headerTimestamp)
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
//                    EventBus.getDefault().post(MessagesRestoredEvent(true))
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

    private fun restoreSelectedMessages() {
        val selected = adapter.getSelectedItems().filter { !it.isHeader }
        val totalToRestore = selected.size
        Thread {
            try {
                selected.forEach {
                    val values = ContentValues().apply {
                       val newThreadId  = getorcreateThreadId(this@ConversationBinactivity,it.address)
                        put(Telephony.Sms.THREAD_ID, newThreadId )
                        put(Telephony.Sms.DATE, it.date)
                        put(Telephony.Sms.BODY, it.body)
                        put(Telephony.Sms.ADDRESS, it.address)
                        put(Telephony.Sms.TYPE, it.type)
                        put(Telephony.Sms.READ, if (it.read) 1 else 0)
                        put(Telephony.Sms.SUBSCRIPTION_ID, it.subscriptionId)
                    }

                    val resultUri = contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
                    db.deleteMessageById(it.id)

                }
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
                Log.e("Restore", "Error while restoring", e)
            }
        }.start()
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
}