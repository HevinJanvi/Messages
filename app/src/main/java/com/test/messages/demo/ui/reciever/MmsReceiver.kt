package com.test.messages.demo.ui.reciever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle MMS-related broadcasts here
        if (intent.action == "android.provider.Telephony.WAP_PUSH_RECEIVED") {
            // Process the MMS push message
        }
    }
}