package com.test.messages.demo.ui.Activity

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.Util.Constants
import com.test.messages.demo.Util.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.Constants.GROUP_SEPARATOR
import com.test.messages.demo.Util.Constants.ISDELETED
import com.test.messages.demo.Util.Constants.ISGROUP
import com.test.messages.demo.Util.Constants.NAME
import com.test.messages.demo.Util.Constants.PREFS_NAME
import com.test.messages.demo.Util.MessageRestoredEvent
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.data.Database.Notification.NotificationSetting
import com.test.messages.demo.data.Database.Pin.PinMessage
import com.test.messages.demo.data.viewmodel.DraftViewModel
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.databinding.ActivityRecyclebinBinding
import com.test.messages.demo.ui.Adapter.RecycleBinAdapter
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.ui.send.getThreadId
import com.test.messages.demo.ui.send.hasReadContactsPermission
import com.test.messages.demo.ui.send.hasReadSmsPermission
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus


@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)
class RecycleBinActivity : BaseActivity() {
    private lateinit var binding: ActivityRecyclebinBinding
    private lateinit var recycleBinAdapter: RecycleBinAdapter
    private val viewModel: MessageViewModel by viewModels()
    private val draftViewModel: DraftViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecyclebinBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(binding.rootView)
        binding.recycleBinRecyclerView.layoutManager = LinearLayoutManager(this)
        recycleBinAdapter = RecycleBinAdapter { selectedCount ->
            updateActionLayout(selectedCount)
        }

