package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityMainBinding
import com.test.messages.demo.ui.Fragment.ConversationFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        if (Telephony.Sms.getDefaultSmsPackage(this) != packageName) {
            startActivity(Intent(this, SmsPermissionActivity::class.java))
        }
        if (savedInstanceState == null) {
            loadConversationFragment()
        }

    }

    private fun loadConversationFragment() {
        val fragment = ConversationFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }


}
