package com.smjcco.wxpusher.ws

import android.util.Log
import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.GsonUtils
import com.smjcco.wxpusher.utils.WxPusherUtils
import com.smjcco.wxpusher.web.WxPusherWebInterface
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


object WsManager {
    const val TAG = "WsManager"
    private val msgListenerMap: MutableMap<Int, MutableList<IWsMessageListener<out BaseWsMsg>>> =
        mutableMapOf()
    private val connectListenerList = mutableListOf<IWsConnectChangedListener>()
    private val client = OkHttpClient
        .Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    //是否已经链接
    private var connected = AtomicBoolean(false)

    fun connect() {
        WxPusherUtils.getIoScopeScope().launch {
            connectInner()
        }
    }

    private fun connectInner() {
        synchronized(this) {
            if (connected.get()) {
                Log.i(TAG, "connect: 已经链接")
                return
            }
            Log.i(TAG, "connect: 开始WS长链接")
            setConnected(true)
            val request: Request = Request.Builder()
                .url("${WxPusherConfig.WsUrl}/ws?version=${WxPusherUtils.getVersionName()}&platform=${WxPusherWebInterface.getDeviceType()}&pushToken=${AppDataUtils.getPushToken()}")
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

    private fun setConnected(connectStatus: Boolean) {
        notifyConnectedChanged(connectStatus)
        connected.set(connectStatus)
    }

    /**
     * 通知链接变化
     */
    private fun notifyConnectedChanged(connectStatus: Boolean) {
        WxPusherUtils.getMainScope().launch {
            if (connectStatus != connected.get()) {
                connectListenerList.forEach {
                    it.onChanged(connectStatus)
                }
            }
        }
    }

    interface IWsConnectChangedListener {
        fun onChanged(connectStatus: Boolean)
    }

    class WsListener() : WebSocketListener() {
        private val TAG = "WsManager"

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "onClosed: 链接关闭，code=${code},reason=${reason}")
            setConnected(false)
            WxPusherUtils.toast("WebSocket-onClosed")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "onClosing: code=${code},reason=${reason}")
            setConnected(false)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.d(TAG, "onFailure: error=${t.message}")
            t.printStackTrace()
            WxPusherUtils.toast("WebSocket-onFailure：" + t.message)
            setConnected(false)
        }


        override fun onMessage(webSocket: WebSocket, text: String) {
            setConnected(true)
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
            listenerList.forEach {
                (it as IWsMessageListener<BaseWsMsg>).onMessage(bizMsg)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.i(TAG, "onMessage: 收到二进制数据")
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "onOpen: WS链接打开")
            setConnected(true)
        }
    }
}
