package com.test.messages.demo.ui.Fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.ISGROUP
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.CommanConstants.PROFILEURL
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
import com.test.messages.demo.Util.DraftChangedEvent
import com.test.messages.demo.Util.MarkasreadEvent
import com.test.messages.demo.Util.ViewUtils.autoScrollToStart
import com.test.messages.demo.ui.Dialogs.DeleteProgressDialog
import com.test.messages.demo.ui.send.hasReadSmsPermission
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)
class ConversationFragment : Fragment() {

    private var leftSwipeAction: Int? = null
    private var rightSwipeAction: Int? = null
    private var blockConversationIds: List<Long> = emptyList()
    private var archivedConversationIds: List<Long> = emptyList()
    private var pinnedThreadIds: List<Long> = emptyList()
    val viewModel: MessageViewModel by viewModels()
    private val draftViewModel: DraftViewModel by viewModels()
    private lateinit var adapter: MessageAdapter
    private lateinit var binding: FragmentConversationBinding
    var onSelectionChanged: ((Int, Int) -> Unit)? = null
    private var blockedNumbers: List<String> = emptyList()
    private var unreadMessageListener: UnreadMessageListener? = null
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var selectedCategory: String
    private var categories: MutableList<String> = mutableListOf()

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

        binding.categoryRecyclerView.visibility =
            if (isCategoryEnabled(requireContext())) View.VISIBLE else View.GONE

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
        viewModel.loadMessages()

