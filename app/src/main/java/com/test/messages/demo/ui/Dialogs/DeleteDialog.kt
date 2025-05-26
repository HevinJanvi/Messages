package com.test.messages.demo.ui.Dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.test.messages.demo.R
import com.test.messages.demo.Util.ActivityFinishEvent
import com.test.messages.demo.Util.ViewUtils.blinkThen
import org.greenrobot.eventbus.EventBus

class DeleteDialog(
    context: Context,
    private val from: String,
    private val shouldFinish: Boolean,
    private val onDeleteConfirmed: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_delete_confirmation)

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

        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        val btnConfirmDelete = findViewById<TextView>(R.id.btnConfirmDelete)
        val txtDialogMessage = findViewById<TextView>(R.id.txtDialogMessage)
        val txtDialogTitle = findViewById<TextView>(R.id.txtDialogTitle)
        Log.d("TAG", "onCreate: binnn "+from)
        if (from.equals("recyclebin")) {
            txtDialogTitle.setText(context.getString(R.string.delete_conversation))
            btnConfirmDelete.setText(context.getString(R.string.delete))
            txtDialogMessage.setText(context.getString(R.string.permanently_delete_conversation))
        } else {
            if (from.equals("conversation_chat")) {
                txtDialogTitle.setText(context.getString(R.string.move_to_bin))
                txtDialogMessage.setText(context.getString(R.string.selected_message_move))
            } else {
                txtDialogTitle.setText(context.getString(R.string.move_to_bin))
                txtDialogMessage.setText(context.getString(R.string.move_this_conversation_to_the_rrecycle_bin))
            }
            btnConfirmDelete.setText(context.getString(R.string.move))
        }

        btnCancel.setOnClickListener {
            it.blinkThen {
                dismiss()
            }
        }
        btnConfirmDelete.setOnClickListener {
            it.blinkThen {
                onDeleteConfirmed.invoke()
                dismiss()
                if (shouldFinish) {
                    EventBus.getDefault().post(ActivityFinishEvent(true))
                }
            }
        }
    }
}
