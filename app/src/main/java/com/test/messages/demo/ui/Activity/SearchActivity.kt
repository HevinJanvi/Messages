package com.test.messages.demo.ui.Activity

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.Util.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.Constants.FROMSEARCH
import com.test.messages.demo.Util.Constants.GROUP_SEPARATOR
import com.test.messages.demo.Util.Constants.ISGROUP
import com.test.messages.demo.Util.Constants.NAME
import com.test.messages.demo.Util.Constants.NUMBER
import com.test.messages.demo.Util.Constants.QUERY
import com.test.messages.demo.Util.ConversationUpdatedEvent
import com.test.messages.demo.Util.DeleteSearchMessageEvent
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.ViewUtils.removeCountryCode
import com.test.messages.demo.data.Model.ContactItem
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.data.Model.SearchableContact
import com.test.messages.demo.data.Model.SearchableMessage
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.databinding.ActivitySearchBinding
import com.test.messages.demo.ui.Adapter.SearchContactAdapter
import com.test.messages.demo.ui.Adapter.SearchMessageAdapter
import com.test.messages.demo.ui.send.getThreadId
import com.test.messages.demo.ui.send.hasReadContactsPermission
import com.test.messages.demo.ui.send.hasReadSmsPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


@AndroidEntryPoint
class SearchActivity : BaseActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var adapter: SearchMessageAdapter
    private var messageList = mutableListOf<ConversationItem>()
    private var contactList = mutableListOf<ContactItem>()
    private lateinit var contactAdapter: SearchContactAdapter
    private var searchJob: Job? = null
    private var isMessageExpanded = false
    private var isContactExpanded = false
    private var lastSearchQuery: String = ""

    private val allMessages = mutableListOf<ConversationItem>()
    private val allContacts = mutableListOf<ContactItem>()
    private var indexedContacts = listOf<SearchableContact>()
    private var indexedMessages = listOf<SearchableMessage>()
    private lateinit var view: View

    fun buildContactIndex(contacts: List<ContactItem>) {
        lifecycleScope.launch(Dispatchers.Default) {
            val newList = contacts.map {
                val searchable = "${it.name ?: ""} ${it.phoneNumber}".lowercase()
                SearchableContact(it, searchable)
            }

            withContext(Dispatchers.Main) {
                indexedContacts = newList
            }
        }
    }

    private val indexMutex = Mutex()

    fun buildSearchIndex(messages: List<ConversationItem>) {
        val indexed = messages.map {
            val searchable = "${it.body} ${it.address}".lowercase()
            SearchableMessage(it, searchable)
        }
        CoroutineScope(Dispatchers.Default).launch {
            indexMutex.withLock {
                indexedMessages = indexed
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageDeleted(event: DeleteSearchMessageEvent) {
        fetchInitialData()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConversationUpdated(event: ConversationUpdatedEvent) {
        clearFilters()
        fetchInitialData()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this) && hasReadSmsPermission() && hasReadContactsPermission()) {
            return
        }

        if (lastSearchQuery.length > 1) {
            performSearch(lastSearchQuery)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        view = binding.getRoot()
        setContentView(binding.root)

        applyWindowInsetsToView(binding.rootView)
        EventBus.getDefault().register(this)
        setupRecyclerViews()
        setupObservers()
        fetchInitialData()

        binding.icBack.setOnClickListener { finish() }
        binding.btnclose.setOnClickListener {
            binding.searchInput.text.clear()
            clearFilters()
        }

        binding.seeAllMessages.setOnClickListener {
            isMessageExpanded = !isMessageExpanded
            isContactExpanded = false
            adapter.updateList(
                viewModel.Filetrmessages.value ?: emptyList(),
                binding.searchInput.text.toString(),
                emptyMap()
            )
            binding.seeAllMessages.visibility = View.GONE

            binding.recyclerViewContacts.visibility = View.GONE
            binding.contactsHeader.visibility = View.GONE
            binding.seeAllContacts.visibility = View.GONE
        }

        binding.seeAllContacts.setOnClickListener {
            isContactExpanded = !isContactExpanded
            isMessageExpanded = false
            contactAdapter.updateList(
                viewModel.filteredContacts.value ?: emptyList(),
                binding.searchInput.text.toString()
            )
            binding.seeAllContacts.visibility = View.GONE
            binding.recyclerViewMessages.visibility = View.GONE
            binding.messagesHeader.visibility = View.GONE
            binding.seeAllMessages.visibility = View.GONE
        }


        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        adapter = SearchMessageAdapter(messageList) { message, searchedText ->
            val isGroup = message.address?.contains(GROUP_SEPARATOR) == true
            val rawAddress = message.address ?: ""

            val number = if (isGroup) {
                val names = rawAddress.split(GROUP_SEPARATOR).map { it.trim() }
                val numbers = names.map { name ->
                    viewModel.getContactNumber(this, name) ?: name
                }
                numbers.joinToString(GROUP_SEPARATOR)
            } else {
                viewModel.getContactNumber(this, rawAddress)
            }
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, message.threadId)
                putExtra(NUMBER, number)
                putExtra(NAME, message.address)
                putExtra(QUERY, searchedText)
                putExtra(FROMSEARCH, true)
                putExtra(ISGROUP, isGroup)
            }
            startActivity(intent)
        }
        binding.recyclerViewMessages.adapter = adapter
        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        contactAdapter = SearchContactAdapter(contactList, "") { contact ->
            val threadId = getThreadId(setOf(contact.phoneNumber))
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, threadId)
                putExtra(NUMBER, contact.phoneNumber)
                putExtra(NAME, contact.name)
                putExtra(FROMSEARCH, true)
            }
            startActivity(intent)
        }
        binding.recyclerViewContacts.adapter = contactAdapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                lastSearchQuery = query
                performSearch(query)
            }
        })

        binding.btnclose.setOnClickListener {
            binding.searchInput.text.clear()
            clearFilters()
        }


        binding.icBack.setOnClickListener {
            binding.searchInput.text.clear()
            onBackPressed()
        }

        binding.recyclerViewMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    view.hideKeyboard(this@SearchActivity)
                }
            }
        })

        binding.recyclerViewContacts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    view.hideKeyboard(this@SearchActivity)
                }
            }
        })

    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupRecyclerViews() {
        adapter = SearchMessageAdapter(messageList) { message, searchedText ->
            val isGroup = message.address?.contains(GROUP_SEPARATOR) == true
            val rawAddress = message.address ?: ""

            val number = if (isGroup) {
                val names = rawAddress.split(GROUP_SEPARATOR).map { it.trim() }
                val numbers = names.map { name ->
                    viewModel.getContactNumber(this, name) ?: name
                }
                numbers.joinToString(GROUP_SEPARATOR)
            } else {
                viewModel.getContactNumber(this, rawAddress)
            }
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, message.threadId)
                putExtra(NUMBER, number)
                putExtra(NAME, message.address)
                putExtra(QUERY, searchedText)
                putExtra(FROMSEARCH, true)
                putExtra(ISGROUP, isGroup)
            }
            startActivity(intent)
        }

        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMessages.adapter = adapter

        contactAdapter = SearchContactAdapter(
            contacts = emptyList(),
            query = "",
            onItemClick = { contact ->
            }
        )
        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewContacts.adapter = contactAdapter
    }

    fun updateAdapterList() {
        lifecycleScope.launch {
            val query = binding.searchInput.text.toString()
            val shouldExpandMessages = isMessageExpanded || contactList.isEmpty()

            if (!shouldExpandMessages) {
                val visibleMessagesData = withContext(Dispatchers.Default) {
                    val grouped = messageList.groupBy { it.threadId }
                    val matchCountMap = grouped.mapValues { (_, msgs) ->
                        msgs.count { it.body.contains(query, ignoreCase = true) }
                    }

                    val topMessages = grouped.map { it.value.first() }
                    val visibleMessages = topMessages.take(3)

                    Triple(visibleMessages, query, matchCountMap)
                }

                val (visibleMessages, matchCountMap, totalThreadCount) = visibleMessagesData


                withContext(Dispatchers.Main) {
                    adapter.updateList(visibleMessages, query, mapOf())

                    binding.recyclerViewMessages.visibility =
                        if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.messagesHeader.visibility =
                        if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.seeAllMessages.visibility =
                        if (messageList.size > 3) View.VISIBLE else View.GONE
                }

            }

            if (!isContactExpanded) {
                val visibleContacts = withContext(Dispatchers.Default) {
                    val grouped = contactList.groupBy { it.phoneNumber }
                    val topContacts = grouped.map { it.value.first() }
                    topContacts.take(3)
                }

                withContext(Dispatchers.Main) {
                    contactAdapter.updateList(visibleContacts, query)

                    binding.recyclerViewContacts.visibility =
                        if (visibleContacts.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.contactsHeader.visibility =
                        if (visibleContacts.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.seeAllContacts.visibility =
                        if (contactList.size > 3) View.VISIBLE else View.GONE
                }
            }
        }
    }

    var safeMessages: List<SearchableMessage> = indexedMessages.toList()
    var safeContacts: List<SearchableContact> = indexedContacts.toList()

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun performSearch(query: String) {
        searchJob?.cancel()

        runOnUiThread {
            binding.textNoResults.visibility = View.GONE
            binding.emptyList.visibility = View.GONE
        }

        searchJob = CoroutineScope(Dispatchers.Default).launch {
            if (query.isNotEmpty() && query.length > 1) {
                val lowerQuery = query.lowercase()
                indexMutex.withLock {
                    safeMessages = ArrayList(indexedMessages)
                    safeContacts = ArrayList(indexedContacts)
                }

                val contactMap = safeContacts.associate { it.original.phoneNumber to it.original.name }

                val filteredMessages = safeMessages
                    .asSequence()
                    .mapNotNull { item ->
                        val rawAddress = item.original.address ?: return@mapNotNull null

                        val messageBodyLower = item.original.body?.lowercase() ?: ""
                        val contactName = rawAddress.split(GROUP_SEPARATOR)
                            .joinToString(GROUP_SEPARATOR) { number ->
                                val cleanNumber = number.trim()
                                contactMap[cleanNumber] ?: cleanNumber
                            }

                        val contactNameLower = contactName.lowercase()
                        return@mapNotNull if (
                            contactNameLower.contains(lowerQuery) ||
                            messageBodyLower.contains(lowerQuery)
                        ) {
                            item.original.copy(address = contactName)
                        } else {
                            null
                        }
                    }
                    .groupBy { it.threadId }
                    .mapValues { it.value.maxByOrNull { msg -> msg.date } }
                    .values
                    .filterNotNull()
                    .sortedByDescending { it.date }
                    .toList()


                val filteredContacts = safeContacts
                    .asSequence()
                    .filter { it.searchText.contains(lowerQuery) }
                    .map { it.original }
                    .toList()

                withContext(Dispatchers.Main) {
                    messageList.clear()
                    contactList.clear()

                    messageList.addAll(filteredMessages)
                    contactList.addAll(filteredContacts)

                    viewModel.setFilteredMessages(filteredMessages)
                    viewModel.setFilteredContacts(filteredContacts)

                    if (filteredContacts.isEmpty()) {
                        isMessageExpanded = true
                        isContactExpanded = false
                    } else {
                        isMessageExpanded = false
                    }
                    updateAdapterList()
                    binding.btnclose.visibility = View.VISIBLE
                    val hasNoResults = filteredMessages.isEmpty() && filteredContacts.isEmpty()
                    binding.textNoResults.visibility = if (hasNoResults) View.VISIBLE else View.GONE
                }

            } else {
                withContext(Dispatchers.Main) {
                    clearFilters()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupObservers() {
        viewModel.Filetrmessages.observe(this) { messages ->
            val limited =
                if (!isMessageExpanded && messages.size > 3) messages.take(3) else messages
            adapter.updateList(limited, binding.searchInput.text.toString(), emptyMap())

            binding.recyclerViewMessages.visibility =
                if (limited.isNotEmpty()) View.VISIBLE else View.GONE
            binding.messagesHeader.visibility =
                if (limited.isNotEmpty()) View.VISIBLE else View.GONE
            binding.seeAllMessages.visibility =
                if (!isMessageExpanded && messages.size > 3) View.VISIBLE else View.GONE
        }

        viewModel.filteredContacts.observe(this) { contacts ->

            val limited =
                if (!isContactExpanded && contacts.size > 3) contacts.take(3) else contacts
            contactAdapter.updateList(limited, binding.searchInput.text.toString())
            binding.recyclerViewContacts.visibility =
                if (limited.isNotEmpty()) View.VISIBLE else View.GONE
            binding.contactsHeader.visibility =
                if (limited.isNotEmpty()) View.VISIBLE else View.GONE
            binding.seeAllContacts.visibility =
                if (!isContactExpanded && contacts.size > 3) View.VISIBLE else View.GONE
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun fetchInitialData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val messages = viewModel.getAllMessages(this@SearchActivity)
            val contacts = viewModel.getAllContacts(this@SearchActivity)

            withContext(Dispatchers.Main) {
                allMessages.clear()
                allMessages.addAll(messages)

                allContacts.clear()
                allContacts.addAll(contacts)
                buildSearchIndex(allMessages)
                buildContactIndex(allContacts)
                if (lastSearchQuery.length > 1) {
                    performSearch(lastSearchQuery)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun clearFilters() {
        binding.btnclose.visibility = View.GONE
        binding.emptyList.visibility = View.VISIBLE

        messageList.clear()
        contactList.clear()


        adapter.updateList(emptyList(), "", emptyMap())
        contactAdapter.updateList(emptyList(), "")

        if (Looper.myLooper() == Looper.getMainLooper()) {
            viewModel.setFilteredMessages(emptyList())
            viewModel.setFilteredContacts(emptyList())
        } else {

            viewModel.setFilteredMessages(emptyList())
            viewModel.setFilteredContacts(emptyList())
        }
        isMessageExpanded = false
        isContactExpanded = false

        binding.recyclerViewMessages.visibility = View.GONE
        binding.messagesHeader.visibility = View.GONE
        binding.seeAllMessages.visibility = View.GONE

        binding.recyclerViewContacts.visibility = View.GONE
        binding.contactsHeader.visibility = View.GONE
        binding.seeAllContacts.visibility = View.GONE
    }

    private fun closeKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun isKeyboardOpen(): Boolean {
        val rootView = window.decorView.rootView
        val rect = Rect()
        rootView.getWindowVisibleDisplayFrame(rect)
        val screenHeight = rootView.height
        val keypadHeight = screenHeight - rect.bottom
        return keypadHeight > screenHeight * 0.15
    }

    override fun onBackPressed() {
        if (binding.searchInput.text.isNotEmpty()) {
            binding.searchInput.text.clear()
            binding.btnclose.visibility = View.GONE
            binding.messagesHeader.visibility = View.GONE
            binding.contactsHeader.visibility = View.GONE
            binding.emptyList.visibility = View.VISIBLE
        } else if (isKeyboardOpen()) {
            closeKeyboard()
        } else {
            super.onBackPressed()
        }
    }


}