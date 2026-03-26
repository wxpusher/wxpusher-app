package com.smjcco.wxpusher.page.web.bridge.handlers

import com.smjcco.wxpusher.page.web.WxpWebViewFragment
import com.smjcco.wxpusher.page.web.bridge.BridgeActionHandler
import com.smjcco.wxpusher.page.web.bridge.BridgeContext
import com.smjcco.wxpusher.page.web.bridge.BridgeRequest
import com.smjcco.wxpusher.page.web.bridge.WxpBridgeEmitter

object SetWebBottomBarBridgeHandler : BridgeActionHandler {
    override fun handle(request: BridgeRequest, context: BridgeContext, emitter: WxpBridgeEmitter) {
        val webFragment = context.fragment as? WxpWebViewFragment
        if (webFragment == null) {
            emitter.sendBridgeCallback(
                callbackId = request.callbackId,
                success = false,
                error = "web fragment not found"
            )
            return
        }
        if (!request.data.containsKey("visible")) {
            webFragment.setBottomBarVisibleOverride(null)
            emitter.sendBridgeCallback(callbackId = request.callbackId, success = true)
            return
        }
        val visible = parseBooleanValue(request.data["visible"])
        if (visible == null) {
            emitter.sendBridgeCallback(
                callbackId = request.callbackId,
                success = false,
                error = "visible must be boolean"
            )
            return
        }
        webFragment.setBottomBarVisibleOverride(visible)
        emitter.sendBridgeCallback(
            callbackId = request.callbackId,
            success = true
        )
    }

    private fun parseBooleanValue(value: Any?): Boolean? {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.toBooleanStrictOrNull()
            else -> null
        }
    }
}
