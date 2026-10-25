package com.smjcco.wxpusher.push

import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.page.messagelist.WxpMessageListMessage
import com.smjcco.wxpusher.push.ws.connect.PushMsgDeviceMsg
import com.smjcco.wxpusher.utils.ThreadUtils

interface IMessageArrivalListener {
    /**
     * 收到带完整数据的推送（WS），可以直接插入列表
     */
    fun onMessageArrived(message: WxpMessageListMessage)

    /**
     * 只知道来了新消息，没有可用的数据（厂商通道），需要拉取一次
     */
    fun onMessageSignal(source: String)
}

/**
 * 推送到达事件的进程内分发，各推送通道收到消息后通知消息列表页。
 * 所有回调都切到主线程执行；没有监听者时直接丢弃，列表页下次创建时会自动刷新。
 */
object WxpMessageArrivalNotifier {
    private const val TAG = "MessageArrival"

    private val listenerList: MutableList<IMessageArrivalListener> = mutableListOf()

    fun addListener(listener: IMessageArrivalListener) {
        ThreadUtils.runOnMainThread {
            if (!listenerList.contains(listener)) {
                listenerList.add(listener)
            }
        }
    }

    fun removeListener(listener: IMessageArrivalListener) {
        ThreadUtils.runOnMainThread {
            listenerList.remove(listener)
        }
    }

    /**
     * WS 推送消息到达，转成列表数据后通知列表页
     * 缺少必要字段时降级为到达信号，让列表页拉一次
     */
    fun notifyWsMessage(message: PushMsgDeviceMsg) {
        if (message.mid <= 0 || message.url.isNullOrEmpty()) {
            WxpLogUtils.i(TAG, "WS消息缺少mid或url，降级为到达信号，mid=${message.mid}")
            notifySignal("ws")
            return
        }
        val listMessage = WxpMessageListMessage(
            messageId = message.mid,
            url = message.url,
            sourceUrl = message.sourceUrl,
            summary = message.summary ?: "",
            name = message.name,
            read = false,
            createTime = message.createTime,
        )
        ThreadUtils.runOnMainThread {
            if (isNotLogin()) {
                return@runOnMainThread
            }
            WxpLogUtils.i(TAG, "推送消息到达，source=ws，messageId=${listMessage.messageId}")
            listenerList.toList().forEach { it.onMessageArrived(listMessage) }
        }
    }

    /**
     * 厂商通道的到达信号
     * @param source 通道名，只用于日志，方便在真机上确认各厂商回调有没有触发
     */
    fun notifySignal(source: String) {
        ThreadUtils.runOnMainThread {
            if (isNotLogin()) {
                return@runOnMainThread
            }
            WxpLogUtils.i(TAG, "推送到达信号，source=${source}，listener=${listenerList.size}")
            listenerList.toList().forEach { it.onMessageSignal(source) }
        }
    }

    private fun isNotLogin(): Boolean {
        return WxpAppDataService.getLoginInfo()?.deviceToken.isNullOrEmpty()
    }
}
