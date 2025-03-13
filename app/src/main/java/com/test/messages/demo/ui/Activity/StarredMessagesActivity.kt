package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.Database.Starred.StarredMessageDao
import com.test.messages.demo.databinding.ActivityStarredMessagesBinding
import com.test.messages.demo.ui.Adapter.StarredMessagesAdapter
import com.test.messages.demo.ui.reciever.MessageUnstarredEvent
import com.test.messages.demo.viewmodel.MessageViewModel
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

class StarredMessagesActivity : AppCompatActivity() {

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
        loadFilteredMessages()
        viewModel.lastStarredMessages.observe(this) { lastStarredMessages ->
            Log.d("DEBUG", "Observed Last Starred Messages: $lastStarredMessages")

            adapter.setLastStarredMessages(lastStarredMessages)
            binding.recyclerViewStarred.post {
                adapter.notifyDataSetChanged()

            }
        }

        viewModel.loadLastStarredMessages()
        adapter.onItemClickListener = { message ->
            val intent = Intent(this, ConversationActivity::class.java)
            intent.putExtra("EXTRA_THREAD_ID", message.threadId)
            intent.putExtra("NUMBER", message.number)
            intent.putExtra("isGroup", message.isGroupChat)
            startActivity(intent)
        }

        binding.icBack.setOnClickListener {
            onBackPressed()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadFilteredMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val starredMessages = starredMessageDao.getAllStarredMessages()
            val starredThreadIds = starredMessages.map { it.thread_id }.toSet()
            withContext(Dispatchers.Main) {
                viewModel.messages.observe(this@StarredMessagesActivity) { messageList ->

                    val filteredList = messageList.filter { it.threadId in starredThreadIds }
                    adapter.submitList(filteredList)

                    if (filteredList.isEmpty()) {
                        binding.emptyList.visibility = View.VISIBLE
                        binding.recyclerViewStarred.visibility = View.GONE
                    } else {
                        binding.emptyList.visibility = View.GONE
                        binding.recyclerViewStarred.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageUnstarred(event: MessageUnstarredEvent) {
        loadFilteredMessages()
        viewModel.loadLastStarredMessages()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

}
