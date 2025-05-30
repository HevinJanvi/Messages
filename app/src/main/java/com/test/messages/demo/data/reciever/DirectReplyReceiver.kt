package com.test.messages.demo.data.reciever

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresApi
import androidx.core.app.RemoteInput
import com.test.messages.demo.Util.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.Constants.MESSAGE_ID
import com.test.messages.demo.Util.Constants.NAME
import com.test.messages.demo.Util.Constants.NUMBER
import com.test.messages.demo.Util.Constants.REPLY
import com.test.messages.demo.Util.MarkasreadEvent
import com.test.messages.demo.Util.SmsUtils
import com.test.messages.demo.Util.ViewUtils.getUseSIMIdAtNumber
import com.test.messages.demo.data.repository.MessageRepository
import com.test.messages.demo.ui.send.MessageUtils
import com.test.messages.demo.ui.send.SmsSend
import com.test.messages.demo.ui.send.notificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class DirectReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MessageRepository

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val address = intent.getStringExtra(NUMBER)
        val senderName = intent.getStringExtra(NAME)
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, 0L)
        val messageId = intent.getLongExtra(MESSAGE_ID, 0L)
        var body =
            RemoteInput.getResultsFromIntent(intent)?.getCharSequence(REPLY)?.toString() ?: return

        if (address != null) {
            var subscriptionId: Int? = null

            val subscriptionManager =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeSims = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                subscriptionManager.activeSubscriptionInfoList
            } else {
                emptyList()
            }

            if (!activeSims.isNullOrEmpty()) {
                val simIndex = getUseSIMIdAtNumber(context, address)
                val selectedSim: SubscriptionInfo? = activeSims.getOrNull(simIndex)
                subscriptionId = selectedSim?.subscriptionId
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val sender = SmsSend(context, MessageUtils(context))
                    sender.sendSmsMessage(
                        text = body,
                        addresses = setOf(address),
                        subId = subscriptionId ?: SmsManager.getDefaultSmsSubscriptionId(),
                        requireDeliveryReport = false
                    )

                    delay(300)

                    withContext(Dispatchers.Main) {
                        context.notificationHelper.showMessageNotification(
                            messageId,
                            address,
                            body,
                            threadId,
                            null,
                            senderName,
                            alertOnlyOnce = true
                        )
                    }
                    SmsUtils.markThreadAsRead(context, threadId)
                    repository.getMessages()
                    EventBus.getDefault().post(MarkasreadEvent(threadId, true))

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }
}
