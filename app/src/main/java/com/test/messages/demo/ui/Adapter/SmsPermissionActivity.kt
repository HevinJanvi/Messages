package com.test.messages.demo.ui.Adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.test.messages.demo.R
import com.test.messages.demo.ui.Activity.MainActivity

class SmsPermissionActivity : AppCompatActivity() {
    private lateinit var intentLauncher: ActivityResultLauncher<Intent>

    companion object {
        const val role = RoleManager.ROLE_SMS
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_permission)
        prepareIntentLauncher()
        findViewById<Button>(R.id.btnSetDefaultSms).setOnClickListener {
            askDefaultSmsHandlerPermission()
        }
    }

    private fun prepareIntentLauncher() {
        intentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    Log.d("TAG", "Failed requesting ROLE_SMS: ")
                }
            }
    }

    private fun askDefaultSmsHandlerPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager: RoleManager = getSystemService(RoleManager::class.java)
            val isRoleAvailable = roleManager.isRoleAvailable(role)
            if (isRoleAvailable) {
                val isRoleHeld = roleManager.isRoleHeld(role)
                if (!isRoleHeld) {
                    intentLauncher.launch(roleManager.createRequestRoleIntent(role))
                } else {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            startActivityForResult(intent, 1001)
        }
    }


}
