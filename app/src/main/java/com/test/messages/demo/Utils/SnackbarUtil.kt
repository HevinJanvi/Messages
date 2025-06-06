package com.test.messages.demo.Utils

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import com.test.messages.demo.R

object SnackbarUtil {
    fun showSnackbar(view: View, message: String, actionText: String? = null, action: (() -> Unit)? = null) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(view.context, R.color.snackbar_bg))
                val snackbarView = snackbar.view

        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(view.context, R.color.snackbar_text))

        val typeface = ResourcesCompat.getFont(view.context, R.font.product_sans_regular)
        textView.typeface = typeface

        actionText?.let {
            snackbar.setAction(it) { action?.invoke() }
                .setActionTextColor(ContextCompat.getColor(view.context, R.color.colorPrimary))
            val actionTextView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
            actionTextView.typeface = typeface
        }
        snackbar.show()
    }
}
