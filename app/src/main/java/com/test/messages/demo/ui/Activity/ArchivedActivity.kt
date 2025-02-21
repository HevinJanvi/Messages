package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityArchivedBinding
import com.test.messages.demo.ui.Adapter.ArchiveMessageAdapter
import com.test.messages.demo.ui.Utils.DeleteDialog
import com.test.messages.demo.ui.Utils.PopupMenuHelper
import com.test.messages.demo.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ArchivedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArchivedBinding
    private lateinit var adapter: ArchiveMessageAdapter
    private val viewModel: MessageViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchivedBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)

        binding.archiveRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ArchiveMessageAdapter { selectedCount ->
            updateSelectedItemsCount()
        }
        binding.archiveRecyclerView.adapter = adapter

        viewModel.archivedThreadIds.observe(this) {
            filterAndDisplayMessages()
        }
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        viewModel.messages.observe(this) {
            filterAndDisplayMessages()
        }
        viewModel.loadArchivedThreads()
        adapter.onArchiveItemClickListener = { message ->
            val intent = Intent(this, ConversationActivity::class.java)
            intent.putExtra("EXTRA_THREAD_ID", message.threadId)
            intent.putExtra("NUMBER", message.number)
            startActivity(intent)
        }
        setupClickListeners()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupClickListeners() {
        binding.btnUnarchive.setOnClickListener {
            val selectedThreadIds = adapter.getSelectedThreadIds()
            if (selectedThreadIds.isNotEmpty()) {
                viewModel.unarchiveConversations(selectedThreadIds)
                adapter.clearSelection()
                viewModel.loadMessages()
            }
        }
        binding.btnDelete.setOnClickListener {
            val deleteDialog = DeleteDialog(this) {
                val selectedThreadIds = adapter?.getSelectedThreadIds() ?: emptyList()
                if (selectedThreadIds.isNotEmpty()) {
                    deleteMessages(selectedThreadIds)
                    adapter?.removeItems(selectedThreadIds)
                    adapter?.clearSelection()
                    viewModel.loadMessages()
                }
            }
            deleteDialog.show()

        }
        binding.icMore.setOnClickListener {
            val popupMenuHelper = PopupMenuHelper(this)
            popupMenuHelper.showPopup(binding.icMore)
        }
    }

    private fun filterAndDisplayMessages() {
        val threadIds = viewModel.archivedThreadIds.value ?: emptySet()
        val allMessages = viewModel.messages.value ?: emptyList()
        val archivedMessages = allMessages.filter { it.threadId in threadIds }
        adapter.submitList(archivedMessages)
        if (archivedMessages.isEmpty()) {
            binding.emptyList.visibility = View.VISIBLE
        } else {
            binding.emptyList.visibility = View.GONE
        }
    }

    private fun updateSelectedItemsCount() {
        binding.txtSelectedCount.text =
            "${adapter.selectedMessages.size}" + " " + getString(R.string.selected)
        if (adapter.selectedMessages.size > 0) {
            binding.lySelectedItemsArchive.visibility = View.VISIBLE
            binding.selectMenuArchive.visibility = View.VISIBLE
            binding.toolBarArchive.visibility = View.GONE
        } else {
            binding.lySelectedItemsArchive.visibility = View.GONE
            binding.selectMenuArchive.visibility = View.GONE
            binding.toolBarArchive.visibility = View.VISIBLE
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteMessages(threadIds: List<Long>) {
        if (adapter.selectedMessages.isEmpty()) return
        val contentResolver = contentResolver
        val updatedList = viewModel.messages.value?.toMutableList() ?: mutableListOf()

        Thread {
            try {
                for (threadId in threadIds) {
                    val uri = Uri.parse("content://sms/conversations/$threadId")
                    val deletedRows = contentResolver.delete(uri, null, null)
                    if (deletedRows > 0) {
                        updatedList.removeAll { it.threadId == threadId }
                    } else {
                        Log.d("SMS_DELETE", "Failed to delete thread ID $threadId.")
                    }
                }
                Handler(Looper.getMainLooper()).post {
                    filterAndDisplayMessages()
                }
            } catch (e: Exception) {
                Log.d("SMS_DELETE", "Error deleting threads: ${e.message}")
            }
        }.start()
    }

    override fun onBackPressed() {
        if (adapter.selectedMessages.size > 0) {
            adapter.clearSelection()
        }else{
            super.onBackPressed()
        }
    }
}