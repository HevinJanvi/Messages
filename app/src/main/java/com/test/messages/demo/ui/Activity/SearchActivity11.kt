package com.test.messages.demo.ui.Activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.FROMSEARCH
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.CommanConstants.QUERY
import com.test.messages.demo.Util.DeleteSearchMessageEvent
import com.test.messages.demo.data.Model.ContactItem
import com.test.messages.demo.databinding.ActivitySearchBinding
import com.test.messages.demo.ui.Adapter.SearchContactAdapter
import com.test.messages.demo.ui.Adapter.SearchMessageAdapter
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.data.Model.ConversationItem
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@AndroidEntryPoint
class SearchActivity11 : BaseActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var adapter: SearchMessageAdapter
    private var messageList = mutableListOf<ConversationItem>()
    private var threadId: String? = null
    private var contactList = mutableListOf<ContactItem>()
    private lateinit var contactAdapter: SearchContactAdapter
    private var searchJob: Job? = null
    private var isMessageExpanded = false
    private var isContactExpanded = false
    private val filteredMap = HashMap<Long, MutableList<ConversationItem>>() // threadId -> matching messages

    private val deleteMessageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deletedMessageThreadId = result.data?.getStringExtra("deletedMessageThreadId")
            Log.d("TAG", "serach delete:1 "+deletedMessageThreadId)

            /*deletedMessageThreadId?.let {
                Log.d("TAG", "serach delete: ")
                messageList.removeAll { message -> message.threadId.toString() == it }
                val currentQuery = binding.searchInput.text.toString()

                adapter.updateList(messageList, currentQuery)
            }*/
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageDeleted(event: DeleteSearchMessageEvent) {
        Log.d("TAG", "Deleting messages with IDs: ${event.deletedMessageIds}")
       /* val originalList = viewModel.Filetrmessages.value ?: return
        val updatedList = originalList.filterNot { it.id in event.deletedMessageIds }
        viewModel.setFilteredMessages(updatedList)*/

        messageList.removeAll { it.id in event.deletedMessageIds }
        updateAdapterList()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        EventBus.getDefault().register(this)

        threadId = intent.getStringExtra("THREAD_ID")

        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        adapter = SearchMessageAdapter(messageList) { message, searchedText ->
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, message.threadId)
                putExtra(NUMBER, "message.number")
                putExtra(NAME, message.address)
                putExtra(QUERY, searchedText)
                putExtra(FROMSEARCH, true)
            }
            startActivity(intent)
