package com.test.messages.demo.ui.Activity

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.databinding.ActivityArchivedBinding
import com.test.messages.demo.ui.Adapter.ArchiveMessageAdapter
import com.test.messages.demo.ui.Dialogs.BlockDialog
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.SnackbarUtil
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)
class ArchivedActivity : BaseActivity() {
    private lateinit var binding: ActivityArchivedBinding
    private lateinit var adapter: ArchiveMessageAdapter
    private val viewModel: MessageViewModel by viewModels()
    private var pinnedThreadIds: List<Long> = emptyList()

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val conversationResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null) {

                adapter.notifyDataSetChanged()
                viewModel.loadMessages()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchivedBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)

        binding.archiveRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ArchiveMessageAdapter(
            onArchiveSelectionChanged = { selectedCount ->
                val pinnedCount = adapter.selectedMessages.count { it.isPinned }
                updatePinLayout(selectedCount, pinnedCount)
                updateSelectedItemsCount()
            }
        )

        binding.archiveRecyclerView.adapter = adapter

        adapter.onArchiveItemClickListener = { message ->
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, message.threadId)
                putExtra(NUMBER, message.number)
                putExtra(NAME, message.sender)
            }
            conversationResultLauncher.launch(intent)
        }

        viewModel.messages.observe(this) { messageList ->
            CoroutineScope(Dispatchers.IO).launch {
                val archivedConversationIds =
                    viewModel.getArchivedConversations().map { it.conversationId }

                pinnedThreadIds = viewModel.getPinnedThreadIds() ?: emptyList()
                val archivedMessages = messageList
                    .filter { archivedConversationIds.contains(it.threadId) }
                    .map { message ->
                        message.copy(isPinned = pinnedThreadIds.contains(message.threadId))
                    }
                    .sortedByDescending { it.isPinned }

                withContext(Dispatchers.Main) {
                    val layoutManager =
                        binding.archiveRecyclerView.layoutManager as LinearLayoutManager
                    val lastPosition = layoutManager.findFirstVisibleItemPosition()

                    adapter.submitList(archivedMessages)
                    binding.archiveRecyclerView.post {
                        layoutManager.scrollToPosition(lastPosition)
                    }

                    binding.emptyList.visibility =
                        if (archivedMessages.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        viewModel.loadArchivedThreads()

        setupClickListeners()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupClickListeners() {
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        binding.icSetting.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        binding.btnSelectAll.setOnCheckedChangeListener { _, isChecked ->
            adapter.selectAll(isChecked)
        }
        binding.btnPin.setOnClickListener {
            val selectedIds = adapter.selectedMessages.map { it.threadId }
            if (selectedIds.isNotEmpty()) {
                viewModel.togglePin(selectedIds)
            }
            adapter.clearSelection()

            Handler(Looper.getMainLooper()).postDelayed({
                adapter.notifyDataSetChanged()
            }, 100)
        }
        binding.btnUnarchive.setOnClickListener {
            val selectedThreadIds = adapter.getSelectedThreadIds()
            if (selectedThreadIds.isNotEmpty()) {
                val removedMessages = adapter.selectedMessages.toList()
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.unarchiveConversations(selectedThreadIds)
                    withContext(Dispatchers.Main) {
                        adapter.removeItems(selectedThreadIds)
                        adapter.clearSelection()

                        val updatedList = viewModel.messages.value?.filterNot { it.threadId in selectedThreadIds } ?: emptyList()
                        viewModel.updateMessages(updatedList)

                        SnackbarUtil.showSnackbar(binding.root,
                            getString(R.string.unarchived_successfully), getString(R.string.undo)) {
                            restoreArchivedMessages(removedMessages)
                        }

                    }
                }
            }
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
        binding.btnBlock.setOnClickListener {
            val selectedThreadIds = adapter.getSelectedThreadIds()

            if (selectedThreadIds.isNotEmpty()) {
                val blockDialog = BlockDialog(this) {
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.unarchiveConversations(selectedThreadIds)
                        val selectedIds = adapter.selectedMessages.map { it.threadId }
                        viewModel.blockSelectedConversations(selectedIds)
                        withContext(Dispatchers.Main) {
                            adapter.removeItems(selectedThreadIds)
                            adapter.clearSelection()
                        }
                    }
                }
                blockDialog.show()
            }
        }

        binding.icMore.setOnClickListener {
            showPopup(it)
        }
    }

    private fun restoreArchivedMessages(messages: List<MessageItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.archiveSelectedConversations(messages.map { it.threadId })
            withContext(Dispatchers.Main) {
                val currentList = viewModel.messages.value.orEmpty()
                val restoredList = (currentList + messages)
                    .distinctBy { it.threadId }
                    .sortedByDescending { it.timestamp }

                viewModel.updateMessages(restoredList)
            }
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
        val deletedThreads = mutableListOf<Long>()

        Thread {
            try {
                for (threadId in threadIds) {
                    val uri = Uri.parse("content://sms/conversations/$threadId")
                    val deletedRows = contentResolver.delete(uri, null, null)
                    if (deletedRows > 0) {
                        deletedThreads.add(threadId)
                    } else {
                        Log.d("SMS_DELETE", "Failed to delete thread ID $threadId.")
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
                Log.d("SMS_DELETE", "Error deleting threads: ${e.message}")
            }
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun showPopup(view: View) {
        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialog = layoutInflater.inflate(R.layout.popup_menu, null)

        val popupWindow = PopupWindow(
            dialog, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )

        popupWindow.setBackgroundDrawable(BitmapDrawable())
        popupWindow.isOutsideTouchable = true

        val lyMarkAsRead: LinearLayout = dialog.findViewById(R.id.txtMarkAsRead)
        lyMarkAsRead.setOnClickListener {
            popupWindow.dismiss()
            markThreadsAsRead()
            adapter.clearSelection()
        }
        popupWindow.showAsDropDown(view, 0, 0)
    }

    private fun markThreadsAsRead() {
        val selectedThreadIds = adapter.getSelectedThreadIds()
        if (selectedThreadIds.isEmpty()) return

        val contentValues = ContentValues().apply { put(Telephony.Sms.READ, 1) }
        val selection = "${Telephony.Sms.THREAD_ID} IN (${selectedThreadIds.joinToString(",")})"

        val updatedRows =
            contentResolver.update(Telephony.Sms.CONTENT_URI, contentValues, selection, null)
        if (updatedRows > 0) {
            val updatedList = viewModel.messages.value?.map { message ->
                if (selectedThreadIds.contains(message.threadId)) {
                    message.copy(isRead = true)
                } else {
                    message
                }
            } ?: emptyList()

            viewModel.updateMessages(updatedList)
            adapter.updateReadStatus(selectedThreadIds)
        }
    }

    private fun updatePinLayout(selectedCount: Int, pinnedCount: Int) {
        if (selectedCount > 0) {
            binding.btnPin.visibility = View.VISIBLE
            val pinTextView = binding.txtPinArchiv

            val unpinnedCount = selectedCount - pinnedCount

            when {
                pinnedCount > unpinnedCount -> {
                    binding.icPinArchiv.setImageResource(R.drawable.ic_unpin)
                    pinTextView.text = getString(R.string.unpin)
                }

                unpinnedCount > pinnedCount -> {
                    binding.icPinArchiv.setImageResource(R.drawable.ic_pin)
                    pinTextView.text = getString(R.string.pin)
                }

                else -> {
                    binding.icPinArchiv.setImageResource(R.drawable.ic_unpin)
                    pinTextView.text = getString(R.string.unpin)
                }
            }
        } else {
            binding.btnPin.visibility = View.GONE
        }
    }


    private fun updatePinButton() {
        val selectedIds = adapter.selectedMessages.map { it.threadId }
        val pinnedIds = selectedIds.filter { it in pinnedThreadIds }

        if (selectedIds.isEmpty()) {
            binding.txtPinArchiv.text = getString(R.string.pin)
            binding.icPinArchiv.setImageResource(R.drawable.ic_pin)
            return
        }

        if (pinnedIds.size == selectedIds.size) {
            binding.txtPinArchiv.setText(getString(R.string.unpin))
            binding.icPinArchiv.setImageResource(R.drawable.ic_unpin)
        } else {
            binding.txtPinArchiv.setText(getString(R.string.pin))
            binding.icPinArchiv.setImageResource(R.drawable.ic_pin)
        }
    }


    override fun onBackPressed() {
        if (adapter.selectedMessages.size > 0) {
            adapter.clearSelection()
        } else {
            super.onBackPressed()
        }
    }
}