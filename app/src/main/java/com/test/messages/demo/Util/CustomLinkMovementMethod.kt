package com.test.messages.demo.Util

import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView

class CustomLinkMovementMethod(
    private val onLongClick: (() -> Unit)? = null
) : LinkMovementMethod() {
    private var handler: Handler? = null
    private var longPressed = false
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                longPressed = false
                handler = Handler(Looper.getMainLooper())
                handler?.postDelayed({
                    longPressed = true
                    onLongClick?.invoke()
                }, longPressTimeout)
            }

            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_CANCEL -> {
                handler?.removeCallbacksAndMessages(null)
            }

            MotionEvent.ACTION_UP -> {
                handler?.removeCallbacksAndMessages(null)
                if (longPressed) return true
            }
        }

        return super.onTouchEvent(widget, buffer, event)
    }

    override fun canSelectArbitrarily(): Boolean {
        return true
    }
}