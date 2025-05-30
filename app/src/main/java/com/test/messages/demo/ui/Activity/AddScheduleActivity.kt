package com.test.messages.demo.ui.Activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.ISGROUP
import com.test.messages.demo.Util.CommanConstants.ISSCHEDULED
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.CommanConstants.PROFILEURL
import com.test.messages.demo.data.Model.ContactItem
import com.test.messages.demo.databinding.ActivityAddScheduleBinding
import com.test.messages.demo.ui.Adapter.ContactAdapter
import com.test.messages.demo.ui.send.MessageUtils
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddScheduleActivity : BaseActivity() {

    private lateinit var binding: ActivityAddScheduleBinding
    private lateinit var contactAdapter: ContactAdapter
    private var selectedContacts = mutableListOf<ContactItem>()
    private val selectedContactViews = mutableMapOf<String, View>()

    private var allContacts = listOf<ContactItem>()
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var filteredContacts: List<ContactItem>
    private lateinit var messageUtils: MessageUtils
    private var isNumberKeyboard = false
    private lateinit var view: View

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddScheduleBinding.inflate(layoutInflater)
        view = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(binding.rootView)

        messageUtils = MessageUtils(this)
        view.hideKeyboard(this)
        binding.icBack.setOnClickListener {
            Log.d("TAG", "onCreate:back ")
            onBackPressed()


        }
        binding.btnKeypad.setOnClickListener {
            if (isNumberKeyboard) {
                setTextKeyboard()
            } else {
                setNumberKeyboard()
            }
        }

        binding.SrecyclerView.layoutManager = LinearLayoutManager(this)

        contactAdapter = ContactAdapter(allContacts) { contact -> openConversation(contact) }
        binding.SrecyclerView.adapter = contactAdapter

        binding.fastscroller.setRecyclerView(binding.SrecyclerView)
        binding.fastscroller.setViewsToUse(
            R.layout.recycler_view_fast_scroller__fast_scroller,
            R.id.fastscroller_bubble, R.id.fastscroller_handle
        )
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
                binding.SrecyclerView.visibility =
                    if (filteredContacts.isNotEmpty()) View.VISIBLE else View.GONE
                contactAdapter.submitList(filteredContacts)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun openConversation(contact: ContactItem) {
        view.hideKeyboard(this@AddScheduleActivity)
        val threadId = getThreadId(this, contact.phoneNumber)
        val intent = Intent(this, ConversationActivity::class.java)
        intent.putExtra(EXTRA_THREAD_ID, threadId)
        intent.putExtra(NUMBER, contact.phoneNumber)
        intent.putExtra(NAME, contact.name)
        intent.putExtra(ISGROUP, false)
        intent.putExtra(PROFILEURL, contact.profileImageUrl)
        intent.putExtra(ISSCHEDULED, true)
        startActivity(intent)
        finish()
    }

    private fun updateSelectedContactsHeader() {
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

            selectedContactViews[contact.phoneNumber] = contactView
        }
    }


    private fun getThreadId(context: Context, sender: String): Long {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.THREAD_ID)
        val selection = "${Telephony.Sms.ADDRESS} = ?"
        val selectionArgs = arrayOf(sender)

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                }
            }
        return 0L
    }


    private fun removeFromSelectedContacts(contact: ContactItem) {
        if (!selectedContacts.contains(contact)) return
        selectedContacts.remove(contact)
        val contactView = selectedContactViews[contact.phoneNumber]
        if (contactView != null) {
            selectedContactViews.remove(contact.phoneNumber)
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
        if (binding.editTextSearch.text.isNotEmpty()) {
            binding.editTextSearch.text.clear()
        } else {
            binding.editTextSearch.hideKeyboard(this)
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