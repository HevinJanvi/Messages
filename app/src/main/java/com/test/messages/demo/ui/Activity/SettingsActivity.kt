package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.databinding.ActivitySettingsBinding
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.ui.Dialogs.FontsizeDialog

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)

        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        binding.themeLy.setOnClickListener {
            val intent = Intent(this, ThemeActivity::class.java)
            startActivity(intent)
        }
        binding.langLy.setOnClickListener {
            val intent = Intent(this, LanguageActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.categoryLy.setOnClickListener {

            val categories = ViewUtils.getCategoriesFromPrefs(this)
            val intent = Intent(this, EditCategoryActivity::class.java)
            intent.putStringArrayListExtra("category_list", ArrayList(categories))
            startActivity(intent)
            finish()
        }
        binding.recycleLy.setOnClickListener {
            val intent = Intent(this, RecycleBinActivity::class.java)
            startActivity(intent)
        }
        binding.notificationLy.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        binding.fontLy.setOnClickListener {
            FontsizeDialog(this, ViewUtils.getFontSize(this)) { newFontSize ->
                updateFontSizeText(newFontSize)
            }.show()
        }
        val savedFontSize = ViewUtils.getFontSize(this)
        updateFontSizeText(savedFontSize)

        binding.swipeLy.setOnClickListener {
            val intent = Intent(this, SwipeActivity::class.java)
            startActivity(intent)
        }
        binding.policyLy.setOnClickListener {
            val url = "https://yourprivacypolicy.com"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
        binding.rateLy.setOnClickListener {
            rateApp()
        }
        binding.shareLy.setOnClickListener {
            shareApp()
        }
        binding.aboutLy.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateFontSizeText(fontSize: Int) {
        val fontLabel = when (fontSize) {
            CommanConstants.ACTION_SMALL -> getString(R.string.small)
            CommanConstants.ACTION_NORMAL -> getString(R.string.normal)
            CommanConstants.ACTION_LARGE -> getString(R.string.large)
            CommanConstants.ACTION_EXTRALARGE -> getString(R.string.extra_large)
            else -> getString(R.string.normal)
        }
        binding.fontSizeTextView.text = fontLabel
    }

    private fun shareApp() {
        val appPackageName = packageName
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out this awesome app: https://play.google.com/store/apps/details?id=$appPackageName"
            )
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun rateApp() {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
            startActivity(intent)
        }
    }

}