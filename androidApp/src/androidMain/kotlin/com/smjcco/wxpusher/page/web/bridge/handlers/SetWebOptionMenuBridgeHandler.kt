package com.smjcco.wxpusher.page.web.bridge.handlers

import com.smjcco.wxpusher.page.web.WxpWebViewFragment
import com.smjcco.wxpusher.page.web.bridge.BridgeActionHandler
import com.smjcco.wxpusher.page.web.bridge.BridgeContext
import com.smjcco.wxpusher.page.web.bridge.BridgeRequest
import com.smjcco.wxpusher.page.web.bridge.WxpBridgeEmitter

object SetWebOptionMenuBridgeHandler : BridgeActionHandler {
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
        val hasVisibleField = request.data.containsKey("visible")
        val hasOptionsField = request.data.containsKey("options")
        if (!hasVisibleField && !hasOptionsField) {
            webFragment.setOptionMenuVisibleOverride(null)
            webFragment.setOptionMenuItemsOverride(null)
            emitter.sendBridgeCallback(callbackId = request.callbackId, success = true)
            return
        }
        if (hasVisibleField) {
            val visible = parseBooleanValue(request.data["visible"])
            if (visible == null) {
                emitter.sendBridgeCallback(
                    callbackId = request.callbackId,
                    success = false,
                    error = "visible must be boolean"
                )
                return
            }
            webFragment.setOptionMenuVisibleOverride(visible)
        }
        if (hasOptionsField) {
            val parsedOptions = parseOptionKeys(request.data["options"])
            if (parsedOptions == null) {
                emitter.sendBridgeCallback(
                    callbackId = request.callbackId,
                    success = false,
                    error = "options must be string array"
                )
                return
            }
            val unsupportedOptions = parsedOptions.filterNot { WxpWebViewFragment.SUPPORTED_OPTION_MENU_KEYS.contains(it) }
            if (unsupportedOptions.isNotEmpty()) {
                emitter.sendBridgeCallback(
                    callbackId = request.callbackId,
                    success = false,
                    error = "unsupported options: ${unsupportedOptions.joinToString(",")}"
                )
                return
            }
            webFragment.setOptionMenuItemsOverride(parsedOptions.toSet())
        }
        emitter.sendBridgeCallback(
            callbackId = request.callbackId,
            success = true
        )
    }

    private fun parseOptionKeys(value: Any?): List<String>? {
        if (value == null) {
            return null
        }
        val listValue = value as? List<*> ?: return null
        val parsed = mutableListOf<String>()
        for (item in listValue) {
            val text = item as? String ?: return null
            parsed.add(text)
        }
        return parsed
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
