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
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.Util.ViewUtils.blinkThen
import com.test.messages.demo.databinding.DialogFontSizeBinding

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
            binding.rbSmall to Constants.ACTION_SMALL,
            binding.rbNormal to Constants.ACTION_NORMAL,
            binding.rbLarge to Constants.ACTION_LARGE,
            binding.rbExtraLarge to Constants.ACTION_EXTRALARGE,
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
                    radioButtons.entries.find { it.key.isChecked }?.value
                        ?: Constants.ACTION_NORMAL
                ViewUtils.saveFontSize(context, selectedAction)
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