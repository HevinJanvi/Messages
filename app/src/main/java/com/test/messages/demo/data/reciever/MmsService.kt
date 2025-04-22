package com.test.messages.demo.data.reciever

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class MmsService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MmsService", "MMS Received!")
        return START_STICKY
    }
}