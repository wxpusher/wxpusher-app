package com.smjcco.wxpusher.page.web.bridge

import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.utils.GsonUtils
import com.smjcco.wxpusher.utils.ThreadUtils

class WxpBridgeEmitter(
    private val context: BridgeContext
) {
    fun sendBridgeCallback(
        callbackId: String?,
        success: Boolean,
        data: Map<String, Any?>? = null,
        error: String? = null
    ) {
        if (callbackId.isNullOrBlank()) {
            return
        }
        try {
            val callbackBody = mutableMapOf<String, Any?>(
                "callbackId" to callbackId,
                "success" to success
            )
            if (data != null) {
                callbackBody["data"] = data
            }
            if (!error.isNullOrBlank()) {
                callbackBody["error"] = error
            }
            val bodyJson = GsonUtils.toJson(callbackBody)
            val escapedJson = escapeForSingleQuotedJs(bodyJson)
            val js =
                "window.dispatchEvent(new CustomEvent('wxpusherBridgeCallback', { detail: '$escapedJson' }));"
            ThreadUtils.runOnMainThread {
                context.evaluateJavascript(js, null)
            }
        } catch (e: Exception) {
            WxpLogUtils.w(message = "发送桥回调失败: callbackId=$callbackId", throwable = e)
        }
    }

    fun sendNativeEvent(action: String, data: Map<String, Any>) {
        try {
            val jsonData = GsonUtils.toJson(data)
            WxpLogUtils.i(message = "发送数据给webview，action=$action,data=$jsonData")
            val escapedJson = escapeForSingleQuotedJs(jsonData)
            val js =
                "window.dispatchEvent(new CustomEvent('nativeEvent_$action', { detail: '$escapedJson' }));"
            ThreadUtils.runOnMainThread {
                context.evaluateJavascript(js) { result ->
                    if (result != null) {
                        WxpLogUtils.d(message = "发送消息到WebView成功: action=$action")
                    } else {
                        WxpLogUtils.d(message = "发送消息到WebView失败: action=$action,result=$result")
                    }
                }
            }
        } catch (e: Exception) {
            WxpLogUtils.w(message = "发送消息到WebView失败: action=$action", throwable = e)
        }
    }

    private fun escapeForSingleQuotedJs(source: String): String {
        return source.replace("\\", "\\\\").replace("'", "\\'")
    }
}
