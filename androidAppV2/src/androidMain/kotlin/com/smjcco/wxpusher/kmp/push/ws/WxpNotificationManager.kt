package com.smjcco.wxpusher.kmp.push.ws

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.kmp.page.main.WxpMainActivity
import com.smjcco.wxpusher.kmp.push.ws.connect.PushMsgDeviceMsg
import com.smjcco.wxpusher.page.WebViewActivity
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


object WxpNotificationManager {

    private var messageId = AtomicInteger(10000)
    const val WxPusherSystemChannelId = "WxPusherSystemChannelId"
    private var sysNotificationManager: NotificationManager? = null
    private var init = AtomicBoolean(false)

    fun init() {
        if (init.get()) {
            return
        }
        init.set(true)
        initNotificationChannelGroup()
        createNotificationChannel(
            WxPusherSystemChannelId,
            ChannelGroup.WxPusherSystem,
            "WxPusher系统公告和通知", "WxPusher的公告、升级通知、异常提醒、订阅通知等",
        )
    }

    /**
     * 发送业务消息推送通知
     */
    fun sendBizMessageNotification(message: PushMsgDeviceMsg) {
        val channel: String = WxPusherSystemChannelId
        // 创建Intent，用于在点击通知时启动Activity
        val intent = Intent(ApplicationUtils.getApplication(), WxpMainActivity::class.java)
        intent.putExtra(
            WebViewActivity.INTENT_KEY_URL,
            message.url
        )
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            ApplicationUtils.getApplication(),
            messageId.get(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(ApplicationUtils.getApplication(), channel)
                .setContentTitle(message.title)
                .setTicker(message.summary)
                .setContentText(message.summary)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setGroup("bizMsg")
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                //显示更多文本，长按可以展开
                .setStyle(NotificationCompat.BigTextStyle().bigText(message.summary))
                .build()

        sendNotification(notification)
    }

    private fun sendNotification(notification: Notification) {
        val id = messageId.incrementAndGet()
        getSysNotificationManager().notify(id, notification)
    }

    /**
     * 创建业务消息的通知渠道
     */
    private fun createNotificationChannel(
        id: String,
        group: ChannelGroup,
        name: String,
        des: String
    ) {
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
        channel.description = des
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        channel.setShowBadge(true)
        channel.setSound(
            Settings.System.DEFAULT_NOTIFICATION_URI,
            Notification.AUDIO_ATTRIBUTES_DEFAULT
        )
        channel.group = group.id
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            channel.setAllowBubbles(true)
        }
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        createNotificationChannel(channel)
    }


    /**
     * 创建推送渠道
     */
    private fun createNotificationChannel(channel: NotificationChannel) {
        if (getSysNotificationManager().getNotificationChannel(channel.id) != null) {
            return
        }
        getSysNotificationManager().createNotificationChannel(channel)
    }

    /**
     * 初始化消息通知分组
     */
    private fun initNotificationChannelGroup() {
        ChannelGroup.entries.forEach {
            createNotificationChannelGroup(it.id, it.title)
        }
    }

    /**
     * 创建通知分组
     */
    private fun createNotificationChannelGroup(id: String, name: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return
        }
        if (getSysNotificationManager().getNotificationChannelGroup(id) != null) {
            return
        }
        val group = NotificationChannelGroup(id, name)
        getSysNotificationManager().createNotificationChannelGroup(group)
    }

    /**
     * 是否有某个通知通道
     */
    fun hasNotificationChannel(id: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return true
        }
        return getSysNotificationManager().getNotificationChannel(id) != null
    }

    /**
     * 查询某一个通知是否存在
     */
    fun hasNotificationById(id: Int): Boolean {
        return getSysNotificationManager().activeNotifications.find { it.id == id } != null
    }

    fun getSysNotificationManager(): NotificationManager {
        if (sysNotificationManager == null) {
            sysNotificationManager =
                ApplicationUtils.getApplication()
                    .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return sysNotificationManager!!
    }
}

enum class ChannelGroup(val id: String, val title: String) {
    WxPusherSystem("WxPusherSystem", "WxPusher平台消息"),
}