//            deleteMessageResultLauncher.launch(intent)
        }
        binding.recyclerViewMessages.adapter = adapter
        loadInitialData()
        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        contactAdapter = SearchContactAdapter(contactList,"") { contact  ->
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, threadId)
                putExtra(NUMBER, contact.phoneNumber)
                putExtra(NAME, contact.name)
            }
            startActivity(intent)
        }
        binding.recyclerViewContacts.adapter = contactAdapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val query = s.toString()

                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    if (query != binding.searchInput.text.toString()) return@launch

                    if (query.isNotEmpty()) {
                        filterData(query)
                    } else {
                        clearFilters()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
        /*binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val query = s.toString()

                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    if (query != binding.searchInput.text.toString()) return@launch

                    if (query.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
//                            viewModel.searchMessages(this@SearchActivity, query, threadId,isMessageExpanded)
//                            viewModel.searchContacts(query)
                        }

                        withContext(Dispatchers.Main) {
                            binding.btnclose.visibility = View.VISIBLE
                            binding.emptyList.visibility = View.GONE
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isMessageExpanded = false
                            isContactExpanded = false
                            messageList.clear()
                            contactList.clear()
                            adapter.updateList(messageList, "", emptyMap())
                            contactAdapter.updateList(contactList, "")
                            binding.btnclose.visibility = View.GONE
                            binding.emptyList.visibility = View.VISIBLE
                            binding.messagesHeader.visibility = View.GONE
                            binding.contactsHeader.visibility = View.GONE
                            binding.recyclerViewMessages.visibility = View.GONE
                            binding.recyclerViewContacts.visibility = View.GONE
                            binding.seeAllMessages.visibility = View.GONE
                            binding.seeAllContacts.visibility = View.GONE
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })*/


        binding.btnclose.setOnClickListener {
            binding.searchInput.text.clear()
            clearFilters()
        }
       /* viewModel.Filetrmessages.observe(this) { messages ->
            val currentQuery = binding.searchInput.text.toString().trim()

            val filteredMessages = if (currentQuery.isNotEmpty()) {
                messages.filter {
                    it.body.contains(currentQuery, ignoreCase = true) ||
                            it.address.contains(currentQuery, ignoreCase = true)
                }
            } else {
                messages
            }

            if (!isContactExpanded) {
                val grouped = filteredMessages.groupBy { it.threadId }
                val matchCountMap = grouped.mapValues { (_, msgs) ->
                    msgs.count { it.body.contains(currentQuery, ignoreCase = true) }
                }

                val allTopMessages = grouped.map { it.value.first() }
                val visibleMessages = if (isMessageExpanded) allTopMessages else allTopMessages.take(3)

                adapter.updateList(visibleMessages, currentQuery, matchCountMap)

                binding.recyclerViewMessages.visibility = if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
                binding.messagesHeader.visibility = if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
                binding.seeAllMessages.visibility = if (!isMessageExpanded && allTopMessages.size > 3) View.VISIBLE else View.GONE
            } else {
                binding.recyclerViewMessages.visibility = View.GONE
                binding.messagesHeader.visibility = View.GONE
            }
        }

        viewModel.filteredContacts.observe(this) { contacts ->
            val query = binding.searchInput.text.toString()

            if (contacts.isEmpty()) {
                if (!isMessageExpanded) {
                    isMessageExpanded = true

                    viewModel.Filetrmessages.value?.let { messages ->

                        Log.d("TAG", "onCreate:contact ")
                        val threadCountMap = messages
                            .groupBy { it.threadId }
                            .mapValues { it.value.size }

                        val visibleMessages = messages
                        adapter.updateList(visibleMessages, query, threadCountMap)
                        binding.recyclerViewMessages.visibility = if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
                        binding.messagesHeader.visibility = if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
                        binding.seeAllMessages.visibility = View.GONE
                    }
                }

                binding.recyclerViewContacts.visibility = View.GONE
                binding.contactsHeader.visibility = View.GONE
                binding.seeAllContacts.visibility = View.GONE

            } else {
                if (!isMessageExpanded) {
                    val visibleContacts = if (isContactExpanded) contacts else contacts.take(3)
                    contactAdapter.updateList(visibleContacts, query)

                    binding.recyclerViewContacts.visibility = if (visibleContacts.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.contactsHeader.visibility = if (visibleContacts.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.seeAllContacts.visibility = if (!isContactExpanded && contacts.size > 3) View.VISIBLE else View.GONE
                } else {
                    binding.recyclerViewContacts.visibility = View.GONE
                    binding.contactsHeader.visibility = View.GONE
                }
            }
        }

        binding.seeAllMessages.setOnClickListener {
            isMessageExpanded = !isMessageExpanded
            isContactExpanded = false

            val currentQuery = binding.searchInput.text.toString()
            viewModel.Filetrmessages.value?.let { messageList ->
                // Group by threadId and count
                val messageCountMap = messageList.groupBy { it.threadId }.mapValues { it.value.size }

                val visibleMessages = if (isMessageExpanded) messageList else messageList.take(3)

                adapter.updateList(messageList, currentQuery, messageCountMap)

                binding.seeAllMessages.text = if (isMessageExpanded) "" else getString(R.string.see_all)
                binding.recyclerViewMessages.visibility = if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
            }


            binding.recyclerViewContacts.visibility = View.GONE
            binding.contactsHeader.visibility = View.GONE
            binding.seeAllContacts.visibility = View.GONE
        }

        binding.seeAllContacts.setOnClickListener {
            isContactExpanded = true
            isMessageExpanded = false

            val currentQuery = binding.searchInput.text.toString()
            viewModel.filteredContacts.value?.let {
                contactAdapter.updateList(it, currentQuery)
            }
            binding.recyclerViewMessages.visibility = View.GONE
            binding.messagesHeader.visibility = View.GONE
            binding.seeAllContacts.visibility = View.GONE
        }*/


        binding.icBack.setOnClickListener {
            binding.searchInput.text.clear()
            onBackPressed()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadInitialData() {
        // Load all messages and contacts from the ViewModel or from a cursor
        viewModel.Filetrmessages.observe(this) { messages ->
            messageList.clear()
            messageList.addAll(messages)
            updateAdapterList()
        }

        viewModel.filteredContacts.observe(this) { contacts ->
            contactList.clear()
            contactList.addAll(contacts)
            contactAdapter.updateList(contactList, "")
        }
    }
    private fun filterData(query: String) {
        val filteredMessages = messageList.filter {
            it.body.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true)
        }

        val filteredContacts = contactList.filter {
            it.name!!.contains(query, ignoreCase = true) || it.phoneNumber.contains(query, ignoreCase = true)
        }

        updateAdapterList(filteredMessages, filteredContacts)
    }

    private fun clearFilters() {
        // Reset the lists and adapter
        adapter.updateList(messageList, "", emptyMap())
        contactAdapter.updateList(contactList, "")
    }

    private fun updateAdapterList(filteredMessages: List<ConversationItem> = messageList, filteredContacts: List<ContactItem> = contactList) {
        val query = binding.searchInput.text.toString().trim()

        // Handle messages filtering and update UI
        if (!isMessageExpanded) {
            val grouped = filteredMessages.groupBy { it.threadId }
            val matchCountMap = grouped.mapValues { (_, msgs) ->
                msgs.count { it.body.contains(query, ignoreCase = true) }
            }

            val allTopMessages = grouped.map { it.value.first() }
            val visibleMessages = if (isMessageExpanded) allTopMessages else allTopMessages.take(3)

            adapter.updateList(visibleMessages, query, matchCountMap)

            binding.recyclerViewMessages.visibility = if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
            binding.messagesHeader.visibility = if (visibleMessages.isNotEmpty()) View.VISIBLE else View.GONE
            binding.seeAllMessages.visibility = if (!isMessageExpanded && allTopMessages.size > 3) View.VISIBLE else View.GONE
        }

        // Handle contacts filtering and update UI
        if (!isContactExpanded) {
            val visibleContacts = if (isContactExpanded) filteredContacts else filteredContacts.take(3)
            contactAdapter.updateList(visibleContacts, query)

            binding.recyclerViewContacts.visibility = if (visibleContacts.isNotEmpty()) View.VISIBLE else View.GONE
            binding.contactsHeader.visibility = if (visibleContacts.isNotEmpty()) View.VISIBLE else View.GONE
            binding.seeAllContacts.visibility = if (!isContactExpanded && filteredContacts.size > 3) View.VISIBLE else View.GONE
        }
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

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }

}