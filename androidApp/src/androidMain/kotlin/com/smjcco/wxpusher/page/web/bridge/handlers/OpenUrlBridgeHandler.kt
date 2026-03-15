package com.smjcco.wxpusher.page.web.bridge.handlers

import androidx.appcompat.app.AppCompatActivity
import com.smjcco.wxpusher.page.web.bridge.BridgeActionHandler
import com.smjcco.wxpusher.page.web.bridge.BridgeContext
import com.smjcco.wxpusher.page.web.bridge.BridgeRequest
import com.smjcco.wxpusher.page.web.bridge.WxpBridgeEmitter
import com.smjcco.wxpusher.utils.WxpJumpPageUtils

object OpenUrlBridgeHandler : BridgeActionHandler {
    override fun handle(request: BridgeRequest, context: BridgeContext, emitter: WxpBridgeEmitter) {
        val targetUrl = request.data["url"] as? String
        if (targetUrl.isNullOrBlank()) {
            emitter.sendBridgeCallback(
                callbackId = request.callbackId,
                success = false,
                error = "url is empty"
            )
            return
        }
        val activityHost = context.fragment.activity as? AppCompatActivity
        WxpJumpPageUtils.jumpToWebUrl(targetUrl, activityHost)
        emitter.sendBridgeCallback(
            callbackId = request.callbackId,
            success = true,
            data = mapOf("opened" to true)
        )
    }
}
