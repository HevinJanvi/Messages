package com.test.messages.demo.ui.Fragment

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.BlockedNumberContract
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.test.messages.demo.data.MessageItem
import com.test.messages.demo.databinding.FragmentConversationBinding
import com.test.messages.demo.ui.Activity.ConversationActivity
import com.test.messages.demo.ui.Activity.MainActivity
import com.test.messages.demo.ui.Activity.NewConversationActivtiy
import com.test.messages.demo.ui.Adapter.MessageAdapter
import com.test.messages.demo.ui.Dialogs.BlockDialog
import com.test.messages.demo.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Nullable

@AndroidEntryPoint
class ConversationFragment : Fragment() {

    private var blockConversationIds: List<Long> = emptyList()
    private var archivedConversationIds: List<Long> = emptyList()
    private var pinnedThreadIds: List<Long> = emptyList()
    val viewModel: MessageViewModel by viewModels()

    private lateinit var adapter: MessageAdapter
    private lateinit var binding: FragmentConversationBinding
    private var lastVisibleItemPosition: Int = 0
    var onSelectionChanged: ((Int, Int) -> Unit)? = null
    private var blockedNumbers: List<String> = emptyList()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View? {
        binding = FragmentConversationBinding.inflate(inflater, container, false);

        setupRecyclerView()
        checkPermissionsAndLoadMessages()

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_SMS
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_SMS),
                100
            )
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                101
            )
        }

        viewModel.messages.observe(viewLifecycleOwner) { messageList ->
            (activity as? MainActivity)?.updateTotalMessagesCount(messageList.size)
            CoroutineScope(Dispatchers.IO).launch {
                blockedNumbers = viewModel.getBlockedNumbers()
                archivedConversationIds =
                    viewModel.getArchivedConversations().map { it.conversationId }
                blockConversationIds = viewModel.getBlockedConversations().map { it.conversationId }
                pinnedThreadIds = viewModel.getPinnedThreadIds() ?: emptyList()


                val filteredMessages = messageList
                    .filter { it.number !in blockedNumbers }
                    .filter { it.threadId !in archivedConversationIds }
                    .filter { it.threadId !in blockConversationIds }
                    .map { message ->
                        message.copy(isPinned = pinnedThreadIds.contains(message.threadId))
                    }
                    .sortedByDescending { it.isPinned }

                withContext(Dispatchers.Main) {
                    if (adapter.selectedMessages != filteredMessages) {
                        adapter.submitList(filteredMessages)
                    }
                    if (lastVisibleItemPosition == adapter.itemCount - 1) {
                        binding.conversationList.scrollToPosition(lastVisibleItemPosition)
                    }
                }
            }

        }

        binding.newConversation.setOnClickListener {
            val intent = Intent(requireContext(), NewConversationActivtiy::class.java)
            startActivity(intent)
        }
        return binding.getRoot();

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupRecyclerView() {
        adapter = MessageAdapter(
            onSelectionChanged = { count ->
                val pinnedCount = adapter.selectedMessages.count { it.isPinned }
                onSelectionChanged?.invoke(count, pinnedCount)
            }
        )

        binding.conversationList.itemAnimator = null
        binding.conversationList.layoutManager = LinearLayoutManager(requireActivity())
        binding.conversationList.adapter = adapter
        adapter.onItemClickListener = { message ->
            val intent = Intent(requireContext(), ConversationActivity::class.java)
            intent.putExtra("EXTRA_THREAD_ID", message.threadId)
            intent.putExtra("NUMBER", message.number)
            intent.putExtra("isGroup", message.isGroupChat)
            intent.putExtra("ProfileUrl", message.profileImageUrl)
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteSelectedMessages() {
        if (adapter.selectedMessages.isEmpty()) return

        val threadIds = adapter.selectedMessages.map { it.threadId }.toSet()
        val contentResolver = requireActivity().contentResolver
        val updatedList = viewModel.messages.value?.toMutableList() ?: mutableListOf()

        Thread {
            try {
                for (threadId in threadIds) {
                    val uri = Uri.parse("content://sms/conversations/$threadId")
                    val deletedRows = contentResolver.delete(uri, null, null)

                    if (deletedRows > 0) {
                        Log.d(
                            "SMS_DELETE",
                            "Deleted thread ID $threadId with $deletedRows messages."
                        )
                        updatedList.removeAll { it.threadId == threadId }
                    } else {
                        Log.d("SMS_DELETE", "Failed to delete thread ID $threadId.")
                    }
                }
                Handler(Looper.getMainLooper()).post {
                    adapter.selectedMessages.clear()
                    adapter.submitList(updatedList)
                    onSelectionChanged?.invoke(
                        adapter.selectedMessages.size,
                        adapter.selectedMessages.count { it.isPinned })
                }

            } catch (e: Exception) {
                Log.d("SMS_DELETE", "Error deleting threads: ${e.message}")
            }
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun archiveMessages() {
        val selectedIds = adapter.selectedMessages.map { it.threadId }
        viewModel.archiveSelectedConversations(selectedIds)
        val updatedList = adapter.getAllMessages().toMutableList()
        updatedList.removeAll { selectedIds.contains(it.threadId) }
        adapter.submitList(updatedList)
        adapter.clearSelection()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun pinMessages() {
        val selectedIds = adapter.selectedMessages.map { it.threadId }
        CoroutineScope(Dispatchers.IO).launch {
            val pinnedIds =
                selectedIds.filter { viewModel.isPinned(it) } // Get only pinned messages
            val unpinnedIds =
                selectedIds.filterNot { viewModel.isPinned(it) } // Get only unpinned messages
            when {
                pinnedIds.isNotEmpty() && unpinnedIds.isNotEmpty() -> {
                    if (pinnedIds.size > unpinnedIds.size) {
                        viewModel.togglePin(pinnedIds)
                    }
                }

                pinnedIds.isNotEmpty() -> {
                    viewModel.togglePin(pinnedIds)
                }

                unpinnedIds.isNotEmpty() -> {
                    viewModel.togglePin(unpinnedIds)
                }
            }
            withContext(Dispatchers.Main) {
                adapter.clearSelection()
            }
        }
    }

    /*@RequiresApi(Build.VERSION_CODES.Q)
    fun BlockMessages() {
        val selectedMessages = adapter.selectedMessages.toList()
        if (selectedMessages.isNotEmpty()) {
            viewModel.blockContacts(selectedMessages)
            adapter.clearSelection()
        }
    }*/

    @RequiresApi(Build.VERSION_CODES.Q)
    fun BlockMessages() {
        val selectedIds = adapter.selectedMessages.map { it.threadId }
        val blockDialog = BlockDialog(requireContext()) {
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.blockSelectedConversations(selectedIds)
                withContext(Dispatchers.Main) {
                    val updatedList = adapter.getAllMessages().toMutableList()
                    updatedList.removeAll { selectedIds.contains(it.threadId) }
                    adapter.submitList(updatedList)
                    adapter.clearSelection()
                }
            }
        }
        blockDialog.show()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun markReadMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val updatedList = adapter.getAllMessages().toMutableList()
            val unreadMessages = updatedList.filter { !it.isRead }
            if (unreadMessages.isEmpty()) return@launch
            val unreadThreadIds = unreadMessages.map { it.threadId }.distinct()
            val contentValues = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
            }
            val selection = "${Telephony.Sms.THREAD_ID} IN (${unreadThreadIds.joinToString(",")})"
            val updatedRows = requireContext().contentResolver.update(
                Telephony.Sms.CONTENT_URI, contentValues, selection, null
            )
            if (updatedRows > 0) {
                updatedList.replaceAll { message ->
                    if (unreadThreadIds.contains(message.threadId)) message.copy(isRead = true) else message
                }
                withContext(Dispatchers.Main) {
                    adapter.submitList(updatedList)
                }
            }
        }
    }


    fun clearSelection() {
        adapter.clearSelection()
    }

    fun toggleSelectAll(selectAll: Boolean) {
        if (selectAll) {
            adapter.selectedMessages.clear()
            val allMessages = adapter.getAllMessages()
            adapter.selectedMessages.addAll(allMessages)
            Log.d("ConversationFragment", "All messages selected: ${adapter.selectedMessages.size}")
        } else {
            adapter.selectedMessages.clear()
        }
        onSelectionChanged?.invoke(
            adapter.selectedMessages.size,
            adapter.selectedMessages.count { it.isPinned })
        adapter.notifyDataSetChanged()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionsAndLoadMessages() {
        val smsPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.READ_SMS
        )
        val contactsPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.READ_CONTACTS
        )

        if (smsPermission == PackageManager.PERMISSION_GRANTED && contactsPermission == PackageManager.PERMISSION_GRANTED) {
            Log.d("ObserverDebug", "checkPermissionsAndLoadMessages: ")
            viewModel.loadMessages()
        } else {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_SMS,
                    android.Manifest.permission.READ_CONTACTS
                ),
                101
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            101 -> {
                val smsPermissionGranted =
                    grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
                val contactsPermissionGranted =
                    grantResults.getOrNull(1) == PackageManager.PERMISSION_GRANTED

                if (smsPermissionGranted && contactsPermissionGranted) {
                    viewModel.loadMessages()
                } else {
                    Toast.makeText(requireActivity(), "Permissions denied", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }


}