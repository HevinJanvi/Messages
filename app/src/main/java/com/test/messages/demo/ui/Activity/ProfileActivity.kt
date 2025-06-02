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
import android.provider.Telephony
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
import com.test.messages.demo.Util.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.Constants.FROMARCHIVE
import com.test.messages.demo.Util.Constants.FROMBLOCK
import com.test.messages.demo.Util.Constants.ISGROUP
import com.test.messages.demo.Util.Constants.MESSAGE_SIZE
import com.test.messages.demo.Util.Constants.NAME
import com.test.messages.demo.Util.Constants.NUMBER
import com.test.messages.demo.Util.MessagesRefreshEvent
import com.test.messages.demo.databinding.ActivityProfileBinding
import com.test.messages.demo.ui.Dialogs.BlockDialog
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.ui.Dialogs.UnblockDialog
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.TimeUtils
import com.test.messages.demo.Util.ViewUtils.isServiceNumber
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.ui.send.hasReadContactsPermission
import com.test.messages.demo.ui.send.hasReadSmsPermission
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.RecyclerBin.DeletedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var threadId: Long = -1
    private lateinit var number: String
    private var messageSize: Int = 0
    private lateinit var name: String
    private var fromBlock: Boolean = false
    private var fromArchive: Boolean = false
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var addContactLauncher: ActivityResultLauncher<Intent>
    private var contactId: String? = null
    private var isArchived = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsetsToView(binding.rootView)
        addContactLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val updatedName = getContactName(number)
                val updatedPhotoUri = getContactPhotoUri(number)
                binding.adreesUser.text = updatedName
                binding.number.text = number
                if (updatedPhotoUri != null && updatedPhotoUri.toString().isNotBlank()) {
                    profileEdit(updatedName, updatedPhotoUri)
                } else {
                    profileEdit(updatedName, Uri.EMPTY)
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    viewModel.loadMessages()
                }, 300)
            }

        threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        number = intent.getStringExtra(NUMBER).toString()
        name = intent.getStringExtra(NAME).toString()
        fromBlock = intent.getBooleanExtra(FROMBLOCK, false)
        fromArchive = intent.getBooleanExtra(FROMARCHIVE, false)
        messageSize = intent.getIntExtra(MESSAGE_SIZE, 0)
        Log.d("TAG", "onCreate:messageSize "+messageSize)
        if (messageSize == 0) {
            binding.ontherLy.isEnabled = false
            binding.ontherLy.alpha = 0.3f
            binding.notifyLy.isEnabled = false
            binding.notifyLy.isClickable = false
            binding.lyArchive.isEnabled = false
            binding.lyArchive.isClickable = false
            binding.deleteLy.isEnabled = false
            binding.deleteLy.isClickable = false
            binding.profileBlock.isEnabled = false
            binding.profileBlock.isClickable = false
            binding.profileBlock.alpha = 0.3f
        } else {
            binding.ontherLy.isEnabled = true
            binding.ontherLy.alpha = 1f
        }
        binding.adreesUser.text = name
        binding.number.text = number
        setupClickListeners()
        if (threadId != -1L) {
            checkIfBlocked(threadId)
        }
        if (threadId != -1L) {
            checkIfArchived(threadId)
        }



        val latestName = getContactName(number) ?: name
        val latestPhotoUri = getContactPhotoUri(number)

        binding.adreesUser.text = latestName
        binding.number.text = number

        if (latestPhotoUri != null && latestPhotoUri.toString().isNotBlank()) {
            profileEdit(latestName, latestPhotoUri)
        } else {
            profileEdit(latestName, Uri.EMPTY)
        }
    }


    override fun finish() {
        val resultIntent = Intent()
        resultIntent.putExtra("UPDATED_NAME", binding.adreesUser.text.toString())
        resultIntent.putExtra(FROMBLOCK, fromBlock)
        setResult(Activity.RESULT_OK, resultIntent)
        super.finish()
    }

    fun profileEdit(name: String, profileuri: Uri) {
        val firstChar = name?.trim()?.firstOrNull()
        val startsWithSpecialChar = firstChar != null && !firstChar.isLetterOrDigit()
        val hasValidPhoto = profileuri != Uri.EMPTY && profileuri.toString().isNotBlank()

        if (startsWithSpecialChar || hasValidPhoto) {
            Glide.with(this)
                .load(profileuri)
                .skipMemoryCache(true)
                .placeholder(R.drawable.user_icon)
                .into(binding.imgProfile)

            binding.imgProfile.visibility = View.VISIBLE
            binding.profileContainer.visibility = View.GONE
        } else {
            binding.imgProfile.visibility = View.GONE
            binding.profileContainer.visibility = View.VISIBLE
            binding.initialsTextView.text = TimeUtils.getInitials(name ?: "")
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

            checkIfContactExists(number) { exists, id ->
                contactId = id
                if (exists) {
                    binding.profileContact.setImageResource(R.drawable.profile_edit)
                    binding.profileContact.setOnClickListener {
                        openContactEditor(number)
                    }
                } else {
                    binding.profileContact.setImageResource(R.drawable.profile_contact)
                    binding.profileContact.setOnClickListener {
                        openAddContact()
                    }
                }
            }
        }
    }

    private fun checkIfContactExists(number: String, callback: (Boolean, String) -> Unit) {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )

        val cursor = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val contactId =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
                callback(true, contactId)
                return
            }
        }
        callback(false, "")
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
                putExtra(FROMBLOCK, fromBlock)
                putExtra(ISGROUP, false)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
            overridePendingTransition(0, 0)
        }
        binding.deleteLy.setOnClickListener {
            val deleteDialog = DeleteDialog(this, "profile", true) {
                deleteMessagesForCurrentThread(threadId)
                viewModel.deleteStarredMessagesForThread(threadId)
            }
            deleteDialog.show()
        }
        binding.notifyLy.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            intent.putExtra(EXTRA_THREAD_ID, threadId)
            intent.putExtra(NUMBER, number)
            intent.putExtra(NAME, name)
            startActivity(intent)
        }
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
                    val isBlocked = blockConversationIds.contains(threadId)
                    if (isBlocked) {
                        disableLy()
                        binding.profileBlock.setImageResource(R.drawable.profile_unblock)
                        binding.profileBlock.setOnClickListener {
                            if (messageSize != 0) {
                                val unblockDialog = UnblockDialog(this@ProfileActivity) {
                                    binding.profileBlock.isEnabled = false
                                    fromBlock = false
                                    viewModel.unblockConversations(listOf(threadId))
                                    binding.profileBlock.setImageResource(R.drawable.profile_block)
                                    refreshCheck(threadId)
                                }
                                unblockDialog.show()
                            }else{
                                binding.profileBlock.isEnabled = false
                                binding.profileBlock.isClickable = false
                                binding.profileBlock.alpha = 0.3f
                            }

                        }
                    } else {
                        enableLy()
                        binding.profileBlock.setImageResource(R.drawable.profile_block)

                        binding.profileBlock.setOnClickListener {
                            if (messageSize != 0) {
                                val blockDialog = BlockDialog(this@ProfileActivity) {
                                    binding.profileBlock.isEnabled = false
                                    viewModel.blockSelectedConversations(listOf(threadId)) {
                                        fromBlock = true
                                        binding.profileBlock.setImageResource(R.drawable.profile_unblock)
                                        refreshCheck(threadId)
                                    }
                                }
                                blockDialog.show()
                            }else{
                                binding.profileBlock.isEnabled = false
                                binding.profileBlock.isClickable = false
                                binding.profileBlock.alpha = 0.3f
                            }

                        }
                    }

                }
            }
        }
        refreshCheck(threadId)
    }

    private fun refreshCheck(threadId: Long) {
        try {
            Handler(Looper.getMainLooper()).postDelayed({
                viewModel.loadBlockThreads()
                checkIfBlocked(threadId)
                binding.profileBlock.isEnabled = true
            }, 500)
        } catch (e: Exception) {
        }

    }

    private fun checkIfArchived(threadId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val archivedConversationIds =
                viewModel.getArchivedConversations().map { it.conversationId }

            withContext(Dispatchers.Main) {
                isArchived = archivedConversationIds.contains(threadId)
                updateArchiveUI()
                binding.lyArchive.setOnClickListener {
                    if (isArchived) {
                        viewModel.unarchiveConversations(listOf(threadId))
                        isArchived = false
                    } else {
                        viewModel.archiveSelectedConversations(listOf(threadId))
                        isArchived = true
                    }
                    updateArchiveUI()
                }
            }
        }
    }

    private fun updateArchiveUI() {
        if (isArchived) {
            binding.archiveText.text = getString(R.string.unarchived)
            binding.icArchive.setImageResource(R.drawable.ic_unarchive)
        } else {
            binding.archiveText.text = getString(R.string.archive)
            binding.icArchive.setImageResource(R.drawable.ic_archive)
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteMessagesForCurrentThread(threadId: Long) {
        val contentResolver = contentResolver
        val db = AppDatabase.getDatabase(this).recycleBinDao()

        Thread {
            try {
                val deletedMessages = mutableListOf<DeletedMessage>()
                val existingBodyDatePairs =
                    mutableSetOf<Pair<String, Long>>()

                val cursor = contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    null,
                    "thread_id = ?",
                    arrayOf(threadId.toString()),
                    Telephony.Sms.DEFAULT_SORT_ORDER
                )

                cursor?.use {
                    val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                    val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                    val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)
                    val readIndex = it.getColumnIndex(Telephony.Sms.READ)
                    val subIdIndex = it.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)
                    val messageIdIndex = it.getColumnIndex(Telephony.Sms._ID)

                    while (it.moveToNext()) {
                        val body = it.getString(bodyIndex) ?: ""
                        val date = it.getLong(dateIndex)
                        val key = Pair(body, date)

                        if (existingBodyDatePairs.contains(key)) continue
                        existingBodyDatePairs.add(key)

                        val deletedMessage = DeletedMessage(
                            messageId = it.getLong(messageIdIndex),
                            threadId = threadId,
                            address = it.getString(addressIndex) ?: "",
                            date = date,
                            body = body,
                            type = it.getInt(typeIndex),
                            read = it.getInt(readIndex) == 1,
                            subscriptionId = it.getInt(subIdIndex),
                            deletedTime = System.currentTimeMillis()
                        )

                        deletedMessages.add(deletedMessage)
                    }
                }
                val uri = Uri.parse("content://sms/conversations/$threadId")
                contentResolver.delete(uri, null, null)
                db.insertMessages(deletedMessages)
                Handler(Looper.getMainLooper()).postDelayed({
                    EventBus.getDefault().post(MessagesRefreshEvent(true))
                    finish()
                    overridePendingTransition(0, 0)
                }, 100)


            } catch (e: Exception) {
                e.printStackTrace()
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
        binding.archiveText.setTextColor(ContextCompat.getColor(this, R.color.gray_txtcolor))
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
        binding.archiveText.setTextColor(ContextCompat.getColor(this, R.color.textcolor))
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this) && hasReadSmsPermission() && hasReadContactsPermission()) {
            return
        }
    }

    fun openContactEditor(phoneNumber: String) {
        val contentResolver = contentResolver

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val cursor = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val contactId =
                    it.getLong(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
                val lookupKey =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.LOOKUP_KEY))

                val contactUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey)

                val intent = Intent(Intent.ACTION_EDIT).apply {
                    setDataAndType(contactUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                    putExtra(
                        "finishActivityOnSaveCompleted",
                        true
                    )
                }
                editContactLauncher.launch(intent)
            } else {
                Toast.makeText(this, getString(R.string.no_result_found), Toast.LENGTH_SHORT).show()
            }
        }
    }


    private val editContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updatedName = getContactName(number)
                val updatedPhotoUri = getContactPhotoUri(number)
                binding.adreesUser.text = updatedName
                binding.number.text = number
                if (updatedPhotoUri != null && updatedPhotoUri.toString().isNotBlank()) {
                    profileEdit(updatedName, updatedPhotoUri)
                } else {
                    profileEdit(updatedName, Uri.EMPTY)
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    viewModel.loadMessages()
                }, 500)
            }
        }

    fun getContactName(phoneNumber: String?): String {
        if (phoneNumber.isNullOrEmpty()) return ""

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                }
            }

            phoneNumber
        } catch (e: Exception) {
            e.printStackTrace()
            phoneNumber ?: ""
        }
    }

    fun getContactPhotoUri(phoneNumber: String): Uri? {
        if (phoneNumber.isNullOrBlank()) return null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.PHOTO_URI)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val photoUri =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
                return if (photoUri != null) Uri.parse(photoUri) else null
            }
        }
        return null
    }

}