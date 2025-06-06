package com.test.messages.demo.ui.Fragment

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.databinding.FragmentConversationBinding
import com.test.messages.demo.ui.Activity.ConversationActivity
import com.test.messages.demo.ui.Activity.EditCategoryActivity
import com.test.messages.demo.ui.Activity.MainActivity
import com.test.messages.demo.ui.Activity.NewConversationActivtiy
import com.test.messages.demo.ui.Adapter.CategoryAdapter
import com.test.messages.demo.ui.Adapter.MessageAdapter
import com.test.messages.demo.ui.Dialogs.BlockDialog
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.Util.ViewUtils.getCategoriesFromPrefs
import com.test.messages.demo.Util.ViewUtils.isCategoryEnabled
import com.test.messages.demo.Util.CategoryUpdateEvent
import com.test.messages.demo.Util.CategoryVisibilityEvent
import com.test.messages.demo.Util.Constants
import com.test.messages.demo.Util.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.Constants.ISGROUP
import com.test.messages.demo.Util.Constants.NAME
import com.test.messages.demo.Util.Constants.NUMBER
import com.test.messages.demo.Util.Constants.PROFILEURL
import com.test.messages.demo.Util.MessageDeletedEvent
import com.test.messages.demo.Util.MessageRestoredEvent
import com.test.messages.demo.Util.MessagesRestoredEvent
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.SnackbarUtil
import com.test.messages.demo.Util.SwipeActionEvent
import com.test.messages.demo.data.reciever.UnreadMessageListener
import com.test.messages.demo.data.viewmodel.DraftViewModel
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.annotations.Nullable
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.SimpleItemAnimator
import com.test.messages.demo.Util.Constants.GROUP_SEPARATOR
import com.test.messages.demo.Util.DraftChangedEvent
import com.test.messages.demo.Util.MarkasreadEvent
import com.test.messages.demo.Util.NewSmsEvent
import com.test.messages.demo.Util.ViewUtils.autoScrollToStart
import com.test.messages.demo.ui.Dialogs.BlockProgressDialog
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.ui.Dialogs.DeleteProgressDialog
import com.test.messages.demo.ui.SMSend.hasReadSmsPermission
import kotlinx.coroutines.delay

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)
class ConversationFragment : Fragment() {

    private var leftSwipeAction: Int? = null
    private var rightSwipeAction: Int? = null
    private var blockConversationIds: List<Long> = emptyList()
    private var archivedConversationIds: List<Long> = emptyList()
    private var pinnedThreadIds: List<Long> = emptyList()

   private var mutedThreadIds: List<Long> = emptyList()
    val viewModel: MessageViewModel by viewModels()
    private val draftViewModel: DraftViewModel by viewModels()
    private lateinit var adapter: MessageAdapter
    private lateinit var binding: FragmentConversationBinding
    var onSelectionChanged: ((Int, Int, Int) -> Unit)? = null
    private var blockedNumbers: List<String> = emptyList()
    private var unreadMessageListener: UnreadMessageListener? = null
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var selectedCategory: String
    private var categories: MutableList<String> = mutableListOf()
    var onSelectAllStateChanged: ((Boolean) -> Unit)? = null

    companion object {
        private const val REQUEST_EDIT_CATEGORY = 1001
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(requireActivity())) {
            return
        }
        if (context is UnreadMessageListener) {
            unreadMessageListener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        unreadMessageListener = null
    }


    private fun updateMessageCount() {
        val totalMessages = adapter.getAllMessages().size
        (activity as? MainActivity)?.updateTotalMessagesCount(totalMessages)
    }

