package com.smjcco.wxpusher.page.web.bridge

import android.webkit.WebView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment

data class BridgeRequest(
    val action: String,
    val data: Map<String, Any?>,
    val callbackId: String?
)

fun interface BridgeActionHandler {
    fun handle(request: BridgeRequest, context: BridgeContext, emitter: WxpBridgeEmitter)
}

data class BridgeHandler(
    val requiresWhitelist: Boolean,
    val handler: BridgeActionHandler
)

data class BridgeContext(
    val fragment: Fragment,
    val webView: WebView
) {
    @Volatile
    private var currentUrlSnapshot: String? = null

    val currentUrl: String?
        get() = currentUrlSnapshot

    val currentHost: String?
        get() = currentUrl?.toUri()?.host

    fun updateCurrentUrl(url: String?) {
        currentUrlSnapshot = url
    }

    fun evaluateJavascript(script: String, callback: ((String?) -> Unit)? = null) {
        webView.evaluateJavascript(script, callback)
    }
}
