package com.test.messages.demo.Database.Notification

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_settings")
data class NotificationSetting(
    @PrimaryKey val threadId: Long,
    val isNotificationOn: Int = 0, // 🔹 0 = Unmuted (Default), 1 = Muted
    val isWakeScreenOn: Boolean = true, // ON by default
    val isCustom: Boolean = false,// False by default, changes when updated from message screen
    val previewOption: Int = 0 ,// Default preview option (0 = Show name & message)
    val isDefault: Long = -1 // Default preview option (0 = Show name & message)
)