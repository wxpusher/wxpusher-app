package com.smjcco.wxpusher.page.web.bridge

import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.page.web.bridge.handlers.GetLoginInfoBridgeHandler
import com.smjcco.wxpusher.page.web.bridge.handlers.OpenUrlBridgeHandler
import com.smjcco.wxpusher.page.web.bridge.handlers.PayRequestBridgeHandler
import com.smjcco.wxpusher.page.web.bridge.handlers.ShowToastBridgeHandler
import com.smjcco.wxpusher.page.web.bridge.handlers.WxpGetEnvBaseUrlBridgeHandler
import com.smjcco.wxpusher.web.WxpWebHostPolicy

class WxpWebBridgeManager(
    private val context: BridgeContext,
    private val parser: WxpBridgeMessageParser = WxpBridgeMessageParser()
) {
    private val emitter = WxpBridgeEmitter(context)
    private val handlers = mutableMapOf<String, BridgeHandler>()

    init {
        registerDefaultHandlers()
    }

    fun registerHandler(action: String, requiresWhitelist: Boolean, handler: BridgeActionHandler) {
        handlers[action] = BridgeHandler(
            requiresWhitelist = requiresWhitelist,
            handler = handler
        )
    }

    fun registerDefaultHandlers() {
        registerHandler("payRequest", requiresWhitelist = true, handler = PayRequestBridgeHandler)
        registerHandler("openUrl", requiresWhitelist = false, handler = OpenUrlBridgeHandler)
        registerHandler("getLoginInfo", requiresWhitelist = true, handler = GetLoginInfoBridgeHandler)
        registerHandler("getEnvBaseUrl", requiresWhitelist = true, handler = WxpGetEnvBaseUrlBridgeHandler)
        registerHandler("showToast", requiresWhitelist = true, handler = ShowToastBridgeHandler)
    }

    fun onMessage(messageJson: String) {
        val request = parser.parse(messageJson) ?: return
        dispatch(request)
    }

    private fun dispatch(request: BridgeRequest) {
        val bridgeHandler = handlers[request.action]
        if (bridgeHandler == null) {
            WxpLogUtils.w(message = "未知的action: ${request.action}")
            emitter.sendBridgeCallback(
                callbackId = request.callbackId,
                success = false,
                error = "unknown action: ${request.action}"
            )
            return
        }
        if (bridgeHandler.requiresWhitelist && !isHostInWhitelist(context.currentHost)) {
            WxpLogUtils.w(message = "非白名单地址，不允许调用桥: ${context.currentUrl}")
            emitter.sendBridgeCallback(
                callbackId = request.callbackId,
                success = false,
                error = "host is not allowed"
            )
            return
        }
        try {
            bridgeHandler.handler.handle(request, context, emitter)
        } catch (e: Exception) {
            WxpLogUtils.w(message = "处理桥接逻辑失败: action=${request.action}", throwable = e)
            emitter.sendBridgeCallback(
                callbackId = request.callbackId,
                success = false,
                error = "bridge handler error"
            )
        }
    }

    private fun isHostInWhitelist(host: String?): Boolean {
        return WxpWebHostPolicy.isHostInWhitelist(host)
    }
}
