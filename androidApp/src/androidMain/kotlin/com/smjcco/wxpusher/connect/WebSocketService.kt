package com.smjcco.wxpusher.connect

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.ws.WsManager

class WebSocketService : Service() {
    val TAG = "WebSocketService"
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
//        createChannel()
//        foreground()
        connectWs()
    }

    private fun connectWs() {
        WsManager.connect();
    }


    fun foreground() {
        val notification = NotificationCompat.Builder(applicationContext, "123")
            .setChannelId("Service")
            .setContentTitle("Service通知")
            .setTicker("setTicker")
            .setContentText("WxPusher")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .build()
        startForeground(1, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel() {
        val channel = NotificationChannel(
            "Service", "WxPusher消息通知",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "用户监听WxPusher消息通知"
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

}