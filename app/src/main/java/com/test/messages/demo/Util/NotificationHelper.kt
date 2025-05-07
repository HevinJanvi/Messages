package com.test.messages.demo.Util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.test.messages.demo.R
import com.test.messages.demo.Util.CommanConstants.EXTRA_THREAD_ID
import com.test.messages.demo.Util.CommanConstants.LOCK_SCREEN_SENDER
import com.test.messages.demo.Util.CommanConstants.LOCK_SCREEN_SENDER_MESSAGE
import com.test.messages.demo.Util.CommanConstants.MARK_AS_READ
import com.test.messages.demo.Util.CommanConstants.MESSAGE_ID
import com.test.messages.demo.Util.CommanConstants.NAME
import com.test.messages.demo.Util.CommanConstants.NOTIFICATION_CHANNEL
import com.test.messages.demo.Util.CommanConstants.NUMBER
import com.test.messages.demo.Util.CommanConstants.REPLY
import com.test.messages.demo.Util.ViewUtils.generateRandomId
import com.test.messages.demo.Util.ViewUtils.getLockScreenVisibilitySetting
import com.test.messages.demo.Util.ViewUtils.getPreviewOption
import com.test.messages.demo.Util.ViewUtils.isNougatPlus
import com.test.messages.demo.Util.ViewUtils.isShortCodeWithLetters
import com.test.messages.demo.Util.ViewUtils.updateMessageCount
import com.test.messages.demo.data.reciever.DeleteSmsReceiver
import com.test.messages.demo.data.reciever.DirectReplyReceiver
import com.test.messages.demo.data.reciever.MarkAsReadReceiver
import com.test.messages.demo.ui.Activity.ConversationActivity
import com.test.messages.demo.ui.Activity.MainActivity
import com.test.messages.demo.ui.send.notificationManager
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class NotificationHelper(private val context: Context) {

    private val notificationManager = context.notificationManager
    var previewOption: Int = 0

    private val soundUri get() = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    private val user = Person.Builder()
        .setName(context.getString(R.string.me))
        .build()

    private var currentOpenThreadId: Long = -1

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onConversationOpened(event: ConversationOpenedEvent) {
        currentOpenThreadId = event.threadId
    }

    fun createMessagingStyle(
        threadId: Int,
        senderNumber: String,
        message: String,
        senderName: String?,
        previewOption: Int
    ): NotificationCompat.MessagingStyle {

//        val senderName = getContactName(context, senderNumber)
        val userPerson = Person.Builder().setName(context.getString(R.string.me)).build()
        val style = NotificationCompat.MessagingStyle(userPerson)

        if (previewOption == 0) {
            getOldMessages(threadId).forEach {
                style.addMessage(it)
            }
        } else {
        }

        val count = updateMessageCount(context, threadId.toLong())

        val messageText = when (previewOption) {
            0 -> message
            1, 2 -> {
                if (count == 1) {
                    "$count ${context.getString(R.string.new_message)}"
                } else {
                    "$count ${context.getString(R.string.new_messages)}"
                }
            }

            else -> message
        }

        val senderDisplay = if (previewOption == 2) "" else senderName

        val msg = NotificationCompat.MessagingStyle.Message(
            messageText,
            System.currentTimeMillis(),
            Person.Builder().setName(senderDisplay).build()
        )
        style.addMessage(msg)
        return style
    }

    @SuppressLint("NewApi")
    suspend fun showMessageNotification(
        messageId: Long,
        address: String,
        body: String,
        threadId: Long,
        bitmap: Bitmap?,
        sender: String?,
        alertOnlyOnce: Boolean = false
    ) {
        maybeCreateChannel(name = context.getString(R.string.channel_received_sms))

        val notificationId = threadId.hashCode()
        if(previewOption!=null){
            previewOption = getPreviewOption(context, threadId) ?: 0
        }else{
            previewOption=0
        }

        val contentIntent = Intent(context, ConversationActivity::class.java).apply {
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(NAME, sender)
            putExtra(NUMBER, address)
        }
        /*val contentPendingIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )*/
        val contentPendingIntent = TaskStackBuilder.create(context).run {
            addParentStack(ConversationActivity::class.java)
            addNextIntent(contentIntent)

            getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        }
        val markAsReadIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = MARK_AS_READ
            putExtra(EXTRA_THREAD_ID, threadId)
        }
        val markAsReadPendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId,
                markAsReadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

        val deleteSmsIntent = Intent(context, DeleteSmsReceiver::class.java).apply {
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(MESSAGE_ID, messageId)
        }

        val deleteSmsPendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId,
                deleteSmsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )


        var replyAction: NotificationCompat.Action? = null
        val isNoReplySms = isShortCodeWithLetters(address)
        if (isNougatPlus() && !isNoReplySms ) {
            if (previewOption == 0) {
                val replyLabel = context.getString(R.string.reply)
                val remoteInput = RemoteInput.Builder(REPLY)
                    .setLabel(replyLabel)
                    .build()

                val replyIntent = Intent(context, DirectReplyReceiver::class.java).apply {
                    putExtra(EXTRA_THREAD_ID, threadId)
                    putExtra(NUMBER, address)
                    putExtra(NAME, sender)
                    putExtra(MESSAGE_ID, messageId)
                }

                val replyPendingIntent =
                    PendingIntent.getBroadcast(
                        context.applicationContext,
                        notificationId,
                        replyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                replyAction = NotificationCompat.Action.Builder(
                    R.drawable.ic_send_enable,
                    replyLabel,
                    replyPendingIntent
                )
                    .addRemoteInput(remoteInput)
                    .build()
            }

        }


        val largeIcon = bitmap ?: if (sender != null) {
//            SimpleContactsHelper(context).getContactLetterIcon(sender)
        } else {
            null
        }

        val lockScreenVisibilitySetting = getLockScreenVisibilitySetting(context)

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL).apply {
            when (lockScreenVisibilitySetting) {
                LOCK_SCREEN_SENDER_MESSAGE -> {
                    setLargeIcon(largeIcon)
                    setStyle(getMessagesStyle(address, body, notificationId, sender))
                }

                LOCK_SCREEN_SENDER -> {
                    setContentTitle(sender)
                    setLargeIcon(largeIcon)
                    val summaryText = context.getString(R.string.new_message)
                    setStyle(
                        NotificationCompat.BigTextStyle().setSummaryText(summaryText).bigText(body)
                    )
                }
            }

            val style = createMessagingStyle(notificationId, address, body,sender, previewOption)

            val primaryColor = ContextCompat.getColor(context, R.color.colorPrimary)
            color = primaryColor
            setStyle(style)
            setSmallIcon(R.drawable.notification_logo)
            setContentIntent(contentPendingIntent)
            priority = NotificationCompat.PRIORITY_MAX
            setDefaults(Notification.DEFAULT_LIGHTS)
            setCategory(Notification.CATEGORY_MESSAGE)
            setAutoCancel(true)
            setOnlyAlertOnce(alertOnlyOnce)
            setSound(soundUri, AudioManager.STREAM_NOTIFICATION)
        }

        if (replyAction != null && lockScreenVisibilitySetting == LOCK_SCREEN_SENDER_MESSAGE) {
            builder.addAction(replyAction)
        }

        builder.addAction(
            R.drawable.ic_selected,
            context.getString(R.string.mark_as_read),
            markAsReadPendingIntent
        )
            .setChannelId(NOTIFICATION_CHANNEL)
        if (isNoReplySms) {
            builder.addAction(
                R.drawable.ic_delete,
                context.getString(R.string.clear),
                deleteSmsPendingIntent
            ).setChannelId(NOTIFICATION_CHANNEL)
        }
        notificationManager.notify(notificationId, builder.build())
    }

    private fun setLargeIcon(largeIcon: Any?) {

    }

    @SuppressLint("NewApi")
    /*fun showSendingFailedNotification(recipientName: String, threadId: Long) {
        maybeCreateChannel(name = context.getString(R.string.message_not_sent_short))

        val notificationId = generateRandomId().hashCode()
        val intent = Intent(context, ConversationActivity::class.java).apply {
            putExtra(EXTRA_THREAD_ID, threadId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val summaryText =
            String.format(context.getString(R.string.message_sending_error), recipientName)
//        val largeIcon = SimpleContactsHelper(context).getContactLetterIcon(recipientName)
        val primaryColor = ContextCompat.getColor(context, R.color.colorPrimary)

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setContentTitle(context.getString(R.string.message_not_sent_short))
            .setContentText(summaryText)
            .setColor(primaryColor)
            .setSmallIcon(R.drawable.notification_logo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setChannelId(NOTIFICATION_CHANNEL)

        notificationManager.notify(notificationId, builder.build())
    }*/

    fun maybeCreateChannel(name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                .build()

            val id = NOTIFICATION_CHANNEL
            val importance = IMPORTANCE_HIGH
            NotificationChannel(id, name, importance).apply {
                setBypassDnd(false)
                enableLights(true)
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    fun createNotificationChannel(context: Context, contactNumber: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "${CommanConstants.KEY_SMS_CHANNEL}$contactNumber"
            val channelName = "$contactNumber"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotificationChannelGlobal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = CommanConstants.KEY_SMS_CHANNEL
            val channelName = "Default"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getMessagesStyle(
        address: String,
        body: String,
        notificationId: Int,
        name: String?
    ): NotificationCompat.MessagingStyle {
        val sender = if (name != null) {
            Person.Builder()
                .setName(name)
                .setKey(address)
                .build()
        } else {
            null
        }

        return NotificationCompat.MessagingStyle(user).also { style ->
            getOldMessages(notificationId).forEach {
                style.addMessage(it)
            }
            val newMessage =
                NotificationCompat.MessagingStyle.Message(body, System.currentTimeMillis(), sender)
            style.addMessage(newMessage)
        }
    }

    private fun getOldMessages(notificationId: Int): List<NotificationCompat.MessagingStyle.Message> {
        if (!isNougatPlus()) {
            return emptyList()
        }
        val currentNotification =
            notificationManager.activeNotifications.find { it.id == notificationId }
        return if (currentNotification != null) {
            val activeStyle =
                NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(
                    currentNotification.notification
                )
            activeStyle?.messages.orEmpty()
        } else {
            emptyList()
        }
    }


}
