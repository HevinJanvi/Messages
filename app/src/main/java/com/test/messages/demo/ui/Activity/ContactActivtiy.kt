package com.test.messages.demo.ui.Activity


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import com.test.messages.demo.Util.ActivityFinishEvent
import com.test.messages.demo.Util.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.Constants.FORWARD
import com.test.messages.demo.Util.Constants.FORWARDMSGS
import com.test.messages.demo.Util.Constants.NAME
import com.test.messages.demo.Util.Constants.NUMBER
import com.test.messages.demo.Util.Constants.SHARECONTACT
import com.test.messages.demo.Util.Constants.SHARECONTACTNAME
import com.test.messages.demo.Util.Constants.SHARECONTACTNUMBER
import com.test.messages.demo.Util.Constants.SOURCE
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.data.Model.ContactItem
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.databinding.ActivityContactBinding
import com.test.messages.demo.ui.Adapter.ContactAdapter
import com.test.messages.demo.ui.SMSend.MessageUtils
import com.test.messages.demo.ui.SMSend.getThreadId
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus

@AndroidEntryPoint
class ContactActivtiy : BaseActivity() {
    private lateinit var binding: ActivityContactBinding
    private var selectedContacts = mutableListOf<ContactItem>()
    private val selectedContactViews = mutableMapOf<String, View>()
    private lateinit var view: View

    private var allContacts = listOf<ContactItem>()
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var filteredContacts: List<ContactItem>
    private lateinit var messageUtils: MessageUtils
    private var isNumberKeyboard = false
    private lateinit var source: String

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        view = binding.getRoot()
        setContentView(binding.root)
        applyWindowInsetsToView(binding.rootView)
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
        source = intent.getStringExtra(SOURCE) ?: ""

        if (source.equals(FORWARD)) {
            binding.btmly.visibility = View.GONE
        } else if (source.equals(SHARECONTACT)) {
            binding.btmly.visibility = View.GONE
        } else {
            if (selectedContacts.isEmpty()) {
                binding.btmly.visibility = View.GONE
            } else {
                binding.btmly.visibility = View.VISIBLE
            }
        }
        val forwardedMessage = intent.getStringExtra(FORWARDMSGS)
        val contactAdapter = ContactAdapter(allContacts) { contact ->
            when (source) {
                FORWARD -> {
                    if (source == FORWARD && !forwardedMessage.isNullOrEmpty()) {
                        val selectedNumber = contact.normalizeNumber
                        val contactName = viewModel.getContactNameOrNumber(selectedNumber)
                        val threadId = getThreadId(setOf(selectedNumber))

                        val intent = Intent(this, ConversationActivity::class.java).apply {
                            putExtra(EXTRA_THREAD_ID, threadId)
                            putExtra(NUMBER, selectedNumber)
                            putExtra(NAME, contactName)
                            putExtra(FORWARDMSGS, forwardedMessage)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                        finish()
                        EventBus.getDefault().post(ActivityFinishEvent(true))

                    }
                }

                SHARECONTACT -> {
                    val resultIntent = Intent().apply {
                        putExtra(SHARECONTACTNUMBER, contact.normalizeNumber)
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
        binding.fastscroller.setRecyclerView(binding.contactRecyclerView)
        binding.fastscroller.setViewsToUse(
            R.layout.recycler_view_fast_scroller__fast_scroller,
            R.id.fastscroller_bubble, R.id.fastscroller_handle
        )
        binding.contactRecyclerView.adapter = contactAdapter
        binding.contactRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    view.hideKeyboard(this@ContactActivtiy)
                }
            }
        })

        viewModel.loadContacts()
        binding.progressBar.visibility = View.VISIBLE
        binding.contactRecyclerView.visibility=View.GONE
        viewModel.contacts.observe(this) { contacts ->

            val uniqueContacts = removeDuplicateContacts(contacts)
            val sortedContacts = sortContactsPutSpecialLast(uniqueContacts)

            allContacts = sortedContacts
            filteredContacts = sortedContacts

            contactAdapter.submitList(filteredContacts)
            binding.progressBar.visibility = View.GONE
            binding.contactRecyclerView.visibility=View.VISIBLE
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
                        ) == true || it.normalizeNumber.contains(query)
                    }

                }

                val isValidNumber = query.matches(Regex("^\\d{1,16}$"))
                val numberExistsInContacts = allContacts.any { it.normalizeNumber == query }

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

    private fun updateSelectedContactsHeader() {
        binding.selectedContactsLayout.removeAllViews()
        binding.btmly.visibility = View.GONE
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
            binding.btmly.visibility = View.VISIBLE

            selectedContactViews[contact.normalizeNumber] = contactView
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
        val contactView = selectedContactViews[contact.normalizeNumber]
        if (contactView != null) {
            binding.selectedContactsLayout.removeView(contactView)
            selectedContactViews.remove(contact.normalizeNumber)
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

    private fun removeDuplicateContacts(contacts: List<ContactItem>): List<ContactItem> {
        val seen = mutableSetOf<String>()
        return contacts.filter { contact ->
            val normalized = contact.normalizeNumber.trim()
            if (normalized in seen) {
                false
            } else {
                seen.add(normalized)
                true
            }
        }
    }

    override fun onBackPressed() {
            if (source.equals(FORWARD)) {
                finish()
            } else {
                binding.editTextSearch.hideKeyboard(this)
                super.onBackPressed()
            }
    }

    private fun sortContactsPutSpecialLast(list: List<ContactItem>): List<ContactItem> {
        val letterContacts = list.filter {
            val firstChar = it.name?.trim()?.firstOrNull()
            firstChar != null && firstChar.isLetter()
        }.sortedBy { it.name?.lowercase() }

        val digitContacts = list.filter {
            val firstChar = it.name?.trim()?.firstOrNull()
            firstChar != null && firstChar.isDigit()
        }.sortedBy { it.name?.lowercase() }

        val specialContacts = list.filter {
            val firstChar = it.name?.trim()?.firstOrNull()
            firstChar == null || (!firstChar.isLetter() && !firstChar.isDigit())
        }.sortedBy { it.name?.lowercase() }

        return letterContacts + digitContacts + specialContacts
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }
}