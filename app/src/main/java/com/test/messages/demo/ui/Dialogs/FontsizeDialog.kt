package com.test.messages.demo.ui.Dialogs

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.CommanConstants.SWIPE_NONE
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.databinding.DialogFontSizeBinding
import com.test.messages.demo.databinding.DialogSwipeActionBinding

class FontsizeDialog(
    context: Context,
    private val selectedAction: Int,
    private val onActionSelected: (Int) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogFontSizeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogFontSizeBinding.inflate(layoutInflater)
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
            binding.rbSmall to CommanConstants.ACTION_SMALL,
            binding.rbNormal to CommanConstants.ACTION_NORMAL,
            binding.rbLarge to CommanConstants.ACTION_LARGE,
            binding.rbExtraLarge to CommanConstants.ACTION_EXTRALARGE,
        )
        radioButtons.forEach { (radioButton, action) ->
            radioButton.isChecked = (action == selectedAction)
            updateRadioButtonTint()

            radioButton.setOnCheckedChangeListener { _, _ ->
                updateRadioButtonTint()
            }
        }
        binding.btnOk.setOnClickListener {
            val selectedAction =
                radioButtons.entries.find { it.key.isChecked }?.value ?: CommanConstants.ACTION_NORMAL
            ViewUtils.saveFontSize(context, selectedAction)
            onActionSelected(selectedAction)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

    }

    private fun updateRadioButtonTint() {
        val colorPrimary = ContextCompat.getColor(context, R.color.colorPrimary)
        val defaultColor = ContextCompat.getColor(context, R.color.gray)

        val radioButtons = listOf(
            binding.rbSmall,
            binding.rbNormal,
            binding.rbLarge,
            binding.rbExtraLarge,
        )

        for (radio in radioButtons) {
            val color = if (radio.isChecked) colorPrimary else defaultColor
            radio.buttonTintList = ColorStateList.valueOf(color)
        }
    }


}