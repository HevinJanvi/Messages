package com.test.messages.demo.data

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.test.messages.demo.Util.CommanConstants.PREFS_NAME
import com.test.messages.demo.Util.CommanConstants.THEMEMODE
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val selectedMode = sharedPref.getInt(THEMEMODE, 1)

        when (selectedMode) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            3 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }


    }
}