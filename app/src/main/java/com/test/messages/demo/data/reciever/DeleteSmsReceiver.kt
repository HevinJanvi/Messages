package com.test.messages.demo.data.reciever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.test.messages.demo.Helper.Constants
import com.test.messages.demo.SMSHelper.notificationManager

class DeleteSmsReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(Constants.EXTRA_THREAD_ID, 0L)
        context.notificationManager.cancel(threadId.hashCode())

    }
}
