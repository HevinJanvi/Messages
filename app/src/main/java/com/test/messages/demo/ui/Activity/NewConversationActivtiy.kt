package com.test.messages.demo.ui.Activity

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.data.Model.ContactItem
import com.test.messages.demo.databinding.ActivityNewConversationBinding
import com.test.messages.demo.ui.Adapter.ConversationContactAdapter
import com.test.messages.demo.ui.send.MessageUtils
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.SmsSender
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewConversationActivtiy : AppCompatActivity() {
    private lateinit var binding: ActivityNewConversationBinding
    private lateinit var contactAdapter: ConversationContactAdapter
    private var selectedContacts = LinkedHashSet<ContactItem>()
    private val selectedContactViews = mutableMapOf<String, View>()
    private var allContacts = listOf<ContactItem>()
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var filteredContacts: List<ContactItem>
    private lateinit var messageUtils: MessageUtils
    private var subscriptionId: Int = -1
    private var threadId: Long = -1

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        messageUtils = MessageUtils(this)

        binding.contactRecyclerView.layoutManager = LinearLayoutManager(this)
        contactAdapter = ConversationContactAdapter(allContacts) { contact ->
            if (!selectedContacts.contains(contact)) {
                addToSelectedContacts(contact)
            }
        }
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

        binding.btnSendMessage.setOnClickListener {
            sendMessage()
        }
        binding.addContact.setOnClickListener {
            val intent = Intent(this, ContactActivtiy::class.java)
            intent.putParcelableArrayListExtra("selectedContacts", ArrayList(selectedContacts))
            startActivityForResult(intent, 111)
        }
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 111 && resultCode == RESULT_OK) {
            val updatedContacts = data?.getParcelableArrayListExtra<ContactItem>("selectedContacts")
            updatedContacts?.let { newContacts ->
                newContacts.forEach { contact ->
                    if (!selectedContacts.contains(contact)) {
                        selectedContacts.add(contact)
                    }
                }
                updateSelectedContactsHeader()
            }
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

        binding.editTextSearch.text.clear()
        filteredContacts = allContacts
        contactAdapter.submitList(filteredContacts)
    }


    private fun removeFromSelectedContacts(contact: ContactItem) {
        if (!selectedContacts.contains(contact)) return
        selectedContacts.remove(contact)
        val contactView = selectedContactViews[contact.phoneNumber]
        if (contactView != null) {
            binding.selectedContactsLayout.removeView(contactView)
            selectedContactViews.remove(contact.phoneNumber)
        }
        if (selectedContacts.isEmpty()) {
            binding.selectedContactsScroll.visibility = View.GONE
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun sendMessage() {
        val text = binding.editTextMessage.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.failed_to_send_message), Toast.LENGTH_LONG).show()
            return
        }

        subscriptionId = SmsManager.getDefaultSmsSubscriptionId()
        val selectedNumbers = selectedContacts.map { it.phoneNumber }.toSet()
        val selectedNames = selectedContacts.map { it.name ?: it.phoneNumber }.toSet()

        if (selectedNumbers.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_contacts_selected), Toast.LENGTH_SHORT).show()
            return
        }

        threadId = getThreadId(selectedNumbers)
        if (threadId == -1L || threadId == 0L) {
            threadId = getThreadId(selectedNumbers)
        }

        if (selectedNumbers.size > 1) {
            val groupAddress = selectedContacts.joinToString(", ") { it.name ?: it.phoneNumber }
            messageUtils.insertSmsMessage(
                subId = subscriptionId,
                dest = groupAddress,
                text = text,
                timestamp = System.currentTimeMillis(),
                threadId = threadId,
                status = Telephony.Sms.Sent.STATUS_COMPLETE,
                type = Telephony.Sms.Sent.MESSAGE_TYPE_SENT,
                messageId = null
            )
        }

        sendSmsMessage(
            text = text,
            addresses = selectedNumbers,
            subId = subscriptionId,
            requireDeliveryReport = false
        )
        binding.editTextMessage.text.clear()
        binding.contactRecyclerView.itemAnimator = null
        viewModel.loadMessages()
        viewModel.loadConversation(threadId)

        val combinedNumbers = selectedNumbers.joinToString(",")

        val intent = Intent(this, ConversationActivity::class.java).apply {
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(NUMBER, combinedNumbers)
            putExtra(NAME,selectedNames.joinToString(","))
        }
        startActivity(intent)
        finish()

    }

    private fun sendSmsMessage(
        text: String,
        addresses: Set<String>,
        subId: Int,
        requireDeliveryReport: Boolean,
        messageId: Long? = null
    ) {
        if (addresses.isEmpty()) return

        for (address in addresses) {
            val personalThreadId = getThreadId(setOf(address))
            val messageUri = messageUtils.insertSmsMessage(
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
                    messageUri = messageUri
                )
            } catch (e: Exception) {
            }
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