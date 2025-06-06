package com.test.messages.demo.Helper

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.test.messages.demo.R
import com.test.messages.demo.Helper.Constants.EXTRA_THREAD_ID
import com.test.messages.demo.Helper.Constants.LOCK_SCREEN_SENDER
import com.test.messages.demo.Helper.Constants.LOCK_SCREEN_SENDER_MESSAGE
import com.test.messages.demo.Helper.Constants.MARK_AS_READ
import com.test.messages.demo.Helper.Constants.MESSAGE_ID
import com.test.messages.demo.Helper.Constants.NAME
import com.test.messages.demo.Helper.Constants.NUMBER
import com.test.messages.demo.Helper.Constants.REPLY
import com.test.messages.demo.SMSHelper.notificationManager
import com.test.messages.demo.Utils.ViewUtils
import com.test.messages.demo.Utils.ViewUtils.getLockScreenVisibilitySetting
import com.test.messages.demo.Utils.ViewUtils.getPreviewOption
import com.test.messages.demo.Utils.ViewUtils.isNougatPlus
import com.test.messages.demo.Utils.ViewUtils.isShortCodeWithLetters
import com.test.messages.demo.Utils.ViewUtils.removeCountryCode
import com.test.messages.demo.Utils.ViewUtils.updateMessageCount
import com.test.messages.demo.data.reciever.CopyOtpReceiver
import com.test.messages.demo.data.reciever.DeleteSmsReceiver
import com.test.messages.demo.data.reciever.DirectReplyReceiver
import com.test.messages.demo.data.reciever.MarkAsReadReceiver
import com.test.messages.demo.Ui.Activity.ConversationActivity
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Locale

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

        val localizedContext = setLanguage(context) ?: context
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
                    "$count ${localizedContext.getString(R.string.new_message)}"
                } else {
                    "$count ${localizedContext.getString(R.string.new_messages)}"
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

    fun setLanguage(context: Context): Context? {
        val languageCode = ViewUtils.getSelectedLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
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

        val localizedContext = setLanguage(context) ?: context
        val NOTIFICATION_CHANNEL = "${Constants.KEY_SMS_CHANNEL}${address.removeCountryCode()}"

        maybeCreateChannel(NOTIFICATION_CHANNEL, context.getString(R.string.channel_received_sms))

        val notificationId = threadId.hashCode()
        if (previewOption != null) {
            previewOption = getPreviewOption(context, threadId) ?: 0
        } else {
            previewOption = 0
        }

        val contentIntent = Intent(context, ConversationActivity::class.java).apply {
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(NAME, sender)
            putExtra(NUMBER, address)
        }

        val contentPendingIntent = TaskStackBuilder.create(context).run {
            addParentStack(ConversationActivity::class.java)
            addNextIntent(contentIntent)

            getPendingIntent(
                notificationId,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }
        val markAsReadIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = MARK_AS_READ
            putExtra(EXTRA_THREAD_ID, threadId)
        }

        val copyOtpIntent = Intent(context, CopyOtpReceiver::class.java).apply {
            putExtra("BODY", body)
        }
        val copyOtpPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100,
            copyOtpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

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
        if (isNougatPlus() && !isNoReplySms) {
            if (previewOption == 0) {
                val replyLabel = localizedContext.getString(R.string.reply)
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
                    val summaryText = localizedContext.getString(R.string.new_message)
                    setStyle(
                        NotificationCompat.BigTextStyle().setSummaryText(summaryText).bigText(body)
                    )
                }
            }

            val style = createMessagingStyle(notificationId, address, body, sender, previewOption)

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
            localizedContext.getString(R.string.mark_as_read),
            markAsReadPendingIntent
        )
        val otpRegex = Regex("\\b\\d{4,8}\\b")
        if (otpRegex.containsMatchIn(body) && !isNumberInContacts(context, address)) {
            builder.addAction(
                R.drawable.ic_copy,
                localizedContext.getString(R.string.copy_otp),
                copyOtpPendingIntent
            ).setChannelId(NOTIFICATION_CHANNEL)
        }


        if (isNoReplySms) {
            builder.addAction(
                R.drawable.ic_delete,
                localizedContext.getString(R.string.clear),
                deleteSmsPendingIntent
            ).setChannelId(NOTIFICATION_CHANNEL)
        }
        notificationManager.notify(notificationId, builder.build())
    }

    private fun setLargeIcon(largeIcon: Any?) {

    }

    fun isNumberInContacts(context: Context, phoneNumber: String): Boolean {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        )
        cursor?.use {
            return it.count > 0
        }
        return false
    }

    fun maybeCreateChannel(channl_id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                .build()

            val id = channl_id
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
