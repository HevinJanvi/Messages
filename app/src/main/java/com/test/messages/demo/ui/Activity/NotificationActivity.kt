package com.test.messages.demo.ui.Activity

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.test.messages.demo.data.Database.Notification.NotificationDao
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityNotificationBinding
import com.test.messages.demo.ui.Dialogs.NotificationViewDialog
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.data.reciever.createNotificationChannel
import com.test.messages.demo.data.reciever.createNotificationChannelGlobal
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
    private lateinit var notificationDao: NotificationDao
    private val viewModel: MessageViewModel by viewModels()


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        threadId = intent.getLongExtra("EXTRA_THREAD_ID", -1)
        number = intent.getStringExtra("NUMBER").toString()
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

        /* val sharedPreferences = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
         val isGlobalWakeEnabled = sharedPreferences.getBoolean("KEY_WAKE_SCREEN_GLOBAL", false)
         val isWakeEnabled = if (threadId == -1L) {
             isGlobalWakeEnabled
         } else {
             sharedPreferences.getBoolean("KEY_WAKE_SCREEN_$threadId", isGlobalWakeEnabled)
         }
         binding.switchWakeNoti.isChecked = isWakeEnabled

         binding.switchWakeNoti.setOnCheckedChangeListener { _, isChecked ->
             val sharedPreferences = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
             val editor = sharedPreferences.edit()

             if (threadId == -1L) {
                 editor.putBoolean("KEY_WAKE_SCREEN_GLOBAL", isChecked)
             } else {
                 editor.putBoolean("KEY_WAKE_SCREEN_$threadId", isChecked)
             }
             editor.apply()
         }*/

    }

    private fun updatePreviewText(option: Int) {
        val text = when (option) {
            0 -> getString(R.string.show_name_and_message)
            1 -> getString(R.string.show_only_sender)
            2 -> getString(R.string.show_nothing)
            else -> getString(R.string.show_name_and_message)
        }
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
