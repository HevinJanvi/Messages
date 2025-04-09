package com.test.messages.demo.Util

import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView

class CustomLinkMovementMethod : LinkMovementMethod() {
        private var longPressTriggered = false
        private var handler: Handler? = null
        private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()

        override fun onTouchEvent(
            widget: TextView,
            buffer: Spannable,
            event: MotionEvent,
        ): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    longPressTriggered = false
                    handler = Handler(Looper.getMainLooper())
                    handler?.postDelayed({
                        longPressTriggered = true
                        widget.performLongClick()
                    }, longPressTimeout)
                }

                MotionEvent.ACTION_MOVE -> {
                    handler?.removeCallbacksAndMessages(null)
                }

                MotionEvent.ACTION_UP -> {
                    handler?.removeCallbacksAndMessages(null)
                    if (longPressTriggered) return true
                }

                MotionEvent.ACTION_CANCEL -> {
                    handler?.removeCallbacksAndMessages(null)
                }
            }
            return super.onTouchEvent(widget, buffer, event)
        }

        override fun canSelectArbitrarily(): Boolean {
            return true
        }
    }