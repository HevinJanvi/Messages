package com.test.messages.demo.ui.Activity

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.text.format.DateUtils.formatDateTime
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.ISDELETED
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.MessageRestoredEvent
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.data.viewmodel.BackupViewModel
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.databinding.ActivityRecyclebinBinding
import com.test.messages.demo.ui.Adapter.RecycleBinAdapter
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)
class RecycleBinActivity : BaseActivity() {
    private lateinit var binding: ActivityRecyclebinBinding
    private lateinit var recycleBinAdapter: RecycleBinAdapter
    private val selectedMessages = mutableSetOf<DeletedMessage>()
    private val viewModel: MessageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecyclebinBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)

        binding.recycleBinRecyclerView.layoutManager = LinearLayoutManager(this)
        recycleBinAdapter = RecycleBinAdapter { selectedCount ->
            updateActionLayout(selectedCount)
        }
        binding.recycleBinRecyclerView.adapter = recycleBinAdapter
        recycleBinAdapter.onBinItemClick = { deletedMessage ->
            val intent = Intent(this, RecycleConversationactivity::class.java)
            intent.putExtra(ISDELETED, true)
            intent.putExtra(EXTRA_THREAD_ID, deletedMessage.threadId)
            intent.putExtra(NAME, deletedMessage.address)
            startActivity(intent)
        }

        loadGroupedMessages()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.icBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnDelete.setOnClickListener {
            deleteSelectedMessages()
        }

        binding.btnRestore.setOnClickListener {
            restoreSelectedMessages()
        }

        binding.btnSelectAll.setOnClickListener {
            if (recycleBinAdapter.selectedMessages.size == recycleBinAdapter.itemCount) {
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
            val groupedMessages = dao.getGroupedDeletedMessages()

            runOnUiThread {
                recycleBinAdapter.submitList(groupedMessages)

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

    private fun deleteSelectedMessages() {
        if (recycleBinAdapter.selectedMessages.isNotEmpty()) {
            val messagesToDelete = recycleBinAdapter.selectedMessages.toList()
            lifecycleScope.launch(Dispatchers.IO) {
                AppDatabase.getDatabase(this@RecycleBinActivity).recycleBinDao()
                    .deleteMessages(messagesToDelete)
            }
            recycleBinAdapter.clearSelection()
            loadGroupedMessages()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun restoreSelectedMessages() {
        val contentResolver = contentResolver
        val db = AppDatabase.getDatabase(this).recycleBinDao()

        val dialogView = layoutInflater.inflate(R.layout.dialog_progress, null)
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

        Thread {
            try {
                val restoredThreadMap = mutableMapOf<Long, Pair<String, Long>>()
                val allMessagesToRestore = mutableListOf<DeletedMessage>()

                for (selectedThread in recycleBinAdapter.selectedMessages) {
                    val threadMessages = db.getAllMessagesByThread(selectedThread.threadId)
                    allMessagesToRestore.addAll(threadMessages)
                }

                for (deletedMessage in allMessagesToRestore) {
                    var threadId = deletedMessage.threadId
                    if (!isThreadExists(threadId)) {
                        threadId = getThreadId(deletedMessage.address)
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
                    restoredThreadMap[threadId] = Pair(deletedMessage.body, deletedMessage.date)
                }

                val elapsed = System.currentTimeMillis() - startTime
                val minDisplay = 800L
                if (elapsed < minDisplay) {
                    Thread.sleep(minDisplay - elapsed)
                }

                Handler(Looper.getMainLooper()).post {
                    dialog.dismiss()

                    for ((threadId, pair) in restoredThreadMap) {
                        val (lastMessage, lastTime) = pair
                        EventBus.getDefault().post(MessageRestoredEvent(threadId, lastMessage, lastTime))
                    }

                    recycleBinAdapter.clearSelection()
                    loadGroupedMessages()
                    viewModel.loadMessages()

                }

            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    dialog.dismiss()
                    Toast.makeText(this, getString(R.string.restore_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }


    fun getThreadId(address: String): Long {
        return try {
            Telephony.Threads.getOrCreateThreadId(this, address)
        } catch (e: Exception) {
            0L
        }
    }

    private fun isThreadExists(threadId: Long): Boolean {
        val uri = Telephony.Threads.CONTENT_URI
        val projection = arrayOf(Telephony.Threads._ID)
        val selection = "${Telephony.Threads._ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        contentResolver.query(uri, projection, selection, selectionArgs, null).use { cursor ->
            return cursor != null && cursor.moveToFirst()
        }
    }


    private fun updateActionLayout(selectedCount: Int) {

        val isSelectionActive = selectedMessages.isNotEmpty()
        binding.selectMenuBin.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
        binding.lySelectedItemsBin.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
        binding.toolBarBin.visibility = if (selectedCount > 0) View.GONE else View.VISIBLE
    }

}