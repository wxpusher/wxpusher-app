package com.smjcco.wxpusher.page.web.bridge.handlers

import com.smjcco.wxpusher.page.web.WxpWebViewFragment
import com.smjcco.wxpusher.page.web.bridge.BridgeActionHandler
import com.smjcco.wxpusher.page.web.bridge.BridgeContext
import com.smjcco.wxpusher.page.web.bridge.BridgeRequest
import com.smjcco.wxpusher.page.web.bridge.WxpBridgeEmitter

object CloseWebPageBridgeHandler : BridgeActionHandler {
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
        webFragment.closeWebPage()
        emitter.sendBridgeCallback(callbackId = request.callbackId, success = true)
    }
}
