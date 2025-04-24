package com.test.messages.demo.ui.Activity

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.data.Database.Scheduled.ScheduledMessage
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityScheduleBinding
import com.test.messages.demo.ui.Adapter.ScheduledMessageAdapter
import com.test.messages.demo.ui.Dialogs.ScheduleDialog
import com.test.messages.demo.ui.send.MessageUtils
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.SmsSender
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase

@AndroidEntryPoint
class ScheduleActivity : BaseActivity() {
    private lateinit var binding: ActivityScheduleBinding
    private lateinit var adapter: ScheduledMessageAdapter
    private val viewModel: MessageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)

        adapter = ScheduledMessageAdapter { message ->
            showScheduleDialog(message)
        }
        binding.scheduleRecycleview.layoutManager = LinearLayoutManager(this)
        binding.scheduleRecycleview.adapter = adapter
        loadScheduledMessages()
        binding.addSchedule.setOnClickListener {
            val intent = Intent(this, AddScheduleActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadScheduledMessages() {
        AppDatabase.getDatabase(this).scheduledMessageDao().allScheduledMessages.observe(
            this,
            { scheduledMessages ->
                if (scheduledMessages.isEmpty()) {
                    binding.emptyList.visibility = View.VISIBLE
                    binding.emptyImage.visibility = View.VISIBLE
                    binding.scheduleRecycleview.visibility = View.GONE
                } else {
                    binding.emptyList.visibility = View.GONE
                    binding.emptyImage.visibility = View.GONE
                    binding.scheduleRecycleview.visibility = View.VISIBLE
                    adapter.submitList(scheduledMessages)
                }
            })

    }

    private fun showScheduleDialog(message: ScheduledMessage) {
        ScheduleDialog(
            context = this,
            onDeleteConfirmed = {
                Thread {
                    AppDatabase.getDatabase(this).scheduledMessageDao().delete(message)
                    runOnUiThread { loadScheduledMessages() }

                }.start()
            },
            onSendNow = {
                sendMessageImmediately(message)
            },
            onCopyText = {
                copyToClipboard(message.message)
            }
        ).show()
    }

    private fun sendMessageImmediately(message: ScheduledMessage) {
        val messagingUtils = MessageUtils(this)
        Log.d("TAG", "sendMessageImmediately: ")
        Thread {
            val message =
                AppDatabase.getDatabase(this).scheduledMessageDao().getMessageById(message.threadId)
//            val scheduledMsg = AppDatabase.getDatabase(this).scheduledMessageDao().getMessageById(message.threadId)

            message?.let {
                Log.d("TAG", "sendMessageImmediately:- ")
                val subId = SmsManager.getDefaultSmsSubscriptionId()
                val personalThreadId = it.threadId.toLongOrNull() ?: -1L

                val messageUri = messagingUtils.insertSmsMessage(
                    subId = subId,
                    dest = it.recipient,
                    text = it.message,
                    timestamp = System.currentTimeMillis(),
                    threadId = personalThreadId,
                )

                try {
                    SmsSender.getInstance(applicationContext as Application).sendMessage(
                        subId = subId,
                        destination = it.recipient,
                        body = it.message,
                        serviceCenter = null,
                        requireDeliveryReport = false,
                        messageUri = messageUri
                    )
                    val threadId = message.threadId
                    Log.d("TAG", "Deleting scheduled message for threadId: ${threadId}")

                    AppDatabase.getDatabase(this).scheduledMessageDao().deleteByThreadId(it.threadId.toLong())
                    runOnUiThread {
                        viewModel.loadMessages()
                        loadScheduledMessages() // refresh list
                    }

                } catch (e: Exception) {
                }
            }
        }.start()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Scheduled Message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.message_copied), Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

}