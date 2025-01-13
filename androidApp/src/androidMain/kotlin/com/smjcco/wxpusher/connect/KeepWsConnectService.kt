package com.smjcco.wxpusher.connect

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.notification.ChannelGroup
import com.smjcco.wxpusher.ws.WsManager

class KeepWsConnectService : Service() {
    val TAG = "KeepWsConnectService"
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        fun start(context: Context) {
            context.startService(Intent(context, KeepWsConnectService::class.java))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        foreground()
        WsManager.init()
    }

    fun foreground() {
        val notification = NotificationCompat.Builder(applicationContext, "123")
            .setChannelId(ChannelGroup.WxPusherSystem.id)
            .setContentTitle("WxPusher消息推送平台")
            .setContentText("消息监听中")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

}