package com.test.messages.demo.ui.Dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.test.messages.demo.R
import com.test.messages.demo.Util.ViewUtils.blinkThen


class RenameDialog(
    context: Context, var name: String,
    private val onConfirmed: (String) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_rename)

        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(
            ContextCompat.getDrawable(
                context,
                android.R.color.transparent
            )
        )
        window?.setGravity(Gravity.BOTTOM)

        val txtEdittext = findViewById<EditText>(R.id.txtEdittext)
        txtEdittext.setText(name)
        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        val btnConfirm = findViewById<TextView>(R.id.btnConfirm)

        btnCancel.setOnClickListener {
            it.blinkThen { dismiss() }
        }
        btnConfirm.setOnClickListener {
            it.blinkThen {
                val updatedName = txtEdittext.text.toString().trim()
                if (updatedName.isNotEmpty()) {
                    onConfirmed.invoke(updatedName)
                }
                dismiss()
            }
        }
    }
}
