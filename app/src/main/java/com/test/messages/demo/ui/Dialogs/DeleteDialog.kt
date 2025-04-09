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
import com.test.messages.demo.Util.ViewUtils.blinkThen

class DeleteDialog(
    context: Context,
   private val fromBin:Boolean,
    private val onDeleteConfirmed: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_delete_confirmation)

        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ContextCompat.getDrawable(context, android.R.color.transparent))
        window?.setGravity(Gravity.BOTTOM)

        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        val btnConfirmDelete = findViewById<TextView>(R.id.btnConfirmDelete)

        if(fromBin){
            btnConfirmDelete.setText(context.getString(R.string.delete))
        }else{
            btnConfirmDelete.setText(context.getString(R.string.move_to_recycle_bin))
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
            }
        }
    }
}
