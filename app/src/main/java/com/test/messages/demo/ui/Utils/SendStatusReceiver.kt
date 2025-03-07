package com.test.messages.demo.ui.Utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Looper

abstract class SendStatusReceiver : BroadcastReceiver() {
    abstract fun updateAndroidDatabase(context: Context, intent: Intent, receiverResultCode: Int)

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
        const val SMS_SENT_ACTION = "receiver.SMS_SENT"
        const val EXTRA_ERROR_CODE = "errorCode"
        const val EXTRA_SUB_ID = "subId"
        const val NO_ERROR_CODE = -1
    }
}
