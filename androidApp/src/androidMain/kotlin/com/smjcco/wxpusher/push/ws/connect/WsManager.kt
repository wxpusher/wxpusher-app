package com.smjcco.wxpusher.push.ws.connect

import android.app.AlarmManager
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpScopeUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.push.ws.WxpNotificationManager.sendBizMessageNotification
import com.smjcco.wxpusher.utils.DeviceUtils
import com.smjcco.wxpusher.utils.GsonUtils
import com.smjcco.wxpusher.utils.ThreadUtils
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.Calendar
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
        //通过发送ping，来保持客户端的连接，服务端长时间没有检测到ping，就会断开连接
        .pingInterval(25, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS) // 设置连接超时时间
        .readTimeout(10, TimeUnit.SECONDS)    // 设置读取超时时间
        .build()

    //是否已经链接
    private var connectStatus = AtomicReference(WsConnectStatus.NotConnect)

    //不同的重试次数，延迟不一样
    private val RETRY_SECONDS = listOf(5, 10, 15, 20, 30, 45, 60, 120)

    //持续重试次数
    private var reTryCount = 0

    private var webSocket: WebSocket? = null

    private var init = AtomicBoolean(false)

    //拒绝链接
    private var disableConnect = false

    private var alarmManager: AlarmManager? = null


    fun init() {
        if (init.get()) {
            return
        }
        alarmManager =
            ApplicationUtils.getApplication().getSystemService(ALARM_SERVICE) as AlarmManager
        init.set(true)
        //初始化监听器
        initMsgListener()
        //监听网络变化，尝试建立连接
        listenNetworkAvailable()
    }

    /**
     * 监听网络可用的时候，重新建立连接
     * 避免用户关闭网络后，连接断开，不能及时建立连接
     */
    fun listenNetworkAvailable() {
        val connectivityManager =
            ApplicationUtils.getApplication()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(object :
            ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                WxpLogUtils.i(TAG, "监听到网络可用，尝试重新连接WS连接")
                tryConnect()
            }
        })
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
                WxpLogUtils.i(TAG, "收到自建长链接Ws的pushToken=${message.pushToken}")
                PushManager.onGetPushToken(message.pushToken, DevicePlatform.Android)
            }
        }
        addMsgListener(WsMessageTypeEnum.DEVICE_INIT.code, pushTokenListener)
    }

    private fun getWsUrl(): String {
        val sb = StringBuilder()
        sb.append(WxpConfig.wsUrl)
        sb.append("/ws?")
        sb.append("version=${WxpBaseInfoService.getAppVersionName()}")
        sb.append("&")
        sb.append("platform=${DeviceUtils.getPlatform().getPlatform()}")
        val pushToken = WxpAppDataService.getPushToken()
        if (!pushToken.isNullOrEmpty() && pushToken.startsWith("PT_")) {
            sb.append("&")
            sb.append("pushToken=${pushToken}")
        }
        return sb.toString()
    }

    /**
     * 尝试进行WS连接
     */
    fun tryConnect() {
        synchronized(this) {
            if (connectStatus.get() == WsConnectStatus.Connected) {
//                连接状态不打印日志，否则日志太多了
                WxpLogUtils.d(TAG, "connect: 已经链接，不重建连接")
                return
            }
            if (connectStatus.get() == WsConnectStatus.Connecting) {
                WxpLogUtils.i(TAG, "connect: 链接中，不进行链接")
                return
            }
            if (connectStatus.get() == WsConnectStatus.Closing) {
                WxpLogUtils.i(TAG, "connect: 关闭中，不进行链接")
                return
            }
            if (!DeviceUtils.isNetworkConnected()) {
                WxpLogUtils.d(TAG, "connect: 网络不可用，不进行链接")
                return
            }
            if (disableConnect) {
                WxpLogUtils.i(TAG, "connect:客户端版本低，不进行链接")
                return
            }
            webSocket?.close(1000, "重新建立连接前，关闭原来的WS连接")

            reTryCount++
            WxpLogUtils.i(TAG, "connect: 开始WS长链接")
            setConnectStatus(WsConnectStatus.Connecting)
            val wsUrl = getWsUrl()
            WxpLogUtils.i(TAG, "wsUrl: ${wsUrl}")
            val request: Request = Request.Builder()
                .url(wsUrl)
                .build()
            webSocket = client.newWebSocket(request, WsListener())
        }
    }


    /**
     * 当连接断开后，延迟一点时间，重新建立连接
     */
    private fun tryConnectDelay() {
        val retrySeconds = RETRY_SECONDS.getOrNull(reTryCount) ?: RETRY_SECONDS.last()
        WxpLogUtils.d(message = "延迟${retrySeconds}重新尝试WS连接")
        val reconnectTime = Calendar.getInstance()
        reconnectTime.add(Calendar.SECOND, retrySeconds)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (alarmManager?.canScheduleExactAlarms() == true) {
                alarmManager?.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reconnectTime.timeInMillis,
                    "WS-RECONNECT",
                    { tryConnect() },
                    null
                )
            } else {
                WxpLogUtils.d(message = "不能调用alarmManager，通过post delay来重启WS")
                ThreadUtils.runOnMainThread({ tryConnect() }, retrySeconds.toLong())
            }
        } else {
            alarmManager?.setExact(
                AlarmManager.RTC_WAKEUP,
                reconnectTime.timeInMillis,
                "WS-RECONNECT",
                { tryConnect() },
                null
            )
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
        WxpLogUtils.i(TAG, "disconnect() called,主动断开ws链接")
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
            WxpScopeUtils.getMainScope().launch {
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
            WxpLogUtils.i(TAG, "onClosed: 链接关闭，code=${code},reason=${reason}")
            setConnectStatus(WsConnectStatus.NotConnect)
            tryConnectDelay()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            WxpLogUtils.i(TAG, "onClosing: code=${code},reason=${reason}")
            setConnectStatus(WsConnectStatus.NotConnect)
            tryConnectDelay()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            WxpLogUtils.i(TAG, "onFailure: error=${t.message}")
            t.printStackTrace()
            setConnectStatus(WsConnectStatus.NotConnect)
            tryConnectDelay()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            setConnectStatus(WsConnectStatus.Connected)
            reTryCount = 0
            WxpLogUtils.i(TAG, "onMessage() called with: webSocket = $webSocket, text = $text")
            val baseWsMsg = GsonUtils.toObj(text, BaseWsMsg::class.java)
            if (baseWsMsg == null) {
                WxpLogUtils.i(TAG, "onMessage() 消息基础类型反序列化错误：text=${text}")
                return
            }
            val typeEnum = WsMessageTypeEnum.findByCode(baseWsMsg.msgType)
            if (typeEnum == null) {
                WxpLogUtils.i(TAG, "onMessage() 不能识别的消息类型：text=${text}")
                return
            }
            val bizMsg = GsonUtils.toObj(text, typeEnum.cls)
            if (bizMsg == null) {
                WxpLogUtils.i(TAG, "onMessage() 消息反序列化错误：text=${text}")
                return
            }
            if (bizMsg.msgType == WsMessageTypeEnum.UPDATE_CLIENT.code) {
                disconnect()
                return
            }
            val listenerList: MutableList<IWsMessageListener<*>>? =
                msgListenerMap.get(baseWsMsg.msgType)
            if (listenerList.isNullOrEmpty()) {
                WxpLogUtils.i(TAG, "onMessage() 没有消息监听器")
                return
            }
            WxpScopeUtils.getMainScope().launch {
                listenerList.forEach {
                    (it as IWsMessageListener<BaseWsMsg>).onMessage(bizMsg)
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            WxpLogUtils.i(TAG, "onMessage: 收到二进制数据")
            setConnectStatus(WsConnectStatus.Connected)
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            WxpLogUtils.i(TAG, "onOpen: WS链接打开")
            setConnectStatus(WsConnectStatus.Connected)
            reTryCount = 0
        }
    }
}
