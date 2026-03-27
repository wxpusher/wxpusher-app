package com.smjcco.wxpusher.page.web.bridge.handlers

import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.page.web.bridge.BridgeActionHandler
import com.smjcco.wxpusher.page.web.bridge.BridgeContext
import com.smjcco.wxpusher.page.web.bridge.BridgeRequest
import com.smjcco.wxpusher.page.web.bridge.WxpBridgeEmitter

object WxpGetEnvBaseUrlBridgeHandler : BridgeActionHandler {
    override fun handle(request: BridgeRequest, context: BridgeContext, emitter: WxpBridgeEmitter) {
        emitter.sendBridgeCallback(
            callbackId = request.callbackId,
            success = true,
            data = mapOf(
                "apiBaseUrl" to WxpConfig.baseUrl,
                "appFeBaseUrl" to WxpConfig.appFeUrl
            )
        )
    }
}
