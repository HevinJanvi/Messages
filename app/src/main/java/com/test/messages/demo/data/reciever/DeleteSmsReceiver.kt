package com.test.messages.demo.data.reciever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.CommanConstants.IS_MMS
import com.test.messages.demo.Util.CommanConstants.MESSAGE_ID
import com.test.messages.demo.ui.send.notificationManager

class DeleteSmsReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(CommanConstants.EXTRA_THREAD_ID, 0L)
        val messageId = intent.getLongExtra(MESSAGE_ID, 0L)
        val isMms = intent.getBooleanExtra(IS_MMS, false)
        Log.d("TAG", "onReceive:delet "+threadId.hashCode())
        context.notificationManager.cancel(threadId.hashCode())

    }
}
