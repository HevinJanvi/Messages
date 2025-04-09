package com.test.messages.demo.ui.Activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsManager
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.test.messages.demo.data.Database.Scheduled.ScheduledMessage
import com.test.messages.demo.data.Database.Starred.StarredMessage
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.FORWARD
import com.test.messages.demo.Util.CommanConstants.FORWARDMSGS
import com.test.messages.demo.Util.CommanConstants.FROMBLOCK
import com.test.messages.demo.Util.CommanConstants.GROUP_MEMBERS
import com.test.messages.demo.Util.CommanConstants.GROUP_NAME
import com.test.messages.demo.Util.CommanConstants.GROUP_NAME_KEY
import com.test.messages.demo.Util.CommanConstants.ISSCHEDULED
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.CommanConstants.PROFILEURL
import com.test.messages.demo.Util.CommanConstants.QUERY
import com.test.messages.demo.Util.CommanConstants.SHARECONTACT
import com.test.messages.demo.Util.CommanConstants.SHARECONTACTNAME
import com.test.messages.demo.Util.CommanConstants.SHARECONTACTNUMBER
import com.test.messages.demo.Util.CommanConstants.SOURCE
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.databinding.ActivityConversationBinding
import com.test.messages.demo.ui.Adapter.ConversationAdapter
import com.test.messages.demo.ui.Dialogs.LearnMoreDialog
import com.test.messages.demo.ui.send.MessageUtils
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.SmsSender
import com.test.messages.demo.Util.TimeUtils.formatHeaderDate
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.Util.ViewUtils.resetMessageCount
import com.test.messages.demo.data.reciever.MessageScheduler
import com.test.messages.demo.Util.MessageUnstarredEvent
import com.test.messages.demo.Util.RefreshMessagesEvent
import com.test.messages.demo.Util.UpdateGroupNameEvent
import com.test.messages.demo.Util.ViewUtils.blinkThen
import com.test.messages.demo.data.viewmodel.DraftViewModel
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.ui.Dialogs.MessageDetailsDialog
import com.test.messages.demo.ui.Fragment.ConversationFragment
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)
class ConversationActivity : BaseActivity() {

    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageViewModel by viewModels()
    private val draftViewModel: DraftViewModel by viewModels()
    private lateinit var adapter: ConversationAdapter
    private var threadId: Long = -1
    private var number: String = ""
    private var name: String = number
    private lateinit var profileUrl: String
    private lateinit var groupName: String
    private var subscriptionId: Int = -1
    private lateinit var messagingUtils: MessageUtils
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var oldBottom = 0
    var isfromBlock: Boolean = false
    var isScheduled: Boolean = false
    private val starredMessageIds = mutableSetOf<Long>()
    private var highlightQuery: String? = null
    private var selectedTimeInMillis: Long = 0L
    private lateinit var profileLauncher: ActivityResultLauncher<Intent>

    private fun markThreadAsRead(threadId: Long) {
        if (threadId == -1L) return
        val contentValues = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        val uri = Telephony.Sms.CONTENT_URI
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        val updatedRows = contentResolver.update(uri, contentValues, selection, selectionArgs)
        Log.d(
            "ConversationActivity",
            "Marked $updatedRows archive messages as read in thread $threadId"
        )
        Handler(Looper.getMainLooper()).post { viewModel.loadMessages() }
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        messagingUtils = MessageUtils(this)
        EventBus.getDefault().register(this)

        threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        number = intent.getStringExtra(NUMBER).toString()
        name = intent.getStringExtra(NAME).toString()
        profileUrl = intent.getStringExtra(PROFILEURL).toString()
        isfromBlock = intent.getBooleanExtra(FROMBLOCK, false)
        highlightQuery = intent.getStringExtra(QUERY)
        isScheduled = intent.getBooleanExtra(ISSCHEDULED, false)

        val forwardedMessage = intent.getStringExtra("forwardedMessage")
        if (!forwardedMessage.isNullOrEmpty()) {
            binding.editTextMessage.setText(forwardedMessage)
            binding.editTextMessage.setSelection(forwardedMessage.length)
        }


        if (isScheduled) {
            showDateTimePickerDialog()
            binding.close.setOnClickListener {
                binding.schedulLy.visibility = View.GONE
                isScheduled = false
            }
        } else {
            binding.schedulLy.visibility = View.GONE
        }
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

        draftViewModel.getDraft(threadId).observe(this) { draftText ->
            binding.editTextMessage.setText(draftText)
        }

        binding.editTextMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if(!isScheduled){
                    draftViewModel.saveDraft(threadId, s.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val trimmedText = s?.toString()?.trim()
                if (trimmedText.isNullOrEmpty()) {
                    binding.buttonSend.isEnabled = false
                    binding.buttonSend.setImageResource(R.drawable.ic_send_disable)
                } else {
                    binding.buttonSend.isEnabled = true
                    binding.buttonSend.setImageResource(R.drawable.ic_send_enable)
                }
            }
        })

