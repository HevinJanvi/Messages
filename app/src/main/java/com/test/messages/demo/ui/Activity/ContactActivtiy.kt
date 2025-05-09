package com.test.messages.demo.ui.Activity


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.FORWARD
import com.test.messages.demo.Util.CommanConstants.FORWARDMSGS
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.CommanConstants.SHARECONTACT
import com.test.messages.demo.Util.CommanConstants.SHARECONTACTNAME
import com.test.messages.demo.Util.CommanConstants.SHARECONTACTNUMBER
import com.test.messages.demo.Util.CommanConstants.SOURCE
import com.test.messages.demo.data.Model.ContactItem
import com.test.messages.demo.databinding.ActivityContactBinding
import com.test.messages.demo.ui.Adapter.ContactAdapter
import com.test.messages.demo.ui.send.MessageUtils
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContactActivtiy : BaseActivity() {
    private lateinit var binding: ActivityContactBinding
    private lateinit var contactAdapter: ContactAdapter
    private var selectedContacts = mutableListOf<ContactItem>()
    private val selectedContactViews = mutableMapOf<String, View>()

    private var allContacts = listOf<ContactItem>()
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var filteredContacts: List<ContactItem>
    private lateinit var messageUtils: MessageUtils
    private var isNumberKeyboard = false

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)
        messageUtils = MessageUtils(this)

        val receivedContacts = intent.getParcelableArrayListExtra<ContactItem>("selectedContacts")
        receivedContacts?.let {
            selectedContacts.addAll(it)
            updateSelectedContactsHeader()
        }
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        binding.btnKeypad.setOnClickListener {
            if (isNumberKeyboard) {
                setTextKeyboard()
            } else {
                setNumberKeyboard()
            }
        }
        val source = intent.getStringExtra(SOURCE)
        if (source.equals(FORWARD)) {
            binding.btmly.visibility = View.GONE
        } else if (source.equals(SHARECONTACT)) {
            binding.btmly.visibility = View.GONE
        } else {
            binding.btmly.visibility = View.VISIBLE
        }
        val forwardedMessage = intent.getStringExtra(FORWARDMSGS)
        val contactAdapter = ContactAdapter(allContacts) { contact ->
            when (source) {
                FORWARD -> {

                    if (source == FORWARD && !forwardedMessage.isNullOrEmpty()) {
                        val selectedNumber = contact.phoneNumber
                        val contactName = viewModel.getContactNameOrNumber(selectedNumber)
                        val threadId = getThreadId(setOf(selectedNumber))

                        val intent = Intent(this, ConversationActivity::class.java).apply {
                            putExtra(EXTRA_THREAD_ID, threadId)
                            putExtra(NUMBER, selectedNumber)
                            putExtra(NAME, contactName)
                            putExtra(FORWARDMSGS, forwardedMessage)
                        }
                        startActivity(intent)
                        finish()
                    }
                }

                SHARECONTACT -> {
                    val resultIntent = Intent().apply {
                        putExtra(SHARECONTACTNUMBER, contact.phoneNumber)
                        putExtra(SHARECONTACTNAME, contact.name)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }

                else -> {
                    addToSelectedContacts(contact)
                }
            }
        }


        binding.contactRecyclerView.layoutManager =
            object : LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {
                override fun onLayoutCompleted(state: RecyclerView.State?) {
                    super.onLayoutCompleted(state)
                    val firstVisibleItemPosition = findFirstVisibleItemPosition()
                    val lastVisibleItemPosition = findLastVisibleItemPosition()
                    val itemsShown = lastVisibleItemPosition - firstVisibleItemPosition + 1
                    binding.fastscroller.setVisibility(if (contactAdapter.getItemCount() > itemsShown) View.VISIBLE else View.GONE)
                }
            }

        /* contactAdapter = ContactAdapter(allContacts) { contact ->
             if (!selectedContacts.contains(contact)) {
                 addToSelectedContacts(contact)
             }
         }*/

        binding.fastscroller.setRecyclerView(binding.contactRecyclerView)
        binding.fastscroller.setViewsToUse(
            R.layout.recycler_view_fast_scroller__fast_scroller,
            R.id.fastscroller_bubble, R.id.fastscroller_handle
        )
        binding.contactRecyclerView.adapter = contactAdapter

        viewModel.loadContacts()
        viewModel.contacts.observe(this) { contacts ->
            allContacts = contacts
            filteredContacts = contacts
            contactAdapter.submitList(filteredContacts)
        }
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().toLowerCase()

                filteredContacts = if (query.isEmpty()) {
                    allContacts
                } else {
                    allContacts.filter {
                        it.name?.contains(
                            query,
                            ignoreCase = true
                        ) == true || it.phoneNumber.contains(query)
                    }

                }

                val isValidNumber = query.matches(Regex("^\\d{1,16}$"))
                val numberExistsInContacts = allContacts.any { it.phoneNumber == query }

                if (isValidNumber && !numberExistsInContacts) {
                    val newContact = ContactItem(
                        cid = "",
                        name = query,
                        phoneNumber = query,
                        normalizeNumber = query,
                        profileImageUrl = ""
                    )
                    filteredContacts = listOf(newContact) + filteredContacts
                }
                binding.contactRecyclerView.visibility =
                    if (filteredContacts.isNotEmpty()) View.VISIBLE else View.GONE
                contactAdapter.submitList(filteredContacts)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnDone.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putParcelableArrayListExtra(
                "selectedContacts",
                ArrayList(selectedContacts)
            )
            setResult(RESULT_OK, resultIntent)
            finish()
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

    private fun updateSelectedContactsHeader() {
        binding.selectedContactsLayout.removeAllViews()
        binding.selectedContactsScroll.visibility = View.GONE
        selectedContactViews.clear()

        selectedContacts.forEach { contact ->
            val contactView =
                LayoutInflater.from(this).inflate(R.layout.item_selected_contact, null)
            val contactNameTextView = contactView.findViewById<TextView>(R.id.contactName)
            val minusButton = contactView.findViewById<ImageView>(R.id.minusButton)

            contactNameTextView.text = contact.name

            minusButton.setOnClickListener {
                removeFromSelectedContacts(contact)
            }

            binding.selectedContactsLayout.addView(contactView)
            binding.selectedContactsScroll.post {
                binding.selectedContactsScroll.fullScroll(View.FOCUS_RIGHT)
            }
            binding.selectedContactsScroll.visibility = View.VISIBLE

            selectedContactViews[contact.phoneNumber] = contactView
        }
    }

    private fun addToSelectedContacts(contact: ContactItem) {
        if (selectedContacts.contains(contact)) {
            return
        }
        selectedContacts.add(contact)
        updateSelectedContactsHeader()
    }

    private fun removeFromSelectedContacts(contact: ContactItem) {
        if (!selectedContacts.contains(contact)) return
        selectedContacts.remove(contact)
        val contactView = selectedContactViews[contact.phoneNumber]
        if (contactView != null) {
            binding.selectedContactsLayout.removeView(contactView)
            selectedContactViews.remove(contact.phoneNumber) // Remove from map
        }
        if (selectedContacts.isEmpty()) {
            binding.selectedContactsScroll.visibility = View.GONE
        }
    }

    private fun setNumberKeyboard() {
        binding.editTextSearch.inputType = InputType.TYPE_CLASS_NUMBER
        binding.btnKeypad.setImageResource(R.drawable.ic_txt_keypad)
        isNumberKeyboard = true
        updateKeyboard()
    }

    private fun setTextKeyboard() {
        binding.editTextSearch.inputType = InputType.TYPE_CLASS_TEXT
        binding.btnKeypad.setImageResource(R.drawable.ic_num_keypad)
        isNumberKeyboard = false
        updateKeyboard()
    }

    private fun updateKeyboard() {
        binding.editTextSearch.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.restartInput(binding.editTextSearch)
    }

    override fun onBackPressed() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus
        if (view != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
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