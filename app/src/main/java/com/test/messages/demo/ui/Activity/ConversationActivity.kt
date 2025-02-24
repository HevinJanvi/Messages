package com.test.messages.demo.ui.Activity

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.databinding.ActivityConversationBinding
import com.test.messages.demo.ui.Adapter.ConversationAdapter
import com.test.messages.demo.ui.Utils.MessageUtils
import com.test.messages.demo.ui.Utils.SmsSender
import com.test.messages.demo.ui.reciever.NewSmsEvent
import com.test.messages.demo.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus

@AndroidEntryPoint
class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var adapter: ConversationAdapter
    private var threadId: Long = -1
    private lateinit var number: String
    private var subscriptionId: Int = -1
    private lateinit var messagingUtils: MessageUtils
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var oldBottom = 0

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun markThreadAsRead(threadId: Long) {
        if (threadId == -1L) return
        val contentValues = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
        }
        val uri = Telephony.Sms.CONTENT_URI
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        val updatedRows = contentResolver.update(uri, contentValues, selection, selectionArgs)
        Log.d("ConversationActivity", "Marked $updatedRows messages as read in thread $threadId")
//        viewModel.loadMessages()
        Handler(Looper.getMainLooper()).post { viewModel.loadMessages() }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        messagingUtils = MessageUtils(this)

        threadId = intent.getLongExtra("EXTRA_THREAD_ID", -1)
        number = intent.getStringExtra("NUMBER").toString()
        Log.d("TAG", "onCreate: threadId :- " + threadId)
        setupRecyclerView()
        if (threadId != -1L) {
            viewModel.loadConversation(threadId)
        } else {
            threadId = getThreadId(setOf(number))
        }
        markThreadAsRead(threadId)

        viewModel.loadConversation(threadId)
        observeViewModel()
        binding.buttonSend.setOnClickListener {
            sendMessage()
        }

    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter()
        linearLayoutManager = LinearLayoutManager(this)
        binding.recyclerViewConversation.layoutManager = linearLayoutManager
        binding.recyclerViewConversation.adapter = adapter
        scrolltoBottom()
        setKeyboardVisibilityListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.emptyConversation()

    }

    private fun setKeyboardVisibilityListener() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val rect = Rect()
                val rootView = findViewById<View>(android.R.id.content)
                rootView.getWindowVisibleDisplayFrame(rect)

                val bottom = rect.bottom
                val heightDifference = oldBottom - bottom
                oldBottom = bottom
                val lastVisibleItem = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                val totalItems = linearLayoutManager.itemCount
                if (heightDifference > 0) {
                    binding.recyclerViewConversation.scrollBy(0, heightDifference)
                } else if (heightDifference < 0 && lastVisibleItem != totalItems - 1) {
                    scrollToBottom()
                }
                return true
            }
        })
    }

    private fun scrollToBottom() {
        val layoutManager = binding.recyclerViewConversation.layoutManager as LinearLayoutManager
        layoutManager.scrollToPositionWithOffset(adapter.itemCount - 1, 0)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun sendMessage() {
        val text = binding.editTextMessage.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_LONG).show()
            return
        }
        subscriptionId = SmsManager.getDefaultSmsSubscriptionId()
        val numbersSet = number.split(", ").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        threadId = if (threadId == -1L || threadId == 0L) {
            viewModel.findGroupThreadId(numbersSet)
                ?: getThreadId(numbersSet)
        } else {
            threadId
        }
        Log.d("TAG", "Sending message to $number in thread $threadId")
        if (numbersSet.size > 1) {
            messagingUtils.insertSmsMessage(
                subId = subscriptionId,
                dest = numbersSet.joinToString("|"), // Group identifier
                text = text,
                timestamp = System.currentTimeMillis(),
                threadId = threadId
            )
        }

        sendSmsMessage(
            text = text,
            addresses = numbersSet,
            subId = subscriptionId,
            requireDeliveryReport = false
        )

        binding.editTextMessage.text.clear()
        binding.recyclerViewConversation.itemAnimator = null
        scrolltoBottom()
        viewModel.loadMessages()
        viewModel.loadConversation(threadId)
    }

    private fun observeViewModel() {
        viewModel.conversation.observe(this) { conversationList ->
            adapter.submitList(conversationList)
            binding.recyclerViewConversation.scrollToPosition(conversationList.size - 1)
            scrolltoBottom()

            if (conversationList.isNotEmpty()) {
                val senderNumber = conversationList.firstOrNull()?.address ?: number
                binding.address.text = viewModel.getContactNameOrNumber(senderNumber)
            }
        }

    }

    @SuppressLint("NewApi")
    fun Context.getThreadId(addresses: Set<String>): Long {
        return try {
            Telephony.Threads.getOrCreateThreadId(this, addresses)
        } catch (e: Exception) {
            0L
        }
    }

    fun sendSmsMessage(
        text: String,
        addresses: Set<String>,
        subId: Int,
        requireDeliveryReport: Boolean,
        messageId: Long? = null
    ) {
        if (addresses.isEmpty()) return

        for (address in addresses) {
            val personalThreadId = getThreadId(setOf(address))
            val messageUri = messagingUtils.insertSmsMessage(
                subId = subId,
                dest = address,
                text = text,
                timestamp = System.currentTimeMillis(),
                threadId = personalThreadId,
                messageId = messageId
            )

            try {
                SmsSender.getInstance(applicationContext as Application).sendMessage(
                    subId = subId,
                    destination = address,
                    body = text,
                    serviceCenter = null,
                    requireDeliveryReport = requireDeliveryReport,
                    messageUri = messageUri
                )
            } catch (e: Exception) {
                Log.d("TAG", "Failed to send message to $address", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
//        val threadId = intent.getLongExtra("EXTRA_THREAD_ID", -1)
//        if (threadId != -1L) {
//            viewModel.markMessageAsRead(threadId) // 🔥 Only modify affected items
//        }
    }

    fun scrolltoBottom() {
        binding.recyclerViewConversation.postDelayed({
            binding.recyclerViewConversation.scrollToPosition(adapter.itemCount - 1)
        }, 100)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}
