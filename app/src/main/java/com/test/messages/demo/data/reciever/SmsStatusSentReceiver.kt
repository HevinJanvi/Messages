package com.test.messages.demo.data.reciever

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony.Sms
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.test.messages.demo.Util.RefreshMessagesEvent
import com.test.messages.demo.ui.SMSend.MessageUtils

import org.greenrobot.eventbus.EventBus

class SmsStatusSentReceiver : SendStatusReceiver() {

    override fun updateAndroidDatabase(context: Context, intent: Intent, receiverResultCode: Int) {
        val messageUri: Uri? = intent.data
        val resultCode = resultCode
        val messagingUtils = MessageUtils(context)
        val groupId = intent.getLongExtra(SendStatusReceiver.EXTRA_GROUP_ID, -1L)
        val uri = intent.getStringExtra(SendStatusReceiver.EXTRA_GROUP_URI)
        var groupUri:Uri?=null
        if(uri!=null) {
            groupUri = Uri.parse(uri)
        }

        val type = if (resultCode == Activity.RESULT_OK) {
            Sms.MESSAGE_TYPE_SENT
        } else {
            Sms.MESSAGE_TYPE_FAILED
        }
        messagingUtils.updateSmsMessageSendingStatus(messageUri, type)
        if(groupId!=-1L&&groupUri!=null){
            Log.e("TAG", "updateAndroidDatabase: $groupUri : $type")
            messagingUtils.updateSmsMessageSendingStatus(groupUri,type)
        }

    }

    override fun updateAppDatabase(context: Context, intent: Intent, receiverResultCode: Int) {
        val messageUri = intent.data
        if (messageUri != null) {
            val messageId = messageUri.lastPathSegment?.toLong() ?: 0L
            ensureBackgroundThread {
                val type = if (receiverResultCode == Activity.RESULT_OK) {
                    Sms.MESSAGE_TYPE_SENT
                } else {
                    showSendingFailedNotification(context, messageId)
                    Sms.MESSAGE_TYPE_FAILED
                }
                EventBus.getDefault().post(RefreshMessagesEvent())

            }
        }
    }

    private fun showSendingFailedNotification(context: Context, messageId: Long) {
        Handler(Looper.getMainLooper()).post {
            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                return@post
            }
        }
    }



}
