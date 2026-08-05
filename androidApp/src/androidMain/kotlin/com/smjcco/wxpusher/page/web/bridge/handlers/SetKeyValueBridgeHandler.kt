package com.smjcco.wxpusher.page.web.bridge.handlers

import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.page.web.bridge.BridgeActionHandler
import com.smjcco.wxpusher.page.web.bridge.BridgeContext
import com.smjcco.wxpusher.page.web.bridge.BridgeRequest
import com.smjcco.wxpusher.page.web.bridge.WxpBridgeEmitter

/**
 * 通用存储写入。直接写 WxpSaveService（原生与 H5 同一 key 空间，无前缀）。
 * 安全由桥的 requiresWhitelist 保证（仅白名单域可调）。
 */
object SetKeyValueBridgeHandler : BridgeActionHandler {
    override fun handle(request: BridgeRequest, context: BridgeContext, emitter: WxpBridgeEmitter) {
        val key = request.data["key"] as? String
        if (key.isNullOrBlank()) {
            emitter.sendBridgeCallback(
                callbackId = request.callbackId,
                success = false,
                error = "key is empty"
            )
            return
        }
        val value = request.data["value"] as? String ?: ""
        WxpSaveService.set(key, value)
        emitter.sendBridgeCallback(
            callbackId = request.callbackId,
            success = true
        )
    }
}
