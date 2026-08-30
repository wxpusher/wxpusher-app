package com.smjcco.wxpusher.push.ws

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.page.WebViewActivity
import com.smjcco.wxpusher.page.main.WxpMainActivity
import com.smjcco.wxpusher.push.ws.alert.WsAlertPlayer
import com.smjcco.wxpusher.push.ws.connect.PushMsgDeviceMsg
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


object WxpNotificationManager {

    private var messageId = AtomicInteger(10000)

    /**
     * 旧的业务消息渠道，声音和震动写死在渠道属性里，用户改不了。
     * 只保留 id 用于升级时删除，不要再往这个渠道发通知。
     */
    private const val LegacyBizChannelId = "WxPusherSystemChannelId"

    /**
     * 业务消息渠道。
     *
     * 渠道本身是静音无震动的，提醒全部交给 [WsAlertPlayer] 按用户设置执行——
     * NotificationChannel 创建之后声音和震动就无法用代码修改，想让用户自定义只能这么做。
     * 换了新 id 是因为删掉再用同名 id 重建，系统会把旧属性一起恢复回来。
     */
    const val WxPusherWsMessageChannelId = "WxPusherWsMessageChannelIdV2"

    private var sysNotificationManager: NotificationManager? = null
    private var init = AtomicBoolean(false)

    fun init() {
        if (init.get()) {
            return
        }
        init.set(true)
        initNotificationChannelGroup()
        createBizNotificationChannel(
            WxPusherWsMessageChannelId,
            ChannelGroup.WxPusherSystem,
            "WxPusher自建链接通知", "通过WxPusher自建链接发送订阅通知提醒，提醒方式在App内设置",
        )
        // 旧渠道自带声音和震动，留着会和 App 自己的提醒双响。
        runCatching { getSysNotificationManager().deleteNotificationChannel(LegacyBizChannelId) }
    }

    /**
     * 发送业务消息推送通知
     */
    fun sendBizMessageNotification(message: PushMsgDeviceMsg) {
        val channel: String = WxPusherWsMessageChannelId
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
                .setSmallIcon(R.mipmap.ic_launcher_transparent)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setGroup("bizMsg")
                // 不设 DEFAULT_ALL，也不能用 setSilent：前者会重新引入渠道之外的声音和震动，
                // 后者会连悬浮通知一起压掉。悬浮通知只取决于渠道的 importance。
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                //显示更多文本，长按可以展开
                .setStyle(NotificationCompat.BigTextStyle().bigText(message.summary))
                .build()

        sendNotification(notification)
        // 渠道是静音的，震动、闪光灯、响铃由 App 按用户设置执行。
        WsAlertPlayer.alert()
    }

    private fun sendNotification(notification: Notification) {
        val id = messageId.incrementAndGet()
        getSysNotificationManager().notify(id, notification)
    }

    /**
     * 创建业务消息的通知渠道。
     *
     * 刻意不设声音和震动：这两项一旦写进渠道就再也改不了，而提醒方式是要让用户自定义的，
     * 所以统一交给 [WsAlertPlayer]。importance 仍然是 HIGH，悬浮通知不受影响。
     */
    private fun createBizNotificationChannel(
        id: String,
        group: ChannelGroup,
        name: String,
        des: String
    ) {
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
        channel.description = des
        channel.enableLights(true)
        channel.enableVibration(false)
        channel.setShowBadge(true)
        channel.setSound(null, null)
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
     fun initNotificationChannelGroup() {
        ChannelGroup.entries.forEach {
            createNotificationChannelGroup(it.id, it.title)
        }
    }

    /**
     * 创建通知分组
     */
    fun createNotificationChannelGroup(id: String, name: String) {
        if (hasNotificationChannelGroup(id)) {
            return
        }
        val group = NotificationChannelGroup(id, name)
        getSysNotificationManager().createNotificationChannelGroup(group)
    }

    fun hasNotificationChannelGroup(id: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return true
        }
        if (getSysNotificationManager().getNotificationChannelGroup(id) != null) {
            return true
        }
        return false;
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