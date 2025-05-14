package com.smjcco.wxpusher.push.ws

import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.notification.NotificationManager
import com.smjcco.wxpusher.notification.NotificationManager.sendBizMessageNotification
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.GsonUtils
import com.smjcco.wxpusher.utils.WxPusherUtils
import com.smjcco.wxpusher.web.WxPusherWebInterface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


object WsManager {
    const val TAG = "WsManager"
    private val msgListenerMap: MutableMap<Int, MutableList<IWsMessageListener<out BaseWsMsg>>> =
        mutableMapOf()
    private val connectListenerList = mutableListOf<IWsConnectChangedListener>()
    private val client = OkHttpClient
        .Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS) // 设置连接超时时间
        .readTimeout(10, TimeUnit.SECONDS)    // 设置读取超时时间
        .build()

    //是否已经链接
    private var connectStatus = AtomicReference(WsConnectStatus.NotConnect)

    private var webSocket: WebSocket? = null

    private var init = AtomicBoolean(false)

    //拒绝链接
    private var disableConnect = false

    fun init() {
        if (init.get()) {
            return
        }
        init.set(true)
        NotificationManager.init()
        //初始化监听器
        initMsgListener()
        //开始死循环监听链接状态
        WxPusherUtils.getIoScopeScope().launch {
            while (true) {
                if (connectStatus.get() == WsConnectStatus.NotConnect) {
                    connectInner()
                }
                WxPusherLog.d(TAG, "延迟10秒检查链接")
                delay(10 * 1000)
            }
        }
    }

    private fun initMsgListener() {
        //当收到消息的时候，发送到通知栏
        val pushListener = object : IWsMessageListener<PushMsgDeviceMsg> {
            override fun onMessage(message: PushMsgDeviceMsg) {
                sendBizMessageNotification(message)
            }
        }
        addMsgListener(WsMessageTypeEnum.PUSH_NOTE.code, pushListener)
        val pushTokenListener = object : IWsMessageListener<InitDeviceMsg> {
            override fun onMessage(message: InitDeviceMsg) {
                WxPusherLog.i(TAG, "收到自建长链接Ws的pushToken=${message.pushToken}")
                if (AppDataUtils.getPushToken() == message.pushToken) {
                    WxPusherLog.i(
                        TAG,
                        "自建长链接token未变化，不用更新 pushToken=${message.pushToken}"
                    )
                    return
                }
                PushManager.onGetPushToken(message.pushToken, DevicePlatform.Android)
            }
        }
        addMsgListener(WsMessageTypeEnum.DEVICE_INIT.code, pushTokenListener)
    }

    private fun getHostUrl(): String {
        val sb = StringBuilder()
        sb.append(WxPusherConfig.WsUrl)
        sb.append("/ws?")
        sb.append("version=${WxPusherUtils.getVersionName()}")
        sb.append("&")
        sb.append("platform=${WxPusherWebInterface.getDeviceType()}")
        if (!AppDataUtils.getPushToken().isNullOrEmpty()
            && AppDataUtils.getPushToken()?.startsWith("PT_") == true
        ) {
            sb.append("&")
            sb.append("pushToken=${AppDataUtils.getPushToken()}")
        }
        return sb.toString()
    }

    private fun connectInner() {
        synchronized(this) {
            if (connectStatus.get() == WsConnectStatus.Connected) {
                WxPusherLog.i(TAG, "connect: 已经链接，不进行链接")
                return
            }
            if (connectStatus.get() == WsConnectStatus.Connecting) {
                WxPusherLog.i(TAG, "connect: 链接中，不进行链接")
                return
            }
            if (connectStatus.get() == WsConnectStatus.Closing) {
                WxPusherLog.i(TAG, "connect: 关闭中，不进行链接")
                return
            }
            if (AppDataUtils.getLoginInfo()?.deviceId.isNullOrEmpty()) {
                WxPusherLog.i(TAG, "connect: 没有deviceId（设备未注册），不进行链接")
                return
            }
            if (AppDataUtils.getLoginInfo()?.deviceToken.isNullOrEmpty()) {
                WxPusherLog.i(TAG, "connect: 没有deviceToken（可能没有登录/已经退出登录），不进行链接")
                return
            }
            if (disableConnect) {
                WxPusherLog.i(TAG, "connect:客户端版本低，不进行链接")
                return
            }
            webSocket?.close(1000, null)

            WxPusherLog.i(TAG, "connect: 开始WS长链接")
            setConnectStatus(WsConnectStatus.Connecting)
            val wsUrl = getHostUrl()
            WxPusherLog.i(TAG, "wsUrl: ${wsUrl}")
            val request: Request = Request.Builder()
                .url(wsUrl)
                .build()
            webSocket = client.newWebSocket(request, WsListener())
        }
    }

    fun addMsgListener(msgType: Int, listener: IWsMessageListener<out BaseWsMsg>) {
        var listenerList = msgListenerMap.get(msgType)
        if (listenerList == null) {
            listenerList = mutableListOf()
            msgListenerMap.put(msgType, listenerList)
        }
        listenerList.add(listener)
    }

    fun addConnectChangedListener(listener: IWsConnectChangedListener) {
        connectListenerList.add(listener)
    }

    fun removeConnectChangedListener(listener: IWsConnectChangedListener) {
        connectListenerList.remove(listener)
    }

    private fun setConnectStatus(status: WsConnectStatus) {
        notifyConnectedChanged(status)
        connectStatus.set(status)
    }

    fun getConnectStatus(): WsConnectStatus = connectStatus.get()

    /**
     * 关闭链接
     */
    fun disconnect() {
        WxPusherLog.i(TAG, "disconnect() called,主动断开ws链接")
        disableConnect = true
        webSocket?.close(1000, null)
    }

    /**
     * 通知链接变化
     */
    private fun notifyConnectedChanged(status: WsConnectStatus) {
        //链接状态变成已经链接或者未链接，才进行通知
        if (connectStatus.get() != status &&
            (status == WsConnectStatus.Connected || status == WsConnectStatus.NotConnect)
        ) {
            WxPusherUtils.getMainScope().launch {
                connectListenerList.forEach {
                    it.onChanged(status == WsConnectStatus.Connected)
                }
            }
        }
    }

    /**
     * 网络链接状态
     */
    enum class WsConnectStatus(val code: Int, val des: String) {
        NotConnect(1, "无链接"),
        Connecting(2, "链接中"),
        Connected(3, "已链接"),
        Closing(4, "链接关闭中"),
    }

    interface IWsConnectChangedListener {
        fun onChanged(connectStatus: Boolean)
    }

    class WsListener() : WebSocketListener() {
        private val TAG = "WsManager"

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            WxPusherLog.i(TAG, "onClosed: 链接关闭，code=${code},reason=${reason}")
            setConnectStatus(WsConnectStatus.NotConnect)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            WxPusherLog.i(TAG, "onClosing: code=${code},reason=${reason}")
            setConnectStatus(WsConnectStatus.NotConnect)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            WxPusherLog.i(TAG, "onFailure: error=${t.message}")
            t.printStackTrace()
            setConnectStatus(WsConnectStatus.NotConnect)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            setConnectStatus(WsConnectStatus.Connected)
            WxPusherLog.i(TAG, "onMessage() called with: webSocket = $webSocket, text = $text")
            val baseWsMsg = GsonUtils.toObj(text, BaseWsMsg::class.java)
            if (baseWsMsg == null) {
                WxPusherLog.i(TAG, "onMessage() 消息基础类型反序列化错误：text=${text}")
                return
            }
            val typeEnum = WsMessageTypeEnum.findByCode(baseWsMsg.msgType)
            if (typeEnum == null) {
                WxPusherLog.i(TAG, "onMessage() 不能识别的消息类型：text=${text}")
                return
            }
            val bizMsg = GsonUtils.toObj(text, typeEnum.cls)
            if (bizMsg == null) {
                WxPusherLog.i(TAG, "onMessage() 消息反序列化错误：text=${text}")
                return
            }
            if (bizMsg.msgType == WsMessageTypeEnum.UPDATE_CLIENT.code) {
                disableConnect = true
            }
            val listenerList: MutableList<IWsMessageListener<*>>? =
                msgListenerMap.get(baseWsMsg.msgType)
            if (listenerList.isNullOrEmpty()) {
                WxPusherLog.i(TAG, "onMessage() 没有消息监听器")
                return
            }
            WxPusherUtils.getMainScope().launch {
                listenerList.forEach {
                    (it as IWsMessageListener<BaseWsMsg>).onMessage(bizMsg)
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            WxPusherLog.i(TAG, "onMessage: 收到二进制数据")
            setConnectStatus(WsConnectStatus.Connected)
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            WxPusherLog.i(TAG, "onOpen: WS链接打开")
            setConnectStatus(WsConnectStatus.Connected)
        }
    }
}
