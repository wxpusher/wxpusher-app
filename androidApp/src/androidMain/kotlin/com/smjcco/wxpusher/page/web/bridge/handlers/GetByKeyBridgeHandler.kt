package com.smjcco.wxpusher.page.web.bridge.handlers

import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.page.web.bridge.BridgeActionHandler
import com.smjcco.wxpusher.page.web.bridge.BridgeContext
import com.smjcco.wxpusher.page.web.bridge.BridgeRequest
import com.smjcco.wxpusher.page.web.bridge.WxpBridgeEmitter

/**
 * 通用存储读取。直接读 WxpSaveService（原生与 H5 同一 key 空间，无前缀），空值视为不存在返回空 data。
 */
object GetByKeyBridgeHandler : BridgeActionHandler {
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
        val value = WxpSaveService.get(key, "")
        val data: Map<String, Any?> = if (value.isEmpty()) emptyMap() else mapOf("value" to value)
        emitter.sendBridgeCallback(
            callbackId = request.callbackId,
            success = true,
            data = data
        )
    }
}