        setupClickListeners()

        profileLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val updatedName = result.data?.getStringExtra("UPDATED_NAME")
                        ?: return@registerForActivityResult
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.address.text = updatedName
                    }, 100)
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupClickListeners() {
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        binding.btnCall.setOnClickListener {
            makeCall(number)
        }
        binding.btnInfo.setOnClickListener {

            Log.d("INFO_CLICK", "Raw Number String: $number")
            val numbersList = number.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            if (numbersList.size > 1) {
                // It's a group chat
                val intent = Intent(this, GroupProfileActivity::class.java)
                intent.putExtra(EXTRA_THREAD_ID, threadId)
                intent.putExtra(GROUP_NAME, groupName)
                intent.putStringArrayListExtra(GROUP_MEMBERS, ArrayList(numbersList))
                startActivity(intent)
            } else {
                // It's a one-on-one chat
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra(EXTRA_THREAD_ID, threadId)
                intent.putExtra(NUMBER, number)
                intent.putExtra(NAME, name)
                intent.putExtra(PROFILEURL, profileUrl)
                profileLauncher.launch(intent)

            }

            if (isfromBlock) {
                finish()
            }
        }
        binding.buttonSend.setOnClickListener {
            if (!isDefaultSmsApp()) {
                requestDefaultSmsApp()
            } else {
                proceedWithMessageSending()
            }
        }


        binding.buttonAdd.setOnClickListener {
            if (binding.addLyouts.visibility == View.VISIBLE) {
                binding.addLyouts.visibility = View.GONE
                binding.buttonAdd.setImageResource(R.drawable.ic_add_disable)
            } else {
                binding.addLyouts.visibility = View.VISIBLE
                binding.buttonAdd.setImageResource(R.drawable.ic_add)
            }
        }

        binding.lySchedule.setOnClickListener {
            isScheduled=true
            showDateTimePickerDialog()
            binding.close.setOnClickListener {
                binding.schedulLy.visibility = View.GONE
                isScheduled = false
            }
        }

        binding.lyShareContact.setOnClickListener {
            val intent = Intent(this, ContactActivtiy::class.java)
            intent.putExtra(SOURCE, SHARECONTACT)
            startActivityForResult(intent, 401)
            binding.addLyouts.visibility = View.GONE
            binding.buttonAdd.setImageResource(R.drawable.ic_add_disable)
        }

        binding.icClose.setOnClickListener { adapter.clearSelection() }
        binding.btnDelete.setOnClickListener { deleteSelectedMessages() }
        binding.btnCopy.setOnClickListener { copySelectedMessages() }
        binding.btnStar.setOnClickListener { starSelectedMessages() }
        binding.learnMore.setOnClickListener {
            val dialog = LearnMoreDialog(this)
            dialog.show()
        }
        binding.btnMore.setOnClickListener {
            showPopupMore(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 401 && resultCode == Activity.RESULT_OK) {
            val selectedNumber = data?.getStringExtra(SHARECONTACTNUMBER) ?: return
            val contactName = data.getStringExtra(SHARECONTACTNAME) ?: getString(R.string.unknown)

            val contactText = "$contactName\n$selectedNumber"
            binding.editTextMessage.setText(contactText)
            binding.editTextMessage.setSelection(contactText.length)
        }
    }



    fun showPopupMore(view: View) {
        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialog = layoutInflater.inflate(R.layout.popup_more, null)

        val popupWindow = PopupWindow(
            dialog,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val marginDp = 16
        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginDp.toFloat(),
            view.resources.displayMetrics
        ).toInt()


        dialog.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = dialog.measuredWidth
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1] + view.height
        val isRTL = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val x = if (isRTL) {
            anchorX + marginPx
        } else {
            anchorX + view.width - popupWidth - marginPx
        }

        popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, x, anchorY)

        val txtShare: TextView = dialog.findViewById(R.id.txtShare)
        txtShare.setOnClickListener {
            shareMessages()
            popupWindow.dismiss()
        }

        val txtForward: TextView = dialog.findViewById(R.id.txtForward)
        txtForward.setOnClickListener {
            it.blinkThen {

                val selectedBodies = adapter.selectedItems.map { it.body }
                val joinedMessage = selectedBodies.joinToString("\n\n")

                val intent = Intent(this, ContactActivtiy::class.java)
                intent.putExtra(SOURCE, FORWARD)
                intent.putExtra(FORWARDMSGS, joinedMessage)
                startActivity(intent)
                finish()
                popupWindow.dismiss()
            }
        }

        val txtDetails: TextView = dialog.findViewById(R.id.txtDetails)

        val selectedMessages = adapter.selectedItems

        if (selectedMessages.size == 1) {
            txtDetails.visibility = View.VISIBLE
            txtDetails.setOnClickListener {
                it.blinkThen {
                    val message = selectedMessages.first()
                    val dialog = MessageDetailsDialog(this, message)
                    dialog.show()
                    popupWindow.dismiss()
                }
            }
        } else {
            txtDetails.visibility = View.GONE
        }

        /*txtDetails.setOnClickListener {
            it.blinkThen {
                val dialog = MessageDetailsDialog(this)
                dialog.show()
                popupWindow.dismiss()
            }
        }*/
    }


    private fun setupRecyclerView() {
        adapter = ConversationAdapter(this) { selectedCount ->
            updateUI(selectedCount)
            adapter.setSearchQuery(highlightQuery)
        }
        adapter.setOnRetryListener(object : ConversationAdapter.OnMessageRetryListener {
            override fun onRetry(message: ConversationItem) {
                resendMessage(message)
            }
        })

        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        binding.recyclerViewConversation.layoutManager = linearLayoutManager
        binding.recyclerViewConversation.adapter = adapter
        setKeyboardVisibilityListener()
        binding.learnMoreLy.visibility = View.GONE
        binding.secureLy.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.emptyConversation()
        EventBus.getDefault().unregister(this)
    }

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
                if (!highlightQuery.isNullOrEmpty()) {
                    adapter.setSearchQuery(highlightQuery)
                }
                scrolltoBottom()
            }

            if (groupedList.isNotEmpty()) {
                binding.emptyText.visibility = View.GONE
                CoroutineScope(Dispatchers.IO).launch {
                    val sharedPreferences =
                        getSharedPreferences(CommanConstants.PREFS_NAME, Context.MODE_PRIVATE)
                    val savedGroupName =
                        sharedPreferences.getString("${GROUP_NAME_KEY}$threadId", null)

                    val contactName =
                        savedGroupName ?: name
                    withContext(Dispatchers.Main) {
                        binding.address.text = contactName
                        groupName = contactName
                    }
                }

                updateLyouts(ViewUtils.isOfferSender(name))
                markThreadAsRead(threadId)
            } else {
                val hasForwardedText =
                    intent.getStringArrayListExtra("forwardedMessages")?.isNotEmpty() == true
                if (hasForwardedText) {
                    binding.emptyText.visibility = View.GONE
                } else {
                    binding.emptyText.visibility = View.VISIBLE
                }

//                binding.emptyText.visibility = View.VISIBLE
                binding.address.text = name
                updateLyouts(ViewUtils.isOfferSender(name))
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

    @SuppressLint("NewApi")
    fun Context.getThreadId(addresses: Set<String>): Long {
        return try {
            Telephony.Threads.getOrCreateThreadId(this, addresses)
        } catch (e: Exception) {
            0L
        }
    }

    private fun sendMessage() {
        highlightQuery = null
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
        draftViewModel.deleteDraft(threadId)
        binding.recyclerViewConversation.itemAnimator = null
        scrolltoBottom()
        viewModel.loadMessages()
        viewModel.loadConversation(threadId)
        adapter.setSearchQuery(null)
    }

    fun resendMessage(message: ConversationItem) {
        val addresses = number.split(", ").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val subId = SmsManager.getDefaultSmsSubscriptionId()

        sendSmsMessage(
            text = message.body ?: return,
            addresses = addresses,
            subId = subId,
            requireDeliveryReport = false,
            messageId = message.id
        )
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

    private fun shareMessages() {
        val selectedMessages = adapter.getSelectedItems().toList()

        if (selectedMessages.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.no_messages_selected_to_share), Toast.LENGTH_SHORT
            ).show()
            return
        }
        val shareContent = selectedMessages.joinToString(separator = "\n\n") { it.body }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareContent)
        }
        val chooser = Intent.createChooser(shareIntent, "Share messages via")
        startActivity(chooser)

    }

    private fun deleteSelectedMessages() {
        val selectedMessages = adapter.getSelectedItems().toList()

        Thread {
            val db = AppDatabase.getDatabase(this).recycleBinDao()

            for (message in selectedMessages) {
                val deletedMessage = DeletedMessage(
                    messageId = message.id,
                    threadId = message.threadId,
                    address = name,
                    date = message.date,
                    body = message.body,
                    type = message.type,
                    read = message.read,
                    subscriptionId = message.subscriptionId
                )

                db.insertMessage(deletedMessage)

                val uri = Uri.parse("content://sms/${message.id}")
                contentResolver.delete(uri, null, null)
            }

            runOnUiThread {
                adapter.clearSelection()
                viewModel.loadConversation(threadId)
            }
        }.start()
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
                binding.btnStar.setImageResource(if (isStarred) R.drawable.ic_star else R.drawable.ic_unstar)
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


    override fun onBackPressed() {
        if (adapter.isMultiSelectionEnabled) {
            adapter.clearSelection()
            updateUI(0)
        } else {
            setActivityResult()
            super.onBackPressed()
        }
    }

    private fun setActivityResult() {
        val resultIntent = Intent().apply {
            putExtra("RESULT_OK", true) // Just a success flag
        }
        setResult(Activity.RESULT_OK, resultIntent)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshMessagesEvent(event: RefreshMessagesEvent) {
        viewModel.loadMessages()
        viewModel.loadConversation(threadId)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGroupNameUpdated(event: UpdateGroupNameEvent) {
        if (event.threadId == threadId) {
            binding.address.text = event.newName
            groupName = event.newName
        }
        viewModel.loadMessages()
        viewModel.loadConversation(threadId)
    }

    private fun showDateTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val datePickerDialog = DatePickerDialog(
            this,
            R.style.CustomDatePickerDialog,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }

                val timePickerDialog = TimePickerDialog(
                    this,
                    R.style.CustomTimePickerDialog,
                    { _, hourOfDay, minute ->
                        val selectedCalendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        if (selectedCalendar.timeInMillis < System.currentTimeMillis()) {
                            Toast.makeText(
                                this,
                                getString(R.string.cannot_select_past_time),
                                Toast.LENGTH_SHORT
                            ).show()
                            showDateTimePickerDialog()
                        } else {
                            selectedTimeInMillis = selectedCalendar.timeInMillis
                            binding.schedulLy.visibility = View.VISIBLE

                            val formattedTime =
                                SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
                                    .format(selectedCalendar.time)

                            val spannable = SpannableStringBuilder().apply {
                                append(getString(R.string.schedule_at) + " ")
                                setSpan(
                                    ForegroundColorSpan(
                                        ContextCompat.getColor(
                                            this@ConversationActivity,
                                            R.color.colorPrimary
                                        )
                                    ),
                                    0,
                                    getString(R.string.schedule_at).length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                val timeSpan = SpannableString(formattedTime)
                                timeSpan.setSpan(
                                    ForegroundColorSpan(
                                        ContextCompat.getColor(
                                            this@ConversationActivity,
                                            R.color.colorPrimary
                                        )
                                    ),
                                    0,
                                    timeSpan.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                append(timeSpan)
                            }
                            binding.selectedTimeTextView.text = spannable
                        }
                    },
                    currentHour,
                    currentMinute,
                    false
                )
                timePickerDialog.show()

            },
            currentYear,
            currentMonth,
            currentDay
        )
        datePickerDialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleMessage() {
        val messageText = binding.editTextMessage.text.toString().trim()
        if (messageText.isEmpty() || selectedTimeInMillis == 0L) {
            Toast.makeText(this, getString(R.string.please_enter), Toast.LENGTH_SHORT)
                .show()
            return
        }
        val recipientNameOrNumber = if (viewModel.getContactNameOrNumber(number).isNotEmpty()) {
            viewModel.getContactNameOrNumber(number)
        } else {
            number
        }
        val scheduledMessage = ScheduledMessage(
            id = 0,
            recipient = recipientNameOrNumber,
            message = messageText,
            scheduledTime = selectedTimeInMillis,
            threadId = threadId.toString(),
            profileUrl = profileUrl
        )

        Thread {
            AppDatabase.getDatabase(this).scheduledMessageDao().insert(scheduledMessage)
            runOnUiThread {
                MessageScheduler.scheduleMessage(this, scheduledMessage)
                Toast.makeText(this, getString(R.string.message_scheduled), Toast.LENGTH_SHORT)
                    .show()
                val intent = Intent(this, ScheduleActivity::class.java)
                startActivity(intent)
                finish()
            }
        }.start()
    }

    private fun isDefaultSmsApp(): Boolean {
        return Telephony.Sms.getDefaultSmsPackage(this) == packageName
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestDefaultSmsApp() {
        val intent = Intent(this, SmsPermissionActivity::class.java)
        smsPermissionLauncher.launch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isDefaultSmsApp()) {
                proceedWithMessageSending()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.please_set_this_app_as_default),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun proceedWithMessageSending() {
        if (isScheduled) {
            scheduleMessage()
        } else {
            sendMessage()
        }
    }

}
