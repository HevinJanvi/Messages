package com.test.messages.demo.ui.Activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
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
import dagger.hilt.android.AndroidEntryPoint
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val numbersList = intent.getStringArrayListExtra("GROUP_MEMBERS") ?: arrayListOf()
        groupName = intent.getStringExtra("GROUP_NAME") ?: getString(R.string.group_chat)
        threadId = intent.getLongExtra("EXTRA_THREAD_ID", -1)

        if (threadId != -1L) {
            loadGroupName(threadId)
        }

        adapter = GroupMemberAdapter(numbersList, this)
        binding.recyclerViewMembers.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMembers.adapter = adapter
        binding.textGroupName.text = groupName

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
            val deleteDialog = DeleteDialog(this) {
                deleteMessage()
            }
            deleteDialog.show()
        }

    }

    private fun checkIfArchived(threadId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val archivedConversationIds =
                viewModel.getArchivedConversations().map { it.conversationId }

            withContext(Dispatchers.Main) {
                if (archivedConversationIds.contains(threadId)) {
                    binding.icArchiveText.text = getString(R.string.unarchived)
                    binding.icArchive.setImageResource(R.drawable.ic_unarchive)

                    binding.lyArchive.setOnClickListener {
                        viewModel.unarchiveConversations(listOf(threadId))
                        refreshListStatus()
                    }
                } else {
                    binding.icArchiveText.text = getString(R.string.archive)
                    binding.icArchive.setImageResource(R.drawable.ic_archive)

                    binding.lyArchive.setOnClickListener {
                        viewModel.archiveSelectedConversations(listOf(threadId))
                        refreshListStatus()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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
        val groupName = sharedPreferences.getString("${GROUP_NAME_KEY}$threadId", GROUP_NAME_DEFAULT)
        binding.textGroupName.text = groupName
    }
    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }

}