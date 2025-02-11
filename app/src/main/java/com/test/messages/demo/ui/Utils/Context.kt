package com.test.messages.demo.ui.Utils

import android.app.Application
import android.content.Context

val Context.smsSender get() = SmsSender.getInstance(applicationContext as Application)
