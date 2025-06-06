package com.test.messages.demo.ui.Activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.Util.Constants.DROPMSG
import com.test.messages.demo.Util.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.Constants.FROMBLOCK
import com.test.messages.demo.Util.Constants.GROUP_SEPARATOR
import com.test.messages.demo.Util.Constants.NAME
import com.test.messages.demo.Util.Constants.NUMBER
import com.test.messages.demo.Util.Constants.PREFS_NAME
import com.test.messages.demo.Util.DraftChangedEvent
import com.test.messages.demo.Util.MessagesRefreshEvent
import com.test.messages.demo.databinding.ActivityBlockBinding
import com.test.messages.demo.ui.Adapter.BlockedMessagesAdapter
import com.test.messages.demo.ui.Dialogs.UnblockDialog
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.data.viewmodel.DraftViewModel
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.ui.Dialogs.DeleteProgressDialog
import com.test.messages.demo.ui.SMSend.hasReadContactsPermission
import com.test.messages.demo.ui.SMSend.hasReadSmsPermission
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@AndroidEntryPoint
class BlockMessageActivity : BaseActivity() {
    private lateinit var binding: ActivityBlockBinding
    private lateinit var adapter: BlockedMessagesAdapter
    private val viewModel: MessageViewModel by viewModels()
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val draftViewModel: DraftViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(binding.rootView)
        EventBus.getDefault().register(this)
        binding.blockRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BlockedMessagesAdapter(
            onSelectionChanged = { selectedCount ->
                updateSelectedItemsCount()

                binding.btnSelectAll.setOnCheckedChangeListener(null)
                binding.btnSelectAll.isChecked = adapter.isAllSelected()
                binding.btnSelectAll.setOnCheckedChangeListener { _, isChecked ->
                    adapter.selectAll(isChecked)
                }
            }
        )
        binding.switchDropMessages.isChecked = prefs.getBoolean(DROPMSG, true)
        binding.switchDropMessages.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(DROPMSG, isChecked).apply()
        }
        binding.blockRecyclerView.adapter = adapter
        adapter.onBlockItemClickListener = { message ->
            val intent = Intent(this, ConversationActivity::class.java)
            intent.putExtra(EXTRA_THREAD_ID, message.threadId)
            intent.putExtra(NUMBER, message.number)
            intent.putExtra(NAME, message.sender)
            intent.putExtra(FROMBLOCK, true)
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
        draftViewModel.draftsLiveData.observe(this) { draftMap ->
            adapter.updateDrafts(draftMap)
        }
        if (hasReadSmsPermission()) {
            draftViewModel.loadAllDrafts()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDraftUpdateEvent(event: DraftChangedEvent) {
        try {
            Handler(Looper.getMainLooper()).postDelayed({
                draftViewModel.loadAllDrafts()
                viewModel.loadMessages()
            }, 500)
        } catch (e: Exception) {
        }

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
                        adapter.getAllMessages().filterNot { it.threadId in selectedThreadIds }
                            ?: emptyList()
                    viewModel.updateMessages(updatedList)
                    adapter.clearSelection()
                    adapter.submitList(updatedList)
                }
            }
            blockDialog.show()
        }
        binding.btnDelete.setOnClickListener {
            val deleteDialog = DeleteDialog(this, "block", true) {
                val selectedThreadIds = adapter?.getSelectedThreadIds() ?: emptyList()
                if (selectedThreadIds.isNotEmpty()) {
                    deleteMessages()
                    viewModel.deleteblockConversations(selectedThreadIds)
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessagesRefreshed(event: MessagesRefreshEvent) {
        if (event.success) {
            Handler(Looper.getMainLooper()).postDelayed({
                viewModel.loadMessages()
            }, 100)

        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteMessages() {
        if (adapter.selectedMessages.isEmpty()) return
        var messages = adapter.selectedMessages.toList()
        adapter.selectedMessages.clear()
        val contentResolver = contentResolver
        val db = AppDatabase.getDatabase(this).recycleBinDao()
        val updatedList = viewModel.messages.value?.toMutableList() ?: mutableListOf()
        val handler = Handler(Looper.getMainLooper())
        val deleteDialog = DeleteProgressDialog(this)
        val showDialogRunnable = Runnable {
            deleteDialog.show(getString(R.string.moving_messages_to_recycle_bin))
        }
        handler.postDelayed(showDialogRunnable, 300)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val deletedMessages = mutableListOf<DeletedMessage>()
                val existingBodyDatePairs = mutableSetOf<Pair<String, Long>>()

                for (item in messages) {
                    val threadId = item.threadId
                    viewModel.deleteStarredMessagesForThread(threadId)
                    val cursor = contentResolver.query(
                        Telephony.Sms.CONTENT_URI,
                        null,
                        "thread_id = ?",
                        arrayOf(threadId.toString()),
                        null
                    )

                    cursor?.use {
                        val idIndex = it.getColumnIndex(Telephony.Sms._ID)
                        val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                        val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                        val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                        val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)
                        val readIndex = it.getColumnIndex(Telephony.Sms.READ)
                        val subIdIndex = it.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)

                        while (it.moveToNext()) {
                            val messageId = it.getLong(idIndex)
                            val address = it.getString(addressIndex) ?: continue
                            val body = it.getString(bodyIndex) ?: ""
                            val date = it.getLong(dateIndex)
                            val key = body to date
                            if (!existingBodyDatePairs.add(key)) continue
                            val subscriptionId = if (subIdIndex != -1) it.getInt(subIdIndex) else -1
                            val deletedMessage = DeletedMessage(
                                messageId = messageId,
                                threadId = threadId,
                                address = address,
                                date = date,
                                body = body,
                                type = it.getInt(typeIndex),
                                read = it.getInt(readIndex) == 1,
                                subscriptionId = subscriptionId,
                                deletedTime = System.currentTimeMillis(),
                                isGroupChat = address.contains(GROUP_SEPARATOR),
                                profileImageUrl = item.profileImageUrl
                            )
                            deletedMessages.add(deletedMessage)
                        }
                    }

                    val uri = Uri.parse("content://sms/conversations/$threadId")
                    contentResolver.delete(uri, null, null)
                    updatedList.removeAll { it.threadId == threadId }
                }

                if (deletedMessages.isNotEmpty()) db.insertMessages(deletedMessages)

                withContext(Dispatchers.Main) {
                    viewModel.loadMessages()
                    handler.removeCallbacks(showDialogRunnable)
                    deleteDialog.dismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    handler.removeCallbacks(showDialogRunnable)
                    deleteDialog.dismiss()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (adapter.selectedMessages.size > 0) {
            adapter.clearSelection()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this) && hasReadSmsPermission() && hasReadContactsPermission()) {
            return
        }
    }
}