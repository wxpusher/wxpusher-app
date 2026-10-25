package com.smjcco.wxpusher.push.vivo

import com.smjcco.wxpusher.push.WxpMessageArrivalNotifier
import com.vivo.push.model.UPSNotificationMessage
import com.vivo.push.sdk.OpenClientPushMessageReceiver

class VIVOClientPushMessageReceiver : OpenClientPushMessageReceiver() {

    /**
     * 应用在前台时通知消息到达。SDK 里 onNotificationMessageArrived 是 final 不能覆写，只能用这个。
     * 数据不全，只当作到达信号，让消息列表拉一次
     */
    override fun onForegroundMessageArrived(message: UPSNotificationMessage?) {
        super.onForegroundMessageArrived(message)
        WxpMessageArrivalNotifier.notifySignal("vivo")
    }
}
