package com.test.messages.demo.ui.Utils  // Change this to match your package

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.animation.TranslateAnimation
import androidx.core.content.ContextCompat


object ViewUtils {
    private val fixedColor = android.R.color.darker_gray // Fixed color

    fun View.setVisibleWithAnimation(visible: Boolean, duration: Long = 300) {
        val context = this.context
        val startColor = if (visible) ContextCompat.getColor(context, android.R.color.transparent)
        else ContextCompat.getColor(context, fixedColor)
        val endColor = if (visible) ContextCompat.getColor(context, fixedColor)
        else ContextCompat.getColor(context, android.R.color.transparent)

        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)
        colorAnimation.duration = duration
        colorAnimation.addUpdateListener { animator -> this.setBackgroundColor(animator.animatedValue as Int) }

        if (visible) {
            this.visibility = View.VISIBLE
            colorAnimation.start()
        } else {
            colorAnimation.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    this@setVisibleWithAnimation.visibility = View.GONE
                }
            })
            colorAnimation.start()
        }

    }


}

