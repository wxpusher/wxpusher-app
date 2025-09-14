package com.smjcco.wxpusher.push.ws

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.notification.NotificationManager
import com.smjcco.wxpusher.page.WebViewActivity

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
        WsManager.init()
        foreground()
    }

    fun foreground() {
        // 创建Intent，用于在点击通知时启动Activity
        val intent = Intent(ApplicationUtils.getApplication(), WebViewActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            ApplicationUtils.getApplication(),
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(
                applicationContext,
                NotificationManager.WxPusherSystemChannelId
            )
                .setContentTitle("WxPusher消息推送平台")
                .setContentText("保持本通知以及时接收消息")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build()
        startForeground(NotificationManager.WxPusherSystemForegroundNotificationId, notification)
    }

}