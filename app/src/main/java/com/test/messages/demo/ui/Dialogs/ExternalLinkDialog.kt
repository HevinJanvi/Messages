package com.test.messages.demo.ui.Dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.test.messages.demo.R
import com.test.messages.demo.Util.ViewUtils.blinkThen

class ExternalLinkDialog(
    context: Context, var link: String
) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_external_link)

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
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        val btnContinue = findViewById<TextView>(R.id.btnContinue)
        val checkAgree = findViewById<CheckBox>(R.id.checkAgree)

        btnContinue.isEnabled = false
        btnContinue.alpha = 0.5f
        btnContinue.setTextColor(context.resources.getColor(R.color.gray_txtcolor))
        checkAgree.setOnCheckedChangeListener { _, isChecked ->
            btnContinue.isEnabled = isChecked
            btnContinue.alpha = if (isChecked) 1.0f else 0.5f
            if(btnContinue.isEnabled){
                btnContinue.setTextColor(context.resources.getColor(R.color.colorPrimary))
            }else{
                btnContinue.setTextColor(context.resources.getColor(R.color.gray_txtcolor))
            }
        }

        btnCancel.setOnClickListener {
            it.blinkThen {dismiss() }}
        btnContinue.setOnClickListener {
            it.blinkThen {
                val fixedLink = if (link.startsWith("http://") || link.startsWith("https://")) {
                    link
                } else {
                    "http://$link"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixedLink))
                context.startActivity(intent)
                dismiss()
            }
        }

    }
}
