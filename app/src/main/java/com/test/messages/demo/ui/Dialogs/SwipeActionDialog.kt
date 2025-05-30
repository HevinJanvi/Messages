package com.test.messages.demo.ui.Dialogs

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.test.messages.demo.R
import com.test.messages.demo.Util.Constants
import com.test.messages.demo.Util.Constants.SWIPE_NONE
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.Util.ViewUtils.blinkThen
import com.test.messages.demo.databinding.DialogSwipeActionBinding

class SwipeActionDialog(
    context: Context,
    private val selectedAction: Int,
    private val isRightSwipe: Boolean,
    private val onActionSelected: (Int) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogSwipeActionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSwipeActionBinding.inflate(layoutInflater)
        setContentView(binding.root)
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

        val radioButtons = mapOf(
            binding.rbNone to Constants.SWIPE_NONE,
            binding.rbDelete to Constants.SWIPE_DELETE,
            binding.rbArchive to Constants.SWIPE_ARCHIVE,
            binding.rbCall to Constants.SWIPE_CALL,
            binding.rbMarkRead to Constants.SWIPE_MARK_READ,
            binding.rbMarkUnread to Constants.SWIPE_MARK_UNREAD
        )
        radioButtons.forEach { (radioButton, action) ->
            radioButton.isChecked = (action == selectedAction)
            updateRadioButtonTint()

            radioButton.setOnCheckedChangeListener { _, _ ->
                updateRadioButtonTint()
            }
        }
        binding.btnOk.setOnClickListener {
            it.blinkThen {
                val selectedAction =
                    radioButtons.entries.find { it.key.isChecked }?.value ?: SWIPE_NONE
                ViewUtils.saveSwipeAction(context, selectedAction, isRightSwipe)
                onActionSelected(selectedAction)
                dismiss()
            }
        }

        binding.btnCancel.setOnClickListener {
            it.blinkThen {
                dismiss()
            }
        }

    }

    private fun updateRadioButtonTint() {
        val colorPrimary = ContextCompat.getColor(context, R.color.colorPrimary)
        val defaultColor = ContextCompat.getColor(context, R.color.gray)

        val radioButtons = listOf(
            binding.rbNone,
            binding.rbDelete,
            binding.rbArchive,
            binding.rbCall,
            binding.rbMarkRead,
            binding.rbMarkUnread
        )

        for (radio in radioButtons) {
            val color = if (radio.isChecked) colorPrimary else defaultColor
            radio.buttonTintList = ColorStateList.valueOf(color)
        }
    }


}