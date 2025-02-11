package com.test.messages.demo.ui.Activity

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
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
import com.klinker.android.send_message.Utils
import com.test.messages.demo.databinding.ActivityConversationBinding
import com.test.messages.demo.ui.Adapter.ConversationAdapter
import com.test.messages.demo.ui.Utils.MessagingUtils
import com.test.messages.demo.ui.Utils.SmsSender
import com.test.messages.demo.ui.Utils.smsSender
import com.test.messages.demo.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var adapter: ConversationAdapter
    private var threadId: Long = -1
    private lateinit var number: String
    private var subscriptionId: Int = -1
    private lateinit var messagingUtils: MessagingUtils
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var oldBottom = 0

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        messagingUtils = MessagingUtils(this)

        threadId = intent.getLongExtra("EXTRA_THREAD_ID", -1)
        number = intent.getStringExtra("NUMBER").toString()

        Log.d("TAG", "onCreate: threadId :- " + threadId)
        setupRecyclerView()
        if (threadId != -1L) {
            viewModel.loadConversation(threadId)
        } else {
            threadId = getThreadId(setOf(number))
        }
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
        scrollToBottom()
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
        var text = binding.editTextMessage.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "failed to send message", Toast.LENGTH_LONG).show()
            return
        }
        subscriptionId = SmsManager.getDefaultSmsSubscriptionId()
        threadId = getThreadId(setOf(number))
        if (threadId == -1L || threadId == 0L) {
            threadId = getThreadId(setOf(number))
        }
        Log.d("TAG", "Sending message to $number in thread $threadId")
        binding.editTextMessage.text.clear()
        sendSmsMessage(text, setOf(number), subscriptionId, false, null)
        binding.recyclerViewConversation.itemAnimator = null
        scrolltoBottom()
        viewModel.loadConversation(threadId)
        viewModel.loadMessages()
    }

    fun scrolltoBottom() {
        binding.recyclerViewConversation.postDelayed({
            binding.recyclerViewConversation.scrollToPosition(adapter.itemCount - 1)
        }, 100)
    }

    private fun observeViewModel() {
        viewModel.conversation.observe(this) { conversationList ->
            adapter.submitList(conversationList)
            binding.recyclerViewConversation.scrollToPosition(conversationList.size - 1)
            scrolltoBottom()
            val senderNumber = conversationList.first().address ?: number
            binding.address.text = senderNumber

            if (conversationList.isNotEmpty()) {
                val senderNumber = conversationList.first().address ?: number
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
        if (addresses.size > 1) {
            // insert a dummy message for this thread if it is a group message
            val broadCastThreadId = getThreadId(addresses.toSet())
            val mergedAddresses = addresses.joinToString("|")
            messagingUtils.insertSmsMessage(
                subId = subId,
                dest = mergedAddresses,
                text = text,
                timestamp = System.currentTimeMillis(),
                threadId = broadCastThreadId,
                status = Telephony.Sms.Sent.STATUS_COMPLETE,
                type = Telephony.Sms.Sent.MESSAGE_TYPE_SENT,
                messageId = messageId
            )
        }


        for (address in addresses) {
            val messageUri = messagingUtils.insertSmsMessage(
                subId = subId, dest = address, text = text,
                timestamp = System.currentTimeMillis(), threadId = threadId,
                messageId = messageId
            )
            try {
                SmsSender.getInstance(applicationContext as Application).sendMessage(
                    subId = subId, destination = address, body = text, serviceCenter = null,
                    requireDeliveryReport = requireDeliveryReport, messageUri = messageUri
                )
            } catch (e: Exception) {
                throw e
            }
        }
    }

}
