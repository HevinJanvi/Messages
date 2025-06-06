package com.test.messages.demo.Ui.Activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.test.messages.demo.Utils.SmsPermissionUtils
import com.test.messages.demo.Utils.ViewUtils
import com.test.messages.demo.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity() {
    private lateinit var binding: ActivitySplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsetsToView(binding.rootView)

        lifecycleScope.launch {
            delay(1000)

            when {
                !ViewUtils.isLanguageSelected(this@SplashActivity) -> {
                    startActivity(Intent(this@SplashActivity, LanguageActivity::class.java))
                }

                !ViewUtils.isIntroShown(this@SplashActivity) -> {
                    startActivity(Intent(this@SplashActivity, IntroActivity::class.java))
                }

                !SmsPermissionUtils.isDefaultSmsApp(this@SplashActivity) -> {
                    startActivity(Intent(this@SplashActivity, SmsPermissionActivity::class.java))
                }

                else -> {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                }
            }

            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

}
