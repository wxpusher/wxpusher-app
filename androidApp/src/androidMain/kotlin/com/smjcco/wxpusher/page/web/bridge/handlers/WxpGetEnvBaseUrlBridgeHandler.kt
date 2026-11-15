package com.smjcco.wxpusher.page.web.bridge.handlers

import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
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
                "appFeBaseUrl" to WxpConfig.appFeUrl,
                // 桥协议 D7：app-fe 内嵌消息详情页时据此传 wxpClient（monorepo docs/message-detail-sandbox-design.md 第 6.10 节）
                "platform" to WxpBaseInfoService.getClientPlatform(),
                "versionName" to WxpBaseInfoService.getAppVersionName(),
                "frameSafe" to context.frameSafe
            )
        )
    }
}
