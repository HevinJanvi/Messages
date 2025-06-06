package com.test.messages.demo.Ui.Activity

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.Helper.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Helper.Constants.GROUP_SEPARATOR
import com.test.messages.demo.Helper.Constants.ISGROUP
import com.test.messages.demo.Helper.Constants.NAME
import com.test.messages.demo.Helper.Constants.NUMBER
import com.test.messages.demo.data.Model.ContactItem
import com.test.messages.demo.databinding.ActivityNewConversationBinding
import com.test.messages.demo.Ui.Adapter.ConversationContactAdapter
import com.test.messages.demo.SMSHelper.MessageUtils
import com.test.messages.demo.Utils.SmsPermissionUtils
import com.test.messages.demo.SMSHelper.SmsSender
import com.test.messages.demo.Utils.ViewUtils
import com.test.messages.demo.data.Model.SIMCard
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.SMSHelper.getThreadId
import com.test.messages.demo.SMSHelper.hasReadContactsPermission
import com.test.messages.demo.SMSHelper.hasReadSmsPermission
import com.test.messages.demo.SMSHelper.hasReadStatePermission
import com.test.messages.demo.SMSHelper.subscriptionManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class NewConversationActivtiy : BaseActivity() {
    private lateinit var binding: ActivityNewConversationBinding
    private lateinit var contactAdapter: ConversationContactAdapter
    private var selectedContacts = LinkedHashSet<ContactItem>()
    private val selectedContactViews = mutableMapOf<String, View>()
    private var allContacts = listOf<ContactItem>()
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var filteredContacts: List<ContactItem>
    private lateinit var messageUtils: MessageUtils
    private var threadId: Long = -1
    private var sharetxt: String? = null
    private val availableSIMCards = mutableListOf<SIMCard>()
    private var selectedSimIndex: Long = 1L
    private var searchJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        messageUtils = MessageUtils(this)
        sharetxt = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!sharetxt.isNullOrEmpty()) {
            binding.editTextMessage.setText(sharetxt)
            checkSendButtonState()
        }
        setupSimSwitcher()
        binding.contactRecyclerView.layoutManager = LinearLayoutManager(this)
        contactAdapter = ConversationContactAdapter(allContacts) { contact ->
            if (!selectedContacts.contains(contact)) {
                addToSelectedContacts(contact)
            }
        }
        binding.contactRecyclerView.adapter = contactAdapter

        viewModel.loadContacts()
        binding.progressBar.visibility = View.VISIBLE
        binding.contactRecyclerView.visibility = View.GONE
        viewModel.contacts.observe(this) { contacts ->
            allContacts = contacts
            filteredContacts = sortContactsPutSpecialLast(contacts)
            contactAdapter.submitList(filteredContacts)
            binding.progressBar.visibility = View.GONE
            binding.contactRecyclerView.visibility = View.VISIBLE

        }

        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val rawQuery = s.toString().trim()
                val query = rawQuery.lowercase()
                val normalizedQuery = normalizeNumber(rawQuery)

                searchJob?.cancel()
                searchJob = CoroutineScope(Dispatchers.Default).launch {
                    delay(150)

                    filteredContacts = if (query.isEmpty()) {
                        allContacts
                    } else {
                        allContacts.filter {
                            it.name?.contains(
                                query,
                                ignoreCase = true
                            ) == true || it.normalizeNumber.contains(normalizedQuery)
                        }
                    }
                    val isValidNumber = normalizedQuery.matches(Regex("^\\+?\\d{1,16}$"))
                    val numberExistsInContacts =
                        allContacts.any { it.normalizeNumber == normalizedQuery }

                    if (isValidNumber && !numberExistsInContacts) {
                        val newContact = ContactItem(
                            cid = "",
                            name = query,
                            phoneNumber = rawQuery,
                            normalizeNumber = normalizedQuery,
                            profileImageUrl = ""
                        )
                        filteredContacts = listOf(newContact) + filteredContacts
                    }
                    filteredContacts = sortContactsPutSpecialLast(filteredContacts)
                    withContext(Dispatchers.Main) {
                        binding.contactRecyclerView.visibility =
                            if (filteredContacts.isNotEmpty()) View.VISIBLE else View.GONE
                        contactAdapter.submitList(filteredContacts)
                    }


                }

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
            binding.editTextSearch.hideKeyboard(this)
            onBackPressedDispatcher.onBackPressed()
        }

        binding.editTextMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkSendButtonState()
            }
        })
    }

    fun normalizeNumber(number: String?): String {
        return number?.replace(Regex("[\\s\\-()]+"), "")?.lowercase() ?: ""
    }

    private fun checkSendButtonState() {
        val trimmedText = binding.editTextMessage.text.toString().trim()
        if (trimmedText.isEmpty()) {
            binding.btnSendMessage.isEnabled = false
            binding.btnSendMessage.setImageResource(R.drawable.ic_send_disable)
        } else {
            binding.btnSendMessage.isEnabled = true
            binding.btnSendMessage.setImageResource(R.drawable.ic_send_enable)
        }
    }

    private fun sortContactsPutSpecialLast(list: List<ContactItem>): List<ContactItem> {
        val uniqueContacts = list.distinctBy { it.normalizeNumber }

        val letterContacts = uniqueContacts.filter {
            val firstChar = it.name?.trim()?.firstOrNull()
            firstChar != null && firstChar.isLetter()
        }.sortedBy { it.name?.lowercase() }

        val digitContacts = uniqueContacts.filter {
            val firstChar = it.name?.trim()?.firstOrNull()
            firstChar != null && firstChar.isDigit()
        }.sortedBy { it.name?.lowercase() }

        val specialContacts = uniqueContacts.filter {
            val firstChar = it.name?.trim()?.firstOrNull()
            firstChar == null || (!firstChar.isLetter() && !firstChar.isDigit())
        }.sortedBy { it.name?.lowercase() }

        return letterContacts + digitContacts + specialContacts
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 111 && resultCode == RESULT_OK) {
            val updatedContacts = data?.getParcelableArrayListExtra<ContactItem>("selectedContacts")
            if (updatedContacts != null) {
                selectedContacts.clear()
                selectedContacts.addAll(updatedContacts)
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
            binding.selectedContactsScroll.post {
                binding.selectedContactsScroll.fullScroll(View.FOCUS_RIGHT)
            }
            binding.selectedContactsScroll.visibility = View.VISIBLE
            selectedContactViews[contact.normalizeNumber] = contactView
        }

    }

    private fun addToSelectedContacts(contact: ContactItem) {

        if (selectedContacts.contains(contact)) {
            return
        }
        selectedContacts.add(contact)
        updateSelectedContactsHeader()

        binding.editTextSearch.text.clear()
        filteredContacts = sortContactsPutSpecialLast(allContacts)
        contactAdapter.submitList(filteredContacts)
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


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun sendMessage() {
        val text = binding.editTextMessage.text.toString().trim()
        if (text.isEmpty()) {
            return
        }
        val selectedSimId =
            availableSIMCards.getOrNull((selectedSimIndex - 1).toInt())?.subscriptionId
                ?: SubscriptionManager.getDefaultSmsSubscriptionId()

        val selectedNumbers = selectedContacts.map { it.normalizeNumber }.toSet()
        val selectedNames = selectedContacts.map { it.name ?: it.normalizeNumber }.toSet()

        if (selectedNumbers.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_contacts_selected), Toast.LENGTH_SHORT)
                .show()
            return
        }

        threadId = getThreadId(selectedNumbers)
        if (threadId == -1L || threadId == 0L) {
            threadId = getThreadId(selectedNumbers)
        }
        var groupId = -1L
        var groupUri: Uri? = null
        if (selectedNumbers.size > 1) {
            val groupThreadId = getThreadId(selectedNumbers.toSet())
            groupId = groupThreadId
            val groupAddress = selectedContacts.joinToString(GROUP_SEPARATOR) { it.name ?: it.normalizeNumber }
            groupUri = messageUtils.insertSmsMessage(
                subId = selectedSimId,
                dest = groupAddress,
                text = text,
                timestamp = System.currentTimeMillis(),
                threadId = threadId
            )
        }

        sendSmsMessage(
            text = text,
            addresses = selectedNumbers,
            subId = selectedSimId,
            requireDeliveryReport = false,
            messageId = null,
            groupId, groupUri
        )
        binding.editTextMessage.text.clear()
        binding.contactRecyclerView.itemAnimator = null
        viewModel.loadMessages()
        viewModel.loadConversation(threadId)

        val combinedNumbers = selectedNumbers.joinToString(GROUP_SEPARATOR)

        val intent = Intent(this, ConversationActivity::class.java).apply {
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(NUMBER, combinedNumbers)
            putExtra(NAME, selectedNames.joinToString(GROUP_SEPARATOR))
            putExtra(ISGROUP, selectedNumbers.size > 1)

        }
        startActivity(intent)
        finish()

    }

    private fun sendSmsMessage(
        text: String,
        addresses: Set<String>,
        subId: Int,
        requireDeliveryReport: Boolean,
        messageId: Long? = null,
        groupId: Long = -1L, groupUri: Uri? = null
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
                    messageUri = messageUri,
                    groupId,
                    groupUri
                )
            } catch (e: Exception) {
            }
        }

    }

    private fun setupSimSwitcher() {
        if (!hasReadStatePermission()) {
            binding.imageSimSwitch.visibility = View.GONE
            return
        }
        val availableSIMs = subscriptionManagerCompat().activeSubscriptionInfoList ?: return

        availableSIMCards.clear()
        if (availableSIMs.size > 1) {
            availableSIMs.forEachIndexed { index, subscriptionInfo ->
                var label = subscriptionInfo.displayName?.toString() ?: ""
                var number = ""
                if (subscriptionInfo.number?.isNotEmpty() == true) {
                    number = subscriptionInfo.number
                }
                val simCard = SIMCard(index + 1, subscriptionInfo.subscriptionId, label, number)
                availableSIMCards.add(simCard)
            }

            val savedSimId = ViewUtils.getSavedSimId(this@NewConversationActivtiy)
            availableSIMCards.forEachIndexed { index, simCard ->
                if (simCard.subscriptionId == savedSimId) {
                    selectedSimIndex = (index + 1).toLong()
                    return@forEachIndexed
                }
            }

            updateSimIcon()

            binding.imageSimSwitch.setOnClickListener {
                toggleSim()
            }

            binding.imageSimSwitch.visibility = View.VISIBLE
        } else {
            binding.imageSimSwitch.visibility = View.GONE
        }

    }

    private fun toggleSim() {
        selectedSimIndex = if (selectedSimIndex == 1L) 2L else 1L
        val subscriptionId =
            availableSIMCards.getOrNull((selectedSimIndex - 1).toInt())?.subscriptionId
        if (subscriptionId != null) {
            ViewUtils.saveSelectedSimId(this, subscriptionId)
        }
        updateSimIcon()
    }

    private fun updateSimIcon() {
        when (selectedSimIndex) {
            1L -> binding.imageSimSwitch.setImageResource(R.drawable.sim_1)
            2L -> binding.imageSimSwitch.setImageResource(R.drawable.sim_2)
            else -> binding.imageSimSwitch.setImageResource(R.drawable.sim_1)
        }
    }


    override fun onBackPressed() {
        binding.editTextSearch.hideKeyboard(this)
        super.onBackPressed()

    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this) && hasReadSmsPermission() && hasReadContactsPermission()) {
            return
        }
    }
}