package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.BlockedNumberContract
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.data.MessageItem
import com.test.messages.demo.databinding.ActivityBlockContactBinding
import com.test.messages.demo.ui.Adapter.BlockedContactAdapter
import com.test.messages.demo.ui.Utils.SmsPermissionUtils
import com.test.messages.demo.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockedContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockContactBinding
    private lateinit var adapter: BlockedContactAdapter
    val viewModel: MessageViewModel by viewModels()
    private var messageList: List<MessageItem> = emptyList()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.blockContactRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BlockedContactAdapter(
            onItemClick = { blockedMessage -> checkAndOpenConversation(blockedMessage) },
            onSelectionChanged = { showBottomLayout(it) }
        )
        binding.blockContactRecyclerView.adapter = adapter

        viewModel.blockedMessages.observe(this) { messages ->
            adapter.updateList(messages)
        }
        viewModel.messages.observe(this) { messages ->
            messageList = messages
        }
        viewModel.loadBlockedMessages()
        binding.btnDelete.setOnClickListener {
            deleteSelectedMessages()
        }
        binding.btnUnblock.setOnClickListener {
            unblockSelectedNumbers()
        }
        binding.btnSelectAll.setOnCheckedChangeListener { _, isChecked ->
            toggleSelectAll(isChecked)
        }
    }

    private fun toggleSelectAll(selectAll: Boolean) {
        adapter.selectedItems.clear()
        if (selectAll) {
            adapter.selectedItems.addAll(adapter.getAllMessages()) // ✅ Select all messages
            adapter.isMultiSelectMode = true
        } else {
            adapter.isMultiSelectMode = false
        }
        adapter.notifyDataSetChanged()
        updateSelectAllCheckbox()
        showBottomLayout(adapter.selectedItems.isNotEmpty())
    }


    /*private fun deleteSelectedMessages() {
        if (adapter.selectedItems.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_messages_selected), Toast.LENGTH_SHORT)
                .show()
            return
        }
        val selectedMessages = adapter.selectedItems.map { adapter.messages[it] }
        val updatedList = adapter.messages.toMutableList()
        selectedMessages.forEach { updatedList.remove(it) }
        adapter.updateList(updatedList)
        adapter.clearSelection()
    }*/


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteSelectedMessages() {
        if (adapter.selectedItems.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_messages_selected), Toast.LENGTH_SHORT).show()
            return
        }

        val contentResolver = contentResolver
        val selectedMessages = adapter.selectedItems.toList() // Get selected messages

        Thread {
            try {
                for (message in selectedMessages) {
                    val messageUri = Uri.parse("content://sms/${message.threadId}") // Target specific SMS
                    val deletedRows = contentResolver.delete(messageUri, null, null)

                    if (deletedRows > 0) {
                        Log.d("SMS_DELETE", "Deleted message ID: ${message.threadId}")
                    } else {
                        Log.d("SMS_DELETE", "Failed to delete message ID: ${message.threadId}")
                    }
                }

                // Update UI on main thread
                Handler(Looper.getMainLooper()).post {
                    val updatedList = adapter.messages.toMutableList().apply {
                        removeAll(selectedMessages)
                    }
                    adapter.updateList(updatedList)
                    adapter.clearSelection()
                    Toast.makeText(this, "Deleted selected messages", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("SMS_DELETE", "Error deleting messages: ${e.message}")
            }
        }.start()
    }




//    @RequiresApi(Build.VERSION_CODES.Q)
//    fun deleteSelectedMessages() {
//        if (adapter.selectedItems.isEmpty()) return
//        val threadIds = adapter.selectedItems.map { it.threadId }.toSet()
//        val contentResolver = contentResolver
//
//        Thread {
//            try {
//                for (threadId in threadIds) {
//                    val uri = Uri.parse("content://sms/conversations/$threadId")
//                    val deletedRows = contentResolver.delete(uri, null, null)
//
//                    if (deletedRows > 0) {
//                        Log.d(
//                            "SMS_DELETE",
//                            "Deleted thread ID $threadId with $deletedRows messages."
//                        )
//                        updatedList.removeAll { it.threadId == threadId }
//                    } else {
//                        Log.d("SMS_DELETE", "Failed to delete thread ID $threadId.")
//                    }
//                }
//                Handler(Looper.getMainLooper()).post {
//                    adapter.updateList(updatedList)
//                    adapter.clearSelection()
//                    /*adapter.selectedMessages.clear()
//                    adapter.updateList(updatedList)
//                    onSelectionChanged?.invoke(
//                        adapter.selectedMessages.size,
//                        adapter.selectedMessages.count { it.isPinned })*/
//                }
//
//            } catch (e: Exception) {
//                Log.d("SMS_DELETE", "Error deleting threads: ${e.message}")
//            }
//        }.start()
//    }


    private fun unblockSelectedNumbers() {
        if (adapter.selectedItems.isEmpty()) {
            Toast.makeText(this, "No numbers selected", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedNumbers = adapter.selectedItems.map { it.number }.toSet()
        val contentResolver = contentResolver

        Thread {
            try {
                for (number in selectedNumbers) {
                    val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
                    val selection = "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER}=?"
                    val deletedRows = contentResolver.delete(uri, selection, arrayOf(number))

                    if (deletedRows > 0) {
                        Log.d("UNBLOCK", "Unblocked number: $number")
                    } else {
                        Log.d("UNBLOCK", "Failed to unblock number: $number")
                    }
                }

                Handler(Looper.getMainLooper()).post {
                    val updatedList = adapter.messages.filterNot { it.number in selectedNumbers }

                    adapter.updateList(updatedList)
                    adapter.clearSelection()
                    Toast.makeText(this, "Unblocked selected numbers", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("UNBLOCK", "Error unblocking numbers: ${e.message}")
            }
        }.start()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkAndOpenConversation(blockedMessage: MessageItem) {
        val conversationExists = messageList.any { it.number == blockedMessage.number }

        if (conversationExists) {
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra("EXTRA_THREAD_ID", blockedMessage.threadId)
                putExtra("NUMBER", blockedMessage.number)
                .putExtra("NAME", blockedMessage.sender)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, getString(R.string.no_conversation_found), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun showBottomLayout(show: Boolean) {
        binding.txtSelectedCount.text =
            "${adapter.selectedItems.size}" + " " + getString(R.string.selected)
        binding.selectMenublock.visibility = if (show) View.VISIBLE else View.GONE
        binding.lySelectedItemsblock.visibility = if (show) View.VISIBLE else View.GONE
        binding.toolBarblock.visibility = if (show) View.GONE else View.VISIBLE
        updateSelectAllCheckbox()
    }

    private fun updateSelectAllCheckbox() {
        binding.btnSelectAll.setOnCheckedChangeListener(null) // Prevent infinite loop
        binding.btnSelectAll.isChecked = adapter.selectedItems.size == adapter.getAllMessages().size
        binding.btnSelectAll.setOnCheckedChangeListener { _, isChecked -> toggleSelectAll(isChecked) }
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }

}