    private fun unreadMessageCount() {
        val unreadMessages = adapter.getAllMessages().count { !it.isRead }
        unreadMessageListener?.onUnreadMessagesCountUpdated(unreadMessages)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View? {
        binding = FragmentConversationBinding.inflate(inflater, container, false);

        EventBus.getDefault().register(this)
        selectedCategory = requireActivity().getString(R.string.inbox)

        if (binding.categoryRecyclerView.visibility == View.VISIBLE) {
            binding.categoryRecyclerView.post {
                binding.categoryRecyclerView.scrollToPosition(categoryAdapter.itemCount - 1)
            }
        }

        categories = getCategoriesFromPrefs(requireContext()).toMutableList()
        if (categories.isEmpty()) {
            categories.add(getString(R.string.inbox))
        }
        selectedCategory = categories.first()

        setupCategoryRecyclerView()
        setupRecyclerView()
        binding.progress.visibility = View.VISIBLE
        viewModel.loadMessages()
        updateCategoryVisibility()
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progress.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.emptyList.visibility = View.GONE
            }
        }
        viewModel.messages.observe(viewLifecycleOwner) { messageList ->
            (activity as? MainActivity)?.updateTotalMessagesCount(messageList.size)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                val filteredMessages = prepareFilteredMessages(messageList)
                    .filter { !it.sender.isNullOrBlank() }


                val unreadMessagesCount = filteredMessages.count { !it.isRead }

                val finalSortedList = filteredMessages.sortedWith(
                    compareByDescending<MessageItem> { it.isPinned }
                        .thenByDescending { it.lastMsgDate }
                )

                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE

                    unreadMessageListener?.onUnreadMessagesCountUpdated(unreadMessagesCount)
                    binding.emptyList.visibility =
                        if (finalSortedList.isEmpty()) View.VISIBLE else View.GONE

                    val layoutManager = binding.conversationList.layoutManager as LinearLayoutManager
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    val offset = binding.conversationList.getChildAt(0)?.top ?: 0

                    adapter.submitList(finalSortedList)

                    layoutManager.scrollToPositionWithOffset(firstVisible, offset)

                    val allSelected = adapter.isAllSelected()
                    onSelectAllStateChanged?.invoke(allSelected)
                    val pinnedIndexes =
                        finalSortedList.indices.filter { adapter.messages[it].isPinned }
                    pinnedIndexes.forEach(adapter::notifyItemChanged)

                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        val threadIds = messageList.map { it.threadId }.distinct()
                        viewModel.insertMissingThreadIds(threadIds)
                    }
                }
            }
        }

        draftViewModel.draftsLiveData.observe(viewLifecycleOwner) { draftMap ->
            adapter.updateDrafts(draftMap)
        }
        if (requireContext().hasReadSmsPermission()) {
            draftViewModel.loadAllDrafts()
        }

        binding.newConversation.setOnClickListener {
            val intent = Intent(requireContext(), NewConversationActivtiy::class.java)
            startActivity(intent)
        }
        return binding.getRoot();
    }

    private suspend fun prepareFilteredMessages(messageList: List<MessageItem>): List<MessageItem> {
        blockedNumbers = viewModel.getBlockedNumbers()
        archivedConversationIds = viewModel.getArchivedConversations().map { it.conversationId }
        blockConversationIds = viewModel.getBlockedConversations().map { it.conversationId }
        pinnedThreadIds = viewModel.getPinnedThreadIds() ?: emptyList()
        mutedThreadIds = viewModel.getMutedThreadIds()
        return messageList.map { message ->
            val isPinnedThread = pinnedThreadIds.contains(message.threadId)
            val isMutedThread = mutedThreadIds.contains(message.threadId)
            message.copy(
                isPinned = isPinnedThread,
                isMuted = isMutedThread
            )
        }.filter {
            it.number !in blockedNumbers &&
                    it.threadId !in archivedConversationIds &&
                    it.threadId !in blockConversationIds &&
                    filterByCategory(it, selectedCategory)
        }

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupCategoryRecyclerView() {
        categoryAdapter = CategoryAdapter(requireContext(), categories) { category ->
            selectedCategory = category
            viewModel.messages.value?.let { messageList ->
                applyCategoryFilter(messageList)
            }
            val index = categories.indexOf(category)
            binding.categoryRecyclerView.smoothScrollToPosition(index)
        }

        binding.categoryRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.categoryRecyclerView.adapter = categoryAdapter
        binding.categoryRecyclerView.post {
            binding.categoryRecyclerView.scrollToPosition(0)
        }
    }

    private fun filterByCategory(message: MessageItem, category: String): Boolean {
        val isPersonal = message.number.startsWith("+") ||
                message.number.startsWith("0") ||
                (message.number.length in 7..15 && message.number.all { it.isDigit() })

        val bodyLower = message.body?.lowercase()?.trim() ?: ""
        return when (category) {
            getString(R.string.inbox) -> true
            getString(R.string.personal) -> isPersonal
            getString(R.string.transactions) -> {
                !isPersonal && (bodyLower.contains("debited") ||
                        bodyLower.contains("credited") ||
                        bodyLower.contains("debit") ||
                        bodyLower.contains("credit"))
            }

            getString(R.string.otps) -> {
                !isPersonal && bodyLower.contains("otp")
            }

            getString(R.string.offers) -> {
                !isPersonal && bodyLower.contains("offer")
            }

            else -> true
        }
    }

    private fun refreshMessages() {
        viewModel.messages.value?.let { messageList ->
            applyCategoryFilter(messageList)
        }
    }

    private fun applyCategoryFilter(messageList: List<MessageItem>) {
        CoroutineScope(Dispatchers.Default).launch {
            val filteredList = prepareFilteredMessages(messageList)
            val finalSortedList = filteredList.sortedWith(
                compareByDescending<MessageItem> { it.isPinned }
                    .thenByDescending { it.timestamp }
            )
            withContext(Dispatchers.Main) {
                binding.emptyList.visibility =
                    if (finalSortedList.isEmpty()) View.VISIBLE else View.GONE
                adapter.submitList(finalSortedList)
                binding.conversationList.doOnPreDraw {
                    binding.conversationList.scrollToPosition(0)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupRecyclerView() {
        adapter = MessageAdapter(
            onSelectionChanged = { count ->
                val pinnedCount = adapter.selectedMessages.count { it.isPinned }
                val mutedCount = adapter.selectedMessages.count { it.isMuted }
                val unmutedCount = count - mutedCount
                val shouldUnmute = mutedCount < unmutedCount
                (activity as? MainActivity)?.updateMuteUnmuteUI(shouldUnmute)

                val hasGroupSelected = adapter.selectedMessages.any { it.isGroupChat }
                (activity as? MainActivity)?.updateBlockUI(!hasGroupSelected)

                if (count > 0) {
                    updateCategoryVisibility()
                    binding.newConversation.visibility = View.GONE
                } else {
                    updateCategoryVisibility()
                    binding.newConversation.visibility = View.VISIBLE
                }

                onSelectionChanged?.invoke(count, pinnedCount, mutedCount)
            }
        )
        adapter.onSelectAllStateChanged = { allSelected ->
            onSelectAllStateChanged?.invoke(allSelected)
        }

        adapter.autoScrollToStart(binding.conversationList) { }
        (binding.conversationList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            true
        binding.conversationList.layoutManager = LinearLayoutManager(requireActivity())
        binding.conversationList.adapter = adapter

        setupSwipeGesture()

        adapter.onItemClickListener = { message ->
            val intent = Intent(requireContext(), ConversationActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, message.threadId)
                putExtra(NUMBER, message.number)
                putExtra(NAME, message.sender)
                putExtra(ISGROUP, message.isGroupChat)
                putExtra(PROFILEURL, message.profileImageUrl)

            }
            startActivity(intent)
        }
    }


    val itemTouchHelperCallback = object :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun getSwipeDirs(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            if (adapter.selectedMessages.isNotEmpty()) return 0

            val context = requireContext()
            val leftAction = ViewUtils.getSwipeAction(context, isRightSwipe = false)
            val rightAction = ViewUtils.getSwipeAction(context, isRightSwipe = true)

            val leftEnabled = leftAction != Constants.SWIPE_NONE
            val rightEnabled = rightAction != Constants.SWIPE_NONE

            return when {
                leftEnabled && rightEnabled -> ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                leftEnabled -> ItemTouchHelper.LEFT
                rightEnabled -> ItemTouchHelper.RIGHT
                else -> 0
            }
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val context = requireContext()
            val message = adapter.getAllMessages()[position]

            when (direction) {
                ItemTouchHelper.LEFT -> {
                    val leftAction = ViewUtils.getSwipeAction(context, false)
                    handleSwipeAction(leftAction, position, message)
                }

                ItemTouchHelper.RIGHT -> {
                    val rightAction = ViewUtils.getSwipeAction(context, true)
                    handleSwipeAction(rightAction, position, message)
                }
            }
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
                return
            }

            val leftSwipeAction =
                ViewUtils.getSwipeAction(requireContext(), isRightSwipe = false)
            val rightSwipeAction =
                ViewUtils.getSwipeAction(requireContext(), isRightSwipe = true)

            val rightSwipeIcon: Int? = getSwipeIcon(rightSwipeAction)
            val leftSwipeIcon: Int? = getSwipeIcon(leftSwipeAction)
            val context = recyclerView.context
            val colorPrimary = ContextCompat.getColor(context, R.color.colorPrimary)
            val whiteColor = ContextCompat.getColor(context, R.color.white)

            val decorator = RecyclerViewSwipeDecorator.Builder(
                c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
            )

            if (dX > 0 && rightSwipeIcon != null) {
                decorator.addSwipeRightBackgroundColor(colorPrimary)
                    .addSwipeRightActionIcon(rightSwipeIcon)
                    .setSwipeRightActionIconTint(whiteColor)
            }

            if (dX < 0 && leftSwipeIcon != null) {
                decorator.addSwipeLeftBackgroundColor(colorPrimary)
                    .addSwipeLeftActionIcon(leftSwipeIcon)
                    .setSwipeLeftActionIconTint(whiteColor)
            }

            decorator.create().decorate()

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        private fun getSwipeIcon(action: Int): Int? {
            return when (action) {
                Constants.SWIPE_DELETE -> R.drawable.ic_delete
                Constants.SWIPE_ARCHIVE -> R.drawable.ic_archive
                Constants.SWIPE_CALL -> R.drawable.ic_call
                Constants.SWIPE_MARK_READ -> R.drawable.ic_mark_read2
                Constants.SWIPE_MARK_UNREAD -> R.drawable.ic_mark_read1
                else -> null
            }
        }

        private fun handleSwipeAction(action: Int, position: Int, message: MessageItem) {
            when (action) {
                Constants.SWIPE_DELETE -> {
                    adapter.selectedMessages.clear()
                    adapter.selectedMessages.add(message)
                    var userConfirmedDelete = false
                    val deleteDialog = DeleteDialog(requireContext(), "fragment", true) {
                        userConfirmedDelete = true
                        adapter.removeMessageAt(position)
                        deleteSelectedMessages()
                    }
                    deleteDialog.setOnDismissListener {
                        adapter.selectedMessages.clear()
                        adapter.clearSelection()
                        if (!userConfirmedDelete) {
                            binding.conversationList.post {
                                adapter.notifyItemChanged(position)
                            }
                        }
                    }
                    deleteDialog.show()
                }

                Constants.SWIPE_ARCHIVE -> {
                    adapter.selectedMessages.clear()
                    adapter.selectedMessages.add(message)
                    archiveMessages()
                }

                Constants.SWIPE_CALL -> {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${message.sender}")
                    }
                    adapter.notifyDataSetChanged()
                    startActivity(intent)
                    adapter.notifyItemChanged(position)
                }

                Constants.SWIPE_MARK_READ -> {
                    adapter.selectedMessages.clear()
                    markThreadAsread(message.threadId)
                    updateMessageList(message.threadId, true)
                }

                Constants.SWIPE_MARK_UNREAD -> {
                    adapter.selectedMessages.clear()
                    markThreadAsUnread(message.threadId)
                    updateMessageList(message.threadId, false)
                }

                else -> {
                    adapter.notifyItemChanged(position)
                }

            }
        }

    }

    private fun setupSwipeGesture() {
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.conversationList)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSwipeActionChanged(event: SwipeActionEvent) {
        if (event.isRightSwipe) {
            rightSwipeAction = event.action
        } else {
            leftSwipeAction = event.action
        }
    }

    fun openEditCategory() {
        val intent = Intent(requireContext(), EditCategoryActivity::class.java)
        intent.putStringArrayListExtra("category_list", ArrayList(categories))
        startActivityForResult(intent, REQUEST_EDIT_CATEGORY)
    }

    fun clearSelection() {
        adapter.clearSelection()
    }

    fun toggleSelectAll(selectAll: Boolean) {
        if (selectAll) {
            adapter.clearSelection()
            val allMessages = adapter.getAllMessages()
            adapter.selectedMessages.addAll(allMessages)
            updateCategoryVisibility()
            binding.newConversation.visibility = View.GONE

        } else {
            adapter.clearSelection()
            updateCategoryVisibility()
            binding.newConversation.visibility = View.VISIBLE
        }

        val selectedMessages = adapter.selectedMessages
        val mutedCount = selectedMessages.count { it.isMuted }
        val unmutedCount = selectedMessages.size - mutedCount
        val shouldUnmute = mutedCount < unmutedCount
        val hasGroupSelected = selectedMessages.any { it.isGroupChat }

        (activity as? MainActivity)?.updateMuteUnmuteUI(shouldUnmute)
        (activity as? MainActivity)?.updateBlockUI(!hasGroupSelected)
        onSelectionChanged?.invoke(
            adapter.selectedMessages.size,
            adapter.selectedMessages.count { it.isPinned },
            adapter.selectedMessages.count { it.isMuted })
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCategoryVisibilityEvent(event: CategoryVisibilityEvent) {
        updateCategoryVisibility()
    }

    private fun updateCategoryVisibility() {
        val hasSelection = adapter.selectedMessages.isNotEmpty()
        binding.categoryRecyclerView.visibility =
            if (!hasSelection && isCategoryEnabled(requireContext())) View.VISIBLE else View.GONE
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCategoryUpdateEvent(event: CategoryUpdateEvent) {
        categories.clear()
        categories.addAll(event.updatedCategories)
        categoryAdapter.updateCategories(categories)
        ViewUtils.saveCategoriesToPrefs(requireContext(), categories)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessagesRestored(event: MessagesRestoredEvent) {
        if (event.success) {
            adapter.notifyDataSetChanged()
            viewModel.loadMessages()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMarkasdread(event: MarkasreadEvent) {
        if (event.success) {
            markThreadAsread(event.threadId)
            Handler(Looper.getMainLooper()).postDelayed({
                adapter.notifyDataSetChanged()
                viewModel.loadMessages()
            },100)

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRecievessagesEvent(event: NewSmsEvent) {
        viewModel.loadMessages()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageDeleted(event: MessageDeletedEvent) {
        viewModel.loadMessages()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageRestored(event: MessageRestoredEvent) {
        updateConversationSnippet(event.threadId, event.lastMessage, event.lastMessageTime)
        viewModel.loadRestoreMessages()

    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteSelectedMessages() {
        if (adapter.selectedMessages.isEmpty()) return
        var messages = adapter.selectedMessages.toList()
        adapter.clearSelection()
        val contentResolver = requireActivity().contentResolver
        val db = AppDatabase.getDatabase(requireContext()).recycleBinDao()
        val handler = Handler(Looper.getMainLooper())
        val deleteDialog = DeleteProgressDialog(requireContext())
        val showDialogRunnable = Runnable {
            deleteDialog.show(getString(R.string.moving_messages_to_recycle_bin))
        }
        handler.postDelayed(showDialogRunnable, 300)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            try {
                val deletedMessages = mutableListOf<DeletedMessage>()
                val existingBodyDatePairs =
                    mutableSetOf<Pair<String, Long>>()
                val existingThreadMap =
                    db.getAllRecycleBinAddresses().associate { it.address to it.threadId }

                for (item in messages) {
                    val threadId = item.threadId
                    viewModel.deleteStarredMessagesForThread(threadId)
                    viewModel.deleteScheduledForThread(threadId)
                    val isPinned = viewModel.getPinnedThreadIds()?.contains(threadId) == true
                    val isMuted = viewModel.getMutedThreadIds()?.contains(threadId) == true

                    val cursor = contentResolver.query(
                        Telephony.Sms.CONTENT_URI,
                        null,
                        "thread_id = ?",
                        arrayOf(threadId.toString()),
                        null
                    )
                    cursor?.use {
                        val idIndex = it.getColumnIndex(Telephony.Sms._ID)
                        val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                        val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                        val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                        val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)
                        val readIndex = it.getColumnIndex(Telephony.Sms.READ)
                        val subIdIndex = it.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)

                        while (it.moveToNext()) {
                            val messageId = it.getLong(idIndex)
                            val address = it.getString(addressIndex) ?: ""

                            if (address.isNullOrEmpty()) {
                                continue
                            }
                            val body = it.getString(bodyIndex) ?: ""
                            val date = it.getLong(dateIndex)
                            val key = Pair(body, date)
                            if (existingBodyDatePairs.contains(key)) {
                                continue
                            }
                            existingBodyDatePairs.add(key)
                            val finalThreadId = existingThreadMap[address] ?: threadId
                            val isGroup = address.contains(GROUP_SEPARATOR)
                            val subscriptionId = if (subIdIndex != -1) it.getInt(subIdIndex) else -1

                            val deletedMessage = DeletedMessage(
                                messageId = messageId,
                                threadId = finalThreadId,
                                address = address,
                                date = date,
                                body = body,
                                type = it.getInt(typeIndex),
                                read = it.getInt(readIndex) == 1,
                                subscriptionId = subscriptionId,
                                deletedTime = System.currentTimeMillis(),
                                isGroupChat = isGroup,
                                profileImageUrl =item.profileImageUrl ,
                                isPinned = isPinned,
                                isMuted = isMuted
                            )
                            deletedMessages.add(deletedMessage)
                        }
                    }
                    val uri = Uri.parse("content://sms/conversations/$threadId")
                    contentResolver.delete(uri, null, null)

                    val selection = "${Telephony.Sms.THREAD_ID} IN ($threadId)"
                    try {
                        contentResolver.delete(Telephony.Sms.CONTENT_URI, selection, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
                if (deletedMessages.isNotEmpty()) {
                    db.insertMessages(deletedMessages)
                }
                withContext(Dispatchers.Main) {
                    handler.removeCallbacks(showDialogRunnable)
                    deleteDialog.dismiss()
                    adapter.removeMessages(messages)
                    viewModel.loadMessages()
                    onSelectionChanged?.invoke(0, 0, 0)
                    updateMessageCount()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    handler.removeCallbacks(showDialogRunnable)
                    deleteDialog.dismiss()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun BlockMessages() {
        val selectedGroups = adapter.selectedMessages.filter { it.isGroupChat }
        if (selectedGroups.isNotEmpty()) {
            return
        }

        val selectedIds = adapter.selectedMessages.map { it.threadId }
        val blockDialog = BlockDialog(requireContext()) {
            if (selectedIds.size > 10) {
                val progressDialog = BlockProgressDialog(requireContext())
                requireActivity().runOnUiThread {
                    progressDialog.show(getString(R.string.block_messages_progress))
                }
                viewModel.blockSelectedConversations(selectedIds) {
                    adapter.clearSelection()
                    updateMessageCount()
                    progressDialog.dismiss()
                }

            } else {
                viewModel.blockSelectedConversations(selectedIds)
                adapter.clearSelection()
                updateMessageCount()
            }

        }

        blockDialog.show()
    }

    fun updateConversationSnippet(threadId: Long, lastMessage: String?, lastMessageTime: Long?) {
        val currentList = adapter.getAllMessages()
        val updatedList = currentList.map {
            if (it.threadId == threadId) {
                it.copy(
                    body = lastMessage ?: "",
                    timestamp = lastMessageTime ?: System.currentTimeMillis()
                )
            } else it
        }

        adapter.submitList(updatedList)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDraftUpdateEvent(event: DraftChangedEvent) {
        draftViewModel.loadAllDrafts()
        viewModel.loadMessages()
        try {
            Handler(Looper.getMainLooper()).postDelayed({
                draftViewModel.loadAllDrafts()
                viewModel.loadMessages()
            }, 500)
        } catch (e: Exception) {
        }

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun unarchiveMessages(messages: List<MessageItem>) {
        CoroutineScope(Dispatchers.Default).launch {
            viewModel.unarchiveConversations(messages.map { it.threadId })
            /*withContext(Dispatchers.Main) {
                val currentList = adapter.getAllMessages().toMutableList()
                val restoredList = (messages + currentList).distinctBy { it.threadId }
            }*/
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun archiveMessages() {
        val selectedIds = adapter.selectedMessages.map { it.threadId }
        val removedMessages = adapter.selectedMessages.toList()

        viewModel.archiveSelectedConversations(selectedIds)
        val updatedList = adapter.getAllMessages().toMutableList()
        updatedList.removeAll { selectedIds.contains(it.threadId) }
        adapter.submitList(updatedList)
        adapter.clearSelection()

        SnackbarUtil.showSnackbar(
            binding.root,
            getString(R.string.archived_successfully), getString(R.string.undo)
        ) {
            unarchiveMessages(removedMessages)
        }
        updateMessageCount()
//            refreshMessages()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun pinMessages() {
        val selectedMessages = adapter.selectedMessages
        val selectedIds = selectedMessages.map { it.threadId }
        val alreadyPinned = viewModel.getPinnedThreadIds() ?: emptyList()
        val newPins = selectedMessages.filter { !it.isPinned }

        if (alreadyPinned.size + newPins.size > 10) {
            Toast.makeText(
                requireContext(),
                getString(R.string.pin_limit_reached),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewModel.togglePin(selectedIds) {
            pinnedThreadIds = viewModel.getPinnedThreadIds() ?: emptyList()
            val updatedPinnedThreadIds = pinnedThreadIds.toMutableSet()
            adapter.clearSelection()
            val updatedList = adapter.messages.map {
                it.copy(
                    isPinned = updatedPinnedThreadIds.contains(it.threadId)
                )
            }
            val pinIndexes = updatedList.mapIndexedNotNull { index, message ->
                if (message.isPinned) index else null
            }
//            pinIndexes.forEach {
//                adapter.notifyItemChanged(it)
//            }
            adapter.submitList(updatedList)
        }
    }

      @RequiresApi(Build.VERSION_CODES.Q)
      fun MuteUnmuteMessages() {
          val selectedIds = adapter.selectedMessages.map { it.threadId }
          adapter.selectedMessages.filter { it.isMuted }.map { it.threadId }
          viewModel.toggleMute(selectedIds) {
              mutedThreadIds = viewModel.getMutedThreadIds() ?: emptyList()
              val updatedmutedThreadIds = mutedThreadIds.toMutableSet()
              adapter.clearSelection()
              val updatedList = adapter.messages.map {
                  it.copy(
                      isMuted = updatedmutedThreadIds.contains(it.threadId)
                  )
              }
              val muteIndexes = updatedList.mapIndexedNotNull { index, message ->
                  if (message.isMuted) index else null
              }
              muteIndexes.forEach {
                  adapter.notifyItemChanged(it)
              }
              adapter.submitList(updatedList)
              Handler(Looper.getMainLooper()).postDelayed({
                  adapter.notifyDataSetChanged()
              }, 500)

          }

      }


    /* @RequiresApi(Build.VERSION_CODES.Q)
     fun markReadMessages() {

         CoroutineScope(Dispatchers.IO).launch {
             val updatedList = adapter.getAllMessages().toMutableList()
             val unreadMessages = updatedList.filter { !it.isRead }
             if (unreadMessages.isEmpty()) return@launch
             val unreadThreadIds = unreadMessages.map { it.threadId }.distinct()

             val contentValues = ContentValues().apply {
                 put(Telephony.Sms.READ, 1)
 //                put(Telephony.Sms.SEEN, 1)
             }
             val updatedSubset = unreadMessages.map { it.copy(isRead = true) }

             withContext(Dispatchers.Main) {
                 adapter.updateSubset(updatedSubset)
                 unreadMessageCount()
                 Handler(Looper.getMainLooper()).postDelayed({
                     adapter.notifyDataSetChanged()
                 }, 100)
             }
             val selection = "${Telephony.Sms.THREAD_ID} IN (${unreadThreadIds.joinToString(",")})"
             requireContext().contentResolver.update(
                 Telephony.Sms.CONTENT_URI, contentValues, null, null
             )
         }
     }*/

    @RequiresApi(Build.VERSION_CODES.Q)
    fun markReadMessages() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_progress, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvProgressMessage)
        tvMessage.text = getString(R.string.marking_messages_as_read)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.apply {
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog.show()

        val startTime = System.currentTimeMillis()


        CoroutineScope(Dispatchers.IO).launch {
            val updatedList = adapter.getAllMessages().toMutableList()
            val unreadMessages = updatedList.filter { !it.isRead }
            if (unreadMessages.isEmpty()) {
                withContext(Dispatchers.Main) { dialog.dismiss() }
                return@launch
            }
            val unreadThreadIds = unreadMessages.map { it.threadId }.distinct()

            val contentValues = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
            }
            val updatedSubset = unreadMessages.map { it.copy(isRead = true) }

            withContext(Dispatchers.Main) {
                adapter.updateSubset(updatedSubset)
                unreadMessageCount()
                Handler(Looper.getMainLooper()).postDelayed({
                    adapter.notifyDataSetChanged()
                }, 100)
            }
            val selection =
                "${Telephony.Sms.THREAD_ID} IN (${unreadThreadIds.joinToString(GROUP_SEPARATOR)})"
            requireContext().contentResolver.update(
                Telephony.Sms.CONTENT_URI, contentValues, selection, null
            )

            val elapsed = System.currentTimeMillis() - startTime
            val remainingTime = 1000 - elapsed
            if (remainingTime > 0) delay(remainingTime)

            withContext(Dispatchers.Main) {
                dialog.dismiss()
            }
        }
    }

    private fun updateMessageList(threadId: Long, isRead: Boolean) {
        val position = adapter.getPositionForThreadId(threadId)
        if (position != RecyclerView.NO_POSITION) {
            adapter.updateThreadStatus(threadId, isRead)
            Handler(Looper.getMainLooper()).post {
                adapter.notifyItemChanged(position)
                adapter.notifyDataSetChanged()
                unreadMessageCount()
            }
        }
    }

    private fun markThreadAsUnread(threadId: Long) {
        if (threadId == -1L) return
        CoroutineScope(Dispatchers.IO).launch {
            val contentValues = ContentValues().apply {
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.SEEN, 0)
            }
            val uri = Telephony.Sms.CONTENT_URI
            val selection = "${Telephony.Sms.THREAD_ID} = ?"
            val selectionArgs = arrayOf(threadId.toString())
            val updatedRows = requireContext().contentResolver.update(
                uri,
                contentValues,
                selection,
                selectionArgs
            )

        }
    }

    private fun markThreadAsread(threadId: Long) {
        if (threadId == -1L) return
        CoroutineScope(Dispatchers.IO).launch {
            val contentValues = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }
            val uri = Telephony.Sms.CONTENT_URI
            val selection = "${Telephony.Sms.THREAD_ID} = ?"
            val selectionArgs = arrayOf(threadId.toString())
            val updatedRows =
                requireContext().contentResolver.update(
                    uri,
                    contentValues,
                    selection,
                    selectionArgs
                )
        }


    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun markUnreadMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val updatedList = adapter.getAllMessages().toMutableList()
            val unreadMessages = updatedList.filter { it.isRead }
            if (unreadMessages.isEmpty()) return@launch
            val unreadThreadIds = unreadMessages.map { it.threadId }.distinct()
            val contentValues = ContentValues().apply {
                put(Telephony.Sms.READ, 0)
            }
            val selection =
                "${Telephony.Sms.THREAD_ID} IN (${unreadThreadIds.joinToString(GROUP_SEPARATOR)})"
            val updatedRows = requireContext().contentResolver.update(
                Telephony.Sms.CONTENT_URI, contentValues, selection, null
            )
            if (updatedRows > 0) {
                updatedList.replaceAll { message ->
                    if (unreadThreadIds.contains(message.threadId)) message.copy(isRead = true) else message
                }
                withContext(Dispatchers.Main) {
                    adapter.submitList(updatedList)

                    Handler(Looper.getMainLooper()).postDelayed({
                        adapter.notifyDataSetChanged()
                    }, 200)
                }
            }
        }
    }

}