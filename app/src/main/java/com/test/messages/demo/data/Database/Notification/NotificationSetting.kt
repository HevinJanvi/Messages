package com.test.messages.demo.data.Database.Notification

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.errorprone.annotations.Keep

@Keep
@Entity(tableName = "notification_settings")
data class NotificationSetting(
    @PrimaryKey val threadId: Long,
    val isNotificationOn: Int = 0,
    val isWakeScreenOn: Boolean = true,
    val isCustom: Boolean = false,
    val previewOption: Int = 0 ,
    val isDefault: Long = -1
)