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
import com.test.messages.demo.databinding.DialogSwipeActionBinding

class SwipeActionDialog(
    context: Context,
    private val selectedAction: String,
    private val onActionSelected: (String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogSwipeActionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSwipeActionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ContextCompat.getDrawable(context, android.R.color.transparent))
        window?.setGravity(Gravity.BOTTOM)

        val radioButtons = listOf(
            binding.rbNone,
            binding.rbDelete,
            binding.rbArchive,
            binding.rbCall,
            binding.rbMarkRead,
            binding.rbMarkUnread
        )


        radioButtons.forEach { radioButton ->
            val isSelected = radioButton.text.toString() == selectedAction
            radioButton.isChecked = isSelected
            updateRadioButtonTint()

            radioButton.setOnCheckedChangeListener { _, _ ->
                updateRadioButtonTint() // Update tint on selection change
            }
        }

// Handle OK button click
        binding.btnOk.setOnClickListener {
            val selectedRadioButtonId = binding.radioGroupSwipeActions.checkedRadioButtonId
            if (selectedRadioButtonId != -1) {
                val selectedText = findViewById<RadioButton>(selectedRadioButtonId)?.text.toString()
                onActionSelected(selectedText)
            }
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
            binding.rbNone,
            binding.rbDelete,
            binding.rbArchive,
            binding.rbCall,
            binding.rbMarkRead,
            binding.rbMarkUnread
        )

        for (radio in radioButtons) {
            val color = if (radio.isChecked) colorPrimary else defaultColor
            radio.buttonTintList = ColorStateList.valueOf(color)  // Change icon tint only
        }
    }

}