package com.smjcco.wxpusher.notification

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
import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.page.WebViewActivity
import com.smjcco.wxpusher.push.ws.PushMsgDeviceMsg
import com.smjcco.wxpusher.utils.ApplicationUtils
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


object NotificationManager {
    private const val TAG = "WxPusherWebInterface"

    private var messageId = AtomicInteger(10000)
    private const val UnknownChannelId = "UnknownChannelId"
    const val WxPusherSystemChannelId = "WxPusherSystemChannelId"
    const val WxPusherSystemForegroundNotificationId = 1
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

//        createNotificationChannel(
//            UnknownChannelId,
//            ChannelGroup.SubscribeMessage, "未分类的消息", "用于接收没有指定分类的消息"
//        )
//        initSubscribeChannel()
        //只有拿到了通知栏权限，并且创建了消息通知channel，才可以启动service
//        KeepWsConnectService.start(ApplicationUtils.application)

    }

//    /**
//     * 通过网络拉取用户订阅的内容，然后创建推送通道
//     */
//    fun initSubscribeChannel() {
//        WxPusherUtils.getIoScopeScope().launch {
//            val subscribeList = DeviceApi.getSubscribeList()
//            WxPusherUtils.getMainScope().launch {
//                val subscribeListOrder = subscribeList.reversed()
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    //已经删除的订阅，把推送通道也删除了
//                    val nowHasChannelIdList =
//                        subscribeListOrder.map { it.getChannelId() }.toMutableList()
//                    //系统缺省的不能删除了
//                    nowHasChannelIdList.add(WxPusherSystemChannelId)
//                    nowHasChannelIdList.add(UnknownChannelId)
//                    getSysNotificationManager().notificationChannels.filterNot {
//                        nowHasChannelIdList.contains(it.id)
//                    }.forEach {
//                        getSysNotificationManager().deleteNotificationChannel(it.id)
//                    }
//                }
//                for (subscribeListItem in subscribeListOrder) {
//                    Log.d(
//                        TAG, "创建通知通道，ChannelId = ${subscribeListItem.getChannelId()}," +
//                                "subscribeListItem.name=${subscribeListItem.name}"
//                    )
//                    createNotificationChannel(
//                        subscribeListItem.getChannelId(),
//                        ChannelGroup.SubscribeMessage,
//                        subscribeListItem.name,
//                        ""
//                    )
//                }
//            }
//        }
//
//    }

    /**
     * 发送业务消息推送通知
     */
    fun sendBizMessageNotification(message: PushMsgDeviceMsg) {
        val channel: String = WxPusherSystemChannelId
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
//            && message.sourceID.isNotEmpty()
//        ) {
//            if (getSysNotificationManager().getNotificationChannel(message.sourceID) != null) {
//                channel = message.sourceID
//            } else {
//                //遇到没有创建的主题，补偿创建一次，下次生效
//                initSubscribeChannel()
//            }
//        }

        // 创建Intent，用于在点击通知时启动Activity
        val intent = Intent(ApplicationUtils.application, WebViewActivity::class.java)
        intent.putExtra(
            WebViewActivity.INTENT_KEY_URL,
            "${WxPusherConfig.ApiUrl}/api/message/${message.qid}"
        )
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            ApplicationUtils.application,
            messageId.get(),
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
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_MAX)
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
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

    fun getSysNotificationManager(): NotificationManager {
        if (sysNotificationManager == null) {
            sysNotificationManager =
                ApplicationUtils.application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return sysNotificationManager!!
    }
}

enum class ChannelGroup(val id: String, val title: String) {
    WxPusherSystem("WxPusherSystem", "WxPusher平台消息"),
//    SubscribeMessage("SubscribeMessage", "你主动订阅的消息"),
//    Other("other", "其他"),
}