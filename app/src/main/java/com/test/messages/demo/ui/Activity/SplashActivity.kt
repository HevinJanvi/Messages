package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.os.Bundle
import com.test.messages.demo.Util.SmsPermissionUtils
import com.test.messages.demo.Util.ViewUtils

class SplashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*val selectedLanguage = ViewUtils.getSelectedLanguage(this)
        Log.d("Activity", "Selected Language on Splash: $selectedLanguage")*/

        when {
            !ViewUtils.isLanguageSelected(this) -> {
                startActivity(Intent(this, LanguageActivity::class.java))
                finish()
            }
            !ViewUtils.isIntroShown(this) -> {
                startActivity(Intent(this, IntroActivity::class.java))
                finish()
            }
            !SmsPermissionUtils.isDefaultSmsApp(this) -> {
                startActivity(Intent(this, SmsPermissionActivity::class.java))
                finish()
            }
            else -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        finish()
    }

}
