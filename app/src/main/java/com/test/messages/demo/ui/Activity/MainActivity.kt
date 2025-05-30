package com.test.messages.demo.ui.Activity

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.ImageViewCompat
import com.test.messages.demo.R
import com.test.messages.demo.Util.LanguageChangeEvent
import com.test.messages.demo.Util.MessagesRefreshEvent
import com.test.messages.demo.databinding.ActivityMainBinding
import com.test.messages.demo.ui.Fragment.ConversationFragment
import com.test.messages.demo.ui.Dialogs.DeleteDialog
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.ViewUtils.blinkThen
import com.test.messages.demo.data.reciever.UnreadMessageListener
import com.test.messages.demo.data.viewmodel.MessageViewModel
import com.test.messages.demo.ui.send.hasReadContactsPermission
import com.test.messages.demo.ui.send.hasReadSmsPermission
import com.test.messages.demo.ui.send.notificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.Q)
class MainActivity : BaseActivity(), UnreadMessageListener {

    private lateinit var binding: ActivityMainBinding
    private var selectedMessagesCount = 0
    val viewModel: MessageViewModel by viewModels()
    private var doubleBackToExit = false

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this) && hasReadSmsPermission() && hasReadContactsPermission()) {
            return
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val isDrawerOpen = binding.drawerLayout.isOpen
        outState.putBoolean("drawer_open_state", isDrawerOpen)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(findViewById(R.id.rootView))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val layoutParams =  binding.include.header.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.setMargins(0,100,0,0)
            binding.include.header.layoutParams = layoutParams
        }

        EventBus.getDefault().register(this)

        val drawerWasOpen = savedInstanceState?.getBoolean("drawer_open_state", false) ?: false
        handleDrawerState(binding.drawerLayout, drawerWasOpen)
        loadConversationFragment()

        updateSelectedItemsCount(0)
        binding.SelecteAll.setOnCheckedChangeListener { _, isChecked ->
            val fragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
            fragment?.toggleSelectAll(isChecked)
        }
        binding.SelecteAll.setBackgroundResource(R.drawable.round_checkbox)
        binding.icDrawer.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        setupClickListeners()

        CoroutineScope(Dispatchers.IO).launch {
            val threadIds = fetchAllThreadIds()
            viewModel.insertMissingThreadIds(threadIds)
        }
    }

    private fun fetchAllThreadIds(): List<Long> {
        if (!hasReadSmsPermission()) {
            return emptyList()
        }
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
        return threadIds.distinct()
    }


    private fun setupClickListeners() {
        binding.icMore.setOnClickListener {
            showPopupHome(it)
        }
        binding.icSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        binding.deleteLayout.setOnClickListener {
            val deleteDialog = DeleteDialog(this, "mainscreen", true) {
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
            fragment?.MuteUnmuteMessages()
        }
        binding.blockLayout.setOnClickListener {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
            fragment?.BlockMessages()
        }
        //drawer
        binding.include.lyInbox.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        binding.include.lyArchived.setOnClickListener {
            val intent = Intent(this, ArchivedActivity::class.java)
            startActivity(intent)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        binding.include.lyRead.setOnClickListener {
            notificationManager.cancelAll()
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
        binding.include.lyBakup.setOnClickListener {
            val intent = Intent(this, BakupRestoreActivity::class.java)
            startActivity(intent)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChangeLanguage(event: LanguageChangeEvent) {
        recreate()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessagesRefreshed(event: MessagesRefreshEvent) {
        if (event.success) {
            Handler(Looper.getMainLooper()).postDelayed({
                viewModel.loadMessages()
            }, 100)

        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadConversationFragment() {
        val fragment = ConversationFragment()

        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()

        supportFragmentManager.executePendingTransactions()
        val loadedFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment

        fragment.onSelectionChanged = { count, ispinned, ismuted ->
            updatePinLayout(count, ispinned)
            updateMuteLayout(count, ismuted)
            updateSelectedItemsCount(count)
        }

        loadedFragment?.let {
            val totalMessages = it.viewModel.messages.value?.size ?: 0
            updateTotalMessagesCount(totalMessages)
        }

        fragment.onSelectAllStateChanged = { allSelected ->
            binding.SelecteAll.setOnCheckedChangeListener(null)
            binding.SelecteAll.isChecked = allSelected
            binding.SelecteAll.setOnCheckedChangeListener { _, isChecked ->
                val frag =
                    supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
                frag?.toggleSelectAll(isChecked)
            }
        }
    }

    fun updateBlockUI(shouldEnable: Boolean) {
        runOnUiThread {
            binding.blockLayout.isEnabled = shouldEnable
            val color = if (shouldEnable) {
                ContextCompat.getColor(this, R.color.red)
            } else {
                ContextCompat.getColor(this, R.color.default_shadow_color)
            }
            ImageViewCompat.setImageTintList(binding.icblock, ColorStateList.valueOf(color))
            if(shouldEnable){
                binding.txtblock.setTextColor(resources.getColor(R.color.gray_txtcolor))
            }else{
                binding.txtblock.setTextColor(resources.getColor(R.color.default_shadow_color))
            }
        }
    }

    fun updateMuteUnmuteUI(isMuted: Boolean) {
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
                    binding.icpin.setImageResource(R.drawable.ic_unpin2)
                    pinTextView.text = getString(R.string.unpin)
                }

                unpinnedCount > pinnedCount -> {
                    binding.icpin.setImageResource(R.drawable.ic_pin)
                    pinTextView.text = getString(R.string.pin)
                }

                else -> {
                    binding.icpin.setImageResource(R.drawable.ic_unpin2)
                    pinTextView.text = getString(R.string.unpin)
                }
            }
        } else {
            binding.pinLayout.visibility = View.GONE
        }
    }

    private fun updateMuteLayout(selectedCount: Int, muteCount: Int) {
        if (selectedCount > 0) {
            binding.muteLayout.visibility = View.VISIBLE
            val muteTextView = binding.txtMute
            val unmutedCount = selectedCount - muteCount

            when {
                muteCount > unmutedCount -> {
                    binding.icmute.setImageResource(R.drawable.ic_unmute)
                    muteTextView.text = getString(R.string.unmute)
                }

                unmutedCount > muteCount -> {
                    binding.icmute.setImageResource(R.drawable.ic_mute)
                    muteTextView.text = getString(R.string.mute)
                }

                else -> {
                    binding.icmute.setImageResource(R.drawable.ic_unmute)
                    muteTextView.text = getString(R.string.unmute)
                }
            }
        } else {
            binding.muteLayout.visibility = View.GONE
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
        binding.include.unreadCount.text = "$count"
    }


    fun showPopupHome(view: View) {
        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialog = layoutInflater.inflate(R.layout.popup_home_menu, null)

        val popupWindow = PopupWindow(
            dialog,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val marginDp = 16
        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginDp.toFloat(),
            view.resources.displayMetrics
        ).toInt()


        dialog.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = dialog.measuredWidth
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1] + view.height
        val isRTL = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val x = if (isRTL) {
            anchorX + marginPx
        } else {
            anchorX + view.width - popupWidth - marginPx
        }

        popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, x, anchorY)

        val editCat: TextView = dialog.findViewById(R.id.editCategory)
        editCat.setOnClickListener {
            it.blinkThen {
                val fragment =
                    supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
                fragment?.openEditCategory()
                popupWindow.dismiss()
            }
        }

        val recyclebin: TextView = dialog.findViewById(R.id.recyclebinLy)
        recyclebin.setOnClickListener {
            it.blinkThen {
                val intent = Intent(this, RecycleBinActivity::class.java)
                startActivity(intent)

                popupWindow.dismiss()
            }
        }

        val settings: TextView = dialog.findViewById(R.id.settingsTxt)
        settings.setOnClickListener {
            it.blinkThen {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                popupWindow.dismiss()
            }
        }
        val policy: TextView = dialog.findViewById(R.id.policyTxt)
        policy.setOnClickListener {
            it.blinkThen {
                val url = "https://yourprivacypolicy.com"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
                popupWindow.dismiss()
            }
        }
    }


    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        if (binding.drawerLayout.isOpen) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            if (selectedMessagesCount > 0) {
                updateSelectedItemsCount(0)
                val fragment =
                    supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
                fragment?.clearSelection()
            } else {
                if (doubleBackToExit) {
                    finishAffinity();
                    return
                }
                this.doubleBackToExit = true
                Toast.makeText(this, getString(R.string.exit_text), Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    doubleBackToExit = false
                }, 2000)
            }
        }
    }
}
