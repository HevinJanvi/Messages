package com.test.messages.demo.data.reciever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.MARK_AS_READ
import com.test.messages.demo.Util.MarkasreadEvent
import com.test.messages.demo.Util.SmsUtils
import com.test.messages.demo.data.repository.MessageRepository
import com.test.messages.demo.ui.send.notificationManager
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class MarkAsReadReceiver : BroadcastReceiver() {
    @Inject
    lateinit var repository: MessageRepository

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MARK_AS_READ -> {
                val threadId = intent.getLongExtra(EXTRA_THREAD_ID, 0L)
                context.notificationManager.cancel(threadId.hashCode())
                SmsUtils.markThreadAsRead(context, threadId)
                EventBus.getDefault().post(MarkasreadEvent(threadId,true))
            }
        }
    }
}
