package com.test.messages.demo.ui.Activity

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.Util.Constants
import com.test.messages.demo.Util.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.Constants.GROUP_SEPARATOR
import com.test.messages.demo.Util.Constants.NAME
import com.test.messages.demo.Util.Constants.NUMBER
import com.test.messages.demo.Util.DraftChangedEvent
import com.test.messages.demo.Util.MessagesRefreshEvent
import com.test.messages.demo.databinding.ActivityArchivedBinding
import com.test.messages.demo.ui.Adapter.ArchiveMessageAdapter
import com.test.messages.demo.ui.Dialogs.BlockDialog
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.SnackbarUtil
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.data.viewmodel.DraftViewModel
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.ui.Dialogs.BlockProgressDialog
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
@RequiresApi(Build.VERSION_CODES.Q)
class ArchivedActivity : BaseActivity() {
    private lateinit var binding: ActivityArchivedBinding
    private lateinit var adapter: ArchiveMessageAdapter
    private val viewModel: MessageViewModel by viewModels()
    private var pinnedThreadIds: List<Long> = emptyList()
    private val draftViewModel: DraftViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this) && hasReadSmsPermission() && hasReadContactsPermission()) {
            return
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDraftUpdateEvent(event: DraftChangedEvent) {
        draftViewModel.loadAllDrafts()
        viewModel.loadMessages()
        try {
            Handler(Looper.getMainLooper()).postDelayed({
                draftViewModel.loadAllDrafts()
                viewModel.loadMessages()
            }, 500)
        } catch (e: Exception) {
        }

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchivedBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(binding.rootView)
        EventBus.getDefault().register(this)

        binding.archiveRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ArchiveMessageAdapter(
            onArchiveSelectionChanged = { selectedCount ->
                val pinnedCount = adapter.selectedMessages.count { it.isPinned }
                updatePinLayout(selectedCount, pinnedCount)
                updateSelectedItemsCount()

                binding.btnSelectAll.setOnCheckedChangeListener(null)
                binding.btnSelectAll.isChecked = adapter.isAllSelected()
                binding.btnSelectAll.setOnCheckedChangeListener { _, isChecked ->
                    adapter.selectAll(isChecked)
                }
            }
        )

        binding.archiveRecyclerView.adapter = adapter

        adapter.onArchiveItemClickListener = { message ->
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, message.threadId)
                putExtra(NUMBER, message.number)
                putExtra(NAME, message.sender)
                putExtra(Constants.FROMARCHIVE, true)
            }
            startActivity(intent)
        }

        viewModel.messages.observe(this) { messageList ->
            CoroutineScope(Dispatchers.IO).launch {
                val archivedConversationIds =
                    viewModel.getArchivedConversations().map { it.conversationId }

                val blockedConversationIds =
                    viewModel.getBlockedConversations().map { it.conversationId }

                pinnedThreadIds = viewModel.getPinnedThreadIds() ?: emptyList()
                val archivedMessages = messageList
                    .filter {
                        archivedConversationIds.contains(it.threadId) &&
                                !blockedConversationIds.contains(it.threadId)
                    }
                    .map { message ->
                        message.copy(isPinned = pinnedThreadIds.contains(message.threadId))
                    }
                    .sortedByDescending { it.isPinned }

                withContext(Dispatchers.Main) {
                    binding.emptyList.visibility =
                        if (archivedMessages.isEmpty()) View.VISIBLE else View.GONE

                    val layoutManager =
                        binding.archiveRecyclerView.layoutManager as LinearLayoutManager
                    val lastPosition = layoutManager.findFirstVisibleItemPosition()
                    adapter.submitList(archivedMessages)
                    binding.archiveRecyclerView.post {
                        layoutManager.scrollToPosition(lastPosition)
                    }
                }
            }
        }
        viewModel.loadArchivedThreads()
        draftViewModel.draftsLiveData.observe(this) { draftMap ->
            adapter.updateDrafts(draftMap)
        }
        if (hasReadSmsPermission()) {
            draftViewModel.loadAllDrafts()
        }
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
                viewModel.togglePin(selectedIds) {
                    pinnedThreadIds = viewModel.getPinnedThreadIds() ?: emptyList()
                    val updatedPinnedThreadIds = pinnedThreadIds.toMutableSet()
                    adapter.clearSelection()
                    val updatedList = adapter.messages.map {
                        it.copy(
                            isPinned = updatedPinnedThreadIds.contains(it.threadId)
                        )
                    }.sortedWith(
                        compareByDescending<MessageItem> { it.isPinned }
                            .thenByDescending { it.lastMsgDate }
                    ) ?: emptyList()
                    val pinIndexes = updatedList.mapIndexedNotNull { index, message ->
                        if (message.isPinned) index else null
                    }
                    pinIndexes.forEach {
                        adapter.notifyItemChanged(it)
                    }
                    adapter.submitList(updatedList)
                    try {
                        Handler(Looper.getMainLooper()).postDelayed({
                            adapter.notifyDataSetChanged()
                        }, 500)
                    } catch (e: Exception) {
                    }


                }
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

                        val updatedList =
                            viewModel.messages.value?.filterNot { it.threadId in selectedThreadIds }
                                ?: emptyList()
                        viewModel.updateMessages(updatedList)

                        SnackbarUtil.showSnackbar(
                            binding.root,
                            getString(R.string.unarchived_successfully), getString(R.string.undo)
                        ) {
                            restoreArchivedMessages(removedMessages)
                        }

                    }
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            val deleteDialog = DeleteDialog(this, "archive", true) {
                val selectedThreadIds = adapter?.getSelectedThreadIds() ?: emptyList()
                if (selectedThreadIds.isNotEmpty()) {
                    deleteMessages()
                    viewModel.deleteArchiveConversations(selectedThreadIds)
                    adapter?.removeItems(selectedThreadIds)
                    adapter?.clearSelection()
                }
            }
            deleteDialog.show()
        }

        binding.btnBlock.setOnClickListener {
            BlockMessages()
//            blockMessages(adapter.getSelectedThreadIds())

            /*val selectedThreadIds = adapter.getSelectedThreadIds()

            if (selectedThreadIds.isNotEmpty()) {
                val blockDialog = BlockDialog(this) {
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.unarchiveConversations(selectedThreadIds)
                        val selectedIds = adapter.selectedMessages.map { it.threadId }
                        viewModel.blockSelectedConversations(selectedIds) {
                            adapter.removeItems(selectedThreadIds)
                            adapter.clearSelection()
                        }
                    }
                }
                blockDialog.show()
            }*/
        }

        binding.icMore.setOnClickListener {
            showPopup(it)
        }
    }

    fun BlockMessages() {
        val selectedGroups = adapter.selectedMessages.filter { it.isGroupChat }
        if (selectedGroups.isNotEmpty()) {
            return
        }

        val selectedIds = adapter.selectedMessages.map { it.threadId }
        val selectedThreadIds = adapter.getSelectedThreadIds()
        val blockDialog = BlockDialog(this) {
            if (selectedIds.size > 10) {
                val progressDialog = BlockProgressDialog(this)
                runOnUiThread {
                    progressDialog.show(getString(R.string.block_messages_progress))
                }
                viewModel.unarchiveConversations(selectedThreadIds)
                viewModel.blockSelectedConversations(selectedIds) {
                    adapter.removeItems(selectedThreadIds)
                    adapter.clearSelection()
                    progressDialog.dismiss()
                }

            } else {
                viewModel.unarchiveConversations(selectedThreadIds)
                viewModel.blockSelectedConversations(selectedIds)
                adapter.clearSelection()
            }

        }

        blockDialog.show()
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessagesRefreshed(event: MessagesRefreshEvent) {
        if (event.success) {
            Handler(Looper.getMainLooper()).postDelayed({
                viewModel.loadMessages()
            }, 100)

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private fun updateSelectedItemsCount() {
        binding.txtSelectedCount.text =
            "${adapter.selectedMessages.size}" + " " + getString(R.string.selected)

        if (adapter.selectedMessages.size > 0) {
            binding.lySelectedItemsArchive.visibility = View.VISIBLE
            binding.selectMenuArchive.visibility = View.VISIBLE
            binding.toolBarArchive.visibility = View.GONE

            val shouldEnable = adapter.selectedMessages.any { it.isGroupChat }
            binding.btnBlock.isEnabled = !shouldEnable
            val color = if (!shouldEnable) {
                ContextCompat.getColor(this, R.color.red)
            } else {
                ContextCompat.getColor(this, R.color.default_shadow_color)
            }
            ImageViewCompat.setImageTintList(binding.blockic, ColorStateList.valueOf(color))
            if (!shouldEnable) {
                binding.txtblock.setTextColor(resources.getColor(R.color.gray_txtcolor))
            } else {
                binding.txtblock.setTextColor(resources.getColor(R.color.default_shadow_color))
            }

        } else {
            binding.lySelectedItemsArchive.visibility = View.GONE
            binding.selectMenuArchive.visibility = View.GONE
            binding.toolBarArchive.visibility = View.VISIBLE
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteMessages() {
        if (adapter.selectedMessages.isEmpty()) return

        var messages = adapter.selectedMessages.toList()
        adapter.selectedMessages.clear()
        val contentResolver = contentResolver
        val db = AppDatabase.getDatabase(this).recycleBinDao()
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
                    viewModel.deleteScheduledForThread(threadId)

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

    fun showPopup(view: View) {
        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialog = layoutInflater.inflate(R.layout.popup_menu, null)

        val popupWindow = PopupWindow(
            dialog,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val marginDp = 16
        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginDp.toFloat(),
            view.resources.displayMetrics
        ).toInt()


        dialog.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = dialog.measuredWidth
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1] + view.height
        val isRTL = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val x = if (isRTL) {
            anchorX + marginPx
        } else {
            anchorX + view.width - popupWidth - marginPx
        }

        popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, x, anchorY)

        val lyMarkAsRead: LinearLayout = dialog.findViewById(R.id.txtMarkAsRead)
        val selectedMessages = adapter.selectedMessages

        if (selectedMessages.isNotEmpty()) {
            val allUnread = selectedMessages.all { !it.isRead }
            val allRead = selectedMessages.all { it.isRead }
            val firstIsRead = selectedMessages.first().isRead

            val textView = lyMarkAsRead.getChildAt(0) as TextView

            when {
                allUnread -> {
                    textView.text = getString(R.string.mark_as_read)
                    textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_read, 0, 0, 0)
                }

                allRead -> {
                    textView.text = getString(R.string.mark_as_unread)
                    textView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_mark_read,
                        0,
                        0,
                        0
                    )
                }

                else -> {

                    if (firstIsRead) {
                        textView.text = getString(R.string.mark_as_unread)
                        textView.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_mark_read,
                            0,
                            0,
                            0
                        )
                    } else {
                        textView.text = getString(R.string.mark_as_read)
                        textView.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_read,
                            0,
                            0,
                            0
                        )
                    }
                }
            }
        }

        lyMarkAsRead.setOnClickListener {
            popupWindow.dismiss()
            markThreadsReadOrUnread()
            adapter.clearSelection()
        }

    }

    private fun markThreadsReadOrUnread() {
        val selectedMessages = adapter.selectedMessages.toList()
        if (selectedMessages.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            val firstIsRead = selectedMessages.first().isRead
            val newReadStatus = if (firstIsRead) 0 else 1

            val threadIds = selectedMessages.map { it.threadId }
            val contentValues = ContentValues().apply {
                put(Telephony.Sms.READ, newReadStatus)
            }

            val selection =
                "${Telephony.Sms.THREAD_ID} IN (${threadIds.joinToString(GROUP_SEPARATOR)})"
            val updatedRows = contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                contentValues,
                selection,
                null
            )

            if (updatedRows > 0) {
                val updatedList = viewModel.messages.value?.map { message ->
                    if (threadIds.contains(message.threadId)) {
                        message.copy(isRead = newReadStatus == 1)
                    } else message
                } ?: emptyList()

                withContext(Dispatchers.Main) {
                    viewModel.updateMessages(updatedList)
                    adapter.updateReadStatus(threadIds)
                }
            }
        }
    }

    private fun updatePinLayout(selectedCount: Int, pinnedCount: Int) {
        if (selectedCount > 0) {
            binding.btnPin.visibility = View.VISIBLE
            val pinTextView = binding.txtPinArchiv

            val unpinnedCount = selectedCount - pinnedCount

            when {
                pinnedCount > unpinnedCount -> {
                    binding.icPinArchiv.setImageResource(R.drawable.ic_unpin2)
                    pinTextView.text = getString(R.string.unpin)
                }

                unpinnedCount > pinnedCount -> {
                    binding.icPinArchiv.setImageResource(R.drawable.ic_pin)
                    pinTextView.text = getString(R.string.pin)
                }

                else -> {
                    binding.icPinArchiv.setImageResource(R.drawable.ic_unpin2)
                    pinTextView.text = getString(R.string.unpin)
                }
            }
        } else {
            binding.btnPin.visibility = View.GONE
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