package com.test.messages.demo.ui.Activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
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
import android.telephony.SubscriptionManager
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.UnderlineSpan
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
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.internal.ViewUtils.hideKeyboard
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.test.messages.demo.data.Database.Scheduled.ScheduledMessage
import com.test.messages.demo.data.Database.Starred.StarredMessage
import com.test.messages.demo.R
import com.test.messages.demo.Util.ActivityFinishEvent
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.EXTRA_TXT
import com.test.messages.demo.Util.CommanConstants.FORWARD
import com.test.messages.demo.Util.CommanConstants.FORWARDMSGS
import com.test.messages.demo.Util.CommanConstants.FROMARCHIVE
import com.test.messages.demo.Util.CommanConstants.FROMBLOCK
import com.test.messages.demo.Util.CommanConstants.FROMSEARCH
import com.test.messages.demo.Util.CommanConstants.GROUP_MEMBERS
import com.test.messages.demo.Util.CommanConstants.GROUP_NAME
import com.test.messages.demo.Util.CommanConstants.GROUP_NAME_KEY
import com.test.messages.demo.Util.CommanConstants.ISGROUP
import com.test.messages.demo.Util.CommanConstants.ISSCHEDULED
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.CommanConstants.PROFILEURL
import com.test.messages.demo.Util.CommanConstants.QUERY
import com.test.messages.demo.Util.CommanConstants.SHARECONTACT
import com.test.messages.demo.Util.CommanConstants.SHARECONTACTNAME
import com.test.messages.demo.Util.CommanConstants.SHARECONTACTNUMBER
import com.test.messages.demo.Util.CommanConstants.SOURCE
import com.test.messages.demo.Util.ConversationOpenedEvent
import com.test.messages.demo.Util.ConversationUpdatedEvent
import com.test.messages.demo.Util.DeleteSearchMessageEvent
import com.test.messages.demo.Util.DraftChangedEvent
import com.test.messages.demo.Util.MessageDeletedEvent
import com.test.messages.demo.Util.NewSmsEvent
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
import com.test.messages.demo.Util.RefreshMessagesEvent
import com.test.messages.demo.Util.SmsPermissionUtils.isDefaultSmsApp
import com.test.messages.demo.Util.SmsUtils
import com.test.messages.demo.Util.SmsUtils.markThreadAsRead
import com.test.messages.demo.Util.TimeUtils.formatTimestamp
import com.test.messages.demo.Util.TimeUtils.getFormattedHeaderTimestamp
import com.test.messages.demo.Util.UpdateGroupNameEvent
import com.test.messages.demo.Util.ViewUtils.autoScrollToStart
import com.test.messages.demo.Util.ViewUtils.blinkThen
import com.test.messages.demo.data.Model.DraftModel
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.data.Model.SIMCard
import com.test.messages.demo.data.viewmodel.DraftViewModel
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.ui.Dialogs.DeleteProgressDialog
import com.test.messages.demo.ui.Dialogs.MessageDetailsDialog
import com.test.messages.demo.ui.send.getThreadId
import com.test.messages.demo.ui.send.hasReadContactsPermission
import com.test.messages.demo.ui.send.hasReadSmsPermission
import com.test.messages.demo.ui.send.hasReadStatePermission
import com.test.messages.demo.ui.send.subscriptionManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    private lateinit var name: String
    private lateinit var profileUrl: String

    //    private lateinit var groupName: String
    private var groupName: String? = null

    private var subscriptionId: Int = -1
    private lateinit var messagingUtils: MessageUtils
    private lateinit var linearLayoutManager: LinearLayoutManager
    var isfromBlock: Boolean = false
    var isfromArchive: Boolean = false
    var isfromSearch: Boolean = false
    var isScheduled: Boolean = false
    var isGroup: Boolean = false
    var isStarred: Boolean = false
    private var sharetxt: String? = null
    private var highlightQuery: String = ""
    private var forwardedMessage: String = ""
    private var selectedTimeInMillis: Long = 0L
    private lateinit var profileLauncher: ActivityResultLauncher<Intent>
    private var currentDraft: DraftModel? = null
    private var isRestoringDraft = false
    private var shouldScrollToBottom = true
    private val tempMessages = mutableListOf<ConversationItem>()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this) && !hasReadSmsPermission() && !hasReadContactsPermission()) {
            return
        }
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        if (threadId != -1L) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.cancel(threadId.toInt())
        }
        EventBus.getDefault().postSticky(ConversationOpenedEvent(threadId))
    }

    override fun onPause() {
        super.onPause()
        if (!isScheduled || threadId.equals("")) {
            Log.d("DARFT", "onPost: " + binding.editTextMessage.text.toString())
            var s = binding.editTextMessage.text.toString().trim()
            if (s.isNullOrEmpty() && currentDraft != null) {
                draftViewModel.deleteDraft(currentDraft!!.msg_id)
                binding.editTextMessage.setText("")
            } else {
                if (!s.isNullOrEmpty()) {
                    draftViewModel.saveDraft(threadId, s.toString())
                }
            }
            EventBus.getDefault().post(DraftChangedEvent(threadId))
        }
        EventBus.getDefault().removeStickyEvent(ConversationOpenedEvent::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        messagingUtils = MessageUtils(this)
        EventBus.getDefault().register(this)
        setupSimSwitcher()
        threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        number = intent.getStringExtra(NUMBER).toString()
        name = intent.getStringExtra(NAME).toString()
        binding.address.text = name
        profileUrl = intent.getStringExtra(PROFILEURL).toString()
        isfromBlock = intent.getBooleanExtra(FROMBLOCK, false)
        isfromArchive = intent.getBooleanExtra(FROMARCHIVE, false)
        isfromSearch = intent.getBooleanExtra(FROMSEARCH, false)
        highlightQuery = intent.getStringExtra(QUERY).toString()
        isScheduled = intent.getBooleanExtra(ISSCHEDULED, false)

        isGroup = intent.getBooleanExtra(ISGROUP, false)
        isStarred = intent.getBooleanExtra(CommanConstants.ISSTARRED, false)
        setLearnMoreSpannable()
        val forwardedMessage = intent.getStringExtra(FORWARDMSGS)
        if (!forwardedMessage.isNullOrEmpty()) {
            binding.editTextMessage.setText(forwardedMessage)
            binding.editTextMessage.setSelection(forwardedMessage.length)
        }
        if (isScheduled) {
            binding.msgSendLayout.visibility = View.GONE
            showDateTimePickerDialog()
            binding.close.setOnClickListener {
                binding.schedulLy.visibility = View.GONE
                isScheduled = false
            }
        } else {
            binding.schedulLy.visibility = View.GONE
        }
        Log.d("TAG", "onCreate: threadId :- " + threadId)
        Log.d("TAG", "onCreate: isGroup :- " + isGroup)
        setupRecyclerView()
        checkIfThreadBlocked(threadId)
        if (threadId != -1L) {
            resetMessageCount(this, threadId)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.cancel(threadId.toInt())

            viewModel.loadConversation(threadId)
        } else {
            threadId = getThreadId(setOf(number))
        }
        markThreadAsRead(this@ConversationActivity, threadId)
        viewModel.loadStarredMessages()
        observeViewModel()

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
        getDrfat()
        sharetxt = intent.getStringExtra(EXTRA_TXT)
        if (!sharetxt.isNullOrEmpty()) {
            binding.editTextMessage.setText(sharetxt)
            binding.buttonSend.isEnabled = true
            binding.buttonSend.setImageResource(R.drawable.ic_send_enable)
        }
        binding.editTextMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
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
                    isfromBlock = result.data?.getBooleanExtra(FROMBLOCK, false) ?: false
                    /*var isThreadDelete = result.data?.getBooleanExtra("delete", false) ?: false
                    if (isThreadDelete) {
                        finish()
                    }*/

                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.address.text = updatedName
                        if (isfromBlock) {
                            binding.blockLy.visibility = View.VISIBLE
                            binding.btnUnblock.setOnClickListener {
                                isfromBlock = false
                                unblockThread(threadId)
                            }
                        } else {
                            binding.blockLy.visibility = View.GONE
                        }
                    }, 80)
                }
            }
    }

    fun getDrfat(updateText: Boolean = true) {
        draftViewModel.getDraft(threadId).observe(this) { draft ->
//            Log.d("TAG", "onCreate:draftViewModel observe ")
            currentDraft = draft
            if (draft != null) {
//                hasDraftPreviously = draft.draft_label.isNullOrEmpty()
                if (forwardedMessage.isNullOrEmpty() && !draft.draft_label.isNullOrEmpty() && updateText == true) {
                    binding.editTextMessage.setText(draft.draft_label)
                    binding.editTextMessage.setSelection(draft.draft_label.length)
                }
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

            Log.d("TAG", "Raw Number String: $number")
            val numbersList = number.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            if (numbersList.size > 1) {
                // It's a group chat
                val intent = Intent(this, GroupProfileActivity::class.java)
                intent.putExtra(EXTRA_THREAD_ID, threadId)
                intent.putExtra(GROUP_NAME, groupName ?: name)
                intent.putStringArrayListExtra(GROUP_MEMBERS, ArrayList(numbersList))
                startActivity(intent)
            } else {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra(EXTRA_THREAD_ID, threadId)
                intent.putExtra(NUMBER, number)
                intent.putExtra(NAME, name)
                intent.putExtra(FROMBLOCK, isfromBlock)
                intent.putExtra(FROMARCHIVE, isfromArchive)
                intent.putExtra(PROFILEURL, profileUrl)
                profileLauncher.launch(intent)
            }

            /* if (isfromBlock || isfromArchive) {
                 finish()
             }*/
        }
        binding.buttonSend.setOnClickListener {
            proceedWithMessageSending()
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
            isScheduled = true
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
        binding.btnDelete.setOnClickListener {
            val deleteDialog = DeleteDialog(this, "conversation_chat", false) {
                deleteSelectedMessages()
            }
            deleteDialog.show()
        }

        binding.btnCopy.setOnClickListener { copySelectedMessages() }
        binding.btnStar.setOnClickListener {
            starSelectedMessages()
        }
//        binding.learnMore.setOnClickListener {
//            val dialog = LearnMoreDialog(this)
//            dialog.show()
//        }
        binding.btnMore.setOnClickListener {
            it.hideKeyboard(this)
            showPopupMore(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 401 && resultCode == Activity.RESULT_OK) {
            val selectedNumber = data?.getStringExtra(SHARECONTACTNUMBER) ?: return
            val contactName = data.getStringExtra(SHARECONTACTNAME) ?: getString(R.string.unknown)

            val newContactText = "$contactName\n$selectedNumber"

            if (binding.editTextMessage.text.isNotEmpty()) {
                binding.editTextMessage.append("\n\n")
            }

            binding.editTextMessage.append(newContactText)

            binding.editTextMessage.setSelection(binding.editTextMessage.text.length)
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
                val joinedMessage = selectedBodies.joinToString("\n")

                val intent = Intent(this, ContactActivtiy::class.java)
                intent.putExtra(SOURCE, FORWARD)
                intent.putExtra(FORWARDMSGS, joinedMessage)
                startActivity(intent)
//                finish()
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
        val contactName = viewModel.getContactName(this@ConversationActivity, name)
        val isContactSaved = contactName != number

        adapter = ConversationAdapter(
            this,
            isContactSaved,
            availableSIMCards,
            onSelectionChanged = { selectedCount ->
                updateUI(selectedCount)
                adapter.setSearchQuery(highlightQuery)
                searchAndScroll(highlightQuery)

            },
            onStarClicked = { conversation ->
                viewModel.toggleStarredMessage(
                    conversation.id,
                    conversation.threadId,
                    conversation.body,
                    conversation.date,
                    conversation.starred,
                    isGroup,
                    profileUrl,
                    number,
                    name
                )
                adapter.clearSelection()
                updateSnnipet()
            }
        )
        adapter.setOnRetryListener(object : ConversationAdapter.OnMessageRetryListener {
            override fun onRetry(message: ConversationItem) {
                resendMessage(message)
            }
        })
        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        binding.recyclerViewConversation.layoutManager = linearLayoutManager
        binding.recyclerViewConversation.itemAnimator = null
//        (binding.recyclerViewConversation.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = true
        binding.recyclerViewConversation.adapter = adapter
//        setKeyboardVisibilityListener()
        binding.learnMoreLy.visibility = View.GONE
        binding.secureLy.visibility = View.GONE
    }

    fun searchAndScroll(query: String) {
        adapter.setSearchQuery(query)
        val position = adapter.currentList.indexOfLast {
            it.body?.contains(query, ignoreCase = true) == true
        }

        if (position != -1) {
            binding.recyclerViewConversation.post {
                binding.recyclerViewConversation.scrollToPosition(position)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
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

    fun scrolltoBottom() {
        if (!isfromSearch) {
            binding.recyclerViewConversation.scrollToPosition(adapter.itemCount - 1)
            binding.recyclerViewConversation.postDelayed({
                binding.recyclerViewConversation.scrollToPosition(adapter.itemCount - 1)
            }, 300)
        } else {
            if (highlightQuery.isNotEmpty()) {

            } else {
                binding.recyclerViewConversation.postDelayed({
                    binding.recyclerViewConversation.scrollToPosition(adapter.itemCount - 1)
                }, 300)
            }

        }

    }

    private fun updateLyouts(isServiceNumber: Boolean, isGroup: Boolean) {
        if (isServiceNumber && !isGroup) {
            binding.msgSendLayout.visibility = View.GONE
            if (isStarred) {
                binding.learnMoreLy.visibility = View.GONE
                binding.secureLy.visibility = View.GONE
            } else {
                binding.learnMoreLy.visibility = View.VISIBLE
                binding.secureLy.visibility = View.VISIBLE
            }

        } else {
            if (isStarred) {
                binding.msgSendLayout.visibility = View.GONE
            } else {
                binding.msgSendLayout.visibility = View.VISIBLE
            }
            binding.learnMoreLy.visibility = View.GONE
            binding.secureLy.visibility = View.GONE

        }
    }

    private fun observeViewModel() {
        var loaderJob: Job? = null
        binding.msgSendLayout.visibility = View.GONE
        binding.loader.visibility = View.VISIBLE
        binding.emptyText.visibility = View.GONE
        viewModel.conversation.observe(this) { conversation ->
            if (conversation == null) {
                return@observe
            }

            var conversationList = conversation
//            Log.d("TAG", "Received conversation list size: ${conversationList.size}")

            CoroutineScope(Dispatchers.IO).launch {
                val filteredList =
                    conversationList.filter { it.type != Telephony.Sms.MESSAGE_TYPE_DRAFT }
                val starredIds = viewModel.starredMessageIds.value ?: emptySet()
                filteredList.forEach { it.starred = starredIds.contains(it.id) }

                val combinedList = filteredList.toMutableList()

                // Remove temp messages that have been replaced (matching by address+body+temp id <0)
                tempMessages.removeAll { temp ->
                    combinedList.any { real ->
                        real.address == temp.address && real.body == temp.body && real.id > 0
                    }
                }

                combinedList.addAll(tempMessages)

                val visibleMessages =
                    if (isStarred) combinedList.filter { it.starred } else combinedList

//                val visibleMessages = if (isStarred) {
//                    filteredList.filter { it.starred }
//                } else {
//                    filteredList
//                }
                val sortedList = visibleMessages.sortedBy { it.date }
                val groupedList = mutableListOf<ConversationItem>()
                val addedHeaders = mutableSetOf<Long>()
                for (message in sortedList) {

                    val headerKey =
                        getStartOfDayTimestamp(message.date) // For grouping logic (Long)

                    if (!addedHeaders.contains(headerKey)) {
                        val formattedHeaderText =
                            getFormattedHeaderTimestamp(this@ConversationActivity, message.date)

                        groupedList.add(
                            ConversationItem.createHeader(
                                formattedHeaderText,
                                headerKey
                            )
                        )
                        addedHeaders.add(headerKey)
                    }
                    groupedList.add(message)
                }
//                Log.d("TAG", "observeViewModel:--- " + groupedList.size)
                withContext(Dispatchers.Main) {
                    loaderJob?.cancel()
                    adapter.submitList(groupedList) {
                        if (!highlightQuery.isNullOrEmpty()) {
                            adapter.setSearchQuery(highlightQuery)
                            searchAndScroll(highlightQuery)
                        }
                        if (shouldScrollToBottom) {
                            scrolltoBottom()
                        }
                        shouldScrollToBottom = true

                    }

                    binding.loader.visibility = View.GONE
                    binding.msgSendLayout.visibility = View.VISIBLE // ✅ Always show EditText

                    if (groupedList.isNotEmpty()) {
                        binding.emptyText.visibility = View.GONE
//                        binding.loader.visibility = View.GONE
//                        binding.bottomLy.visibility = View.VISIBLE
                        binding.msgSendLayout.visibility = View.VISIBLE

                        CoroutineScope(Dispatchers.IO).launch {
                            val sharedPreferences = getSharedPreferences(
                                CommanConstants.PREFS_NAME,
                                Context.MODE_PRIVATE
                            )
                            val savedGroupName =
                                sharedPreferences.getString("${GROUP_NAME_KEY}$threadId", null)

                            Log.d(
                                "GroupNameCheck",
                                "Checking SharedPreferences for key C : ${GROUP_NAME_KEY}$threadId"
                            )
                            Log.d("GroupNameCheck", "Value for  C = $savedGroupName")

                            val contactName = savedGroupName ?: name

                            withContext(Dispatchers.Main) {
                                binding.address.text = contactName
                                groupName = contactName
                            }
                        }

                        updateLyouts(ViewUtils.isShortCodeWithLetters(number), isGroup)
                        markThreadAsRead(this@ConversationActivity, threadId)
                        viewModel.loadMessages()
//                        { Log.d("MessageRepository", "observeViewModel:read ")
//                            viewModel.loadMessages()
//                        }
                    } else {
                            binding.emptyText.visibility = View.VISIBLE
                            binding.loader.visibility = View.GONE
                            binding.address.text = name
                            updateLyouts(ViewUtils.isShortCodeWithLetters(number), isGroup)

                    }
                }
            }
        }

        viewModel.starredMessageIds.observe(this, Observer { starredMessages ->
            Log.d("STARRED_UPDATE", "Starred Messages updated: $starredMessages")
            adapter.setStarredMessages(starredMessages)
            updateUI(adapter.selectedItems.size)
        })

    }

    private fun checkIfThreadBlocked(threadId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val blockedThreadIds = AppDatabase.getDatabase(this@ConversationActivity)
                .blockDao()
                .getBlockThreadIds()

            val isBlocked = blockedThreadIds.contains(threadId)

            withContext(Dispatchers.Main) {
                if (isBlocked) {
                    binding.blockLy.visibility = View.VISIBLE
                    binding.btnUnblock.setOnClickListener {
                        unblockThread(threadId)
                    }
                } else {
                    binding.blockLy.visibility = View.GONE
                }
            }
        }
    }

    private fun sendMessage() {
        val text = binding.editTextMessage.text.toString().trim()
        if (text.isEmpty()) return
        binding.editTextMessage.setText("")
        val numbersSet = number.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val selectedSimId =
            availableSIMCards.getOrNull((selectedSimIndex - 1).toInt())?.subscriptionId
                ?: SubscriptionManager.getDefaultSmsSubscriptionId()
        lifecycleScope.launch(Dispatchers.IO) {
            var threadIdToUse = threadId
            if (threadIdToUse == -1L || threadIdToUse == 0L) {
                threadIdToUse = getThreadId(numbersSet)
            }

            /*currentDraft?.let {
                draftViewModel.deleteDraft(it.msg_id)
                Log.d("SEND_MSG", "Deleted draft with id: ${it.msg_id}")
            }
            withContext(Dispatchers.Main) {
                chnageDrfat(false)
                EventBus.getDefault().post(DraftChangedEvent(threadIdToUse))
            }*/

            val sendingTime = System.currentTimeMillis()
            // Insert group message
            var groupId = -1L
            var groupUri: Uri? = null
            if (numbersSet.size > 1) {
                val groupThreadId = getThreadId(numbersSet.toSet())
                groupId = groupThreadId
                val tempId = -sendingTime + numbersSet.first().hashCode()
                var tempMessage = ConversationItem(
                    id = tempId,
                    threadId = threadIdToUse,
                    address = numbersSet.joinToString(","),
                    body = text,
                    date = sendingTime,
                    type = Telephony.Sms.MESSAGE_TYPE_OUTBOX,
                    read = true,
                    subscriptionId = subscriptionId,
                    profileImageUrl = "",
                    isHeader = false,
                    starred = false
                )
                withContext(Dispatchers.Main) {
                    tempMessages.add(tempMessage)
                    adapter.addMessages(listOf(tempMessage))
                    scrolltoBottom()
                    Log.d("SEND_MSG", "Temp messages added to adapter and UI updated")
                }
                var groupUri = messagingUtils.insertSmsMessage(
                    subId = selectedSimId,
                    dest = numbersSet.joinToString(","),
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    threadId = threadIdToUse
                )
                try {
                    val simCard = availableSIMCards.getOrNull((selectedSimIndex - 1).toInt())
                    val subscriptionId =
                        simCard?.subscriptionId ?: SubscriptionManager.getDefaultSmsSubscriptionId()
                    sendSmsMessage(
                        text = text,
                        addresses = numbersSet,
                        subId = subscriptionId,
                        requireDeliveryReport = false,
                        messageId = null,
                        groupId, groupUri

                    )
                } catch (e: Exception) {
                    Log.e("SendMessage", "Failed to send to ${numbersSet.first()}", e)
                }

            } else {
                val tempId = -sendingTime + numbersSet.first().hashCode()
                val tempMessage = ConversationItem(
                    id = tempId,
                    threadId = threadIdToUse,
                    address = numbersSet.first(),
                    body = text,
                    date = sendingTime,
                    type = Telephony.Sms.MESSAGE_TYPE_OUTBOX,
                    read = true,
                    subscriptionId = subscriptionId,
                    profileImageUrl = "",
                    isHeader = false,
                    starred = false
                )
                withContext(Dispatchers.Main) {
                    tempMessages.add(tempMessage)
                    adapter.addMessages(listOf(tempMessage))
                    adapter.setSearchQuery("")
                    highlightQuery = ""
                    searchAndScroll(highlightQuery)
                    scrolltoBottom()

                }
                val messageUri = messagingUtils.insertSmsMessage(
                    subId = selectedSimId,
                    dest = numbersSet.first(),
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    threadId = threadIdToUse
                )
                try {
                    SmsSender.getInstance(applicationContext as Application).sendMessage(
                        subId = selectedSimId,
                        destination = numbersSet.first(),
                        body = text,
                        serviceCenter = null,
                        requireDeliveryReport = false,
                        messageUri = messageUri
                    )
                } catch (e: Exception) {
                    Log.e("SendMessage", "Failed to send to ${numbersSet.first()}", e)
                }
            }


            withContext(Dispatchers.Main) {
                updateSnnipet()
                viewModel.loadMessages()
                viewModel.loadConversation(threadIdToUse)
                adapter.setSearchQuery("")
                EventBus.getDefault().post(ConversationUpdatedEvent(threadId))

            }
        }
    }


    fun sendSmsMessage(
        text: String,
        addresses: Set<String>,
        subId: Int,
        requireDeliveryReport: Boolean,
        messageId: Long? = null,
        groupId: Long = -1L, groupUri: Uri? = null
    ) {
        if (addresses.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
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
                        messageUri = messageUri,
                        groupId,
                        groupUri
                    )

                    /*if (isfromSearch) {
                        Log.d("TAG", "sendSmsMessage:frm serch ")
                        withContext(Dispatchers.Main) {
                            EventBus.getDefault().post(ConversationUpdatedEvent(threadId))
                            isfromSearch = false
                            highlightQuery=""
                            adapter.setSearchQuery(highlightQuery)
                            scrolltoBottom()
                        }
                    }*/
                } catch (e: Exception) {
                    Log.d("TAG", "Failed to send message to $address", e)
                }
            }
        }
    }

    fun resendMessage(message: ConversationItem) {
        val selection = "${Telephony.Sms._ID} = ?"
        val selectionArgs = arrayOf(message.id.toString())
        contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            selection,
            selectionArgs
        )


        val addresses = number.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        subscriptionId = SmsManager.getDefaultSmsSubscriptionId()
        val simCard = availableSIMCards.getOrNull((selectedSimIndex - 1).toInt())
        val subscriptionId =
            simCard?.subscriptionId ?: SubscriptionManager.getDefaultSmsSubscriptionId()
        if (isGroup) {
            messagingUtils.insertSmsMessage(
                subId = subscriptionId,
                dest = number,
                text = message.body ?: return,
                timestamp = System.currentTimeMillis(),
                threadId = threadId
            )
        } else {
            sendSmsMessage(
                text = message.body ?: return,
                addresses = addresses,
                subId = subscriptionId,
                requireDeliveryReport = false,
                messageId = null
            )
        }
        threadId = getThreadId(setOf(number))
        Log.d("TAG", "resendMessage:threadId "+threadId)
        viewModel.loadConversation(threadId)
    }


    private val availableSIMCards = mutableListOf<SIMCard>()
    private var selectedSimIndex: Long = 1L

    private fun setupSimSwitcher() {
        if (!hasReadStatePermission()) {
            binding.imageSimSwitch.visibility = View.GONE
            return
        }
        val availableSIMs = subscriptionManagerCompat().activeSubscriptionInfoList ?: return

        availableSIMCards.clear()
        if (availableSIMs.size > 1) {
            availableSIMs.forEachIndexed { index, subscriptionInfo ->
                var label = subscriptionInfo.displayName?.toString() ?: ""
                var number = ""
                if (subscriptionInfo.number?.isNotEmpty() == true) {
                    number = subscriptionInfo.number
                }
                val simCard = SIMCard(index + 1, subscriptionInfo.subscriptionId, label, number)
                availableSIMCards.add(simCard)
            }
            val savedSimId = ViewUtils.getSavedSimId(this@ConversationActivity)
            availableSIMCards.forEachIndexed { index, simCard ->
                if (simCard.subscriptionId == savedSimId) {
                    selectedSimIndex = (index + 1).toLong()
                    return@forEachIndexed
                }
            }
            updateSimIcon()

            binding.imageSimSwitch.setOnClickListener {
                toggleSim()
            }

            binding.imageSimSwitch.visibility = View.VISIBLE
        } else {
            binding.imageSimSwitch.visibility = View.GONE
        }

    }

    private fun toggleSim() {
        selectedSimIndex = if (selectedSimIndex == 1L) 2L else 1L
        val subscriptionId =
            availableSIMCards.getOrNull((selectedSimIndex - 1).toInt())?.subscriptionId
        if (subscriptionId != null) {
            ViewUtils.saveSelectedSimId(this, subscriptionId)
        }
        updateSimIcon()
    }

    private fun updateSimIcon() {
        when (selectedSimIndex) {
            1L -> binding.imageSimSwitch.setImageResource(R.drawable.sim_1)
            2L -> binding.imageSimSwitch.setImageResource(R.drawable.sim_2)
            else -> binding.imageSimSwitch.setImageResource(R.drawable.sim_1)
        }
    }

    private fun unblockThread(threadId: Long) {
        if (threadId == -1L) return
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.unblockConversations(listOf(threadId))
            withContext(Dispatchers.Main) {
                binding.blockLy.visibility = View.GONE
                if (isfromBlock) {
                    finish()
                    isfromBlock = false
                } else {
                    isfromBlock = false
                    binding.blockLy.visibility = View.GONE
                }

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
        val shareContent = selectedMessages.joinToString(separator = "\n") { it.body }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareContent)
        }
        val chooser = Intent.createChooser(shareIntent, "Share messages via")
        startActivity(chooser)
    }

    private fun deleteSelectedMessages() {
        val selectedMessages = adapter.getSelectedItems().toList()
        if (selectedMessages.isEmpty()) return

        val deleteDialog = DeleteProgressDialog(this)
        val handler = Handler(Looper.getMainLooper())
        val showDialogRunnable = Runnable {
            deleteDialog.show(getString(R.string.moving_messages_to_recycle_bin))
        }
        handler.postDelayed(showDialogRunnable, 400)

        viewModel.deleteSelectedMessages(
            context = this,
            selectedMessages = selectedMessages,
            isFromSearch = isfromSearch
        ) { deletedIds ->
            handler.removeCallbacks(showDialogRunnable)
            deleteDialog.dismiss()
            adapter.clearSelection()
            updateSnnipet()
            viewModel.loadConversation(threadId)
            shouldScrollToBottom = false

            if (isfromSearch && deletedIds != null) {
                EventBus.getDefault().post(DeleteSearchMessageEvent(deletedIds))
            }

            Handler(Looper.getMainLooper()).postDelayed({
                Log.d("TAG", "deleteSelectedMessages:-- ")
                if (adapter.itemCount == 0) {
                    finish()
                }
            }, 50)
        }
    }


    private fun copySelectedMessages() {
        if (adapter.selectedItems.isNotEmpty()) {
            val copiedText = adapter.selectedItems.joinToString("\n") { it.body }

            if (copiedText.isNotBlank()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Messages", copiedText)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(this, getString(R.string.messages_copied), Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.no_messages_selected),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }

            adapter.clearSelection()
        }
    }

    private fun starSelectedMessages() {
        val selectedMessages = adapter.selectedItems.toList()
        viewModel.starSelectedMessages(selectedMessages,name,number)
        adapter.clearSelection()
        updateSnnipet()
    }

    fun updateSnnipet() {
        Log.d("TAG", "updateSnnipet: ")
        val lastConversationItem =
            adapter.currentList.lastOrNull { it.threadId == threadId } as? ConversationItem
        val lastMessageText = lastConversationItem?.body
        val lastMessageTime = lastConversationItem?.date

        Log.d("TAG", "updateSnnipet:12 " + lastMessageText)
        EventBus.getDefault().post(
            MessageDeletedEvent(
                threadId,
                lastMessageText,
                lastMessageTime
            )
        )
    }

    private fun updateUI(selectedCount: Int) {
        if (selectedCount > 0) {
            binding.actionSelectItem.visibility = View.VISIBLE
            binding.actionbar.visibility = View.GONE
            binding.countText.text = "$selectedCount" + " " + getString(R.string.selected)

            val selectedItems = adapter.selectedItems
            if (selectedCount == 1) {
                val selectedMessage = selectedItems.first()
                var Starred = adapter.starredMessageIds.contains(selectedMessage.id)
                binding.btnStar.visibility = View.VISIBLE
                binding.btnStar.setImageResource(if (Starred) R.drawable.ic_star else R.drawable.ic_unstar)
            } else {
                binding.btnStar.visibility = View.GONE
            }

        } else {
            binding.actionSelectItem.visibility = View.GONE
            binding.actionbar.visibility = View.VISIBLE
            binding.btnStar.visibility = View.GONE
        }
    }


    override fun onBackPressed() {
        if (adapter.isMultiSelectionEnabled) {
            adapter.clearSelection()
            updateUI(0)
        } else {
            viewModel.emptyConversation()
            super.onBackPressed()
        }
    }


    /* private fun setActivityResult() {
         val resultIntent = Intent().apply {
             putExtra("RESULT_OK", true)
         }
         setResult(Activity.RESULT_OK, resultIntent)
     }*/


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshMessagesEvent(event: RefreshMessagesEvent) {
        viewModel.loadMessages()
        viewModel.loadConversation(threadId)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRecievessagesEvent(event: NewSmsEvent) {
        Log.d("TAG", "onRecievessagesEvent: ")
        viewModel.loadConversation(threadId)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onActivtiyfinishEvent(event: ActivityFinishEvent) {
        finish()
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

    @SuppressLint("RestrictedApi")
    private fun showDateTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val datePickerDialog = DatePickerDialog(
            this@ConversationActivity, R.style.CustomDatePicker,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }

                val timePickerDialog = TimePickerDialog(
                    this, R.style.CustomTimePicker,

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

//                            val formattedTime =
//                                SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
//                                    .format(selectedCalendar.time)
                            val is24Hour =
                                android.text.format.DateFormat.is24HourFormat(this@ConversationActivity)
                            val timePattern =
                                if (is24Hour) "dd MMM yyyy HH:mm" else "dd MMM yyyy hh:mm a"
                            val formattedTime =
                                SimpleDateFormat(timePattern, Locale.ENGLISH).format(
                                    selectedCalendar.time
                                )

                            val spannable = SpannableStringBuilder().apply {
                                append(getString(R.string.schedule_at))
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
        datePickerDialog.setOnCancelListener {
            isScheduled = false
            binding.schedulLy.visibility = View.GONE
        }
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleMessage() {
        val messageText = binding.editTextMessage.text.toString().trim()
        if (messageText.isEmpty() || selectedTimeInMillis == 0L) {
//            Toast.makeText(this, getString(R.string.please_enter), Toast.LENGTH_SHORT)
//                .show()
            return
        }
//        val recipientNameOrNumber = if (viewModel.getContactNameOrNumber(number).isNotEmpty()) {
//            viewModel.getContactNameOrNumber(number)
//        } else {
//            number
//        }
        isRestoringDraft = true
        binding.editTextMessage.text.clear()
        currentDraft?.let {
            Log.d("TAG", "scheduleMessage: drft")
            draftViewModel.deleteDraft(it.msg_id)
            EventBus.getDefault().post(DraftChangedEvent(threadId))
            currentDraft = null
        }

        val simCard = availableSIMCards.getOrNull((selectedSimIndex - 1).toInt())
        val subscriptionId =
            simCard?.subscriptionId ?: SubscriptionManager.getDefaultSmsSubscriptionId()

        val recipientNumbers =
            number.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val recipientName = recipientNumbers.joinToString(",") {
            viewModel.getContactNameOrNumber(it).ifEmpty { it }
        }
        val scheduledMessage = ScheduledMessage(
            id = 0,
            recipientName = recipientName,
            recipientNumber = recipientNumbers.joinToString(","),
            message = messageText,
            scheduledTime = selectedTimeInMillis,
            threadId = threadId.toString(),
            profileUrl = profileUrl,
            subscriptionId = subscriptionId
        )
        binding.editTextMessage.text.clear()

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@ConversationActivity)
            val id = withContext(Dispatchers.IO) {
                db.scheduledMessageDao().insert(scheduledMessage)
            }

            // Ensure inserted ID is used
            val scheduledWithId = scheduledMessage.copy(id = id.toInt())
            Log.d(
                "ScheduleDebug",
                "Scheduled message inserted with ID: $id at ${scheduledMessage.scheduledTime}"
            )


            withContext(Dispatchers.Default) {
                MessageScheduler.scheduleMessage(this@ConversationActivity, scheduledWithId)
            }


            Toast.makeText(
                this@ConversationActivity,
                getString(R.string.message_scheduled),
                Toast.LENGTH_SHORT
            )
                .show()
            startActivity(Intent(this@ConversationActivity, ScheduleActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
            finish()

        }.start()

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun proceedWithMessageSending() {
        if (isScheduled) {
            scheduleMessage()
        } else {
            sendMessage()
        }
    }

    fun setLearnMoreSpannable() {
        val part1 = getString(R.string.can_t_replay)
        val part2 = getString(R.string.learn_more)
        val fullText = part1 + part2
        val spannable = SpannableString(fullText)
        val start = part1.length
        val end = fullText.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val dialog = LearnMoreDialog(this@ConversationActivity)
                dialog.show()
            }
        }

        spannable.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.txtCantReply.text = spannable
        binding.txtCantReply.movementMethod = LinkMovementMethod.getInstance()
//        binding.txtCantReply.highlightColor = Color.BLUE
    }


}
