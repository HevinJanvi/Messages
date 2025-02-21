package com.test.messages.demo.ui.Utils

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.test.messages.demo.R

class PopupMenuHelper(private val context: Context) {
    fun showPopup(anchor: View) {
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_menu, null)
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)

        // Get screen width
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Get anchor position
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]

        val marginEnd = 540 // Change this value for more or less margin (in pixels)
        val popupX = screenWidth - popupWindow.contentView.measuredWidth - marginEnd
        val popupY = anchorY + anchor.height // Show below the anchor view
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, popupX, popupY)
    }
}