        /*   viewModel.messages.observe(viewLifecycleOwner) { messageList ->
               Log.d("ConversationFragment", "Total Messages Before Filtering: ${messageList.size}")
               (activity as? MainActivity)?.updateTotalMessagesCount(messageList.size)

               CoroutineScope(Dispatchers.IO).launch {
                   blockedNumbers = viewModel.getBlockedNumbers()
                   archivedConversationIds =
                       viewModel.getArchivedConversations().map { it.conversationId }
                   blockConversationIds = viewModel.getBlockedConversations().map { it.conversationId }
                   pinnedThreadIds = viewModel.getPinnedThreadIds() ?: emptyList()
                   val mutedThreads = viewModel.getMutedThreadIds()

                   val filteredMessages = messageList
                       .filter { it.number !in blockedNumbers }
                       .filter { it.threadId !in archivedConversationIds }
                       .filter { it.threadId !in blockConversationIds }
                       .map { message ->
                           message.copy(
                               isPinned = pinnedThreadIds.contains(message.threadId),
                               isMuted = mutedThreads.contains(message.threadId)
                           )
                       }
                       .filter { filterByCategory(it, selectedCategory) }
                       .sortedByDescending { it.isPinned }
                   *//*Log.d(
                    "ConversationFragment",
                    "Messages After Filtering (${selectedCategory}): ${filteredMessages.size}"
                )*//*

//                val unreadMessagesCount = filteredMessages.count { !it.isRead }
                val unreadMessagesCount = filteredMessages.count {
                    !it.isRead && filterByCategory(it, selectedCategory)
                }
                withContext(Dispatchers.Main) {
                    unreadMessageListener?.onUnreadMessagesCountUpdated(unreadMessagesCount)
                    adapter.submitList(filteredMessages)
                }
            }
        }*/
        viewModel.messages.observe(viewLifecycleOwner) { messageList ->
            (activity as? MainActivity)?.updateTotalMessagesCount(messageList.size)
            CoroutineScope(Dispatchers.IO).launch {
                val filteredMessages = prepareFilteredMessages(messageList)
                val unreadMessagesCount = filteredMessages.count { !it.isRead }
                val finalSortedList = filteredMessages.sortedWith(
                    compareByDescending<MessageItem> { it.isPinned }
                        .thenByDescending { it.timestamp }
                )
                withContext(Dispatchers.Main) {
                    unreadMessageListener?.onUnreadMessagesCountUpdated(unreadMessagesCount)
                    adapter.submitList(finalSortedList)
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

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if(requireContext().hasReadSmsPermission()){
                val threadIds = fetchAllThreadIds()
                viewModel.insertMissingThreadIds(threadIds)
            }

        }
        return binding.getRoot();
    }

    private suspend fun prepareFilteredMessages(messageList: List<MessageItem>): List<MessageItem> {
        blockedNumbers = viewModel.getBlockedNumbers()
        archivedConversationIds = viewModel.getArchivedConversations().map { it.conversationId }
        blockConversationIds = viewModel.getBlockedConversations().map { it.conversationId }
        pinnedThreadIds = viewModel.getPinnedThreadIds() ?: emptyList()
        val mutedThreads = viewModel.getMutedThreadIds()

        return messageList.map { message ->
            val isPinnedThread = pinnedThreadIds.contains(message.threadId)
            message.copy(
                isPinned = isPinnedThread,
                isMuted = mutedThreads.contains(message.threadId)
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
                (message.number.length == 10 && message.number.all { it.isDigit() })
        return when (category) {
            getString(R.string.inbox) -> true
            getString(R.string.personal) -> {
                message.number.startsWith("+") || (message.number.length == 10 && message.number.all { it.isDigit() })
            }

            getString(R.string.transactions) -> {
                val body = message.body.lowercase()
                body.contains("debited") ||
                        body.contains("credited") ||
                        body.contains("debit") ||
                        body.contains("credit")
            }
            getString(R.string.otps) -> {
                !isPersonal && message.body.contains("OTP", ignoreCase = true)
            }
            getString(R.string.offers) -> message.body.contains("offer", ignoreCase = true)
            else -> true
        }
    }

    private fun applyCategoryFilter(messageList: List<MessageItem>) {
        CoroutineScope(Dispatchers.IO).launch {
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

                onSelectionChanged?.invoke(count, pinnedCount)
            }
        )
        adapter.autoScrollToStart(binding.conversationList) { }
        binding.conversationList.itemAnimator = null
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
            conversationResultLauncher.launch(intent)
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
            val context = requireContext()
            val leftAction = ViewUtils.getSwipeAction(context, isRightSwipe = false)
            val rightAction = ViewUtils.getSwipeAction(context, isRightSwipe = true)

            val leftEnabled = leftAction != CommanConstants.SWIPE_NONE
            val rightEnabled = rightAction != CommanConstants.SWIPE_NONE

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

            // Apply Right Swipe (dX > 0)
            if (dX > 0 && rightSwipeIcon != null) {
                decorator.addSwipeRightBackgroundColor(colorPrimary)
                    .addSwipeRightActionIcon(rightSwipeIcon)
                    .setSwipeRightActionIconTint(whiteColor)
            }

            // Apply Left Swipe (dX < 0)
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
                CommanConstants.SWIPE_DELETE -> R.drawable.ic_delete
                CommanConstants.SWIPE_ARCHIVE -> R.drawable.ic_archive
                CommanConstants.SWIPE_CALL -> R.drawable.ic_call
                CommanConstants.SWIPE_MARK_READ -> R.drawable.ic_mark_read2
                CommanConstants.SWIPE_MARK_UNREAD -> R.drawable.ic_mark_read1
                else -> null
            }
        }

        private fun handleSwipeAction(action: Int, position: Int, message: MessageItem) {
            when (action) {
                CommanConstants.SWIPE_DELETE -> {
                    adapter.selectedMessages.add(message)
                    deleteSelectedMessages()
                }

                CommanConstants.SWIPE_ARCHIVE -> {
                    adapter.selectedMessages.clear()
                    adapter.selectedMessages.add(message)
                    archiveMessages()
                }

                CommanConstants.SWIPE_CALL -> {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${message.sender}")
                    }
                    adapter.notifyDataSetChanged()
                    startActivity(intent)
                    adapter.notifyItemChanged(position)
                }

                CommanConstants.SWIPE_MARK_READ -> {
                    adapter.selectedMessages.clear()
                    markThreadAsread(message.threadId)
                }

                CommanConstants.SWIPE_MARK_UNREAD -> {
                    adapter.selectedMessages.clear()
                    markThreadAsUnread(message.threadId)
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
            adapter.selectedMessages.clear()
            val allMessages = adapter.getAllMessages()
            adapter.selectedMessages.addAll(allMessages)
        } else {
            adapter.selectedMessages.clear()
        }
        onSelectionChanged?.invoke(
            adapter.selectedMessages.size,
            adapter.selectedMessages.count { it.isPinned })
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCategoryVisibilityEvent(event: CategoryVisibilityEvent) {
        binding.categoryRecyclerView.visibility = if (event.isEnabled) View.VISIBLE else View.GONE
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
            Log.d("TAG", "onMessagesRestored: ")
            adapter.notifyDataSetChanged()
            viewModel.loadMessages()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMarkadread(event: MarkasreadEvent) {
        if (event.success) {
            markReadMessages()
            adapter.notifyDataSetChanged()
            viewModel.loadMessages()
        }
    }

    fun getLastMessageForThread(threadId: Long): Pair<String?, Long?> {
        val uri = Uri.parse("content://sms/conversations?simple=true")
        val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE)
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        val cursor = requireContext().contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                Log.d("SnippetUpdate", "Fetched LAST message: $body at $date")
                return Pair(body, date)
            } else {
                Log.d("SnippetUpdate", "No message found for threadId $threadId")
            }
        }

        return Pair(null, null)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageDeleted(event: MessageDeletedEvent) {
        Log.d("TAG", "onMessageDeleted called: threadId=${event.threadId}")
//        val newLastMessage = getLastMessageForThread(event.threadId)
//        updateConversationSnippet(event.threadId, newLastMessage)
        val (newLastMessage, newLastMessageTime) = getLastMessageForThread(event.threadId)
        updateConversationSnippet(event.threadId, newLastMessage, newLastMessageTime)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageRestored(event: MessageRestoredEvent) {
        Log.d(
            "SnippetUpdate",
            "Event received: MessageRestoredEvent -> ThreadId: ${event.threadId}, LastMessage: ${event.lastMessage}, LastMessageTime: ${event.lastMessageTime}"
        )

        val (newLastMessage, newLastMessageTime) = getLastMessageForThread(event.threadId)
        updateConversationSnippet(event.threadId, newLastMessage, newLastMessageTime)
//        updateConversationSnippet(event.threadId, event.lastMessage,event.lastMessageTime)
    }


    fun updateConversationSnippet(threadId: Long, lastMessage: String?, lastMessageTime: Long?) {

        adapter.notifyDataSetChanged()
        val currentList = adapter.getAllMessages().toMutableList()
//        Log.d("SnippetUpdate", "Current messages count: ${currentList.size}")

        val index = currentList.indexOfFirst { it.threadId == threadId }
        Log.d("SnippetUpdate", "Searching for threadId: $threadId -> Found at index: $index")

        if (index != -1) {
            val updatedItem = currentList[index].copy(
                body = lastMessage ?: "",
                timestamp = lastMessageTime ?: System.currentTimeMillis()
            )
            currentList[index] = updatedItem
            Log.d(
                "SnippetUpdate",
                "Updating snippet -> New Body: ${updatedItem.body}, New Timestamp: ${updatedItem.timestamp}"
            )
            adapter.submitList(currentList)

        } else {
            Log.d(
                "SnippetUpdate",
                "ThreadId $threadId not found in adapter list. No update performed."
            )
        }
    }


    private fun fetchAllThreadIds(): List<Long> {
        val threadIds = mutableListOf<Long>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.THREAD_ID)

        requireContext().contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val threadIdIndex = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(threadIdIndex)
                if (threadId != -1L) {
                    threadIds.add(threadId)
                }
            }
        }
        return threadIds.distinct()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDraftRemovedEvent(event: DraftChangedEvent) {
        Log.d("TAG", "onDraftRemovedEvent: ")
        adapter.notifyDataSetChanged()

        viewModel.loadMessages()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.conversationList.smoothScrollToPosition(0)
        }, 1000)

    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private val conversationResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null) {

                adapter.notifyDataSetChanged()
                draftViewModel.loadAllDrafts()
                viewModel.loadMessages()
//                Handler(Looper.getMainLooper()).postDelayed({
//                    binding.conversationList.scrollToPosition(0)
//                }, 500)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteSelectedMessages() {
        if (adapter.selectedMessages.isEmpty()) return

        val contentResolver = requireActivity().contentResolver
        val db = AppDatabase.getDatabase(requireContext()).recycleBinDao()
        val updatedList = viewModel.messages.value?.toMutableList() ?: mutableListOf()
        val handler = Handler(Looper.getMainLooper())
        val deleteDialog = DeleteProgressDialog(requireContext())

        val showDialogRunnable = Runnable {
            deleteDialog.show(getString(R.string.moving_messages_to_recycle_bin))
        }
        handler.postDelayed(showDialogRunnable, 300)


        Thread {
            try {
                val deletedMessages = mutableListOf<DeletedMessage>()
                val existingBodyDatePairs =
                    mutableSetOf<Pair<String, Long>>()

                for (item in adapter.selectedMessages) {
                    val threadId = item.threadId

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
                            val body = it.getString(bodyIndex) ?: ""
                            val date = it.getLong(dateIndex)
                            val key = Pair(body, date)
                            if (existingBodyDatePairs.contains(key)) {
                                continue
                            }
                            existingBodyDatePairs.add(key)
                            val isGroup = address.contains(",")
                            val deletedMessage = DeletedMessage(
                                messageId = messageId,
                                threadId = threadId,
                                address = it.getString(addressIndex) ?: "",
                                date = date,
                                body = body,
                                type = it.getInt(typeIndex),
                                read = it.getInt(readIndex) == 1,
                                subscriptionId = it.getInt(subIdIndex),
                                deletedTime = System.currentTimeMillis(),
                                isGroupChat = isGroup,
                                profileImageUrl = ""
                            )

                            deletedMessages.add(deletedMessage)
                        }
                    }
                    db.insertMessages(deletedMessages)
                    val uri = Uri.parse("content://sms/conversations/$threadId")
                    contentResolver.delete(uri, null, null)
                    updatedList.removeAll { it.threadId == threadId }
                }
                Handler(Looper.getMainLooper()).post {
                    handler.removeCallbacks(showDialogRunnable)
                    deleteDialog.dismiss()
                    adapter.selectedMessages.clear()
//                    adapter.submitList(updatedList)
                    viewModel.loadMessages()
                    onSelectionChanged?.invoke(0, 0)
                    updateMessageCount()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    handler.removeCallbacks(showDialogRunnable)
                    deleteDialog.dismiss()
                }
            }
        }.start()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun unarchiveMessages(messages: List<MessageItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.unarchiveConversations(messages.map { it.threadId })
            withContext(Dispatchers.Main) {
                val currentList = adapter.getAllMessages().toMutableList()
                val restoredList = (messages + currentList).distinctBy { it.threadId }
//                adapter.submitList(restoredList)
            }
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
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun pinMessages() {
        val selectedIds = adapter.selectedMessages.map { it.threadId }
        CoroutineScope(Dispatchers.IO).launch {
            val pinnedIds =
                selectedIds.filter { viewModel.isPinned(it) }
            val unpinnedIds =
                selectedIds.filterNot { viewModel.isPinned(it) }
            when {
                pinnedIds.isNotEmpty() && unpinnedIds.isNotEmpty() -> {
                    if (pinnedIds.size > unpinnedIds.size) {
                        viewModel.togglePin(pinnedIds)
                    }
                }

                pinnedIds.isNotEmpty() -> {
                    viewModel.togglePin(pinnedIds)
                }

                unpinnedIds.isNotEmpty() -> {
                    viewModel.togglePin(unpinnedIds)
                }
            }

            pinnedThreadIds = viewModel.getPinnedThreadIds() ?: emptyList()
            val updatedPinnedThreadIds = pinnedThreadIds.toMutableSet()

            selectedIds.forEach { id ->
                if (updatedPinnedThreadIds.contains(id)) {
                    // Unpin
                    updatedPinnedThreadIds.remove(id)
                } else {
                    // Pin
                    updatedPinnedThreadIds.add(id)
                }
            }
            Log.d("TAG", "pinMessages: ${updatedPinnedThreadIds.joinToString(",")}")
            withContext(Dispatchers.Main) {
                adapter.clearSelection()
                val updatedList = viewModel.messages.value?.map {
                    it.copy(isPinned = updatedPinnedThreadIds.contains(it.threadId))
                }?.sortedWith(
                    compareByDescending<MessageItem> { it.isPinned }
                        .thenByDescending { it.timestamp }
                ) ?: emptyList()
                val pinnedIndexes = updatedList.mapIndexedNotNull { index, message ->
                    if (message.isPinned) index else null
                }
                pinnedIndexes.forEach {
                    Log.d("TAG", "pinMessages:--" + it)
                    adapter.notifyItemChanged(it)
                }
                adapter.submitList(updatedList)


            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun muteMessages() {
        val selectedIds = adapter.selectedMessages.map { it.threadId }

        lifecycleScope.launch(Dispatchers.IO) {
            val notificationDao = AppDatabase.getDatabase(requireContext()).notificationDao()
            val mutedThreads = viewModel.getMutedThreadIds()
            val mutedCount = selectedIds.count { it in mutedThreads }
            val unmutedCount = selectedIds.size - mutedCount
            val shouldUnmute = mutedCount >= unmutedCount
            selectedIds.forEach { threadId ->
                notificationDao.updateNotificationStatus(threadId, !shouldUnmute)
            }

            val updatedMutedThreads = viewModel.getMutedThreadIds()

            val updatedMessages = adapter.getAllMessages().map { message ->
                message.copy(isMuted = updatedMutedThreads.contains(message.threadId))
            }

            withContext(Dispatchers.Main) {
                adapter.submitList(updatedMessages)
                adapter.notifyDataSetChanged()
                adapter.clearSelection()

                (activity as? MainActivity)?.updateMuteUnmuteUI(shouldUnmute)
            }
        }
    }

    /*@RequiresApi(Build.VERSION_CODES.Q)
    fun BlockMessages() {
        val selectedMessages = adapter.selectedMessages.toList()
        if (selectedMessages.isNotEmpty()) {
            viewModel.blockContacts(selectedMessages)
            adapter.clearSelection()
        }
    }*/

    @RequiresApi(Build.VERSION_CODES.Q)
    fun BlockMessages() {
        val selectedGroups = adapter.selectedMessages.filter { it.isGroupChat }
        if (selectedGroups.isNotEmpty()) {
            Log.d("BlockMessages", "Blocking not allowed for groups.")
            return
        }
        val selectedIds = adapter.selectedMessages.map { it.threadId }

        val blockDialog = BlockDialog(requireContext()) {
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.blockSelectedConversations(selectedIds)
                withContext(Dispatchers.Main) {
                    val updatedList = adapter.getAllMessages().toMutableList()
                    updatedList.removeAll { selectedIds.contains(it.threadId) }
                    adapter.submitList(updatedList)
                    adapter.clearSelection()
                    updateMessageCount()
                }
            }
        }
        blockDialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun markReadMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val updatedList = adapter.getAllMessages().toMutableList()
            val unreadMessages = updatedList.filter { !it.isRead }
            if (unreadMessages.isEmpty()) return@launch
            val unreadThreadIds = unreadMessages.map { it.threadId }.distinct()

            val contentValues = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
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
                Telephony.Sms.CONTENT_URI, contentValues, selection, null
            )
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
        val contentValues = ContentValues().apply {
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
        }
        val uri = Telephony.Sms.CONTENT_URI
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        val updatedRows =
            requireContext().contentResolver.update(uri, contentValues, selection, selectionArgs)
        updateMessageList(threadId, false)
    }

    private fun markThreadAsread(threadId: Long) {
        if (threadId == -1L) return
        val contentValues = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        val uri = Telephony.Sms.CONTENT_URI
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        val updatedRows =
            requireContext().contentResolver.update(uri, contentValues, selection, selectionArgs)
        updateMessageList(threadId, true)
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
            val selection = "${Telephony.Sms.THREAD_ID} IN (${unreadThreadIds.joinToString(",")})"
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