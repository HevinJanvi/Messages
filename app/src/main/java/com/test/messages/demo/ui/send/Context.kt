package com.test.messages.demo.ui.send

import android.app.Application
import android.content.Context
import com.test.messages.demo.ui.Utils.SmsSender

val Context.smsSender get() = SmsSender.getInstance(applicationContext as Application)
