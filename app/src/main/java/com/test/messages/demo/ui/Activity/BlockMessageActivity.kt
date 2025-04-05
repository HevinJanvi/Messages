package com.test.messages.demo.ui.Activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.FROMBLOCK
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.databinding.ActivityBlockBinding
import com.test.messages.demo.ui.Adapter.BlockedMessagesAdapter
import com.test.messages.demo.ui.Dialogs.UnblockDialog
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class BlockMessageActivity : BaseActivity() {
    private lateinit var binding: ActivityBlockBinding
    private lateinit var adapter: BlockedMessagesAdapter
    private val viewModel: MessageViewModel by viewModels()
    private val prefs by lazy { getSharedPreferences("block_prefs", Context.MODE_PRIVATE) }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)

        binding.blockRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BlockedMessagesAdapter(
            onSelectionChanged = { selectedCount ->
                updateSelectedItemsCount()
            }
        )
        binding.switchDropMessages.isChecked = prefs.getBoolean("drop_messages", false)
        binding.switchDropMessages.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("drop_messages", isChecked).apply()
        }
        binding.blockRecyclerView.adapter = adapter
        adapter.onBlockItemClickListener = { message ->
            val intent = Intent(this, ConversationActivity::class.java)
            intent.putExtra(EXTRA_THREAD_ID, message.threadId)
            intent.putExtra(NUMBER, message.number)
            intent.putExtra(NAME, message.sender)
            intent.putExtra(FROMBLOCK,true)
            startActivity(intent)
        }
        viewModel.loadBlockThreads()
        viewModel.messages.observe(this) { messageList ->
            CoroutineScope(Dispatchers.IO).launch {
                val blockConversationIds =
                    viewModel.getBlockedConversations().map { it.conversationId }

                val blockMessages = messageList.filter { message ->
                    blockConversationIds.contains(message.threadId)
                }

                withContext(Dispatchers.Main) {
                    val layoutManager =
                        binding.blockRecyclerView.layoutManager as LinearLayoutManager
                    val lastPosition = layoutManager.findFirstVisibleItemPosition()

                    adapter.submitList(blockMessages)
                    binding.blockRecyclerView.post {
                        layoutManager.scrollToPosition(lastPosition)
                    }

                    binding.emptyList.visibility =
                        if (blockMessages.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        setupClickListeners()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupClickListeners() {
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        binding.btnSelectAll.setOnCheckedChangeListener { _, isChecked ->
            adapter.selectAll(isChecked)
        }

        binding.btnUnblock.setOnClickListener {
            val blockDialog = UnblockDialog(this) {
                val selectedThreadIds = adapter.getSelectedThreadIds()
                if (selectedThreadIds.isNotEmpty()) {
                    viewModel.unblockConversations(selectedThreadIds)
                    val updatedList =
                        viewModel.messages.value?.filterNot { it.threadId in selectedThreadIds }
                            ?: emptyList()
                    viewModel.updateMessages(updatedList)
                    adapter.clearSelection()
                    adapter.submitList(updatedList)
                }
            }
            blockDialog.show()
        }
        binding.btnDelete.setOnClickListener {
            val deleteDialog = DeleteDialog(this) {
                val selectedThreadIds = adapter?.getSelectedThreadIds() ?: emptyList()
                if (selectedThreadIds.isNotEmpty()) {
                    deleteMessages(selectedThreadIds)
                    adapter?.removeItems(selectedThreadIds)
                    adapter?.clearSelection()
                }
            }
            deleteDialog.show()
        }

    }

    private fun updateSelectedItemsCount() {
        binding.txtSelectedCount.text =
            "${adapter.selectedMessages.size}" + " " + getString(R.string.selected)
        if (adapter.selectedMessages.size > 0) {
            binding.lySelectedItemsblock.visibility = View.VISIBLE
            binding.selectMenublock.visibility = View.VISIBLE
            binding.toolBarblock.visibility = View.GONE
            binding.switchLy.visibility = View.GONE
            binding.view1.visibility = View.GONE
        } else {
            binding.lySelectedItemsblock.visibility = View.GONE
            binding.selectMenublock.visibility = View.GONE
            binding.toolBarblock.visibility = View.VISIBLE
            binding.switchLy.visibility = View.VISIBLE
            binding.view1.visibility = View.VISIBLE
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteMessages(threadIds: List<Long>) {
        if (adapter.selectedMessages.isEmpty()) return
        val contentResolver = contentResolver
        val deletedThreads = mutableListOf<Long>()

        Thread {
            try {
                for (threadId in threadIds) {
                    val uri = Uri.parse("content://sms/conversations/$threadId")
                    val deletedRows = contentResolver.delete(uri, null, null)
                    if (deletedRows > 0) {
                        deletedThreads.add(threadId)
                    }
                }

                if (deletedThreads.isNotEmpty()) {
                    Handler(Looper.getMainLooper()).post {
                        val updatedList =
                            viewModel.messages.value?.filterNot { it.threadId in deletedThreads }
                                ?: emptyList()
                        viewModel.updateMessages(updatedList)
                        adapter.removeItems(deletedThreads)
                        adapter.clearSelection()
                    }
                }
            } catch (e: Exception) {
            }
        }.start()
    }


    override fun onBackPressed() {
        if (adapter.selectedMessages.size > 0) {
            adapter.clearSelection()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }
}