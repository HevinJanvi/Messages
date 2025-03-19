package com.test.messages.demo.ui.Activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.test.messages.demo.R

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
        findViewById<TextView>(R.id.btnSetDefaultSms).setOnClickListener {
            askDefaultSmsHandlerPermission()
        }
    }

    private fun prepareIntentLauncher() {
        intentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isDefaultSmsApp()) {
                startActivity(Intent(this, MainActivity::class.java))
                finish() // ✅ Prevent coming back to permission screen
            } else {
                Toast.makeText(this, "Please set this app as the default SMS app", Toast.LENGTH_LONG).show()
            }
        }
    }



    /*private fun askDefaultSmsHandlerPermission() {
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
    }*/

    private fun askDefaultSmsHandlerPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager: RoleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(role)) {
                if (!roleManager.isRoleHeld(role)) {
                    intentLauncher.launch(roleManager.createRequestRoleIntent(role))
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            startActivityForResult(intent, 1001)
        }
    }

    private fun isDefaultSmsApp(): Boolean {
        return Telephony.Sms.getDefaultSmsPackage(this) == packageName
    }

}
