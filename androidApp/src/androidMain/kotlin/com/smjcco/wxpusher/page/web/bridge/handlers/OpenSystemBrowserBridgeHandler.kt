package com.smjcco.wxpusher.page.web.bridge.handlers

import android.content.Intent
import androidx.core.net.toUri
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.page.web.bridge.BridgeActionHandler
import com.smjcco.wxpusher.page.web.bridge.BridgeContext
import com.smjcco.wxpusher.page.web.bridge.BridgeRequest
import com.smjcco.wxpusher.page.web.bridge.WxpBridgeEmitter

/**
 * 用系统默认浏览器打开 url（跳出 App 到外部浏览器），区别于 openUrl 的内置 WebView
 */
object OpenSystemBrowserBridgeHandler : BridgeActionHandler {
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
        val activityHost = context.fragment.activity
        if (activityHost == null) {
            emitter.sendBridgeCallback(
                callbackId = request.callbackId,
                success = false,
                error = "activity is null"
            )
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, targetUrl.toUri())
            activityHost.startActivity(intent)
            emitter.sendBridgeCallback(
                callbackId = request.callbackId,
                success = true,
                data = mapOf("opened" to true)
            )
        } catch (e: Exception) {
            WxpLogUtils.w(message = "打开系统浏览器失败: url=$targetUrl", throwable = e)
            emitter.sendBridgeCallback(
                callbackId = request.callbackId,
                success = false,
                error = "open failed"
            )
        }
    }
}
