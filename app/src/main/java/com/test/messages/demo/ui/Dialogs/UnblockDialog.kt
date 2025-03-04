package com.test.messages.demo.ui.Dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.test.messages.demo.R


class UnblockDialog(
    context: Context,
    private val onDeleteConfirmed: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_unblock_confirmation)

        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ContextCompat.getDrawable(context, android.R.color.transparent))
        window?.setGravity(Gravity.BOTTOM)

        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        val btnConfirmDelete = findViewById<TextView>(R.id.btnConfirmDelete)

        btnCancel.setOnClickListener { dismiss() }
        btnConfirmDelete.setOnClickListener {
            onDeleteConfirmed.invoke()
            dismiss()
        }
    }
}
