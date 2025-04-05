package com.test.messages.demo.ui.Activity

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.CommanConstants.PROFILEURL
import com.test.messages.demo.databinding.ActivityProfileBinding
import com.test.messages.demo.ui.Dialogs.BlockDialog
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.ui.Dialogs.UnblockDialog
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.TimeUtils
import com.test.messages.demo.Util.ViewUtils.isServiceNumber
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var threadId: Long = -1
    private lateinit var number: String
    private lateinit var name: String
    private lateinit var profileUrl: String
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var addContactLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        addContactLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        loadContactDetails()
                    }, 500)

                }
            }

        threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        number = intent.getStringExtra(NUMBER).toString()
        name = intent.getStringExtra(NAME).toString()
        profileUrl = intent.getStringExtra(PROFILEURL).toString()
        loadContactDetails()
        setupClickListeners()
        if (threadId != -1L) {
            checkIfBlocked(threadId)
        }
        if (threadId != -1L) {
            checkIfArchived(threadId)
        }

    }


    private fun loadContactDetails() {
        viewModel.getContactNameOrNumberLive(number).observe(this) { contactName ->


            binding.adreesUser.text = contactName
            binding.number.text = number

            if (profileUrl.isNotEmpty() && profileUrl != null) {
                Glide.with(this)
                    .load(profileUrl)
                    .placeholder(R.drawable.ic_user)
                    .into(binding.imgProfile)
                binding.imgProfile.visibility = View.VISIBLE
                binding.profileContainer.visibility = View.GONE
            } else {
                binding.imgProfile.visibility = View.GONE
                binding.profileContainer.visibility = View.VISIBLE
                binding.initialsTextView.text = TimeUtils.getInitials(contactName)
            }

            if (isServiceNumber(number)) {
                binding.profileCall.isEnabled = false
                binding.profileContact.isEnabled = false
                binding.profileContact.setImageResource(R.drawable.profile_contact_disable)
                binding.profileCall.setImageResource(R.drawable.profile_call_disable)
            } else {
                binding.profileCall.isEnabled = true
                binding.profileContact.isEnabled = true
                binding.profileContact.setImageResource(R.drawable.profile_contact)
                binding.profileCall.setImageResource(R.drawable.profile_call)
            }
            viewModel.loadConversation(threadId)
            val resultIntent = Intent()
            resultIntent.putExtra("UPDATED_NAME", contactName)
            setResult(Activity.RESULT_OK, resultIntent)

        }
    }

    private fun setupClickListeners() {
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        binding.icCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Phone Number", number)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show()
        }
        binding.profileCall.setOnClickListener {
            makeCall(number)
        }
        binding.profileMessage.setOnClickListener {
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra(EXTRA_THREAD_ID, threadId)
                putExtra(NUMBER, number)
                putExtra(NAME, name)
                putExtra("isGroup", false)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }

        binding.profileContact.setOnClickListener {
            checkIfContactExistsAndOpen()
           /* val intent = Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, number)
            }
            addContactLauncher.launch(intent)*/
        }
        binding.deleteLy.setOnClickListener {
            val deleteDialog = DeleteDialog(this) {
                deleteMessage()
            }
            deleteDialog.show()
        }
        binding.notifyLy.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            intent.putExtra(EXTRA_THREAD_ID, threadId)
            intent.putExtra(NUMBER, number)
            startActivity(intent)
        }
    }
    private fun checkIfContactExistsAndOpen() {
        val contentResolver = contentResolver
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val cursor = contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                // Contact exists, get the ID and open Edit Contact
                val contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
                openEditContact(contactId)
            } else {
                // Contact does not exist, open Add Contact
                openAddContact()
            }
        }
    }

    private fun openEditContact(contactId: String) {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
        }
        addContactLauncher.launch(intent)
    }

    private fun openAddContact() {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.PHONE, number)
        }
        addContactLauncher.launch(intent)
    }


    private fun makeCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        val chooser = Intent.createChooser(intent, "Choose an app to make a call")
        startActivity(chooser)
    }

    private fun checkIfBlocked(threadId: Long) {
        viewModel.messages.removeObservers(this)
        viewModel.messages.observe(this) { messageList ->
            CoroutineScope(Dispatchers.IO).launch {
                val blockConversationIds =
                    viewModel.getBlockedConversations().map { it.conversationId }

                withContext(Dispatchers.Main) {
                    if (blockConversationIds.contains(threadId)) {
                        disableLy()
                        binding.profileBlock.setOnClickListener {
                            val unblockDialog = UnblockDialog(this@ProfileActivity) {
                                binding.profileBlock.isEnabled = false
                                viewModel.unblockConversations(listOf(threadId))
                                refreshCheck(threadId)
                            }
                            unblockDialog.show()
                        }
                    } else {
                        enableLy()
                        binding.profileBlock.setOnClickListener {
                            val blockDialog = BlockDialog(this@ProfileActivity) {
                                binding.profileBlock.isEnabled = false
                                viewModel.blockSelectedConversations(listOf(threadId))
                                refreshCheck(threadId)
                            }
                            blockDialog.show()
                        }
                    }
                }
            }
        }
        refreshCheck(threadId)
    }

    private fun refreshCheck(threadId: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            viewModel.loadBlockThreads()
            checkIfBlocked(threadId)
            binding.profileBlock.isEnabled = true
        }, 500)
    }

    private fun checkIfArchived(threadId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val archivedConversationIds =
                viewModel.getArchivedConversations().map { it.conversationId }

            withContext(Dispatchers.Main) {
                if (archivedConversationIds.contains(threadId)) {
                    binding.icArchiveText.text = getString(R.string.unarchived)
                    binding.icArchive.setImageResource(R.drawable.ic_unarchive)

                    binding.lyArchive.setOnClickListener {
                        viewModel.unarchiveConversations(listOf(threadId))
                        refreshListStatus()
                    }
                } else {
                    binding.icArchiveText.text = getString(R.string.archive)
                    binding.icArchive.setImageResource(R.drawable.ic_archive)

                    binding.lyArchive.setOnClickListener {
                        viewModel.archiveSelectedConversations(listOf(threadId))
                        refreshListStatus()
                    }
                }
            }
        }
    }

    private fun refreshListStatus() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 500)
        overridePendingTransition(R.anim.fadin, R.anim.fadout);
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteMessage() {
        val updatedList = viewModel.messages.value?.toMutableList() ?: mutableListOf()
        Thread {
            try {
                val uri = Uri.parse("content://sms/conversations/$threadId")
                val deletedRows = contentResolver.delete(uri, null, null)
                if (deletedRows > 0) {
                    updatedList.removeAll { it.threadId == threadId }
                    refreshListStatus()
                }
            } catch (e: Exception) {
            }
        }.start()
    }


    private fun disableLy() {
        binding.icNoti.isEnabled = false
        binding.icNoti.isClickable = false
        binding.notifyLy.isClickable = false
        binding.icNoti.setColorFilter(
            ContextCompat.getColor(this, R.color.gray_txtcolor),
            PorterDuff.Mode.SRC_IN
        )
        binding.icNotiText.setTextColor(ContextCompat.getColor(this, R.color.gray_txtcolor))

        binding.lyArchive.isClickable = false
        binding.icArchive.isEnabled = false
        binding.icArchive.isClickable = false
        binding.icArchive.setColorFilter(
            ContextCompat.getColor(this, R.color.gray_txtcolor),
            PorterDuff.Mode.SRC_IN
        )
        binding.icArchiveText.setTextColor(ContextCompat.getColor(this, R.color.gray_txtcolor))
    }

    private fun enableLy() {
        binding.icNoti.isEnabled = true
        binding.icNoti.isClickable = true
        binding.notifyLy.isClickable = true
        binding.icNoti.setColorFilter(
            ContextCompat.getColor(this, R.color.colorPrimary),
            PorterDuff.Mode.SRC_IN
        )
        binding.icNotiText.setTextColor(ContextCompat.getColor(this, R.color.textcolor))
        binding.icArchive.isEnabled = true
        binding.lyArchive.isClickable = true
        binding.icArchive.setColorFilter(
            ContextCompat.getColor(this, R.color.colorPrimary),
            PorterDuff.Mode.SRC_IN
        )
        binding.icArchiveText.setTextColor(ContextCompat.getColor(this, R.color.textcolor))
    }
    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }
}