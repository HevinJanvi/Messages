package com.test.messages.demo.ui.Fragment

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
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
import com.test.messages.demo.Util.MessageDeletedEvent
import com.test.messages.demo.Util.SwipeActionEvent
import com.test.messages.demo.data.reciever.UnreadMessageListener
import com.test.messages.demo.data.viewmodel.DraftViewModel
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.annotations.Nullable


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

    override fun onAttach(context: Context) {
        super.onAttach(context)
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
        Log.d("ConversationFragment", "Updated Unread Messages: $unreadMessages")
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
        checkPermissionsAndLoadMessages()

        viewModel.messages.observe(viewLifecycleOwner) { messageList ->
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
                Log.d(
                    "ConversationFragment",
                    "Messages After Filtering (${selectedCategory}): ${filteredMessages.size}"
                )

                val unreadMessagesCount = filteredMessages.count { !it.isRead }

                withContext(Dispatchers.Main) {
                    unreadMessageListener?.onUnreadMessagesCountUpdated(unreadMessagesCount)
                    adapter.submitList(filteredMessages)
                }
            }
        }

        draftViewModel.draftsLiveData.observe(viewLifecycleOwner) { draftMap ->
            adapter.updateDrafts(draftMap)
        }
        draftViewModel.loadAllDrafts()

        binding.newConversation.setOnClickListener {
            val intent = Intent(requireContext(), NewConversationActivtiy::class.java)
            startActivity(intent)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val threadIds = fetchAllThreadIds()
            viewModel.insertMissingThreadIds(threadIds)
        }
        return binding.getRoot();
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupCategoryRecyclerView() {

        categoryAdapter = CategoryAdapter(requireContext(), categories) { category ->
            selectedCategory = category
            Log.d("ConversationFragment", "Selected Category: $category")
            viewModel.messages.value?.let { messageList ->
                applyCategoryFilter(messageList)
            }
        }

        binding.categoryRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.categoryRecyclerView.adapter = categoryAdapter
        binding.categoryRecyclerView.post {
            binding.categoryRecyclerView.scrollToPosition(0)
        }
    }

    private fun filterByCategory(message: MessageItem, category: String): Boolean {
        return when (category) {
            getString(R.string.inbox) -> true
            getString(R.string.personal) -> {
                message.number.startsWith("+") || (message.number.length == 10 && message.number.all { it.isDigit() })
            }

            getString(R.string.transactions) -> message.body.contains(
                "transaction",
                ignoreCase = true
            )

            getString(R.string.otps) -> message.body.contains("OTP", ignoreCase = true)
            getString(R.string.offers) -> message.body.contains("offer", ignoreCase = true)
            else -> true
        }
    }

    private fun applyCategoryFilter(messageList: List<MessageItem>) {
        val filteredList = messageList.filter { filterByCategory(it, selectedCategory) }
        binding.emptyList.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE

        adapter.submitList(filteredList)
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
                onSelectionChanged?.invoke(count, pinnedCount)
            }
        )
        binding.conversationList.itemAnimator = null
        binding.conversationList.layoutManager = LinearLayoutManager(requireActivity())
        binding.conversationList.adapter = adapter
        setupSwipeGesture()
        adapter.onItemClickListener = { message ->
            val intent = Intent(requireContext(), ConversationActivity::class.java).apply {
                putExtra("EXTRA_THREAD_ID", message.threadId)
                putExtra("NUMBER", message.number)
                putExtra("NAME", message.sender)
                putExtra("isGroup", message.isGroupChat)
                putExtra("ProfileUrl", message.profileImageUrl)
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

            val leftEnabled = leftAction !=  CommanConstants.SWIPE_NONE
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
                CommanConstants.SWIPE_DELETE  -> R.drawable.ic_delete
                CommanConstants.SWIPE_ARCHIVE -> R.drawable.ic_archive
                CommanConstants.SWIPE_CALL -> R.drawable.ic_call
                CommanConstants.SWIPE_MARK_READ -> R.drawable.ic_mark_read2
                CommanConstants.SWIPE_MARK_UNREAD -> R.drawable.ic_mark_read
                else -> null
            }
        }


        private fun handleSwipeAction(action: Int, position: Int, message: MessageItem) {
            when (action) {
                CommanConstants.SWIPE_DELETE  -> {
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

    fun getLastMessageForThread(threadId: Long): String? {
        val cursor = requireActivity().contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC LIMIT 1"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageDeleted(event: MessageDeletedEvent) {
        Log.d("TAG", "onMessageDeleted called: threadId=${event.threadId}")
        val newLastMessage = getLastMessageForThread(event.threadId)
        updateConversationSnippet(event.threadId, newLastMessage)
    }

    private fun updateConversationSnippet(threadId: Long, newLastMessage: String?) {
        val messages = adapter.getAllMessages().toMutableList()
        for (index in messages.indices) {
            if (messages[index].threadId == threadId) {
                messages[index] = messages[index].copy(body = newLastMessage ?: "")
                break
            }
        }
        adapter.submitList(messages)
        adapter.notifyDataSetChanged()
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
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteSelectedMessages() {
        if (adapter.selectedMessages.isEmpty()) return

        val threadIds = adapter.selectedMessages.map { it.threadId }.toSet()
        val contentResolver = requireActivity().contentResolver
        val updatedList = viewModel.messages.value?.toMutableList() ?: mutableListOf()

        Thread {
            try {
                for (threadId in threadIds) {
                    val uri = Uri.parse("content://sms/conversations/$threadId")
                    val deletedRows = contentResolver.delete(uri, null, null)

                    if (deletedRows > 0) {
                        updatedList.removeAll { it.threadId == threadId }
                    }
                }
                Handler(Looper.getMainLooper()).post {
                    adapter.selectedMessages.clear()
                    adapter.submitList(updatedList)
                    onSelectionChanged?.invoke(
                        adapter.selectedMessages.size,
                        adapter.selectedMessages.count { it.isPinned })
                    updateMessageCount()
                }

            } catch (e: Exception) {
            }
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun archiveMessages() {
        val selectedIds = adapter.selectedMessages.map { it.threadId }
        viewModel.archiveSelectedConversations(selectedIds)
        val updatedList = adapter.getAllMessages().toMutableList()
        updatedList.removeAll { selectedIds.contains(it.threadId) }
        adapter.submitList(updatedList)
        adapter.clearSelection()
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

            withContext(Dispatchers.Main) {
                adapter.clearSelection()
                Handler(Looper.getMainLooper()).postDelayed({
                    adapter.notifyDataSetChanged()
                }, 200)

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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionsAndLoadMessages() {
        val smsPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.READ_SMS
        )
        val contactsPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.READ_CONTACTS
        )

        if (smsPermission == PackageManager.PERMISSION_GRANTED && contactsPermission == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadMessages()
        } else {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_SMS,
                    android.Manifest.permission.READ_CONTACTS
                ),
                101
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            101 -> {
                val smsPermissionGranted =
                    grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
                val contactsPermissionGranted =
                    grantResults.getOrNull(1) == PackageManager.PERMISSION_GRANTED

                if (smsPermissionGranted && contactsPermissionGranted) {
                    viewModel.loadMessages()
                } else {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.permissions_denied), Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }

            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }


}