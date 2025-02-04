package com.smjcco.wxpusher.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.WebViewActivity
import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.api.DeviceApi
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.WxPusherUtils
import com.smjcco.wxpusher.ws.PushMsgDeviceMsg
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger


object NotificationManager {
    private const val TAG = "WxPusherWebInterface"

    private var messageId = AtomicInteger(1000)
    private const val UnknownChannelId = "unknown"
    private const val WxPusherSystem = "WxPusherSystem"
    private lateinit var sysNotificationManager: NotificationManager
    fun init() {
        sysNotificationManager =
            ApplicationUtils.application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        initNotificationChannelGrouop()
        createBizPushChannel(UnknownChannelId, "未分类的消息", "用于接收没有指定分类的消息")
        createWxPusherSystemChannel(
            WxPusherSystem,
            "WxPusher系统公告和通知",
            "WxPusher的公告、升级通知、异常提醒、订阅通知等"
        )
        initSubscribeChannel()

    }

    /**
     * 通过网络拉取用户订阅的内容，然后创建推送通道
     */
    fun initSubscribeChannel() {
        WxPusherUtils.getIoScopeScope().launch {
            val subscribeList = DeviceApi.getSubscribeList()
            WxPusherUtils.getMainScope().launch {
                val subscribeListOrder = subscribeList.reversed()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    //已经删除的订阅，把推送通道也删除了
                    val nowHasChannelIdList =
                        subscribeListOrder.map { it.getChannelId() }.toMutableList()
                    //系统缺省的不能删除了
                    nowHasChannelIdList.add(WxPusherSystem)
                    nowHasChannelIdList.add(UnknownChannelId)
                    sysNotificationManager.notificationChannels.filterNot {
                        nowHasChannelIdList.contains(it.id)
                    }.forEach {
                        sysNotificationManager.deleteNotificationChannel(it.id)
                    }
                }
                for (subscribeListItem in subscribeListOrder) {
                    Log.d(
                        TAG, "创建通知通道，ChannelId = ${subscribeListItem.getChannelId()}," +
                                "subscribeListItem.name=${subscribeListItem.name}"
                    )
                    createBizPushChannel(
                        subscribeListItem.getChannelId(),
                        subscribeListItem.name,
                        ""
                    )
                }
            }
        }

    }

    /**
     * 发送业务消息推送通知
     */
    fun sendBizMessageNotification(message: PushMsgDeviceMsg) {
        var channel: String = UnknownChannelId
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && message.sourceID.isNotEmpty()
        ) {
            if (sysNotificationManager.getNotificationChannel(message.sourceID) != null) {
                channel = message.sourceID
            } else {
                //遇到没有创建的主题，补偿创建一次，下次生效
                initSubscribeChannel()
            }
        }

        // 创建Intent，用于在点击通知时启动Activity
        val intent = Intent(ApplicationUtils.application, WebViewActivity::class.java)
        intent.putExtra(
            WebViewActivity.INTENT_KEY_URL,
            "${WxPusherConfig.ApiUrl}/api/message/${message.qid}"
        )
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            ApplicationUtils.application,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(ApplicationUtils.application, channel)
                .setContentTitle(message.title)
                .setTicker(message.summary)
                .setContentText(message.summary)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        sendNotification(notification)
    }

    private fun sendNotification(notification: Notification) {
        val id = messageId.incrementAndGet()
        sysNotificationManager.notify(id, notification)
    }

    /**
     * 创建业务消息的通知渠道
     */
    private fun createBizPushChannel(id: String, name: String, des: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
        channel.description = des
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.setShowBadge(true)
        channel.group = ChannelGroup.SubscribeMessage.id
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            channel.setAllowBubbles(true)
        }
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        createNotificationChannel(channel)
    }

    private fun createWxPusherSystemChannel(id: String, name: String, des: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
        channel.description = des
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.setShowBadge(true)
        channel.group = ChannelGroup.WxPusherSystem.id
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        if (sysNotificationManager.getNotificationChannel(channel.id) != null) {
            return
        }
        sysNotificationManager.createNotificationChannel(channel)
    }

    /**
     * 初始化消息通知分组
     */
    private fun initNotificationChannelGrouop() {
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
        if (sysNotificationManager.getNotificationChannelGroup(id) != null) {
            return
        }
        val group = NotificationChannelGroup(id, name)
        sysNotificationManager.createNotificationChannelGroup(group)
    }
}

enum class ChannelGroup(val id: String, val title: String) {
    WxPusherSystem("WxPusherSystem", "WxPusher平台消息"),
    SubscribeMessage("SubscribeMessage", "你主动订阅的消息"),
    Other("other", "其他"),
}