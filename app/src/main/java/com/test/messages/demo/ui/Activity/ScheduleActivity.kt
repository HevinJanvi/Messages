package com.test.messages.demo.ui.Activity

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.data.Database.Scheduled.ScheduledMessage
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityScheduleBinding
import com.test.messages.demo.ui.Adapter.ScheduledMessageAdapter
import com.test.messages.demo.ui.Dialogs.ScheduleDialog
import com.test.messages.demo.ui.send.MessageUtils
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.ui.send.SmsSender
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.ui.send.hasReadContactsPermission
import com.test.messages.demo.ui.send.hasReadSmsPermission
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ScheduleActivity : BaseActivity() {
    private lateinit var binding: ActivityScheduleBinding
    private lateinit var adapter: ScheduledMessageAdapter
    private val viewModel: MessageViewModel by viewModels()
    private var scheduleDialog: ScheduleDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(binding.rootView)
        adapter = ScheduledMessageAdapter { message ->
               Handler(Looper.getMainLooper()).postDelayed({
                   showScheduleDialog(message)
               },200)
        }
        binding.scheduleRecycleview.layoutManager = LinearLayoutManager(this)
        binding.scheduleRecycleview.adapter = adapter
        loadScheduledMessages()
        binding.addSchedule.setOnClickListener {
            val intent = Intent(this, AddScheduleActivity::class.java)
            startActivity(intent)
        }
        binding.icBack.setOnClickListener {
            finish()
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
        scheduleDialog = ScheduleDialog(

            context = this,
            onDeleteConfirmed = {
                Thread {
                    AppDatabase.getDatabase(this).scheduledMessageDao().delete(message)
                    runOnUiThread {
                        loadScheduledMessages() }

                }.start()
            },
            onSendNow = {
                sendMessageImmediately(message)
            },
            onCopyText = {
                copyToClipboard(message.message)
            }
        )
        scheduleDialog?.show()

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun sendMessageImmediately(message: ScheduledMessage) {
        val messagingUtils = MessageUtils(this)

        lifecycleScope.launch {
            val scheduledMessage = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@ScheduleActivity).scheduledMessageDao().getMessageById1(message.id)
            }

            scheduledMessage?.let {
                val personalThreadId = it.threadId.toLongOrNull() ?: -1L
                val messageUri = messagingUtils.insertSmsMessage(
                    subId = it.subscriptionId,
                    dest = it.recipientNumber,
                    text = it.message,
                    timestamp = System.currentTimeMillis(),
                    threadId = personalThreadId,
                    status = Telephony.Sms.Sent.STATUS_COMPLETE,
                    type = Telephony.Sms.Sent.MESSAGE_TYPE_SENT,
                    messageId = null
                )

                try {
                    withContext(Dispatchers.IO) {
                        SmsSender.getInstance(applicationContext as Application).sendMessage(
                            subId = it.subscriptionId,
                            destination = it.recipientNumber,
                            body = it.message,
                            serviceCenter = null,
                            requireDeliveryReport = false,
                            messageUri = messageUri
                        )
                        AppDatabase.getDatabase(this@ScheduleActivity).scheduledMessageDao()
                            .deleteById(it.id)
                    }

                    viewModel.loadMessages()
                    loadScheduledMessages()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Scheduled Message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.message_copied), Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this) && !hasReadSmsPermission() && !hasReadContactsPermission()) {
            return
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
    }

}