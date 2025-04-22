package com.test.messages.demo.data.Database.Notification

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notification_settings WHERE threadId = :threadId LIMIT 1")
    fun getSettings(threadId: Long): NotificationSetting?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotificationSettings(settings: List<NotificationSetting>)

    @Query("SELECT threadId FROM notification_settings")
    suspend fun getAllThreadIds(): List<Long>

    @Query("UPDATE notification_settings SET previewOption = :previewOption, isCustom = :isCustom WHERE threadId = :threadId")
    suspend fun updatePreviewOption(threadId: Long, previewOption: Int, isCustom: Boolean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotificationSetting(notificationSetting: NotificationSetting)

    @Query("SELECT * FROM notification_settings WHERE threadId = :threadId LIMIT 1")
    suspend fun getNotificationSetting(threadId: Long): NotificationSetting?
    @Query("SELECT previewOption FROM notification_settings WHERE threadId = :threadId")
    fun getPreviewOptionNow(threadId: Long): Int?

    @Query("SELECT previewOption FROM notification_settings WHERE isDefault = -1 AND isCustom=0")
    fun getPreviewOptionforGlobal(): Int?

    @Query("SELECT previewOption FROM notification_settings WHERE threadId = :threadId")
    suspend fun getPreviewOption(threadId: Long): Int?
    @Query("UPDATE notification_settings SET previewOption = :previewOption WHERE isCustom = 0")
    suspend fun updateGlobalPreviewOption(previewOption: Int)

    @Query("SELECT isWakeScreenOn FROM notification_settings WHERE threadId = :threadId")
    fun getWakeScreenSetting(threadId: Long): Boolean?

    @Query("SELECT isWakeScreenOn FROM notification_settings WHERE  isDefault = -1 AND isCustom = 0")
    fun getWakeScreenSettingGlobal(): Boolean?


    @Query("UPDATE notification_settings SET isWakeScreenOn = :newState WHERE threadId = :threadId")
    fun updateWakeScreenSetting(threadId: Long, newState: Boolean)

    @Query("UPDATE notification_settings SET isWakeScreenOn = :newState WHERE isCustom = 0")
    fun updateWakeScreenSettingGlobal(newState: Boolean)
    @Query("UPDATE notification_settings SET isCustom = :value WHERE threadId = :threadId")
    fun setIsCustom(threadId: Long, value: Int)

    @Query("UPDATE notification_settings SET isNotificationOn = :isMuted WHERE threadId = :threadId")
    suspend fun updateNotificationStatus(threadId: Long, isMuted: Boolean)

    @Query("SELECT isNotificationOn FROM notification_settings WHERE threadId = :threadId")
    suspend fun getNotificationStatus(threadId: Long): Boolean?

    @Query("SELECT threadId FROM notification_settings WHERE isNotificationOn = 1")
    suspend fun getAllMutedThreads(): List<Long>


}
