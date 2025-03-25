package com.test.messages.demo.ui.Activity

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.test.messages.demo.R
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

        val sharedPref = getSharedPreferences("ThemePref", MODE_PRIVATE)
        editor = sharedPref.edit()

        val selectedMode = sharedPref.getInt("dark_mode", 1)
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
            editor!!.putInt("dark_mode", mode)
            editor!!.apply()
            when (mode) {
                1 -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }, 10)
                    finish()
                }

                2 -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }, 10)
                    finish()
                }

                3 -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }, 10)
                    finish()
                }
            }
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