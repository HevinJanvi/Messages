package com.test.messages.demo.data.reciever

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

class CopyOtpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val body = intent.getStringExtra("BODY") ?: return
        val otp = extractOtp(body)
        if (otp.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("OTP", otp)
            clipboard.setPrimaryClip(clip)

        }
    }

    private fun extractOtp(text: String): String {
        val otpRegex = Regex("\\b\\d{4,8}\\b")
        return otpRegex.find(text)?.value ?: ""
    }
}

