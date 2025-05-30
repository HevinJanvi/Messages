package com.test.messages.demo.ui.Activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityIntroBinding
import com.test.messages.demo.ui.Adapter.IntroPagerAdapter
import com.test.messages.demo.Util.ViewUtils

class IntroActivity : BaseActivity() {

    private lateinit var binding: ActivityIntroBinding
    private val images = arrayOf(
        R.drawable.intro_img1, R.drawable.intro_img2, R.drawable.intro_img3
    )
    private lateinit var titles: Array<String>
    private lateinit var descriptions: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(binding.rootView)
        val selectedLanguage = ViewUtils.getSelectedLanguage(this)
        Log.d("TAG", "Selected Language on Splash i: $selectedLanguage")

        titles = arrayOf(
            getString(R.string.intro_txt1),
            getString(R.string.intro_txt2),
            getString(R.string.intro_txt3)
        )
        descriptions = arrayOf(
            getString(R.string.intro_subtxt1),
            getString(R.string.intro_subtxt2),
            getString(R.string.intro_subtxt3)
        )

        val adapter = IntroPagerAdapter(this, images)
        binding.viewPager.adapter = adapter
        binding.dotsIndicator.attachTo(binding.viewPager)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == adapter.itemCount - 1) {
                    binding.skipButton.visibility = View.INVISIBLE
                } else {
                    binding.skipButton.visibility = View.VISIBLE
                }
                binding.introTitle.text = titles[position]
                binding.introDescription.text = descriptions[position]
            }
        })


        binding.nextButton.setOnClickListener {
            val blinkAnim = ObjectAnimator.ofFloat(binding.nextButton, View.ALPHA, 1f, 0.8f, 1f)
            blinkAnim.duration = 200
            blinkAnim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val nextPage = binding.viewPager.currentItem + 1
                    if (nextPage < binding.viewPager.adapter?.itemCount ?: 0) {
                        binding.viewPager.currentItem = nextPage
                    } else {
                        completeIntro()
                    }
                }
            })
            blinkAnim.start()
        }

        binding.skipButton.setOnClickListener {
            val animator = ObjectAnimator.ofFloat(binding.skipButton, View.ALPHA, 1f, 0.3f, 1f)
            animator.duration = 800
            animator.start()
            completeIntro()
        }

    }

    private fun completeIntro() {
        ViewUtils.setIntroShown(this)
        startActivity(Intent(this, SmsPermissionActivity::class.java))
        finish()
    }
}