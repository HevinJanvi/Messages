package com.test.messages.demo.Ui.Dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.provider.Telephony
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.test.messages.demo.R
import com.test.messages.demo.Utils.ViewUtils.blinkThen
import com.test.messages.demo.data.Model.ConversationItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MessageDetailsDialog(
    context: Context,
    private val message: ConversationItem
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_message_details)

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

        val txtType = findViewById<TextView>(R.id.type)
        val txtTo = findViewById<TextView>(R.id.to)
        val txtSent = findViewById<TextView>(R.id.sent)
        val btnOk = findViewById<TextView>(R.id.btnOk)


        val type = when (message.type) {
            Telephony.Sms.MESSAGE_TYPE_INBOX -> context.getString(R.string.sms_received)
            Telephony.Sms.MESSAGE_TYPE_SENT -> context.getString(R.string.sms_sent)
            Telephony.Sms.MESSAGE_TYPE_OUTBOX -> context.getString(R.string.sms_outbox)
            Telephony.Sms.MESSAGE_TYPE_FAILED -> context.getString(R.string.sms_failed)
            Telephony.Sms.MESSAGE_TYPE_QUEUED -> context.getString(R.string.sms_queued)
            else -> context.getString(R.string.unknown)
        }


        val dateFormatted = SimpleDateFormat("dd/MM/yyyy, hh:mm:ss a", Locale.getDefault())
            .format(Date(message.date))

        txtType.text = " : $type"
        txtTo.text =  "${message.address ?: context.getString(R.string.unknown)}"
        txtSent.text = " : $dateFormatted"

        btnOk.setOnClickListener {
            it.blinkThen {dismiss() }}

    }
}
