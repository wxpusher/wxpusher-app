package com.smjcco.wxpusher.ws

import android.util.Log
import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.utils.GsonUtils
import com.smjcco.wxpusher.utils.WxPusherUtils
import com.smjcco.wxpusher.ws.WsManager.connected
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit
import kotlin.reflect.typeOf

private const val TAG = "WsManager"

object WsManager {
    private val msgListenerMap: MutableMap<Int, MutableList<IWsMessageListener<out BaseWsMsg>>> =
        mutableMapOf()
    private val client = OkHttpClient
        .Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    //是否已经链接
    var connected = false
    fun connect() {
        if (connected) {
            Log.i(TAG, "connect: 已经链接")
            return
        }
        Log.i(TAG, "connect: 开始WS长链接")
        connected = true
        val request: Request = Request.Builder()
            .url("wss://${WxPusherConfig.Host}/ws?version=${WxPusherUtils.getVersionName()}&platform=Android&pushToken=PT_001_yFnkfTAZJ8VPsauxTuYdxtvdpmus")
            .build()
        client.newWebSocket(request, WsListener(msgListenerMap))
    }

    fun addMsgListener(msgType: Int, listener: IWsMessageListener<out BaseWsMsg>) {
        var listenerList = msgListenerMap.get(msgType)
        if (listenerList == null) {
            listenerList = mutableListOf()
            msgListenerMap.put(msgType, listenerList)
        }
        listenerList.add(listener)
    }
}

class WsListener(private val listenerMap: Map<Int, MutableList<IWsMessageListener<out BaseWsMsg>>> = mutableMapOf()) :
    WebSocketListener() {

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(TAG, "onClosed: 链接关闭，code=${code},reason=${reason}")
        connected = false
        WxPusherUtils.toast("WebSocket-onClosed")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "onClosing: code=${code},reason=${reason}")
        connected = false
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d(TAG, "onFailure: error=${t.message}")
        t.printStackTrace()
        WxPusherUtils.toast("WebSocket-onFailure：" + t.message)
        connected = false
    }


    override fun onMessage(webSocket: WebSocket, text: String) {
        connected = true
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
        val listenerList: MutableList<IWsMessageListener<*>>? = listenerMap.get(baseWsMsg.msgType)
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
        connected = true
    }
}