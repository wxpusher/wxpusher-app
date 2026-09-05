package com.smjcco.wxpusher.push.ws.connect

import android.app.AlarmManager
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpScopeUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.PushChannelStore
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

/** WebSocket 连接、重连、消息分发及连接状态通知的统一管理器。 */
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

    // 当前连接状态。
    private var connectStatus = AtomicReference(WsConnectStatus.NotConnect)

    // 连续失败次数越多，重连等待时间越长。
    private val RETRY_SECONDS = listOf(5, 10, 15, 20, 30, 45, 60, 120)

    //持续重试次数
    private var reTryCount = 0

    private var webSocket: WebSocket? = null

    private var init = AtomicBoolean(false)

    // 服务端要求停止连接或通道被关闭时阻止继续连接。
    private var disableConnect = false

    // 当前是否允许建立和重连 WS，支持用户在同一进程内反复切换通道。
    @Volatile
    private var enabled = false

    private var alarmManager: AlarmManager? = null

    private val reconnectRunnable = Runnable { tryConnect() }
    private val reconnectAlarmListener = AlarmManager.OnAlarmListener { tryConnect() }

    /** 只初始化一次消息监听和网络监听，不代表当前一定启用 WS。 */
    fun init() {
        if (init.get()) {
            return
        }
        alarmManager =
            ApplicationUtils.getApplication().getSystemService(ALARM_SERVICE) as AlarmManager
        init.set(true)
        // 初始化消息监听器。
        initMsgListener()
        // 监听网络变化，网络恢复后按需重新连接。
        listenNetworkAvailable()
    }

    /** 启用 WS 通道并立即尝试连接。 */
    fun start() {
        enabled = true
        disableConnect = false
        init()
        tryConnect()
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
        sb.append("platform=${DevicePlatform.Android.getPlatform()}")
        // WS token 与厂商 token 分开保存，避免切换通道时覆盖彼此。
        val pushToken = PushChannelStore.getWsToken()
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
            if (!enabled) {
                WxpLogUtils.d(TAG, "connect: WS通道未启用")
                return
            }
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
                WxpLogUtils.i(TAG, "connect: WS连接已禁用")
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
        if (!enabled) {
            return
        }
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
                    reconnectAlarmListener,
                    null
                )
            } else {
                WxpLogUtils.d(message = "不能调用alarmManager，通过post delay来重启WS")
                ThreadUtils.getMainThreadHandler().removeCallbacks(reconnectRunnable)
                ThreadUtils.runOnMainThread(reconnectRunnable, retrySeconds * 1000L)
            }
        } else {
            alarmManager?.setExact(
                AlarmManager.RTC_WAKEUP,
                reconnectTime.timeInMillis,
                "WS-RECONNECT",
                reconnectAlarmListener,
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
        // 状态未变化时不重复通知页面，减少无效刷新。
        if (connectStatus.getAndSet(status) != status) {
            notifyConnectStatusChanged(status)
        }
    }

    fun getConnectStatus(): WsConnectStatus = connectStatus.get()

    /** 兼容原有调用入口，语义等同于完全停止 WS 通道。 */
    fun disconnect() {
        stop()
    }

    /**
     * 停止当前连接，并取消已经安排的所有重连任务。
     * 切换到厂商通道后必须调用，避免旧 WS 通道继续耗电或接收消息。
     */
    fun stop() {
        WxpLogUtils.i(TAG, "stop() called,停止WS通道")
        enabled = false
        disableConnect = true
        ThreadUtils.getMainThreadHandler().removeCallbacks(reconnectRunnable)
        alarmManager?.cancel(reconnectAlarmListener)
        val socket = webSocket
        webSocket = null
        socket?.close(1000, "切换推送通道")
        setConnectStatus(WsConnectStatus.NotConnect)
    }


    /** 在主线程通知页面完整的 WS 连接状态。 */
    private fun notifyConnectStatusChanged(status: WsConnectStatus) {
        WxpScopeUtils.getMainScope().launch {
            connectListenerList.toList().forEach {
                it.onChanged(status)
            }
        }
    }

    /** WebSocket 网络连接状态。 */
    enum class WsConnectStatus(val code: Int, val des: String) {
        NotConnect(1, "无链接"),
        Connecting(2, "链接中"),
        Connected(3, "已链接"),
        Closing(4, "链接关闭中"),
    }

    interface IWsConnectChangedListener {
        fun onChanged(connectStatus: WsConnectStatus)
    }

    class WsListener() : WebSocketListener() {
        private val TAG = "WsManager"

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            // 热切换时旧连接可能晚到回调，必须忽略，避免覆盖新连接状态。
            if (WsManager.webSocket !== webSocket) {
                return
            }
            WsManager.webSocket = null
            WxpLogUtils.i(TAG, "onClosed: 链接关闭，code=${code},reason=${reason}")
            setConnectStatus(WsConnectStatus.NotConnect)
            tryConnectDelay()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            if (WsManager.webSocket !== webSocket) {
                return
            }
            WxpLogUtils.i(TAG, "onClosing: code=${code},reason=${reason}")
            setConnectStatus(WsConnectStatus.NotConnect)
            tryConnectDelay()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (WsManager.webSocket !== webSocket) {
                return
            }
            WsManager.webSocket = null
            WxpLogUtils.i(TAG, "onFailure: error=${t.message}")
            t.printStackTrace()
            setConnectStatus(WsConnectStatus.NotConnect)
            tryConnectDelay()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (!enabled || WsManager.webSocket !== webSocket) {
                return
            }
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
            if (!enabled || WsManager.webSocket !== webSocket) {
                return
            }
            WxpLogUtils.i(TAG, "onMessage: 收到二进制数据")
            setConnectStatus(WsConnectStatus.Connected)
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (!enabled || WsManager.webSocket !== webSocket) {
                webSocket.close(1000, "WS通道已关闭")
                return
            }
            WxpLogUtils.i(TAG, "onOpen: WS链接打开")
            setConnectStatus(WsConnectStatus.Connected)
            reTryCount = 0
        }
    }
}
