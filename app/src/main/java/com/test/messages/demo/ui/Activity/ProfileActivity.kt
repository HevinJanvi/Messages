package com.test.messages.demo.ui.Activity

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
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
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.FROMARCHIVE
import com.test.messages.demo.Util.CommanConstants.FROMBLOCK
import com.test.messages.demo.Util.CommanConstants.ISGROUP
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.CommanConstants.PROFILEURL
import com.test.messages.demo.Util.MessagesRefreshEvent
import com.test.messages.demo.Util.MessagesRestoredEvent
import com.test.messages.demo.databinding.ActivityProfileBinding
import com.test.messages.demo.ui.Dialogs.BlockDialog
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.ui.Dialogs.UnblockDialog
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.TimeUtils
import com.test.messages.demo.Util.ViewUtils.isServiceNumber
import com.test.messages.demo.data.viewmodel.MessageViewModel
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
    private lateinit var name: String
    private lateinit var profileUrl: String
    private var fromBlock: Boolean = false
    private var fromArchive: Boolean = false
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var addContactLauncher: ActivityResultLauncher<Intent>
    private var contactId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        addContactLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val newContactId = getContactIdFromNumber(this, number)
                        if (newContactId != null) {
                            this.contactId = newContactId  // Store it for later use
                            Log.d("TAG", "onCreate:new contactb id "+contactId)
                        }
                        loadContactDetails()
                    }, 500)

                }
            }

        threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        number = intent.getStringExtra(NUMBER).toString()
        name = intent.getStringExtra(NAME).toString()
        profileUrl = intent.getStringExtra(PROFILEURL).toString()
        fromBlock = intent.getBooleanExtra(FROMBLOCK, false)
        fromArchive = intent.getBooleanExtra(FROMARCHIVE, false)
        loadContactDetails()
        setupClickListeners()
        if (threadId != -1L) {
            checkIfBlocked(threadId)
        }
        if (threadId != -1L) {
            checkIfArchived(threadId)
        }
    }

    fun getContactDetailsById(context: Context, contactId: String?): Pair<String?, Uri?> {
        if (contactId.isNullOrBlank()) return Pair(null, null) // Prevent crash

        return try {
            val contentResolver = context.contentResolver
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())

            contentResolver.query(uri, arrayOf(
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_URI
            ), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                    val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))?.let { Uri.parse(it) }
                    return Pair(name, photoUri)
                }
            }
            Pair(null, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, null)
        }
    }


    fun getContactUriFromNumber(context: Context, phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.PHOTO_URI),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
            }
        }
        return null
    }

    fun getContactIdFromNumber(context: Context, number: String): String? {
        val contentResolver = context.contentResolver
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))

        contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
            }
        }
        return null
    }


    private fun loadContactDetails() {
        viewModel.getContactNameOrNumberLive(number).observe(this) { contactName ->

            binding.adreesUser.text = contactName
            binding.number.text = number

            contactId?.let { cid ->
                val (updatedName, updatedPhotoUri) = getContactDetailsById(this, cid)
                binding.adreesUser.text = updatedName

                Log.d("TAG", "loadContactDetails:updatedProfileUri- $updatedPhotoUri")
                Log.d("TAG", "loadContactDetails:profileUrl- $profileUrl")

                val firstChar = updatedName?.trim()?.firstOrNull()
                val startsWithSpecialChar = firstChar != null && !firstChar.isLetterOrDigit()

                if (startsWithSpecialChar || updatedPhotoUri != null) {
                    Glide.with(this)
                        .load(updatedPhotoUri)
                        .skipMemoryCache(true)
                        .placeholder(R.drawable.user_icon)
                        .into(binding.imgProfile)

                    binding.imgProfile.visibility = View.VISIBLE
                    binding.profileContainer.visibility = View.GONE
                } else {
                    binding.imgProfile.visibility = View.GONE
                    binding.profileContainer.visibility = View.VISIBLE
                    binding.initialsTextView.text = TimeUtils.getInitials(updatedName ?: "")
                }
            }

            /*val (updatedName, updatedPhotoUri) = getContactDetailsById(this, contactId)
            binding.adreesUser.text = updatedName
            Log.d("TAG", "loadContactDetails:updatedProfileUri- "+updatedProfileUri)
            Log.d("TAG", "loadContactDetails:profileUrl- "+profileUrl)

            if (startsWithSpecialChar ||  updatedProfileUri != null) {
                Log.d("TAG", "loadContactDetails: "+updatedProfileUri)
                Glide.with(this)
                    .load(updatedProfileUri)
                    .skipMemoryCache(true)
                    .placeholder(R.drawable.user_icon)
                    .into(binding.imgProfile)

                binding.imgProfile.visibility = View.VISIBLE
                binding.profileContainer.visibility = View.GONE
            } else {
                binding.imgProfile.visibility = View.GONE
                binding.profileContainer.visibility = View.VISIBLE
                binding.initialsTextView.text = TimeUtils.getInitials(contactName)
            }*/

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
                            Log.d("TAG", "onCreate:old contactb id "+id)
                            openEditContact(id)
                        }
                    } else {
                        binding.profileContact.setImageResource(R.drawable.profile_contact)
                        binding.profileContact.setOnClickListener {
                            openAddContact()
                        }
                    }
                }
            }
            viewModel.loadConversation(threadId)
            val resultIntent = Intent()
            resultIntent.putExtra("UPDATED_NAME", contactName)
            setResult(Activity.RESULT_OK, resultIntent)

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
                val contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
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
            val deleteDialog = DeleteDialog(this, false) {
                deleteMessagesForCurrentThread(threadId)
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
                    val isBlocked = blockConversationIds.contains(threadId)
                    if (isBlocked) {
                        disableLy()
                        binding.profileBlock.setImageResource(R.drawable.profile_unblock)
                        binding.profileBlock.setOnClickListener {
                            val unblockDialog = UnblockDialog(this@ProfileActivity) {
                                binding.profileBlock.isEnabled = false
                                viewModel.unblockConversations(listOf(threadId))
                                binding.profileBlock.setImageResource(R.drawable.profile_block)
                                refreshCheck(threadId)
                            }
                            unblockDialog.show()
                        }
                    } else {
                        enableLy()
                        binding.profileBlock.setImageResource(R.drawable.profile_block)

                        binding.profileBlock.setOnClickListener {
                            val blockDialog = BlockDialog(this@ProfileActivity) {
                                binding.profileBlock.isEnabled = false
                                viewModel.blockSelectedConversations(listOf(threadId))
                                binding.profileBlock.setImageResource(R.drawable.profile_unblock)
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
            finish()
        }, 500)
        overridePendingTransition(R.anim.fadin, R.anim.fadout);
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
                    if (!fromBlock && !fromArchive) {
                        Log.d("TAG", "deleteMessagesForCurrentThread:if ")
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.d("TAG", "deleteMessagesForCurrentThread:else ")

                        EventBus.getDefault().post(MessagesRefreshEvent(true))
                        onBackPressedDispatcher.onBackPressed()

                    }

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