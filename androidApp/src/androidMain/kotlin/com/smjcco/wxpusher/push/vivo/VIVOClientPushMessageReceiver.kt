package com.smjcco.wxpusher.push.vivo

import com.smjcco.wxpusher.push.WxpMessageArrivalNotifier
import com.vivo.push.model.UPSNotificationMessage
import com.vivo.push.sdk.OpenClientPushMessageReceiver
import com.vivo.push.subscribedialog.ChannelOperation

class VIVOClientPushMessageReceiver : OpenClientPushMessageReceiver() {

    /** 用户在 vivo 订阅弹窗里的选择 */
    override fun onSubscribeDialogResult(operateResult: Int, operations: MutableList<ChannelOperation>?) {
        super.onSubscribeDialogResult(operateResult, operations)
        VivoSubscribeManager.onSubscribeDialogResult(operateResult, operations)
    }

    /**
     * 应用在前台时通知消息到达。SDK 里 onNotificationMessageArrived 是 final 不能覆写，只能用这个。
     * 数据不全，只当作到达信号，让消息列表拉一次
     */
    override fun onForegroundMessageArrived(message: UPSNotificationMessage?) {
        super.onForegroundMessageArrived(message)
        WxpMessageArrivalNotifier.notifySignal("vivo")
    }
}
