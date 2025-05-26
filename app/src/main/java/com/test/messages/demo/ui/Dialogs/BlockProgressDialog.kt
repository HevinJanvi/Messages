package com.test.messages.demo.ui.Dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.LayoutInflater
import android.widget.TextView
import com.test.messages.demo.R

class BlockProgressDialog(private val context: Context) {

    private var dialog: Dialog? = null

    fun show(message: String = context.getString(R.string.block_messages)) {
        if (dialog?.isShowing == true) return

        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)

            val view = LayoutInflater.from(context).inflate(R.layout.dialog_delete_progress, null)
            setContentView(view)

            var dialogtext= view.findViewById<TextView>(R.id.dialogText)
            dialogtext.setText(R.string.block_messages_progress)

            window?.apply {
                setGravity(Gravity.BOTTOM)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            show()
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    fun isShowing(): Boolean {
        return dialog?.isShowing == true
    }
}
