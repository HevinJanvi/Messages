package com.test.messages.demo.Ui.Activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.BlockedNumberContract
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.Helper.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Helper.Constants.NAME
import com.test.messages.demo.Helper.Constants.NUMBER
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.databinding.ActivityBlockContactBinding
import com.test.messages.demo.Ui.Adapter.BlockedContactAdapter
import com.test.messages.demo.Utils.SmsPermissionUtils
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockedContactActivity : BaseActivity() {

    private lateinit var binding: ActivityBlockContactBinding
    private lateinit var adapter: BlockedContactAdapter
    val viewModel: MessageViewModel by viewModels()
    private var messageList: List<MessageItem> = emptyList()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockContactBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsetsToView(binding.rootView)
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
            adapter.selectedItems.addAll(adapter.getAllMessages())
            adapter.isMultiSelectMode = true
        } else {
            adapter.isMultiSelectMode = false
        }
        adapter.notifyDataSetChanged()
        updateSelectAllCheckbox()
        showBottomLayout(adapter.selectedItems.isNotEmpty())
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteSelectedMessages() {
        if (adapter.selectedItems.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_messages_selected), Toast.LENGTH_SHORT).show()
            return
        }

        val contentResolver = contentResolver
        val selectedMessages = adapter.selectedItems.toList()

        Thread {
            try {
                for (message in selectedMessages) {
                    val messageUri = Uri.parse("content://sms/${message.threadId}")
                    val deletedRows = contentResolver.delete(messageUri, null, null)

                    if (deletedRows > 0) {
                    } else {
                    }
                }

                Handler(Looper.getMainLooper()).post {
                    val updatedList = adapter.messages.toMutableList().apply {
                        removeAll(selectedMessages)
                    }
                    adapter.updateList(updatedList)
                    adapter.clearSelection()
                    Toast.makeText(this,
                        getString(R.string.deleted_selected_messages), Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
            }
        }.start()
    }

    private fun unblockSelectedNumbers() {
        if (adapter.selectedItems.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_numbers_selected), Toast.LENGTH_SHORT).show()
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
                    } else {
                    }
                }

                Handler(Looper.getMainLooper()).post {
                    val updatedList = adapter.messages.filterNot { it.number in selectedNumbers }

                    adapter.updateList(updatedList)
                    adapter.clearSelection()
                    Toast.makeText(this,
                        getString(R.string.unblocked_selected_numbers), Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
            }
        }.start()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkAndOpenConversation(blockedMessage: MessageItem) {
        val conversationExists = messageList.any { it.number == blockedMessage.number }

        if (conversationExists) {
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, blockedMessage.threadId)
                putExtra(NUMBER, blockedMessage.number)
                .putExtra(NAME, blockedMessage.sender)
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
        binding.btnSelectAll.setOnCheckedChangeListener(null)
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
