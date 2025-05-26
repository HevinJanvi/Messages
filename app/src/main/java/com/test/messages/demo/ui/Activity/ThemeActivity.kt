package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.PREFS_NAME
import com.test.messages.demo.Util.CommanConstants.THEMEMODE
import com.test.messages.demo.databinding.ActivityThemeBinding

class ThemeActivity : BaseActivity() {
    private lateinit var binding: ActivityThemeBinding
    var editor: SharedPreferences.Editor? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme)

        binding = ActivityThemeBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(binding.rootView)

        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        editor = sharedPref.edit()

        val selectedMode = sharedPref.getInt(THEMEMODE, 1)
        when (selectedMode) {
            1 -> binding.radioSystem.isChecked = true
            2 -> binding.radioLight.isChecked = true
            3 -> binding.radioDark.isChecked = true
        }

        val radioButtons = listOf(binding.radioLight, binding.radioDark, binding.radioSystem)
        updateRadioButtonTint()
        for (radio in radioButtons) {
            radio.setOnClickListener {
                for (btn in radioButtons) {
                    btn.isChecked = btn == radio
                }
                when (radio.id) {
                    R.id.radioLight -> theme(2)
                    R.id.radioDark -> theme(3)
                    R.id.radioSystem -> theme(1)
                }
                updateRadioButtonTint()
            }
        }

        binding.icBack.setOnClickListener { onBackPressed() }
    }

    private fun theme(mode: Int) {
        try {
            editor!!.putInt(THEMEMODE, mode)
            editor!!.apply()

            val resultIntent = Intent()
            resultIntent.putExtra(THEMEMODE, mode)
            setResult(RESULT_OK, resultIntent)
            Handler(Looper.getMainLooper()).postDelayed({
                when (mode) {
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    3 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                finish()
            }, 10)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun updateRadioButtonTint() {
        val colorPrimary = ContextCompat.getColor(this, R.color.colorPrimary)
        val defaultColor = ContextCompat.getColor(this, R.color.gray)

        val radioButtons = listOf(binding.radioLight, binding.radioDark, binding.radioSystem)

        for (radio in radioButtons) {
            val color = if (radio.isChecked) colorPrimary else defaultColor
            radio.buttonTintList = ColorStateList.valueOf(color)
        }
    }

}