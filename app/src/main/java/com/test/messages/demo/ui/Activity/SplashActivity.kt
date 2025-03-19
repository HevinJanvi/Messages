package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import androidx.appcompat.app.AppCompatActivity
import com.test.messages.demo.ui.Utils.SmsPermissionUtils
import com.test.messages.demo.ui.Utils.ViewUtils

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       /* if (isDefaultSmsApp()) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, SmsPermissionActivity::class.java))
        }*/

        when {
            !ViewUtils.isLanguageSelected(this) -> {
                startActivity(Intent(this, LanguageActivity::class.java))
            }
            !ViewUtils.isIntroShown(this) -> {
                startActivity(Intent(this, IntroActivity::class.java))
            }
            !SmsPermissionUtils.isDefaultSmsApp(this) -> {
                startActivity(Intent(this, SmsPermissionActivity::class.java))
            }
            else -> {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }

        finish()
    }

    private fun isDefaultSmsApp(): Boolean {
        return Telephony.Sms.getDefaultSmsPackage(this) == packageName
    }
}
