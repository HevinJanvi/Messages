package com.test.messages.demo.data.reciever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.test.messages.demo.Util.Constants
import com.test.messages.demo.Util.Constants.IS_MMS
import com.test.messages.demo.Util.Constants.MESSAGE_ID
import com.test.messages.demo.ui.send.notificationManager

class DeleteSmsReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(Constants.EXTRA_THREAD_ID, 0L)
        context.notificationManager.cancel(threadId.hashCode())

    }
}
