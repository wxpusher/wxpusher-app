package com.smjcco.wxpusher.ws

import android.util.Log
import com.smjcco.wxpusher.WxPusherConfig
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
        .pingInterval(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS) // 设置连接超时时间
        .readTimeout(10, TimeUnit.SECONDS)    // 设置读取超时时间
        .build()

    //是否已经链接
    private var connectStatus = AtomicReference(WsConnectStatus.NotConnect)
    private var init = AtomicBoolean(false)

    fun init() {
        if (init.get()) {
            return
        }
        init.set(true)
        WxPusherUtils.getIoScopeScope().launch {
            while (true) {
                if (connectStatus.get() == WsConnectStatus.NotConnect) {
                    connectInner()
                } else {
                    Log.d(TAG, "延迟10秒检查链接")
                    delay(10 * 1000)
                }
            }
        }
    }

    private fun getHostUrl(): String {
        val sb = StringBuilder()
        sb.append(WxPusherConfig.WsUrl)
        sb.append("/ws?")
        sb.append("version=${WxPusherUtils.getVersionName()}")
        sb.append("&")
        sb.append("platform=${WxPusherWebInterface.getDeviceType()}")
        if (!AppDataUtils.getPushToken().isNullOrEmpty()) {
            sb.append("&")
            sb.append("pushToken=${AppDataUtils.getPushToken()}")
        }
        return sb.toString()
    }

    private fun connectInner() {
        synchronized(this) {
            if (connectStatus.get() == WsConnectStatus.Connected) {
                Log.d(TAG, "connect: 已经链接，不进行链接")
                return
            }
            if (connectStatus.get() == WsConnectStatus.Connecting) {
                Log.d(TAG, "connect: 链接中，不进行链接")
                return
            }
            if (connectStatus.get() == WsConnectStatus.Closing) {
                Log.d(TAG, "connect: 关闭中，不进行链接")
                return
            }
            Log.d(TAG, "connect: 开始WS长链接")
            setConnectStatus(WsConnectStatus.Connecting)
            val request: Request = Request.Builder()
                .url(getHostUrl())
                .build()
            client.newWebSocket(request, WsListener())
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
            Log.d(TAG, "onClosed: 链接关闭，code=${code},reason=${reason}")
            setConnectStatus(WsConnectStatus.NotConnect)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "onClosing: code=${code},reason=${reason}")
            setConnectStatus(WsConnectStatus.Closing)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.d(TAG, "onFailure: error=${t.message}")
            t.printStackTrace()
            setConnectStatus(WsConnectStatus.NotConnect)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            setConnectStatus(WsConnectStatus.Connected)
            Log.d(TAG, "onMessage() called with: webSocket = $webSocket, text = $text")
            val baseWsMsg = GsonUtils.toObj(text, BaseWsMsg::class.java)
            if (baseWsMsg == null) {
                Log.d(TAG, "onMessage() 消息基础类型反序列化错误：text=${text}")
                return
            }
            val typeEnum = WsMessageTypeEnum.findByCode(baseWsMsg.msgType)
            if (typeEnum == null) {
                Log.d(TAG, "onMessage() 不能识别的消息类型：text=${text}")
                return
            }
            val bizMsg = GsonUtils.toObj(text, typeEnum.cls)
            if (bizMsg == null) {
                Log.d(TAG, "onMessage() 消息反序列化错误：text=${text}")
                return
            }
            val listenerList: MutableList<IWsMessageListener<*>>? =
                msgListenerMap.get(baseWsMsg.msgType)
            if (listenerList.isNullOrEmpty()) {
                Log.d(TAG, "onMessage() 没有消息监听器")
                return
            }
            WxPusherUtils.getMainScope().launch {
                listenerList.forEach {
                    (it as IWsMessageListener<BaseWsMsg>).onMessage(bizMsg)
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d(TAG, "onMessage: 收到二进制数据")
            setConnectStatus(WsConnectStatus.Connected)
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "onOpen: WS链接打开")
            setConnectStatus(WsConnectStatus.Connected)
        }
    }
}
