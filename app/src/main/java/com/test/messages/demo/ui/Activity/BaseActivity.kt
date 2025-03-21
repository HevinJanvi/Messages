package com.test.messages.demo.ui.Activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.test.messages.demo.R
import com.test.messages.demo.ui.Utils.ViewUtils
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { setLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = resources.getColor(R.color.bg, theme)
        }
        setLanguage(this)
    }

    open fun setLanguage(context: Context): Context? {
        val languageCode = ViewUtils.getSelectedLanguage(context)
        Log.d("TAG", "setLanguage: "+languageCode)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    override fun onResume() {
        super.onResume()
        setLanguage(this@BaseActivity)
    }

}