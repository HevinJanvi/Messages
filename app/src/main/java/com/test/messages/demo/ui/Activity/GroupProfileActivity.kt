package com.test.messages.demo.ui.Activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.GROUP_MEMBERS
import com.test.messages.demo.Util.CommanConstants.GROUP_NAME
import com.test.messages.demo.Util.CommanConstants.GROUP_NAME_DEFAULT
import com.test.messages.demo.Util.CommanConstants.GROUP_NAME_KEY
import com.test.messages.demo.Util.CommanConstants.PREFS_NAME
import com.test.messages.demo.databinding.ActivityGroupProfileBinding
import com.test.messages.demo.ui.Adapter.GroupMemberAdapter
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.ui.Dialogs.RenameDialog
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.UpdateGroupNameEvent
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.ui.send.hasReadContactsPermission
import com.test.messages.demo.ui.send.hasReadSmsPermission
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)
class GroupProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityGroupProfileBinding
    private lateinit var adapter: GroupMemberAdapter
    private lateinit var groupName: String
    private var threadId: Long = -1
    private val viewModel: MessageViewModel by viewModels()
    private var isArchived = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsetsToView(binding.rootView)
        val numbersList = intent.getStringArrayListExtra(GROUP_MEMBERS) ?: arrayListOf()
        groupName = intent.getStringExtra(GROUP_NAME) ?: getString(R.string.group_chat)
        threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)

        if (threadId != -1L) {
            loadGroupName(threadId)
        }

        adapter = GroupMemberAdapter(numbersList, this)
        binding.recyclerViewMembers.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMembers.adapter = adapter
        binding.textGroupName.text = groupName
        binding.textGroupName.movementMethod =
            android.text.method.ScrollingMovementMethod.getInstance()
        binding.textGroupName.isVerticalScrollBarEnabled = true

        binding.icEdit.setOnClickListener {
            showRenameDialog()
        }
        if (threadId != -1L) {
            checkIfArchived(threadId)
        }
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        binding.deleteLy.setOnClickListener {
            val deleteDialog = DeleteDialog(this, "group", true) {
                deleteMessagesForCurrentThread(threadId)
            }
            deleteDialog.show()
        }
    }

    private fun checkIfArchived(threadId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val archivedConversationIds =
                viewModel.getArchivedConversations().map { it.conversationId }
            withContext(Dispatchers.Main) {
                isArchived = archivedConversationIds.contains(threadId)
                updateArchiveUI()

                // Set up click listener once
                binding.lyArchive.setOnClickListener {
                    if (isArchived) {
                        viewModel.unarchiveConversations(listOf(threadId))
                        isArchived = false
                    } else {
                        viewModel.archiveSelectedConversations(listOf(threadId))
                        isArchived = true
                    }
                    updateArchiveUI()
                }
            }
        }
    }

    private fun updateArchiveUI() {
        if (isArchived) {
            binding.archiveText.text = getString(R.string.unarchived)
            binding.icArchive.setImageResource(R.drawable.ic_unarchive)
        } else {
            binding.archiveText.text = getString(R.string.archive)
            binding.icArchive.setImageResource(R.drawable.ic_archive)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteMessagesForCurrentThread(threadId: Long) {
        val contentResolver = contentResolver
        val db = AppDatabase.getDatabase(this).recycleBinDao()

        Thread {
            try {
                val deletedMessages = mutableListOf<DeletedMessage>()
                val existingBodyDatePairs =
                    mutableSetOf<Pair<String, Long>>() // To prevent duplicates

                val cursor = contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    null,
                    "thread_id = ?",
                    arrayOf(threadId.toString()),
                    Telephony.Sms.DEFAULT_SORT_ORDER
                )

                cursor?.use {
                    val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                    val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                    val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)
                    val readIndex = it.getColumnIndex(Telephony.Sms.READ)
                    val subIdIndex = it.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)
                    val messageIdIndex = it.getColumnIndex(Telephony.Sms._ID)

                    while (it.moveToNext()) {
                        val body = it.getString(bodyIndex) ?: ""
                        val date = it.getLong(dateIndex)
                        val key = Pair(body, date)
                        val address = it.getString(addressIndex) ?: ""
                        if (address.isNullOrEmpty()) {
                            continue
                        }
                        if (existingBodyDatePairs.contains(key)) continue
                        existingBodyDatePairs.add(key)
                        val isGroup = address.contains(",")
                        val deletedMessage = DeletedMessage(
                            messageId = it.getLong(messageIdIndex),
                            threadId = threadId,
                            address = address,
                            date = date,
                            body = body,
                            type = it.getInt(typeIndex),
                            read = it.getInt(readIndex) == 1,
                            subscriptionId = it.getInt(subIdIndex),
                            deletedTime = System.currentTimeMillis(),
                            isGroupChat = isGroup,
                            profileImageUrl = ""
                        )
                        deletedMessages.add(deletedMessage)
                    }
                }
                val uri = Uri.parse("content://sms/conversations/$threadId")
                contentResolver.delete(uri, null, null)
                db.insertMessages(deletedMessages)
                viewModel.loadMessages()
                Handler(Looper.getMainLooper()).post {
                    finish()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /* @RequiresApi(Build.VERSION_CODES.Q)
     fun deleteMessage() {
         val updatedList = viewModel.messages.value?.toMutableList() ?: mutableListOf()
         Thread {
             try {
                 val uri = Uri.parse("content://sms/conversations/$threadId")
                 val deletedRows = contentResolver.delete(uri, null, null)
                 if (deletedRows > 0) {
                     updatedList.removeAll { it.threadId == threadId }
                     refreshListStatus()
                 }
             } catch (e: Exception) {
             }
         }.start()
     }
 */
    private fun refreshListStatus() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 500)
        overridePendingTransition(R.anim.fadin, R.anim.fadout);
    }

    private fun showRenameDialog() {
        val currentName = binding.textGroupName.text.toString()
        val dialog = RenameDialog(this, currentName) { newName ->
            if (newName.isNotEmpty()) {
                updateGroupName(threadId, newName)
            }
        }
        dialog.show()
    }

    private fun updateGroupName(threadId: Long, newName: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("${GROUP_NAME_KEY}$threadId", newName).apply()
        binding.textGroupName.text = newName
        EventBus.getDefault().post(UpdateGroupNameEvent(threadId, newName))
    }

    private fun loadGroupName(threadId: Long) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val groupName =
            sharedPreferences.getString("${GROUP_NAME_KEY}$threadId", GROUP_NAME_DEFAULT)
        binding.textGroupName.text = groupName
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this) && !hasReadSmsPermission() && !hasReadContactsPermission()) {
            return
        }
        adapter.clearCacheAndReload()
    }

}