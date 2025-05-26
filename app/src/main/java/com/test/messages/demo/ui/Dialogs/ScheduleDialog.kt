package com.test.messages.demo.ui.Dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.test.messages.demo.R

class ScheduleDialog(
    context: Context,
    private val onDeleteConfirmed: () -> Unit,
    private val onSendNow: () -> Unit,
    private val onCopyText: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_schedule)

        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ContextCompat.getDrawable(context, android.R.color.transparent))
        window?.setGravity(Gravity.BOTTOM)


        val delete = findViewById<ConstraintLayout>(R.id.lyDelete)
        val copy = findViewById<ConstraintLayout>(R.id.lyCopy)
        val sendNow = findViewById<ConstraintLayout>(R.id.lySend)

        delete.setOnClickListener {
            Log.d("TAG", "onCreate:showScheduleDialog ")
            dismiss()
            onDeleteConfirmed.invoke()

        }
        sendNow.setOnClickListener {
            onSendNow.invoke()
            dismiss()
        }
        copy.setOnClickListener {
            onCopyText.invoke()
            dismiss()
        }
    }
}