//package com.test.messages.demo.ui.Activity
//
//import android.content.Context
//import android.content.Intent
//import android.graphics.Rect
//import android.os.Build
//import android.os.Bundle
//import android.os.Looper
//import android.text.Editable
//import android.text.TextWatcher
//import android.util.Log
//import android.view.View
//import android.view.inputmethod.InputMethodManager
//import androidx.activity.viewModels
//import androidx.annotation.RequiresApi
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.test.messages.demo.R
//import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
//import com.test.messages.demo.Util.CommanConstants.FROMSEARCH
//import com.test.messages.demo.Util.CommanConstants.ISGROUP
//import com.test.messages.demo.Util.CommanConstants.NAME
//import com.test.messages.demo.Util.CommanConstants.NUMBER
//import com.test.messages.demo.Util.CommanConstants.QUERY
//import com.test.messages.demo.Util.DeleteSearchMessageEvent
//import com.test.messages.demo.data.Model.ContactItem
//import com.test.messages.demo.databinding.ActivitySearchBinding
//import com.test.messages.demo.ui.Adapter.SearchContactAdapter
//import com.test.messages.demo.ui.Adapter.SearchMessageAdapter
//import com.test.messages.demo.Util.SmsPermissionUtils
//import com.test.messages.demo.data.Model.ConversationItem
//import com.test.messages.demo.data.Model.MessageItem
//import com.test.messages.demo.data.Model.SearchableContact
//import com.test.messages.demo.data.Model.SearchableMessage
//import com.test.messages.demo.data.viewmodel.MessageViewModel
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.coroutineScope
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.ensureActive
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import org.greenrobot.eventbus.EventBus
//import org.greenrobot.eventbus.Subscribe
//import org.greenrobot.eventbus.ThreadMode
//
//@AndroidEntryPoint
//class SearchActivity000 : BaseActivity() {
//
//    private lateinit var binding: ActivitySearchBinding
//    private val viewModel: MessageViewModel by viewModels()
//    private lateinit var adapter: SearchMessageAdapter
//    private var messageList = mutableListOf<ConversationItem>()
//    private var threadId: String? = null
//    private var contactList = mutableListOf<ContactItem>()
//    private lateinit var contactAdapter: SearchContactAdapter
//    private var searchJob: Job? = null
//    private var isMessageExpanded = false
//    private var isContactExpanded = false
//
//    private val allMessages = mutableListOf<ConversationItem>()
//    private val allContacts = mutableListOf<ContactItem>()
//
//
//    private var indexedContacts = listOf<SearchableContact>()
//    private var indexedMessages = listOf<SearchableMessage>()
//
//    fun buildContactIndex(contacts: List<ContactItem>) {
//        indexedContacts = contacts.map {
//            val searchable = "${it.name ?: ""} ${it.phoneNumber}".lowercase()
//            SearchableContact(it, searchable)
//        }
//    }
//
//
//    fun buildSearchIndex(messages: List<ConversationItem>) {
//        indexedMessages = messages.map {
//            val searchable = "${it.body} ${it.address}".lowercase()  // Concatenate body and address
//            SearchableMessage(it, searchable)
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    fun onMessageDeleted(event: DeleteSearchMessageEvent) {
//        val originalList = viewModel.Filetrmessages.value ?: return
//        val updatedList = originalList.filterNot { it.id in event.deletedMessageIds }
//        viewModel.setFilteredMessages(updatedList)
//        updateAdapterList()
//    }
//
//
//    override fun onDestroy() {
//        super.onDestroy()
//        EventBus.getDefault().unregister(this)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivitySearchBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        EventBus.getDefault().register(this)
//
//        threadId = intent.getStringExtra("THREAD_ID")
//        setupRecyclerViews()
//        setupObservers()
//        fetchInitialData()
//
//        binding.icBack.setOnClickListener { finish() }
//        binding.btnclose.setOnClickListener {
//            binding.searchInput.text.clear()
//            clearFilters()
//        }
//
//        binding.seeAllMessages.setOnClickListener {
//            isMessageExpanded = !isMessageExpanded
//            isContactExpanded = false
//            adapter.updateList(
//                viewModel.Filetrmessages.value ?: emptyList(),
//                binding.searchInput.text.toString(),
//                emptyMap()
//            )
//            binding.seeAllMessages.visibility = View.GONE
//
//            binding.recyclerViewContacts.visibility = View.GONE
//            binding.contactsHeader.visibility = View.GONE
//            binding.seeAllContacts.visibility = View.GONE
//        }
//
//        binding.seeAllContacts.setOnClickListener {
//            isContactExpanded = !isContactExpanded
//            isMessageExpanded = false
//            contactAdapter.updateList(
//                viewModel.filteredContacts.value ?: emptyList(),
//                binding.searchInput.text.toString()
//            )
//            binding.seeAllContacts.visibility = View.GONE
//            binding.recyclerViewMessages.visibility = View.GONE
//            binding.messagesHeader.visibility = View.GONE
//            binding.seeAllMessages.visibility = View.GONE
//        }
//
//
//        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this)
//        adapter = SearchMessageAdapter(messageList) { message, searchedText ->
//            val isGroup = message.address?.contains(",") == true
//            val number = viewModel.getContactNumber(this, message.address ?: "")
//            val intent = Intent(this, ConversationActivity::class.java).apply {
//                putExtra(EXTRA_THREAD_ID, message.threadId)
//                putExtra(NUMBER, number)
//                putExtra(NAME, message.address)
//                putExtra(QUERY, searchedText)
//                putExtra(FROMSEARCH, true)
//                putExtra(ISGROUP, isGroup
//                )
//            }
//            Log.d("TAG", "onCreate:message.address "+message.address)
//            startActivity(intent)
////            deleteMessageResultLauncher.launch(intent)
//        }
//        binding.recyclerViewMessages.adapter = adapter
//        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
//        contactAdapter = SearchContactAdapter(contactList, "") { contact ->
//            val intent = Intent(this, ConversationActivity::class.java).apply {
//                putExtra(EXTRA_THREAD_ID, threadId)
//                putExtra(NUMBER, contact.phoneNumber)
//                putExtra(NAME, contact.name)
//            }
//            startActivity(intent)
//        }
//        binding.recyclerViewContacts.adapter = contactAdapter
//
//        binding.searchInput.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun afterTextChanged(s: Editable?) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                searchJob?.cancel()
//                val query = s.toString()
//                searchJob = CoroutineScope(Dispatchers.IO).launch {
//                    delay(300)
//                    s?.let {
//                        if (query != binding.searchInput.text.toString()) return@launch
//
//                        if (query.isNotEmpty() && query.length > 1) {
//                            Log.d("TAG", "filterData:1 ")
//
//                            val lowerQuery = query.lowercase()
//
//                            // Perform the search using preprocessed indexed messages
//                            val filteredMessages = indexedMessages
//                                .asSequence()                         // Lazy filtering for better performance
//                                .filter { it.searchText.contains(lowerQuery) }
//                                .map { it.original }
//                                .toList()
//
//                            Log.d("TAG", "filterData:2 ")
//                            val filteredContacts = indexedContacts
//                                .asSequence()
//                                .filter { it.searchText.contains(lowerQuery) }
//                                .map { it.original }
//                                .toList()
//
//                            withContext(Dispatchers.Main) {
//                                messageList.clear()
//                                contactList.clear()
//
//                                messageList.addAll(filteredMessages)
//                                contactList.addAll(filteredContacts)
//
//                                viewModel.setFilteredMessages(filteredMessages)
//                                viewModel.setFilteredContacts(filteredContacts)
//
//                                if (filteredContacts.isEmpty()) {
//                                    isMessageExpanded = true
//                                    isContactExpanded = false
//                                } else {
//                                    isMessageExpanded = false
//                                }
//
//                                updateAdapterList()
//                                binding.btnclose.visibility = View.VISIBLE
//                                binding.emptyList.visibility = View.GONE
//                            }
//
//                        } else {
//                            withContext(Dispatchers.Main) {
//                                clearFilters()
//                            }
//                        }
//                    }
//                }
//            }
//        })
//
//        binding.btnclose.setOnClickListener {
//            binding.searchInput.text.clear()
//            clearFilters()
//        }
//
//
//        binding.icBack.setOnClickListener {
//            binding.searchInput.text.clear()
//            onBackPressed()
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun setupRecyclerViews() {
//        adapter = SearchMessageAdapter(
//            messages = emptyList(),
//            query = "",
//            matchCountMap = emptyMap(),
//            onItemClick = { message, query ->
//                val intent = Intent(this, ConversationActivity::class.java).apply {
//                    putExtra(EXTRA_THREAD_ID, message.threadId)
//                    putExtra(NUMBER, message.address)
//                    putExtra(NAME, message.address)
//                    putExtra(QUERY, query)
//                    putExtra(FROMSEARCH, true)
//                }
//                startActivity(intent)
//            }
//        )
//        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this)
//        binding.recyclerViewMessages.adapter = adapter
//
//        contactAdapter = SearchContactAdapter(
//            contacts = emptyList(),  // Replace with actual contacts
//            query = "",  // Pass the search query if needed
//            onItemClick = { contact ->
//                // Handle contact click here
//            }
//        )
//        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
//        binding.recyclerViewContacts.adapter = contactAdapter
//    }
//
//   /* private fun updateAdapterList() {
//        val shouldExpandMessages = isMessageExpanded || contactList.isEmpty()
//
//        if (!shouldExpandMessages) {
//            val grouped = messageList.groupBy { it.threadId }
//            val matchCountMap = grouped.mapValues { (_, msgs) ->
//                msgs.count {
//                    it.body.contains(
//                        binding.searchInput.text.toString(),
//                        ignoreCase = true
//                    )
//                }
//            }
//
//            val topMessages = grouped.map { it.value.first() }
//            val visibleMessages = topMessages.take(3)
//
//            adapter.updateList(visibleMessages, binding.searchInput.text.toString(), matchCountMap)
//
//            binding.recyclerViewMessages.visibility =
//                if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
//            binding.messagesHeader.visibility =
//                if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
//            binding.seeAllMessages.visibility =  View.VISIBLE
////            binding.seeAllMessages.visibility = if (!isMessageExpanded && topMessages.size > 3) View.VISIBLE else View.GONE
//        }
//
//        if (!isContactExpanded) {
//            val grouped = contactList.groupBy { it.phoneNumber }
//            val topContacts = grouped.map { it.value.first() }
//            val visibleContacts = if (isContactExpanded) topContacts else topContacts.take(3)
//
//            contactAdapter.updateList(visibleContacts, binding.searchInput.text.toString())
//
//            binding.recyclerViewContacts.visibility =
//                if (visibleContacts.isNotEmpty()) View.VISIBLE else View.GONE
//            binding.contactsHeader.visibility =
//                if (visibleContacts.isNotEmpty()) View.VISIBLE else View.GONE
//            binding.seeAllContacts.visibility =
//                if (!isContactExpanded && topContacts.size > 3) View.VISIBLE else View.GONE
//
//        }
//
//
//    }
//
//*/
//
//    fun updateAdapterList() {
//        lifecycleScope.launch {
//            val query = binding.searchInput.text.toString()
//            val shouldExpandMessages = isMessageExpanded || contactList.isEmpty()
//
//            if (!shouldExpandMessages) {
//                val visibleMessagesData = withContext(Dispatchers.Default) {
//                    val grouped = messageList.groupBy { it.threadId }
//                    val matchCountMap = grouped.mapValues { (_, msgs) ->
//                        msgs.count { it.body.contains(query, ignoreCase = true) }
//                    }
//
//                    val topMessages = grouped.map { it.value.first() }
//                    val visibleMessages = topMessages.take(3)
//
//                    Triple(visibleMessages, query, matchCountMap)
//                }
//
//                val (visibleMessages, newQuery, matchCountMap) = visibleMessagesData
//
//                adapter.updateList(visibleMessages, newQuery, matchCountMap)
//
//                binding.recyclerViewMessages.visibility =
//                    if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
//                binding.messagesHeader.visibility =
//                    if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
//                binding.seeAllMessages.visibility = View.VISIBLE
//            }
//
//            if (!isContactExpanded) {
//                val visibleContacts = withContext(Dispatchers.Default) {
//                    val grouped = contactList.groupBy { it.phoneNumber }
//                    val topContacts = grouped.map { it.value.first() }
//                    if (isContactExpanded) topContacts else topContacts.take(3)
//                }
//
//                contactAdapter.updateList(visibleContacts, query)
//
//                binding.recyclerViewContacts.visibility =
//                    if (visibleContacts.isNotEmpty()) View.VISIBLE else View.GONE
//                binding.contactsHeader.visibility =
//                    if (visibleContacts.isNotEmpty()) View.VISIBLE else View.GONE
//                binding.seeAllContacts.visibility =
//                    if (!isContactExpanded && visibleContacts.size > 3) View.VISIBLE else View.GONE
//            }
//        }
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun setupObservers() {
//        viewModel.Filetrmessages.observe(this) { messages ->
//
//            Log.d("TAG", "setupObservers:1 ")
//            val limited =
//                if (!isMessageExpanded && messages.size > 3) messages.take(3) else messages
//            adapter.updateList(limited, binding.searchInput.text.toString(), emptyMap())
//            Log.d("TAG", "setupObservers:2 ")
//
//            binding.recyclerViewMessages.visibility =
//                if (limited.isNotEmpty()) View.VISIBLE else View.GONE
//            binding.messagesHeader.visibility =
//                if (limited.isNotEmpty()) View.VISIBLE else View.GONE
//            binding.seeAllMessages.visibility =
//                if (!isMessageExpanded && messages.size > 3) View.VISIBLE else View.GONE
//        }
//
//        viewModel.filteredContacts.observe(this) { contacts ->
//
//            val limited =
//                if (!isContactExpanded && contacts.size > 3) contacts.take(3) else contacts
//            contactAdapter.updateList(limited, binding.searchInput.text.toString())
//            binding.recyclerViewContacts.visibility =
//                if (limited.isNotEmpty()) View.VISIBLE else View.GONE
//            binding.contactsHeader.visibility =
//                if (limited.isNotEmpty()) View.VISIBLE else View.GONE
//            binding.seeAllContacts.visibility =
//                if (!isContactExpanded && contacts.size > 3) View.VISIBLE else View.GONE
//        }
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun fetchInitialData() {
//        lifecycleScope.launch(Dispatchers.IO) {
//            val messages = viewModel.getAllMessages(this@SearchActivity000)
//            val contacts = viewModel.getAllContacts(this@SearchActivity000)
//
//            withContext(Dispatchers.Main) {
//                allMessages.clear()
//                allMessages.addAll(messages)
//
//                allContacts.clear()
//                allContacts.addAll(contacts)
//                buildSearchIndex(allMessages)
//                buildContactIndex(allContacts)
//            }
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun clearFilters() {
//        binding.btnclose.visibility = View.GONE
//        binding.emptyList.visibility = View.VISIBLE
//
//        messageList.clear()
//        contactList.clear()
//
//
//        adapter.updateList(emptyList(), "", emptyMap())
//        contactAdapter.updateList(emptyList(), "")
//
//
//        if (Looper.myLooper() == Looper.getMainLooper()) {
//            // We are on the main thread
//            viewModel.setFilteredMessages(emptyList())
//            viewModel.setFilteredContacts(emptyList())
//        } else {
//            // We're on a background thread, use postValue() to update LiveData
//            viewModel.setFilteredMessages(emptyList())  // Ensure this method uses postValue inside
//            viewModel.setFilteredContacts(emptyList())  // Ensure this method uses postValue inside
//        }
//        isMessageExpanded = false
//        isContactExpanded = false
//
//        binding.recyclerViewMessages.visibility = View.GONE
//        binding.messagesHeader.visibility = View.GONE
//        binding.seeAllMessages.visibility = View.GONE
//
//        binding.recyclerViewContacts.visibility = View.GONE
//        binding.contactsHeader.visibility = View.GONE
//        binding.seeAllContacts.visibility = View.GONE
//    }
//
//
//    private fun closeKeyboard() {
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
//    }
//
//    private fun isKeyboardOpen(): Boolean {
//        val rootView = window.decorView.rootView
//        val rect = Rect()
//        rootView.getWindowVisibleDisplayFrame(rect)
//        val screenHeight = rootView.height
//        val keypadHeight = screenHeight - rect.bottom
//        return keypadHeight > screenHeight * 0.15
//    }
//
//    override fun onBackPressed() {
//        if (binding.searchInput.text.isNotEmpty()) {
//            binding.searchInput.text.clear()
//            binding.btnclose.visibility = View.GONE
//            binding.messagesHeader.visibility = View.GONE
//            binding.contactsHeader.visibility = View.GONE
//            binding.emptyList.visibility = View.VISIBLE
//        } else if (isKeyboardOpen()) {
//            closeKeyboard()
//        } else {
//            super.onBackPressed()
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
//            return
//        }
//    }
//
//}