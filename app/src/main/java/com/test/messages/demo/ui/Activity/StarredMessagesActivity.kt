package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.ISGROUP
import com.test.messages.demo.Util.CommanConstants.ISSTARRED
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.MessageDeletedEvent
import com.test.messages.demo.data.Database.Starred.StarredMessageDao
import com.test.messages.demo.databinding.ActivityStarredMessagesBinding
import com.test.messages.demo.ui.Adapter.StarredMessagesAdapter
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.MessageUnstarredEvent
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@AndroidEntryPoint

class StarredMessagesActivity : BaseActivity() {

    private lateinit var adapter: StarredMessagesAdapter
    private lateinit var binding: ActivityStarredMessagesBinding
    val viewModel: MessageViewModel by viewModels()
    private lateinit var starredMessageDao: StarredMessageDao

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStarredMessagesBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        EventBus.getDefault().register(this)
        binding.recyclerViewStarred.layoutManager = LinearLayoutManager(this)

        adapter = StarredMessagesAdapter()
        binding.recyclerViewStarred.adapter = adapter
        val database = AppDatabase.getDatabase(this)

        starredMessageDao = database.starredMessageDao()
        viewModel.messages.observe(this) { messageList ->
            filterAndShowMessages(messageList)
        }
        viewModel.lastStarredMessages.observe(this) { lastStarredMessages ->
            adapter.setLastStarredMessages(lastStarredMessages)
            binding.recyclerViewStarred.post {
                adapter.notifyDataSetChanged()
            }
        }

        viewModel.loadLastStarredMessages()
        adapter.onItemClickListener = { message ->
            val intent = Intent(this, ConversationActivity::class.java)
            intent.putExtra(EXTRA_THREAD_ID, message.threadId)
            intent.putExtra(NUMBER, message.number)
            intent.putExtra(NAME, message.sender)
            intent.putExtra(ISGROUP, message.isGroupChat)
            intent.putExtra(ISSTARRED, true)
            startActivity(intent)
        }

        binding.icBack.setOnClickListener {
            onBackPressed()
        }

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageUnstarred(event: MessageDeletedEvent) {
        updateConversationSnippet(event.threadId, event.lastMessage, event.lastMessageTime)
        viewModel.loadLastStarredMessages()
        viewModel.loadMessages()
    }

    fun updateConversationSnippet(threadId: Long, lastMessage: String?, lastMessageTime: Long?) {

        adapter.notifyDataSetChanged()
        val currentList = adapter.getAllMessages().toMutableList()
        val index = currentList.indexOfFirst { it.threadId == threadId }
        if (index != -1) {
            val updatedItem = currentList[index].copy(
                body = lastMessage ?: "",
                timestamp = lastMessageTime ?: System.currentTimeMillis()
            )
            currentList[index] = updatedItem
            adapter.submitList(currentList)

        }
    }

   /* @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadFilteredMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val starredMessages = starredMessageDao.getAllStarredMessages()
            val starredThreadIds = starredMessages.map { it.thread_id }.toSet()
            withContext(Dispatchers.Main) {
                viewModel.messages.observe(this@StarredMessagesActivity) { messageList ->

                    val filteredList = messageList.filter { it.threadId in starredThreadIds }

                    withContext(Dispatchers.Main) {
                        adapter.submitList(filteredList)

                        binding.emptyList.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerViewStarred.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }*/
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun filterAndShowMessages(messageList: List<MessageItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            val starredMessages = starredMessageDao.getAllStarredMessages()
            val starredThreadIds = starredMessages.map { it.thread_id }.toSet()
            val filteredList = messageList.filter { it.threadId in starredThreadIds }
            withContext(Dispatchers.Main) {
                adapter.submitList(filteredList)
                binding.emptyList.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerViewStarred.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }
}
