package com.newmoon.dark.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.newmoon.dark.timer.scheduledTimerTask

class TimerService: Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduledTimerTask(this)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}