        binding.recycleBinRecyclerView.adapter = recycleBinAdapter
        recycleBinAdapter.onBinItemClick = { deletedMessage ->
            val intent = Intent(this, ConversationBinactivity::class.java)
            intent.putExtra(ISDELETED, true)
            intent.putExtra(EXTRA_THREAD_ID, deletedMessage.threadId)
            intent.putExtra(NAME, deletedMessage.address)
            intent.putExtra(ISGROUP, deletedMessage.isGroupChat)
            startActivityForResult(intent, 101)
        }
        loadGroupedMessages()
        setupClickListeners()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            loadGroupedMessages()
            recycleBinAdapter.notifyDataSetChanged()
        }
    }

    private fun setupClickListeners() {
        binding.icBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnDelete.setOnClickListener {
            val deleteDialog = DeleteDialog(this, "recyclebin", true) {
                deleteSelectedMessages()
            }
            deleteDialog.show()
        }

        binding.btnRestore.setOnClickListener {
            restoreSelectedMessages()
        }

        binding.btnSelectAll.setOnClickListener {
            if (recycleBinAdapter.isAllSelected()) {
                recycleBinAdapter.unselectAll()
            } else {
                recycleBinAdapter.selectAll()
            }
            updateActionLayout(recycleBinAdapter.selectedMessages.size)
        }
    }

    private fun loadGroupedMessages() {
        Thread {
            val dao = AppDatabase.getDatabase(this).recycleBinDao()
            val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
            val cutoffTime = System.currentTimeMillis() - thirtyDaysInMillis
            dao.deleteMessagesOlderThan(cutoffTime)

            val groupedMessages = dao.getGroupedDeletedMessages()

            runOnUiThread {
                recycleBinAdapter.submitList(groupedMessages)
                updateEmptyState(groupedMessages.isEmpty())

                if (groupedMessages.isEmpty()) {
                    binding.emptyList.visibility = View.VISIBLE
                    binding.recycleBinRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyList.visibility = View.GONE
                    binding.recycleBinRecyclerView.visibility = View.VISIBLE
                }
            }
        }.start()
    }


    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyList.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recycleBinRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun deleteSelectedMessages() {
        if (recycleBinAdapter.selectedMessages.isNotEmpty()) {
            val selectedThreads = recycleBinAdapter.selectedMessages.map { it.threadId }.distinct()

            lifecycleScope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getDatabase(this@RecycleBinActivity).recycleBinDao()
                selectedThreads.forEach { threadId ->
                    dao.deleteMessagesByThreadId(threadId)
                }

                withContext(Dispatchers.Main) {
                    recycleBinAdapter.clearSelection()
                    loadGroupedMessages()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun restoreSelectedMessages() {
        val contentResolver = contentResolver
        val db = AppDatabase.getDatabase(this).recycleBinDao()
        val pinDao = AppDatabase.getDatabase(this).pinDao()
        val notificationDao = AppDatabase.getDatabase(this).notificationDao()

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

        val startTime = System.currentTimeMillis()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val restoredThreadMap = mutableMapOf<Long, Pair<String, Long>>()
                val allMessagesToRestore = mutableListOf<DeletedMessage>()

                val selectedThreadIds =
                    recycleBinAdapter.selectedMessages.map { it.threadId }.distinct()
//                val pinnedThreadIds =  AppDatabase.getDatabase(this@RecycleBinActivity).pinDao().getAllPinnedThreadIds().toSet()

                for (threadId in selectedThreadIds) {
                    val threadMessages = db.getAllMessagesByThread(threadId)
                    allMessagesToRestore.addAll(threadMessages)
                }

                val reversedList = allMessagesToRestore.asReversed()
                val updatedMutedThreads = mutableSetOf<Long>()
                val updatedPinnedThreads = mutableSetOf<Long>()

                for (deletedMessage in reversedList) {
                    var threadId = getThreadId(setOf(deletedMessage.address))
                    val originalThreadId = deletedMessage.threadId

                    if (!isThreadExists(threadId)) {
                        threadId = if (deletedMessage.isGroupChat) {
                            val normalizedAddress = deletedMessage.address
                                .split(GROUP_SEPARATOR)
                                .map { it.trim() }
                                .map { viewModel.getContactNumber(this@RecycleBinActivity, it) }
                                .joinToString(GROUP_SEPARATOR)
                            createNewGroupThread(normalizedAddress)
                        } else {
                            getThreadId(setOf(deletedMessage.address))
                        }


                        if (deletedMessage.isGroupChat) {
                            val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val oldKey = "${Constants.GROUP_NAME_KEY}${originalThreadId}"
                            val newKey = "${Constants.GROUP_NAME_KEY}$threadId"
                            val oldGroupName = sharedPrefs.getString(oldKey, null)
                            if (!oldGroupName.isNullOrEmpty()) {
                                sharedPrefs.edit().putString(newKey, oldGroupName).apply()
                            }
                        }
                    }
                    //pinned
                    if (!updatedPinnedThreads.contains(originalThreadId) && pinDao.isPinned(listOf(originalThreadId)) == 1) {
                        pinDao.updateThreadId(
                            oldThreadId = originalThreadId,
                            newThreadId = threadId
                        )
                        updatedPinnedThreads.add(originalThreadId)
                    }
                    //muted
                    if (!updatedMutedThreads.contains(originalThreadId) && notificationDao.isThreadExists(originalThreadId) == 1) {
                        val oldSetting = notificationDao.getNotificationSetting(originalThreadId)
                        if (oldSetting != null) {
                            val newSetting = oldSetting.copy(threadId = threadId)
                            notificationDao.insertOrUpdate(newSetting)
                            notificationDao.deleteByThreadId(originalThreadId)
                            updatedMutedThreads.add(originalThreadId)
                        }
                    }

                    val values = ContentValues().apply {
                        put(Telephony.Sms.THREAD_ID, threadId)
                        put(Telephony.Sms.DATE, deletedMessage.date)
                        put(Telephony.Sms.BODY, deletedMessage.body)
                        put(Telephony.Sms.ADDRESS, deletedMessage.address)
                        put(Telephony.Sms.TYPE, deletedMessage.type)
                        put(Telephony.Sms.READ, if (deletedMessage.read) 1 else 0)
                        put(Telephony.Sms.SUBSCRIPTION_ID, deletedMessage.subscriptionId)
                    }

                    contentResolver.insert(Telephony.Sms.CONTENT_URI, values)

                    db.deleteMessage(deletedMessage)

                    val current = restoredThreadMap[threadId]
                    if (current == null || deletedMessage.date > current.second) {
                        restoredThreadMap[threadId] = Pair(deletedMessage.body, deletedMessage.date)
                    }

                }


                val elapsed = System.currentTimeMillis() - startTime
                val minDisplay = 800L
                if (elapsed < minDisplay) {
                    delay(minDisplay - elapsed)
                }

                withContext(Dispatchers.Main) {
                    for ((threadId, pair) in restoredThreadMap) {
                        fetchInsertAndDeleteDraft(threadId)
                        val (lastMessage, lastTime) = pair
                        EventBus.getDefault()
                            .post(MessageRestoredEvent(threadId, lastMessage, lastTime))
                    }
                    delay(200)
                    recycleBinAdapter.clearSelection()
                    loadGroupedMessages()
                    dialog.dismiss()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    Toast.makeText(this@RecycleBinActivity, e.toString(), Toast.LENGTH_LONG).show()
                }
            }
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

    private fun isThreadExists(threadId: Long): Boolean {
        try {
            val uri = Telephony.Threads.CONTENT_URI
            val projection = arrayOf(Telephony.Threads._ID)
            val selection = "${Telephony.Threads._ID} = ?"
            val selectionArgs = arrayOf(threadId.toString())

            contentResolver.query(uri, projection, selection, selectionArgs, null).use { cursor ->
                return cursor != null && cursor.moveToFirst()
            }
        } catch (e: Exception) {
            val uri = Uri.parse("${Telephony.Threads.CONTENT_URI}?simple=true")
            val projection = arrayOf(Telephony.Threads._ID)
            val selection = "${Telephony.Threads._ID} = ?"
            val selectionArgs = arrayOf(threadId.toString())
            contentResolver.query(uri, projection, selection, selectionArgs, null).use { cursor ->
                return cursor != null && cursor.moveToFirst()
            }
        }
    }

    private fun updateActionLayout(selectedCount: Int) {

        binding.btnSelectAll.isChecked = recycleBinAdapter.isAllSelected()
        binding.txtSelectedCount.text = "$selectedCount" + " " + getString(R.string.selected)
        binding.selectMenuBin.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
        binding.lySelectedItemsBin.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
        binding.toolBarBin.visibility = if (selectedCount > 0) View.GONE else View.VISIBLE
    }

    override fun onBackPressed() {
        if (recycleBinAdapter.selectedMessages.size > 0) {
            recycleBinAdapter.clearSelection()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this) && hasReadSmsPermission() && hasReadContactsPermission()) {
            return
        }
    }


}