package com.test.messages.demo.ui.Activity

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityNotificationBinding
import com.test.messages.demo.ui.Dialogs.NotificationViewDialog
import com.test.messages.demo.ui.Utils.SmsPermissionUtils
import com.test.messages.demo.ui.Utils.ViewUtils.getNotificationOption
import com.test.messages.demo.ui.reciever.createNotificationChannel

class NotificationActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private var threadId: Long = -1
    private lateinit var number: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        threadId = intent.getLongExtra("EXTRA_THREAD_ID", -1)
        number = intent.getStringExtra("NUMBER").toString()
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        binding.ly2.setOnClickListener {
            val dialog = NotificationViewDialog(this) { selectedOption ->
                updateSelectedOptionText()
            }
            dialog.show()
        }
        updateSelectedOptionText()
        binding.ly1.setOnClickListener {
            openSmsNotificationSettings(this, number)
        }


        val sharedPreferences = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
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
        }


    }

    private fun updateSelectedOptionText() {
        val selectedOption = getNotificationOption(this)
        val text = when (selectedOption) {
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
    override fun onResume() {
        super.onResume()
        if (!SmsPermissionUtils.checkAndRedirectIfNotDefault(this)) {
            return
        }
    }
}
