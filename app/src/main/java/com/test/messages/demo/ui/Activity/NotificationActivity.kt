package com.test.messages.demo.ui.Activity

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.test.messages.demo.data.Database.Notification.NotificationDao
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.NotificationHelper
import com.test.messages.demo.databinding.ActivityNotificationBinding
import com.test.messages.demo.ui.Dialogs.NotificationViewDialog
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.SmsUtils.createNotificationChannel
import com.test.messages.demo.Util.SmsUtils.createNotificationChannelGlobal
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.data.viewmodel.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import easynotes.notes.notepad.notebook.privatenotes.colornote.checklist.Database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class NotificationActivity : BaseActivity(){

    private lateinit var binding: ActivityNotificationBinding
    private var threadId: Long = -1
    private lateinit var number: String
    private lateinit var name: String
    private lateinit var notificationDao: NotificationDao
    private val viewModel: MessageViewModel by viewModels()


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        number = intent.getStringExtra(NUMBER).toString()
        name = intent.getStringExtra(NAME).toString()
        notificationDao = AppDatabase.getDatabase(this).notificationDao()

        binding.icBack.setOnClickListener {
            onBackPressed()
        }

        viewModel.updateOrInsertThread(threadId)

        lifecycleScope.launch(Dispatchers.IO) {

            if (threadId != -1L) {
                val isWakeScreenOn = notificationDao.getWakeScreenSetting(threadId)
                withContext(Dispatchers.Main) {
                    if (isWakeScreenOn != null) {
                        binding.switchWakeNoti.isChecked = isWakeScreenOn
                    } else {
                        binding.switchWakeNoti.isChecked = false
                    }
                }
            } else {
                val isWakeScreenOn = notificationDao.getWakeScreenSettingGlobal()
                withContext(Dispatchers.Main) {
                    if (isWakeScreenOn != null) {
                        binding.switchWakeNoti.isChecked = isWakeScreenOn
                    } else {
                        binding.switchWakeNoti.isChecked = false
                    }
                }
            }

        }

        binding.switchWakeNoti.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                var isChecked = binding.switchWakeNoti.isChecked
                if (threadId != -1L) {
                    notificationDao.updateWakeScreenSetting(threadId, isChecked)
                    notificationDao.setIsCustom(threadId, 1)
                } else {
                    notificationDao.updateWakeScreenSettingGlobal(isChecked)
                }
                withContext(Dispatchers.Main) {
                    binding.switchWakeNoti.isChecked = isChecked
                }
            }
        }

         binding.ly1.setOnClickListener {
             if(threadId!=-1L){
                 openSmsNotificationSettings(this,number)
             }else{
                 openSmsNotificationGlobalSettings(this)
             }
         }

        binding.ly2.setOnClickListener {
            val dialog = NotificationViewDialog(this, threadId, viewModel) { selectedOption ->
                updatePreviewText(selectedOption)
            }
            dialog.show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val previewOption = ViewUtils.getPreviewOptionActivity(this@NotificationActivity, threadId)
            withContext(Dispatchers.Main) {
                updatePreviewText(previewOption)
            }
        }

    }

    private fun updatePreviewText(option: Int) {
        val text = when (option) {
            0 -> getString(R.string.show_name_and_message)
            1 -> getString(R.string.show_only_sender)
            2 -> getString(R.string.show_nothing)
            else -> getString(R.string.show_name_and_message)
        }
        Log.d("TAG", "updatePreviewText: "+text)
        binding.selectedOpt.text = text
    }


    fun openSmsNotificationSettings(context: Context, contactNumber: String) {
        val channelId = "sms_channel_$contactNumber"
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        var myNotificationChannel = notificationManager.getNotificationChannel(channelId)

        if (myNotificationChannel == null) {
            createNotificationChannel(context, contactNumber)

            myNotificationChannel = notificationManager.getNotificationChannel(channelId)

            if (myNotificationChannel == null) {
                return
            }
        }
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, myNotificationChannel.id)
        }
        context.startActivity(intent)
    }

    fun openSmsNotificationGlobalSettings(context: Context) {
        val channelId = "sms_channel_"
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        var myNotificationChannel = notificationManager.getNotificationChannel(channelId)

        if (myNotificationChannel == null) {
            createNotificationChannelGlobal(context)

            myNotificationChannel = notificationManager.getNotificationChannel(channelId)

            if (myNotificationChannel == null) {
                return
            }
        }
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, myNotificationChannel.id)
        }

        context.startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }


}
