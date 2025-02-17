package com.test.messages.demo.ui.Activity


import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.data.ContactItem
import com.test.messages.demo.databinding.ActivityContactBinding
import com.test.messages.demo.ui.Adapter.ContactAdapter
import com.test.messages.demo.ui.Utils.MessageUtils
import com.test.messages.demo.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContactActivtiy : AppCompatActivity() {
    private lateinit var binding: ActivityContactBinding
    private lateinit var contactAdapter: ContactAdapter
    private var selectedContacts = mutableListOf<ContactItem>()
    private val selectedContactViews = mutableMapOf<String, View>()

    private var allContacts = listOf<ContactItem>()
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var filteredContacts: List<ContactItem>
    private lateinit var messageUtils: MessageUtils

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

//        binding.contactRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.contactRecyclerView.layoutManager = object:LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false){
            override fun onLayoutCompleted(state: RecyclerView.State?) {
                super.onLayoutCompleted(state)
                val firstVisibleItemPosition = findFirstVisibleItemPosition()
                val lastVisibleItemPosition = findLastVisibleItemPosition()
                val itemsShown = lastVisibleItemPosition - firstVisibleItemPosition + 1
                binding.fastscroller.setVisibility(if (contactAdapter.getItemCount() > itemsShown) View.VISIBLE else View.GONE)
            }
        }

        contactAdapter = ContactAdapter(allContacts) { contact ->
            if (!selectedContacts.contains(contact)) {
                addToSelectedContacts(contact)
            }
        }
        binding.fastscroller.setRecyclerView(binding.contactRecyclerView)
        binding.fastscroller.setViewsToUse(R.layout.recycler_view_fast_scroller__fast_scroller,
            R.id.fastscroller_bubble, R.id.fastscroller_handle)
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
            resultIntent.putParcelableArrayListExtra("selectedContacts", ArrayList(selectedContacts))
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }


    private fun updateSelectedContactsHeader() {
        binding.selectedContactsLayout.removeAllViews()
        selectedContactViews.clear()

        selectedContacts.forEach { contact ->
            val contactView = LayoutInflater.from(this).inflate(R.layout.item_selected_contact, null)
            val contactNameTextView = contactView.findViewById<TextView>(R.id.contactName)
            val minusButton = contactView.findViewById<ImageButton>(R.id.minusButton)

            contactNameTextView.text = contact.name

            minusButton.setOnClickListener {
                removeFromSelectedContacts(contact)
            }

            binding.selectedContactsLayout.addView(contactView)
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
    }



}