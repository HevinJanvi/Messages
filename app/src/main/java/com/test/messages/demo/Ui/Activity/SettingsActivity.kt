package com.test.messages.demo.Ui.Activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.test.messages.demo.R
import com.test.messages.demo.Helper.Constants
import com.test.messages.demo.Helper.Constants.LAUNCHFROM
import com.test.messages.demo.Helper.Constants.THEMEMODE
import com.test.messages.demo.databinding.ActivitySettingsBinding
import com.test.messages.demo.Utils.ViewUtils
import com.test.messages.demo.Utils.ViewUtils.getLanguageName
import com.test.messages.demo.Ui.Dialogs.FontsizeDialog

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding
    var editor: SharedPreferences.Editor? = null
    private var selectedLanguage: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(binding.rootView)
        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        val sharedPref = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        editor = sharedPref.edit()
        val selectedMode = sharedPref.getInt(THEMEMODE, 1)
        when (selectedMode) {
            1 -> binding.themeMode.text = getString(R.string.system_default)
            2 -> binding.themeMode.text = getString(R.string.light)
            3 -> binding.themeMode.text = getString(R.string.dark)
        }
        binding.themeLy.setOnClickListener {
            val intent = Intent(this, ThemeActivity::class.java)
            startActivityForResult(intent, 102)
        }
        selectedLanguage = ViewUtils.getSelectedLanguage(this)
        binding.selectLang.setText(getLanguageName(this,selectedLanguage))
        binding.langLy.setOnClickListener {
            val intent = Intent(this, LanguageActivity::class.java)
            intent.putExtra(LAUNCHFROM, "settings")
            startActivityForResult(intent, 100)

        }
        binding.categoryLy.setOnClickListener {
            val categories = ViewUtils.getCategoriesFromPrefs(this)
            val intent = Intent(this, EditCategoryActivity::class.java)
            intent.putStringArrayListExtra("category_list", ArrayList(categories))
            startActivity(intent)
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
            Constants.ACTION_SMALL -> getString(R.string.small)
            Constants.ACTION_NORMAL -> getString(R.string.normal)
            Constants.ACTION_LARGE -> getString(R.string.large)
            Constants.ACTION_EXTRALARGE -> getString(R.string.extra_large)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            recreate()
        } else if (requestCode == 102 && resultCode == RESULT_OK) {
            val selectedTheme = data?.getIntExtra(THEMEMODE, 1) ?: 1
            when (selectedTheme) {
                1 -> binding.themeMode.text = getString(R.string.system_default)
                2 -> binding.themeMode.text = getString(R.string.light)
                3 -> binding.themeMode.text = getString(R.string.dark)
            }
        }

    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        finish()
    }

}