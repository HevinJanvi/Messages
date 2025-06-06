package com.test.messages.demo.data.Service


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.test.messages.demo.R
import com.test.messages.demo.Helper.Constants
import com.test.messages.demo.Helper.Constants.ACTION_START_BACKUP
import com.test.messages.demo.Helper.Constants.ACTION_START_RESTORE
import com.test.messages.demo.Helper.Constants.EXTRA_JSON_DATA
import com.test.messages.demo.Helper.MessagesRestoredEvent
import com.test.messages.demo.Utils.ViewUtils
import com.test.messages.demo.data.repository.BackupRepository
import com.test.messages.demo.Ui.Activity.BakupRestoreActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BackupRestoreService : Service() {
    private val notificationId = 101
    private lateinit var notificationManager: NotificationManager
    private val channelId = "bakup_restore_channel"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var repository: BackupRepository
    private val ACTION_CANCEL = "ACTION_CANCEL"
    private var restoreJob: Job? = null
    private var backupJob: Job? = null


    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleCancellation()
        }
    }

    companion object {
        @Volatile
        var isRunning = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(cancelReceiver, IntentFilter(ACTION_CANCEL), RECEIVER_EXPORTED)
        } else {
            registerReceiver(cancelReceiver, IntentFilter(ACTION_CANCEL))
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        unregisterReceiver(cancelReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RESTORE -> {
                val uri = intent.getStringExtra(EXTRA_JSON_DATA)
                if (uri.isNullOrEmpty()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                notificationBuilder = createNotification(0,"fromRestore")
                startForeground(notificationId, notificationBuilder.build())
                restoreJob = serviceScope.launch {
                    try {
                        repository.restoreMessages(
                            Uri.parse(uri), { progress ->
                                updateNotification(progress, "fromRestore")
                                sendProgressBroadCast(progress)
                            },
                            onComplete = {
                                sendCompletionBroadcast()
                            },
                            onFailure = { error ->
                                val message = error.message ?: "Unknown error"
                                updateNotificationWithError(message)
                                sendFailureBroadcast(message)
                            })
                    } catch (e: CancellationException) {

                    } catch (e: Exception) {
                        updateNotificationWithError(e.localizedMessage)
                    } finally {
                        stopForeground(true)
                        stopSelf()
                    }
                }
            }

            ACTION_START_BACKUP -> {
                val uri = intent.getStringExtra(EXTRA_JSON_DATA)
                if (uri.isNullOrEmpty()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                notificationBuilder = createNotification(0,"fromBackup")
                startForeground(notificationId, notificationBuilder.build())
                backupJob = serviceScope.launch {
                    try {
                        repository.backupMessages(
                            Uri.parse(uri), { progress ->
                                updateNotification(progress, "fromBackup")
                                sendProgressBroadCast(progress)
                            },
                            onComplete = {
                                sendCompletionBroadcastBackup()
                            }
                        )
                    } catch (e: CancellationException) {

                    } catch (e: Exception) {
                        updateNotificationWithError(e.localizedMessage)
                    } finally {
                        stopForeground(true)
                        stopSelf()
                    }
                }
            }


            ACTION_CANCEL -> {
                handleCancellation()
            }

            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun sendCompletionBroadcast() {
        val intent = Intent(Constants.SEND_RESTORE_COMPLETED_BROADCAST)
        EventBus.getDefault().post(MessagesRestoredEvent(true))
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendCompletionBroadcastBackup() {
        val intent = Intent(Constants.SEND_BACKUP_COMPLETED_BROADCAST)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendProgressBroadCast(progress: Int) {
        val broadcastIntent =
            Intent(Constants.SEND_BROADCAST_TO_SEND_RESTORE_COUNT_PROGRESS_BAR).apply {
                putExtra(Constants.SEND_PROGRESS_VALUE, progress)
            }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }
    private fun sendProgressBroadCastCancel() {
        EventBus.getDefault().post(MessagesRestoredEvent(true))
        val broadcastIntent =
            Intent(Constants.SEND_BROADCAST_CANCEL).apply {
            }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }

    private fun sendFailureBroadcast(error: String) {
        val intent = Intent(Constants.SEND_BROADCAST_RESTORE_FAILED).apply {
            putExtra(Constants.SEND_ERROR_MESSAGE, error)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun handleCancellation() {
        sendProgressBroadCastCancel()
        restoreJob?.cancel()
        backupJob?.cancel()
        serviceScope.coroutineContext.cancelChildren()
        stopSelf()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(notificationId)
        isRunning = false
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Message bakup_restore",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows progress of message restoration"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private fun createNotification(progress: Int,from: String): NotificationCompat.Builder {
        val cancelIntent = Intent(ACTION_CANCEL)
        val requestCode = System.currentTimeMillis().toInt()

        val localContext = setLanguage(this) ?: this
        val progressText = if (from.equals("fromRestore")) {
            "${localContext.getString(R.string.restor)} "
        } else {
            "${localContext.getString(R.string.backup)} "
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            cancelIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = Intent(this, BakupRestoreActivity::class.java).apply {

        }
        val contentPendingIntent = TaskStackBuilder.create(this).run {
            addParentStack(BakupRestoreActivity::class.java)
            addNextIntent(contentIntent)

            getPendingIntent(
                notificationId,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        return NotificationCompat.Builder(localContext, channelId)
            .setContentTitle(progressText)
            .setSmallIcon(R.drawable.notification_logo)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .setContentText("${progressText} ($progress%)")
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(null)
            .setSilent(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_close, localContext.getString(R.string.cancel), pendingIntent)

    }

    private var lastUpdateTime = 0L
    private var lastProgress = -1

    private fun updateNotification(progress: Int, from: String) {
        sendProgressBroadCast(progress)
        if (isRunning.not()) {
            sendProgressBroadCast(0)
            notificationManager.cancel(notificationId)
            return
        }
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < 1000) {
            return
        }
        lastUpdateTime = currentTime
        lastProgress = progress

        sendProgressBroadCast(progress)

        val localContext = setLanguage(this) ?: this
        val progressText = if (from.equals("fromRestore")) {
                "${localContext.getString(R.string.restoring)} "
            } else {
                "${localContext.getString(R.string.backing_up)} "
            }
        val percentageLTR = "\u200E$progress%"
        notificationBuilder.setContentText("$progressText ($percentageLTR)")
        notificationBuilder.setProgress(100, progress, false)
        notificationManager.notify(notificationId, notificationBuilder.build())
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

    private fun updateNotificationWithError(error: String?) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Restoration Failed")
            .setContentText(error ?: "Unknown error")
            .setSmallIcon(R.drawable.notification_logo)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}