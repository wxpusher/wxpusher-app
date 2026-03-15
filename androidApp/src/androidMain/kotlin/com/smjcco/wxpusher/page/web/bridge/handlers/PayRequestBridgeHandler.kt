package com.smjcco.wxpusher.page.web.bridge.handlers

import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.page.web.bridge.BridgeActionHandler
import com.smjcco.wxpusher.page.web.bridge.BridgeContext
import com.smjcco.wxpusher.page.web.bridge.BridgeRequest
import com.smjcco.wxpusher.page.web.bridge.WxpBridgeEmitter
import com.smjcco.wxpusher.wxapi.WxpWeixinOpenManager

object PayRequestBridgeHandler : BridgeActionHandler {
    override fun handle(
        request: BridgeRequest,
        context: BridgeContext,
        emitter: WxpBridgeEmitter
    ) {
        WxpLogUtils.i(message = "支付请求: ${request.data}")
        WxpWeixinOpenManager.requestPayment(request.data) { _, error ->
            if (error == null) {
                emitter.sendNativeEvent(
                    action = "payResponse",
                    data = mapOf("success" to true, "message" to "支付成功")
                )
                emitter.sendBridgeCallback(
                    callbackId = request.callbackId,
                    success = true,
                    data = mapOf("success" to true, "message" to "支付成功")
                )
            } else {
                val message = error.message ?: "支付失败"
                emitter.sendNativeEvent(
                    action = "payResponse",
                    data = mapOf("success" to false, "message" to message)
                )
                emitter.sendBridgeCallback(
                    callbackId = request.callbackId,
                    success = false,
                    error = message,
                    data = mapOf("success" to false, "message" to message)
                )
            }
        }
    }
}
