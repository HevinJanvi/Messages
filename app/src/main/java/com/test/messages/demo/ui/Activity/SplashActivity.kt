package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.test.messages.demo.ui.Utils.SmsPermissionUtils
import com.test.messages.demo.ui.Utils.ViewUtils

class SplashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*val selectedLanguage = ViewUtils.getSelectedLanguage(this)
        Log.d("Activity", "Selected Language on Splash: $selectedLanguage")*/

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

}
