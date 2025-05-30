package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.Util.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.Constants.ISGROUP
import com.test.messages.demo.Util.Constants.ISSTARRED
import com.test.messages.demo.Util.Constants.NAME
import com.test.messages.demo.Util.Constants.NUMBER
import com.test.messages.demo.Util.MessageDeletedEvent
import com.test.messages.demo.data.Database.Starred.StarredMessageDao
import com.test.messages.demo.databinding.ActivityStarredMessagesBinding
import com.test.messages.demo.ui.Adapter.StarredMessagesAdapter
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
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
        applyWindowInsetsToView(binding.rootView)

        EventBus.getDefault().register(this)

        adapter = StarredMessagesAdapter()
        binding.recyclerViewStarred.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewStarred.adapter = adapter

        val database = AppDatabase.getDatabase(this)
        starredMessageDao = database.starredMessageDao()

        viewModel.getAllStarredMessages().observe(this) { messages ->
            val latestStarredMessagesPerThread = messages
                .groupBy { it.thread_id }
                .mapNotNull { (_, starredMessages) ->
                    starredMessages.maxByOrNull { it.timestamp }
                }

            adapter.submitList(latestStarredMessagesPerThread)
            binding.emptyList.visibility = if (latestStarredMessagesPerThread.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewStarred.visibility = if (latestStarredMessagesPerThread.isEmpty()) View.GONE else View.VISIBLE

        }

        adapter.onItemClickListener = { message ->
            val intent = Intent(this, ConversationActivity::class.java)
            intent.putExtra(EXTRA_THREAD_ID, message.thread_id)
            intent.putExtra(NUMBER, message.number)
            intent.putExtra(NAME, message.sender)
            intent.putExtra(ISGROUP, message.is_group_chat)
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
