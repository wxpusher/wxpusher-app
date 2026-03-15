package com.smjcco.wxpusher.page.web.bridge

import com.google.gson.JsonObject
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.utils.GsonUtils

class WxpBridgeMessageParser {
    fun parse(messageJson: String): BridgeRequest? {
        return try {
            val messageBody = GsonUtils.toObj(messageJson, JsonObject::class.java) ?: return null
            val action = messageBody.get("action")?.asString ?: return null
            val callbackId = if (messageBody.has("callbackId") && !messageBody.get("callbackId").isJsonNull) {
                messageBody.get("callbackId").asString
            } else {
                null
            }
            val dataElement = messageBody.get("data")
            val dataMap = if (dataElement != null && dataElement.isJsonObject) {
                GsonUtils.jsonToObj(dataElement.asJsonObject, Map::class.java) as? Map<String, Any?>
                    ?: emptyMap()
            } else {
                emptyMap()
            }

            BridgeRequest(
                action = action,
                data = dataMap,
                callbackId = callbackId
            )
        } catch (e: Exception) {
            WxpLogUtils.w(message = "处理WebBridge消息失败", throwable = e)
            null
        }
    }
}
