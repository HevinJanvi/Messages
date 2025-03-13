package com.test.messages.demo.ui.Activity

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
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
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.Database.Starred.StarredMessage
import com.test.messages.demo.R
import com.test.messages.demo.data.ConversationItem
import com.test.messages.demo.databinding.ActivityConversationBinding
import com.test.messages.demo.ui.Adapter.ConversationAdapter
import com.test.messages.demo.ui.Dialogs.LearnMoreDialog
import com.test.messages.demo.ui.Utils.MessageUtils
import com.test.messages.demo.ui.Utils.SmsSender
import com.test.messages.demo.ui.Utils.TimeUtils.formatHeaderDate
import com.test.messages.demo.ui.Utils.ViewUtils
import com.test.messages.demo.ui.Utils.ViewUtils.resetMessageCount
import com.test.messages.demo.ui.reciever.MessageUnstarredEvent
import com.test.messages.demo.ui.reciever.RefreshMessagesEvent
import com.test.messages.demo.ui.reciever.SmsReceiver
import com.test.messages.demo.ui.reciever.UpdateGroupNameEvent
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
import java.util.Calendar

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)
class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var adapter: ConversationAdapter
    private var threadId: Long = -1
    private lateinit var number: String
    private lateinit var profileUrl: String
    private lateinit var groupName: String
    private var subscriptionId: Int = -1
    private lateinit var messagingUtils: MessageUtils
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var oldBottom = 0
    var isfromBlock: Boolean = false
    private val starredMessageIds = mutableSetOf<Long>()

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
        Handler(Looper.getMainLooper()).post { viewModel.loadMessages() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        messagingUtils = MessageUtils(this)
        EventBus.getDefault().register(this)

        threadId = intent.getLongExtra("EXTRA_THREAD_ID", -1)
        number = intent.getStringExtra("NUMBER").toString()
        profileUrl = intent.getStringExtra("ProfileUrl").toString()
        isfromBlock = intent.getBooleanExtra("fromBlock", false)


        Log.d("TAG", "onCreate: threadId :- " + threadId)
        setupRecyclerView()
        if (threadId != -1L) {

            resetMessageCount(this, threadId)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.cancel(threadId.toInt())

            viewModel.loadConversation(threadId)

        } else {
            threadId = getThreadId(setOf(number))
        }
        markThreadAsRead(threadId)

        viewModel.loadConversation(threadId)
        observeViewModel()
        loadStarredMessages()

        val cleanedNumber = number.replace("[^+\\d]".toRegex(), "")
        if (cleanedNumber.contains(",") || !cleanedNumber.matches(Regex("^[+]?[0-9]{7,15}$"))) {
            binding.btnCall.visibility = View.GONE
        } else {
            binding.btnCall.visibility = View.VISIBLE
        }

        if (isfromBlock) {
            binding.blockLy.visibility = View.VISIBLE
            binding.btnUnblock.setOnClickListener {
                unblockThread(threadId)
            }
        } else {
            binding.blockLy.visibility = View.GONE
        }
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        binding.btnCall.setOnClickListener {
            makeCall(number)
        }
        binding.btnInfo.setOnClickListener {
            val numbersList = number.split(", ").map { it.trim() }.filter { it.isNotEmpty() }

            Log.d("TAG", "setupClickListeners:numbersList "+numbersList)
            if (numbersList.size > 1) {
                // It's a group chat
                val intent = Intent(this, GroupProfileActivity::class.java)
                intent.putExtra("EXTRA_THREAD_ID", threadId)
                intent.putExtra("GROUP_NAME", groupName)
                intent.putStringArrayListExtra("GROUP_MEMBERS", ArrayList(numbersList))
                startActivity(intent)
            } else {
                // It's a one-on-one chat
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("EXTRA_THREAD_ID", threadId)
                intent.putExtra("NUMBER", number)
                intent.putExtra("PROFILE_URL", profileUrl)
                startActivity(intent)
            }

            if (isfromBlock) {
                finish()
            }
        }
        binding.buttonSend.setOnClickListener {
            sendMessage()
        }
        binding.icClose.setOnClickListener { adapter.clearSelection() }
        binding.btnDelete.setOnClickListener { deleteSelectedMessages() }
        binding.btnCopy.setOnClickListener { copySelectedMessages() }
        binding.btnStar.setOnClickListener { starSelectedMessages() }
        binding.learnMore.setOnClickListener {
            val dialog = LearnMoreDialog(this)
            dialog.show()
        }
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter { selectedCount ->
            updateUI(selectedCount)
        }
        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        binding.recyclerViewConversation.layoutManager = linearLayoutManager
        binding.recyclerViewConversation.adapter = adapter
//        scrolltoBottom()
        setKeyboardVisibilityListener()
        binding.learnMoreLy.visibility = View.GONE
        binding.secureLy.visibility = View.GONE
    }


    override fun onDestroy() {
        super.onDestroy()
        viewModel.emptyConversation()
        EventBus.getDefault().unregister(this)
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

    /*    private fun observeViewModel() {
            viewModel.conversation.observe(this) { conversationList ->

                Log.d("ConversationActivity", "Received ${conversationList.size} messages")

                val sortedList = conversationList.sortedBy { it.date }.toList()
                Log.d("ConversationActivity", "Sorted messages from oldest to newest")

                adapter.submitList(sortedList) {
                    scrolltoBottom()
                }

                if (sortedList.isNotEmpty()) {
                    binding.emptyText.visibility = View.GONE
                    val senderNumber = sortedList.firstOrNull()?.address ?: number
    //                binding.address.text = viewModel.getContactNameOrNumber(senderNumber)

                    updateLyouts(ViewUtils.isOfferSender(senderNumber))

                    markThreadAsRead(threadId)
                } else {
                    binding.emptyText.visibility = View.VISIBLE
                }
            }
        }*/

    private fun observeViewModel() {
        viewModel.conversation.observe(this) { conversationList ->
            val sortedList = conversationList.sortedBy { it.date }
            val groupedList = mutableListOf<ConversationItem>()
            val addedHeaders = mutableSetOf<Long>()
            for (message in sortedList) {
                val headerTimestamp = getStartOfDayTimestamp(message.date)
                if (!addedHeaders.contains(headerTimestamp)) {
                    val formattedDate = formatHeaderDate(this@ConversationActivity, message.date)
                    groupedList.add(ConversationItem.createHeader(formattedDate, headerTimestamp))
                    addedHeaders.add(headerTimestamp)
                }
                groupedList.add(message)
            }

            adapter.submitList(groupedList) {
                scrolltoBottom()
            }

            if (groupedList.isNotEmpty()) {
                binding.emptyText.visibility = View.GONE
                val senderNumber = sortedList.firstOrNull()?.address ?: number
                CoroutineScope(Dispatchers.IO).launch {
                    val sharedPreferences = getSharedPreferences("GroupPrefs", Context.MODE_PRIVATE)
                    val savedGroupName = sharedPreferences.getString("group_name_$threadId", null)
                    val contactName = savedGroupName ?: viewModel.getContactNameOrNumber(senderNumber)


//                    val contactName = viewModel.getContactNameOrNumber(senderNumber)
                    withContext(Dispatchers.Main) {
                        binding.address.text = contactName
                        groupName = contactName
                    }
                }

                updateLyouts(ViewUtils.isOfferSender(senderNumber))
                markThreadAsRead(threadId)
            } else {
                binding.emptyText.visibility = View.VISIBLE
                val senderNumber = sortedList.firstOrNull()?.address ?: number
                CoroutineScope(Dispatchers.IO).launch {
                    val contactName = viewModel.getContactNameOrNumber(senderNumber)
                    withContext(Dispatchers.Main) {
                        binding.address.text = contactName
                    }
                }
                updateLyouts(ViewUtils.isOfferSender(senderNumber))

            }
        }
    }


    private fun getStartOfDayTimestamp(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun updateLyouts(isServiceNumber: Boolean) {
        if (isServiceNumber) {
            binding.msgSendLayout.visibility = View.GONE
            binding.learnMoreLy.visibility = View.VISIBLE
            binding.secureLy.visibility = View.VISIBLE
        } else {
            binding.msgSendLayout.visibility = View.VISIBLE
            binding.learnMoreLy.visibility = View.GONE
            binding.secureLy.visibility = View.GONE
        }
    }

    private fun sendMessage() {
        val text = binding.editTextMessage.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.failed_to_send_message), Toast.LENGTH_LONG)
                .show()
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

    private fun unblockThread(threadId: Long) {
        if (threadId == -1L) return
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.unblockConversations(listOf(threadId))

            withContext(Dispatchers.Main) {
                binding.blockLy.visibility = View.GONE
                isfromBlock = false
                Toast.makeText(
                    this@ConversationActivity,
                    getString(R.string.contact_blocked), Toast.LENGTH_SHORT
                )
                    .show()
                finish()
            }
        }

    }

    private fun makeCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        val chooser = Intent.createChooser(intent, "Choose an app to make a call")
        startActivity(chooser)
    }

    fun deleteSelectedMessages() {
        val contentResolver = contentResolver
        val selectedMessageItems = adapter.getSelectedItems().toList()

        for (messageItem in selectedMessageItems) {
            val uri = Uri.parse("content://sms/${messageItem.id}")
            contentResolver.delete(uri, null, null)
            val position = adapter.getPositionOfMessage(messageItem)
            if (position != RecyclerView.NO_POSITION) {
                adapter.removeMessageWithAnimation(position)
            }
        }

        adapter.clearSelection()
    }

    private fun copySelectedMessages() {
        if (adapter.selectedItems.isNotEmpty()) {
            val copiedText = adapter.selectedItems.joinToString("\n") { it.body }

            if (copiedText.isNotBlank()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Messages", copiedText)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(this, getString(R.string.messages_copied), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.no_messages_selected), Toast.LENGTH_SHORT)
                    .show()
            }

            adapter.clearSelection()
        }
    }

    private fun starSelectedMessages() {
        if (adapter.selectedItems.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val toStar = mutableSetOf<Long>()
                val toUnstar = mutableSetOf<Long>()

                for (message in adapter.selectedItems) {
                    if (starredMessageIds.contains(message.id)) {
                        // If message is already starred, unstar it
                        AppDatabase.getDatabase(this@ConversationActivity)
                            .starredMessageDao()
                            .deleteStarredMessageById(message.id)

                        toUnstar.add(message.id)
                        withContext(Dispatchers.Main) {
                            EventBus.getDefault().post(MessageUnstarredEvent(threadId))
                        }

                    } else {
                        val starredMessage = StarredMessage(
                            message_id = message.id,
                            thread_id = message.threadId,
                            message = message.body
                        )
                        AppDatabase.getDatabase(this@ConversationActivity)
                            .starredMessageDao()
                            .insertStarredMessage(starredMessage)

                        toStar.add(message.id)
                    }
                }

                withContext(Dispatchers.Main) {
                    starredMessageIds.removeAll(toUnstar)
                    starredMessageIds.addAll(toStar)
                    adapter.setStarredMessages(starredMessageIds)
                    adapter.clearSelection()
                    updateStarIcon()
                }
            }
        }
    }

    private fun updateStarIcon() {
        val allSelectedAreStarred = adapter.selectedItems.all { starredMessageIds.contains(it.id) }

        if (allSelectedAreStarred) {
            binding.btnStar.setImageResource(R.drawable.ic_unstar)
        } else {
            binding.btnStar.setImageResource(R.drawable.ic_star)
        }
    }

    private fun loadStarredMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val ids = AppDatabase.getDatabase(this@ConversationActivity).starredMessageDao()
                .getStarredMessageIds()
            withContext(Dispatchers.Main) {
                starredMessageIds.clear()
                starredMessageIds.addAll(ids)
                adapter.setStarredMessages(starredMessageIds)
            }
        }
    }

    private fun updateUI(selectedCount: Int) {
        if (selectedCount > 0) {
            binding.actionSelectItem.visibility = View.VISIBLE
            binding.actionbar.visibility = View.GONE
            binding.countText.text = "$selectedCount" + " " + getString(R.string.selected)

            if (selectedCount == 1) {
                binding.btnStar.visibility = View.VISIBLE
                val selectedMessage = adapter.selectedItems.first()
                val isStarred = starredMessageIds.contains(selectedMessage.id)
                binding.btnStar.setImageResource(if (isStarred) R.drawable.ic_unstar else R.drawable.ic_star)
            } else {
                binding.btnStar.visibility = View.GONE
            }
        } else {
            binding.actionSelectItem.visibility = View.GONE
            binding.actionbar.visibility = View.VISIBLE
        }
    }

    fun scrolltoBottom() {
        binding.recyclerViewConversation.postDelayed({
            binding.recyclerViewConversation.scrollToPosition(adapter.itemCount - 1)
        }, 100)
    }

    private fun isGroupChat(): Boolean {
        val cleanedNumber = number.replace(" ", "")
        return cleanedNumber.contains(",")
    }

    override fun onBackPressed() {
        if (adapter.isMultiSelectionEnabled) {
            adapter.clearSelection()
            updateUI(0)
        } else {
            super.onBackPressed()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshMessagesEvent(event: RefreshMessagesEvent) {
        viewModel.loadMessages()
        viewModel.loadConversation(threadId)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGroupNameUpdated(event: UpdateGroupNameEvent) {
        Log.d("TAG", "onGroupNameUpdated:evnt ")
        if (event.threadId == threadId) {
            binding.address.text = event.newName
            groupName = event.newName
        }
        viewModel.loadMessages()
        viewModel.loadConversation(threadId)
    }

    override fun onStart() {
        super.onStart()
//        EventBus.getDefault().register(this)
    }


    override fun onStop() {
        super.onStop()
//        EventBus.getDefault().unregister(this)
    }



}
