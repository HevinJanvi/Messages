package com.test.messages.demo.ui.Activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityMainBinding
import com.test.messages.demo.ui.Fragment.ConversationFragment
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.ui.Utils.SmsPermissionUtils
import com.test.messages.demo.ui.reciever.UnreadMessageListener
import com.test.messages.demo.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity(), UnreadMessageListener {

    private lateinit var binding: ActivityMainBinding
    private var selectedMessagesCount = 0
    val viewModel: MessageViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        if (Telephony.Sms.getDefaultSmsPackage(this) != packageName) {
            startActivity(Intent(this, SmsPermissionActivity::class.java))
        }
        if (savedInstanceState == null) {
            loadConversationFragment()
        }


        updateSelectedItemsCount(0)
        binding.icSelecteAll.setOnCheckedChangeListener { _, isChecked ->
            val fragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
            fragment?.toggleSelectAll(isChecked)
        }
        binding.icSelecteAll.setBackgroundResource(R.drawable.round_checkbox)
        binding.icDrawer.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        setupClickListeners()

        CoroutineScope(Dispatchers.IO).launch {
            val threadIds = fetchAllThreadIds() // Get all threads
            viewModel.insertMissingThreadIds(threadIds) // Store in DB
        }
    }

    private fun fetchAllThreadIds(): List<Long> {
        val threadIds = mutableListOf<Long>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.THREAD_ID)

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val threadIdIndex = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(threadIdIndex)
                if (threadId != -1L) {
                    threadIds.add(threadId)
                }
            }
        }
        return threadIds.distinct() // Remove duplicates before inserting
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupClickListeners() {
        //toolbar
        binding.icMore.setOnClickListener {
            showPopupHome(it)
        }
        binding.icSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        binding.icDelete.setOnClickListener {
            val deleteDialog = DeleteDialog(this) {
                val fragment =
                    supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
                fragment?.deleteSelectedMessages()
            }
            deleteDialog.show()
        }
        binding.archiveLayout.setOnClickListener {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
            fragment?.archiveMessages()
        }
        binding.pinLayout.setOnClickListener {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
            fragment?.pinMessages()
        }
        binding.muteLayout.setOnClickListener {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
            fragment?.muteMessages()
        }
        binding.blockLayout.setOnClickListener {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
            fragment?.BlockMessages()
        }
        //drawer
        binding.include.lyArchived.setOnClickListener {
            val intent = Intent(this, ArchivedActivity::class.java)
            startActivity(intent)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        binding.include.lyRead.setOnClickListener {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
            fragment?.markReadMessages()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        binding.include.lyBlock.setOnClickListener {
            val intent = Intent(this, BlockMessageActivity::class.java)
            startActivity(intent)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        binding.include.lyStarred.setOnClickListener {
            val intent = Intent(this, StarredMessagesActivity::class.java)
            startActivity(intent)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        binding.include.lySchedule.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        binding.include.lySetting.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
//        binding.icRecyclerbin.setOnClickListener {
//            val intent = Intent(this, RecycleBinActivity::class.java)
//            startActivity(intent)
//        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadConversationFragment() {
        val fragment = ConversationFragment()
        fragment.onSelectionChanged = { count, ispinned ->
            updatePinLayout(count, ispinned)
            updateSelectedItemsCount(count)
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()

        supportFragmentManager.executePendingTransactions()
        val loadedFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment

        loadedFragment?.let {
            val totalMessages = it.viewModel.messages.value?.size ?: 0
            updateTotalMessagesCount(totalMessages)
        }
    }

    fun updateMuteUnmuteUI(isMuted: Boolean) {
        Log.d("TAG", "updateMuteUnmuteUI: isMuted = $isMuted")

        if (!isMuted) {
            binding.icmute.setImageResource(R.drawable.ic_unmute)
            binding.txtMute.text = getString(R.string.unmute)
        } else {
            binding.icmute.setImageResource(R.drawable.ic_mute)
            binding.txtMute.text = getString(R.string.mute)
        }
    }


    private fun updatePinLayout(selectedCount: Int, pinnedCount: Int) {
        if (selectedCount > 0) {
            binding.pinLayout.visibility = View.VISIBLE
            val pinTextView = binding.txtPin
            val unpinnedCount = selectedCount - pinnedCount

            when {
                pinnedCount > unpinnedCount -> {
                    binding.icpin.setImageResource(R.drawable.ic_unpin)
                    pinTextView.text = getString(R.string.unpin)
                }

                unpinnedCount > pinnedCount -> {
                    binding.icpin.setImageResource(R.drawable.ic_pin)
                    pinTextView.text = getString(R.string.pin)
                }

                else -> {
                    binding.icpin.setImageResource(R.drawable.ic_unpin)
                    pinTextView.text = getString(R.string.unpin)
                }
            }
        } else {
            binding.pinLayout.visibility = View.GONE
        }
    }

    private fun updateSelectedItemsCount(count: Int) {
        selectedMessagesCount = count
        binding.txtSelectedCount.text =
            "$selectedMessagesCount" + " " + getString(R.string.selected)
        if (selectedMessagesCount > 0) {
            binding.lySelectedItems.visibility = View.VISIBLE
            binding.selectMenu.visibility = View.VISIBLE
            binding.toolBar.visibility = View.GONE
        } else {
            binding.lySelectedItems.visibility = View.GONE
            binding.selectMenu.visibility = View.GONE
            binding.toolBar.visibility = View.VISIBLE
        }
    }

    fun updateTotalMessagesCount(count: Int) {
        binding.include.newMessage.text = "$count"
    }

    override fun onUnreadMessagesCountUpdated(count: Int) {
        Log.d("MainActivity", "Unread Messages: $count")
        binding.include.unreadCount.text = "$count"
    }


    fun showPopupHome(view: View) {
        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialog = layoutInflater.inflate(R.layout.popup_home_menu, null)

        val popupWindow = PopupWindow(
            dialog, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )

        popupWindow.setBackgroundDrawable(BitmapDrawable())
        popupWindow.isOutsideTouchable = true

        val editCat: TextView = dialog.findViewById(R.id.editCategory)
        editCat.setOnClickListener {
            popupWindow.dismiss()
        }
        popupWindow.showAsDropDown(view, 0, 0)
    }


    override fun onBackPressed() {
        if (selectedMessagesCount > 0) {
            Log.d("TAG", "onBackPressed: " + selectedMessagesCount)
            updateSelectedItemsCount(0)
            val fragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
            fragment?.clearSelection()
        } else {
            Log.d("TAG", "onBackPressed:else " + selectedMessagesCount)

            super.onBackPressed()
        }
    }
}
