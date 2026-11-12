package com.smjcco.wxpusher.push.ws.connect

import android.app.AlarmManager
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpScopeUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.PushChannelStore
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.push.WxpMessageArrivalNotifier
import com.smjcco.wxpusher.push.ws.WxpNotificationManager.sendBizMessageNotification
import com.smjcco.wxpusher.push.ws.WsDiagnostics
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToLong
import kotlin.random.Random

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
    private const val STABLE_CONNECTION_MILLIS = 60_000L

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

    private const val RECONNECT_LOG_INTERVAL_MILLIS = 5 * 60_000L
    private const val NETWORK_CHECK_INTERVAL_MILLIS = 30_000L
    private var networkUnavailable = false
    private var connectionSource = "caller"
    private var lastFailureStackElapsed = 0L
    private var lastReconnectLogElapsed = 0L
    private var reconnectDueElapsed = 0L
    private var reconnectDueUptime = 0L
    private var reconnectMode = "handler"
    private var lastReconnectReason: String? = null
    private var networkChecks = 0
    private var attempts = 0
    private var maxCheckGapMs = 0L
    private var lastCheckElapsed = 0L
    private var disconnectedSinceUptime = 0L

    // Capture the event under the WS lock; persist on the existing diagnostics executor.
    private fun logEvent(message: String, error: Throwable? = null) {
        WsDiagnostics.event(TAG, message, error)
    }
    private var disconnectedSinceElapsed = 0L
    private val reconnectRunnable = Runnable { runScheduledReconnect() }
    private val reconnectAlarmListener = AlarmManager.OnAlarmListener { runScheduledReconnect() }
    private val stableConnectionRunnable = Runnable {
        synchronized(this) {
            if (enabled && connectStatus.get() == WsConnectStatus.Connected && webSocket != null) {
                reTryCount = 0
                WxpLogUtils.d(TAG, "WS稳定连接满60秒，重置退避次数")
            }
        }
    }

    /** 只初始化一次消息监听和网络监听，不代表当前一定启用 WS。 */
    @Synchronized
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
    @Synchronized
    fun start() {
        enabled = true
        disableConnect = false
        init()
        tryConnect("start")
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
            private var current: Network? = null
            private var blocked: Boolean? = null
            private var lastCapabilities: String? = null

            override fun onAvailable(network: Network) {
                synchronized(WsManager) {
                    if (current == network) return
                    current = network
                    blocked = null
                    lastCapabilities = null
                    tryConnect("network-available")
                }
            }

            override fun onLost(network: Network) {
                synchronized(WsManager) {
                    if (current != network) return
                    current = null
                    blocked = null
                    if (enabled) WsDiagnostics.logState("network-lost")
                }
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                synchronized(WsManager) {
                    if (current != network) return
                    val internet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val signature = "internet=$internet,validated=${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}"
                    if (signature != lastCapabilities) {
                        lastCapabilities = signature
                        if (enabled) WsDiagnostics.logState("network-capabilities", details = signature)
                    }
                    if (networkUnavailable && internet && blocked != true) tryConnect("network-capabilities")
                }
            }

            override fun onBlockedStatusChanged(network: Network, isBlocked: Boolean) {
                synchronized(WsManager) {
                    if (current != network) return
                    val previous = blocked
                    blocked = isBlocked
                    if (previous != isBlocked && enabled) {
                        WsDiagnostics.logState("network-blocked", details = "blocked=$isBlocked")
                    }
                    if (!isBlocked && (previous == true || (previous == null && networkUnavailable))) {
                        tryConnect("network-unblocked")
                    }
                }
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
        //当收到消息的时候，插入消息列表。单独注册，不放进通知逻辑：
        //通知那边会按 mid 去重提前返回，没有通知权限时也发不出来，这些都不应该影响插入列表
        val messageListListener = object : IWsMessageListener<PushMsgDeviceMsg> {
            override fun onMessage(message: PushMsgDeviceMsg) {
                WxpMessageArrivalNotifier.notifyWsMessage(message)
            }
        }
        addMsgListener(WsMessageTypeEnum.PUSH_NOTE.code, messageListListener)
        val pushTokenListener = object : IWsMessageListener<InitDeviceMsg> {
            override fun onMessage(message: InitDeviceMsg) {
                logEvent("收到WS设备注册结果")
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
    fun tryConnect(source: String = "caller") {
        synchronized(this) {
            if (!enabled) {
                WxpLogUtils.d(TAG, "connect: WS通道未启用")
                return
            }
            if (connectStatus.get() == WsConnectStatus.Connected && webSocket == null) {
                logEvent("WS状态修复,status=Connected,socket=null")
                setConnectStatus(WsConnectStatus.NotConnect)
            }
            if (connectStatus.get() == WsConnectStatus.Connected) {
//                连接状态不打印日志，否则日志太多了
                WxpLogUtils.d(TAG, "connect: 已经链接，不重建连接")
                return
            }
            if (connectStatus.get() == WsConnectStatus.Connecting) {
                logEvent("connect: 链接中，不进行链接")
                return
            }
            if (connectStatus.get() == WsConnectStatus.Closing) {
                logEvent("connect: 关闭中，不进行链接")
                return
            }
            if (disableConnect) return
            if (!DeviceUtils.isNetworkConnected()) {
                // 无网络不是连接失败，不推进退避；由 Handler 检查，避免反复精确唤醒。
                networkUnavailable = true
                networkChecks++
                tryConnectDelay("network-unavailable")
                return
            }
            if (disableConnect) {
                logEvent("connect: WS连接已禁用")
                return
            }
            // 仅在实际观察到无网络 -> 有网络时重置，避免重复 onAvailable 冲掉失败退避。
            if (networkUnavailable) {
                networkUnavailable = false
                reTryCount = 0
                logEvent("WS网络恢复，重置连接失败退避,source=$source")
            }
            cancelReconnectTask()
            webSocket?.close(1000, "重新建立连接前，关闭原来的WS连接")

            logEvent("connect: 开始WS长链接,source=$source,retry=$reTryCount")
            setConnectStatus(WsConnectStatus.Connecting)
            connectionSource = source
            attempts++
            try {
                val request = Request.Builder().url(getWsUrl()).build()
                webSocket = client.newWebSocket(request, WsListener())
            } catch (e: IllegalArgumentException) {
                webSocket = null
                setConnectStatus(WsConnectStatus.NotConnect)
                logEvent("WS建连参数异常,type=${e.javaClass.simpleName}")
                tryConnectDelay("request-error")
            }
        }
    }


    /**
     * 当连接断开后，延迟一点时间，重新建立连接
     */
    @Synchronized
    private fun tryConnectDelay(reason: String = "socket-closed") {
        if (!enabled || disableConnect) {
            return
        }
        cancelReconnectTask()
        val now = SystemClock.elapsedRealtime()
        if (disconnectedSinceElapsed == 0L) {
            disconnectedSinceElapsed = now
            disconnectedSinceUptime = SystemClock.uptimeMillis()
        }
        val checkingNetwork = reason == "network-unavailable"
        val retryMillis = if (checkingNetwork) {
            NETWORK_CHECK_INTERVAL_MILLIS
        } else {
            val baseRetrySeconds = RETRY_SECONDS.getOrNull(reTryCount) ?: RETRY_SECONDS.last()
            reTryCount++
            (baseRetrySeconds * 1000L * Random.nextDouble(0.8, 1.2)).roundToLong()
        }
        reconnectDueElapsed = now + retryMillis
        reconnectDueUptime = SystemClock.uptimeMillis() + retryMillis
        var mode = "handler"
        try {
            // 仅真正连接失败时使用精确闹钟；无网络检查不额外唤醒设备。
            if (!checkingNetwork && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    alarmManager?.canScheduleExactAlarms() == true)
            ) {
                val manager = alarmManager
                if (manager != null) {
                    manager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        reconnectDueElapsed,
                        "WS-RECONNECT",
                        reconnectAlarmListener,
                        null
                    )
                    mode = "alarm"
                }
            }
        } catch (e: SecurityException) {
            // 只在下方限频日志记录降级，避免每次重试重复打印异常。
            mode = "handler-permission-denied"
        }
        if (mode != "alarm") {
            ThreadUtils.runOnMainThread(reconnectRunnable, retryMillis)
        }
        reconnectMode = mode
        // 首次断线及持续离线每五分钟记录一次，不随每次无网络检查刷屏。
        if (lastReconnectReason != "$reason/$mode" || lastReconnectLogElapsed == 0L || now - lastReconnectLogElapsed >= RECONNECT_LOG_INTERVAL_MILLIS) {
            lastReconnectReason = "$reason/$mode"
            lastReconnectLogElapsed = now
            logEvent("WS安排重连,reason=$reason,retry=$reTryCount,delayMs=$retryMillis,mode=$mode")
        }
    }

    @Synchronized
    private fun runScheduledReconnect() {
        if (!enabled || disableConnect || reconnectDueElapsed == 0L) return
        val now = SystemClock.elapsedRealtime()
        // A canceled alarm may already be queued. Do not consume a newer schedule early.
        val remaining = if (reconnectMode.startsWith("handler")) {
            reconnectDueUptime - SystemClock.uptimeMillis()
        } else reconnectDueElapsed - now
        if (remaining > 0L) return
        val lateMillis = -remaining
        if (lastCheckElapsed != 0L) maxCheckGapMs = maxOf(maxCheckGapMs, now - lastCheckElapsed)
        lastCheckElapsed = now
        reconnectDueElapsed = 0L
        if (lateMillis > 30_000L) logEvent("WS重连任务迟到,mode=$reconnectMode,lateMs=$lateMillis")
        tryConnect("retry")
    }

    private fun cancelReconnectTask() {
        ThreadUtils.getMainThreadHandler().removeCallbacks(reconnectRunnable)
        alarmManager?.cancel(reconnectAlarmListener)
        reconnectDueElapsed = 0L
    }

    private fun cancelStableConnectionReset() {
        ThreadUtils.getMainThreadHandler().removeCallbacks(stableConnectionRunnable)
    }

    private fun scheduleStableConnectionReset() {
        cancelStableConnectionReset()
        ThreadUtils.runOnMainThread(stableConnectionRunnable, STABLE_CONNECTION_MILLIS)
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
    @Synchronized
    fun stop() {
        logEvent("stop() called,停止WS通道")
        enabled = false
        disableConnect = true
        cancelReconnectTask()
        cancelStableConnectionReset()
        disconnectedSinceElapsed = 0L
        disconnectedSinceUptime = 0L
        networkChecks = 0
        attempts = 0
        maxCheckGapMs = 0L
        lastCheckElapsed = 0L
        lastReconnectReason = null
        lastReconnectLogElapsed = 0L
        reTryCount = 0
        networkUnavailable = false
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
            synchronized(WsManager) {
                // 身份检查与更新一起加锁，避免热切换后旧回调覆盖新连接。
                if (WsManager.webSocket !== webSocket) {
                    return
                }
                WsManager.webSocket = null
                cancelStableConnectionReset()
                logEvent("onClosed: 链接关闭，code=${code},reason=${reason}")
                setConnectStatus(WsConnectStatus.NotConnect)
                tryConnectDelay()
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            synchronized(WsManager) {
                if (WsManager.webSocket !== webSocket) {
                    return
                }
                logEvent("onClosing: code=${code},reason=${reason}")
                setConnectStatus(WsConnectStatus.Closing)
                webSocket.close(code, reason)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            synchronized(WsManager) {
                if (WsManager.webSocket !== webSocket) {
                    return
                }
                WsManager.webSocket = null
                cancelStableConnectionReset()
                val now = SystemClock.elapsedRealtime()
                val includeStack = lastFailureStackElapsed == 0L ||
                    now - lastFailureStackElapsed >= RECONNECT_LOG_INTERVAL_MILLIS
                if (includeStack) {
                    lastFailureStackElapsed = now
                }
                // 每次失败都留在线上；仅完整堆栈独立限频，恢复/切换通道也不重置窗口。
                logEvent(
                    "WS连接失败,type=${t.javaClass.simpleName},retry=$reTryCount,source=$connectionSource",
                    if (includeStack) t else null
                )
                WsDiagnostics.logState("ws-failure", force = includeStack)
                setConnectStatus(WsConnectStatus.NotConnect)
                tryConnectDelay()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            synchronized(WsManager) {
                if (!enabled || WsManager.webSocket !== webSocket) return
            }
            val baseWsMsg = GsonUtils.toObj(text, BaseWsMsg::class.java)
            if (baseWsMsg == null) {
                logEvent("WS消息基础类型解析失败")
                return
            }
            val typeEnum = WsMessageTypeEnum.findByCode(baseWsMsg.msgType)
            if (typeEnum == null) {
                logEvent("WS未知消息类型,type=${baseWsMsg.msgType}")
                return
            }
            val bizMsg = GsonUtils.toObj(text, typeEnum.cls)
            if (bizMsg == null) {
                logEvent("WS消息解析失败,type=${baseWsMsg.msgType}")
                return
            }
            if (bizMsg.msgType == WsMessageTypeEnum.UPDATE_CLIENT.code) {
                synchronized(WsManager) {
                    if (enabled && WsManager.webSocket === webSocket) disconnect()
                }
                return
            }
            val listenerList: MutableList<IWsMessageListener<*>>? =
                msgListenerMap.get(baseWsMsg.msgType)
            if (listenerList.isNullOrEmpty()) {
                logEvent("onMessage() 没有消息监听器")
                return
            }
            WxpScopeUtils.getMainScope().launch {
                if (bizMsg.msgType == WsMessageTypeEnum.DEVICE_INIT.code) {
                    synchronized(WsManager) {
                        if (enabled && WsManager.webSocket === webSocket) {
                            listenerList.forEach { (it as IWsMessageListener<BaseWsMsg>).onMessage(bizMsg) }
                        }
                    }
                } else {
                    // An accepted business message survives a subsequent channel switch.
                    listenerList.forEach { (it as IWsMessageListener<BaseWsMsg>).onMessage(bizMsg) }
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            synchronized(WsManager) {
                if (!enabled || WsManager.webSocket !== webSocket) return
            }
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            synchronized(WsManager) {
                if (!enabled || WsManager.webSocket !== webSocket) {
                    webSocket.close(1000, "WS通道已关闭")
                    return
                }
                val offlineMillis: Any = if (disconnectedSinceElapsed == 0L) "unknown" else
                    SystemClock.elapsedRealtime() - disconnectedSinceElapsed
                logEvent("onOpen: WS链接打开,source=$connectionSource,retry=$reTryCount,offlineMs=$offlineMillis,offlineUptimeMs=${if (disconnectedSinceUptime == 0L) "unknown" else SystemClock.uptimeMillis() - disconnectedSinceUptime},networkChecks=$networkChecks,attempts=$attempts,maxCheckGapMs=$maxCheckGapMs")
                WsDiagnostics.logState("ws-open")
                disconnectedSinceElapsed = 0L
                disconnectedSinceUptime = 0L
                networkChecks = 0
                attempts = 0
                maxCheckGapMs = 0L
                lastCheckElapsed = 0L
                lastReconnectReason = null
                lastReconnectLogElapsed = 0L
                setConnectStatus(WsConnectStatus.Connected)
                scheduleStableConnectionReset()
            }
        }
    }
}
