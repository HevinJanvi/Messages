package com.test.messages.demo.ui.Utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Looper

abstract class SendStatusReceiver : BroadcastReceiver() {
    // Updates the status of the message in the internal database
    abstract fun updateAndroidDatabase(context: Context, intent: Intent, receiverResultCode: Int)

    // allows the implementer to update the status of the message in their database
    abstract fun updateAppDatabase(context: Context, intent: Intent, receiverResultCode: Int)

    override fun onReceive(context: Context, intent: Intent) {
        val resultCode = resultCode
        ensureBackgroundThread {
            updateAndroidDatabase(context, intent, resultCode)
            updateAppDatabase(context, intent, resultCode)
        }
    }

    fun ensureBackgroundThread(callback: () -> Unit) {
        if (isOnMainThread()) {
            Thread {
                callback()
            }.start()
        } else {
            callback()
        }
    }

    fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()

    companion object {
        const val SMS_SENT_ACTION = "com.simplemobiletools.smsmessenger.receiver.SMS_SENT"
        const val SMS_DELIVERED_ACTION = "com.simplemobiletools.smsmessenger.receiver.SMS_DELIVERED"

        // Defined by platform, but no constant provided. See docs for SmsManager.sendTextMessage.
        const val EXTRA_ERROR_CODE = "errorCode"
        const val EXTRA_SUB_ID = "subId"

        const val NO_ERROR_CODE = -1
    }
}
