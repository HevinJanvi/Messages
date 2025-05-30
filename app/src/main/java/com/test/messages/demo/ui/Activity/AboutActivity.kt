package com.test.messages.demo.ui.Activity

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityAboutBinding

class AboutActivity : BaseActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(binding.rootView)

        binding.icBack.setOnClickListener {
            onBackPressed()
        }
        val versionName = try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
        binding.cuurutVersion.text = getString(R.string.current_version) + " " + "$versionName"

        binding.terms.setOnClickListener {
            binding.terms.animate()
                .alpha(0.5f)
                .setDuration(100)
                .withEndAction {
                    binding.terms.animate()
                        .alpha(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
            policy()
        }

        binding.policy.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100)
                }
            policy()
        }
    }

    fun policy() {
        val url = "https://yourprivacypolicy.com"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

}