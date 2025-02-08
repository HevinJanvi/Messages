package com.test.messages.demo.ui.Activity

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.databinding.ActivityConversationBinding
import com.test.messages.demo.ui.Adapter.ConversationAdapter
import com.test.messages.demo.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var adapter: ConversationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the threadId passed from the previous activity
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        setupRecyclerView()
        Log.e("TAG", "onCreate: "+threadId )
        if (threadId != -1L) {
            // Load the conversation for the given threadId
            viewModel.loadConversation(threadId)
        }
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.conversation.observe(this) { conversationList ->
            Log.d("TAG", "Updating conversation list: $conversationList")

            adapter.submitList(conversationList)
        }
    }

    companion object {
        const val EXTRA_THREAD_ID = "EXTRA_THREAD_ID"
    }
}
