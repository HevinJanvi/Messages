package com.test.messages.demo.ui.Activity

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.CommanConstants.QUERY
import com.test.messages.demo.data.Model.ContactItem
import com.test.messages.demo.data.Model.MessageItem
import com.test.messages.demo.databinding.ActivitySearchBinding
import com.test.messages.demo.ui.Adapter.SearchContactAdapter
import com.test.messages.demo.ui.Adapter.SearchMessageAdapter
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : BaseActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var adapter: SearchMessageAdapter
    private var messageList = mutableListOf<MessageItem>()
    private var threadId: String? = null
    private var contactList = mutableListOf<ContactItem>()
    private lateinit var contactAdapter: SearchContactAdapter

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        threadId = intent.getStringExtra("THREAD_ID")

        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        adapter = SearchMessageAdapter(messageList) { message, searchedText ->

            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, message.threadId)
                putExtra(NUMBER, message.number)
                putExtra(NAME, message.sender)
                putExtra(QUERY, searchedText)
            }
            startActivity(intent)
        }
        binding.recyclerViewMessages.adapter = adapter

        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        contactAdapter = SearchContactAdapter(contactList) { contact ->
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
                if (query.isNotEmpty()) {
                    viewModel.searchMessages(query, threadId)
                    viewModel.searchContacts(query)
                    binding.btnclose.visibility = View.VISIBLE
                    binding.emptyList.visibility = View.GONE

                } else {
                    messageList.clear()
                    contactList.clear()
                    adapter.updateList(messageList, "")
                    contactAdapter.updateList(contactList)
                    binding.btnclose.visibility = View.GONE
                    binding.emptyList.visibility = View.VISIBLE
                    binding.messagesHeader.visibility = View.GONE
                    binding.contactsHeader.visibility = View.GONE
                }

            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
        binding.btnclose.setOnClickListener {
            binding.searchInput.text.clear()
            binding.btnclose.visibility = View.GONE
            binding.messagesHeader.visibility = View.GONE
            binding.contactsHeader.visibility = View.GONE
            binding.emptyList.visibility = View.VISIBLE

        }

        viewModel.Filetrmessages.observe(this) { messages ->
            adapter.updateList(messages, binding.searchInput.text.toString())
            binding.recyclerViewMessages.visibility =
                if (messages.isNotEmpty()) View.VISIBLE else View.GONE
            binding.messagesHeader.visibility =
                if (messages.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.filteredContacts.observe(this) { contacts ->
            contactAdapter.updateList(contacts)
            binding.recyclerViewContacts.visibility =
                if (contacts.isNotEmpty()) View.VISIBLE else View.GONE
            binding.contactsHeader.visibility =
                if (contacts.isNotEmpty()) View.VISIBLE else View.GONE

        }

        binding.icBack.setOnClickListener {
            onBackPressed()
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
        return keypadHeight > screenHeight * 0.15  // Keyboard is open if height > 15% of screen
    }

    override fun onBackPressed() {
        if (binding.searchInput.text.isNotEmpty()) {
            binding.searchInput.text.clear()
            binding.btnclose.visibility = View.GONE
            binding.messagesHeader.visibility = View.GONE
            binding.contactsHeader.visibility = View.GONE
            binding.emptyList.visibility = View.VISIBLE
        } else if (isKeyboardOpen()) {
            closeKeyboard()  // Close the keyboard first
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