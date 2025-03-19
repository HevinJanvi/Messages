package com.test.messages.demo.ui.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityLanguageBinding
import com.test.messages.demo.ui.Utils.ViewUtils

class LanguageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)

        binding.btnDone.setOnClickListener {
            ViewUtils.setLanguageSelected(this)
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        }
    }
}