package com.test.messages.demo.ui.Dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.test.messages.demo.R
import com.test.messages.demo.ui.Utils.ViewUtils.getNotificationOption
import com.test.messages.demo.ui.Utils.ViewUtils.saveNotificationOption


class NotificationViewDialog(
    context: Context,
    private val onOptionSelected: (Int) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_notification_view)

        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ContextCompat.getDrawable(context, android.R.color.transparent))
        window?.setGravity(Gravity.BOTTOM)

        val btnOk = findViewById<TextView>(R.id.btnOk)

        val check1 = findViewById<RadioButton>(R.id.check1)
        val check2 = findViewById<RadioButton>(R.id.check2)
        val check3 = findViewById<RadioButton>(R.id.check3)

        val savedOption = getNotificationOption(context)

        when (savedOption) {
            0 -> check1.isChecked = true
            1 -> check2.isChecked = true
            2 -> check3.isChecked = true
        }

        val radioButtons = listOf(check1, check2, check3)
        radioButtons.forEach { radioButton ->
            radioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    radioButtons.forEach { other ->
                        if (other != radioButton) other.isChecked = false
                    }
                }
            }
        }

        btnOk.setOnClickListener {
            val selectedOption = when {
                check1.isChecked -> 0
                check2.isChecked -> 1
                check3.isChecked -> 2
                else -> 0
            }
            saveNotificationOption(context, selectedOption)
            onOptionSelected(selectedOption)
            dismiss()
        }
    }
}
