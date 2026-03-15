package com.smjcco.wxpusher.page.web.bridge.handlers

import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.page.web.bridge.BridgeActionHandler
import com.smjcco.wxpusher.page.web.bridge.BridgeContext
import com.smjcco.wxpusher.page.web.bridge.BridgeRequest
import com.smjcco.wxpusher.page.web.bridge.WxpBridgeEmitter
import com.smjcco.wxpusher.utils.GsonUtils

object GetLoginInfoBridgeHandler : BridgeActionHandler {
    override fun handle(request: BridgeRequest, context: BridgeContext, emitter: WxpBridgeEmitter) {
        val loginInfoStr = WxpAppDataService.getLoginInfoStr()
        val loginInfoMap = try {
            if (loginInfoStr.isNullOrBlank()) {
                emptyMap<String, Any?>()
            } else {
                val rawMap = GsonUtils.toObj(loginInfoStr, Map::class.java) as? Map<*, *>
                rawMap
                    ?.filterKeys { it is String }
                    ?.mapKeys { it.key as String }
                    ?: emptyMap()
            }
        } catch (e: Exception) {
            WxpLogUtils.w(message = "解析登录信息失败", throwable = e)
            emptyMap()
        }
        emitter.sendBridgeCallback(
            callbackId = request.callbackId,
            success = true,
            data = loginInfoMap
        )
    }
}
