package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityMainBinding
import com.test.messages.demo.ui.Fragment.ConversationFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedMessagesCount = 0

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

        updateSelectedItemsCount(0)
        binding.icSelecteAll.setOnCheckedChangeListener { _, isChecked ->
            val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
            fragment?.toggleSelectAll(isChecked)

        }
        binding.icSelecteAll.setBackgroundResource(R.drawable.round_checkbox)
        binding.icDrawer.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.icDelete.setOnClickListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
            fragment?.deleteSelectedMessages()
        }
    }

    private fun loadConversationFragment() {
        val fragment = ConversationFragment()
        fragment.onSelectionChanged = { count ->
            updateSelectedItemsCount(count)
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }


    private fun updateSelectedItemsCount(count: Int) {
        selectedMessagesCount = count
        binding.txtSelectedCount.text = "$selectedMessagesCount"+ " "+getString(R.string.selected)
        if (selectedMessagesCount > 0 ) {
            binding.lySelectedItems.visibility = View.VISIBLE
            binding.selectMenu.visibility = View.VISIBLE
            binding.toolBar.visibility = View.GONE
        } else {
            binding.lySelectedItems.visibility = View.GONE
            binding.selectMenu.visibility = View.GONE
            binding.toolBar.visibility = View.VISIBLE
        }
    }

    override fun onBackPressed() {
        if (selectedMessagesCount > 0) {
            updateSelectedItemsCount(0)
            val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? ConversationFragment
            fragment?.clearSelection()
        } else {
            super.onBackPressed()
        }
    }
}
