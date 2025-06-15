package com.smjcco.wxpusher.biz.common

import com.smjcco.wxpusher.base.WxpSaveService
import com.smjcco.wxpusher.biz.bean.WxpLoginInfo
import kotlinx.serialization.json.Json

object WxpAppDataService {
    private const val SaveLoginInfoKey = "SaveLoginInfoKey"
    private const val PushTokenKey = "PushTokenKey"
    private const val ApiUrl = "ApiUrl"
    private const val WebKey = "WebKey"
    private const val WsUrlKey = "WsUrlKey"


    /**
     * 获取登陆信息
     */
    fun getLoginInfo(): WxpLoginInfo? {
        return getLoginInfoStr()?.let {
            if (it.isEmpty()) {
                return@let null
            }
            return@let Json.decodeFromString(it)
        }
    }

    /**
     * 返回string类型，方便给到容器
     */
    fun getLoginInfoStr(): String? {
        return WxpSaveService.get(SaveLoginInfoKey, "")
    }

    /**
     * 保存登陆信息
     */
    fun saveLoginInfo(loginInfoStr: String?) {
        WxpSaveService.set(SaveLoginInfoKey, loginInfoStr)
    }

    /**
     * 获取pushToken
     */
    fun getPushToken(): String? = WxpSaveService.get(PushTokenKey, "")

    /**
     * 保存pushToken
     */
    fun savePushToken(pushToken: String?) {
        WxpSaveService.set(PushTokenKey, pushToken)
    }

    /**
     * 保存后端api接口
     */
    fun saveApiUrl(baseApiUrl: String?) {
        WxpSaveService.set(ApiUrl, baseApiUrl)
    }

    fun getApiUrl(): String = WxpSaveService.get(ApiUrl, "https://wxpusher.zjiecode.com")


    /**
     * 保存web加载地址
     */
    fun saveWebUrl(webUrl: String?) {
        WxpSaveService.set(WebKey, webUrl)
    }

    fun getWebUrl(): String =
        WxpSaveService.get(WebKey, "https://static.zjiecode.com/wxpusher/web-app")

    /**
     * 保存websocket连接地址
     */
    fun saveWsUrl(wsUrl: String?) {
        WxpSaveService.set(WsUrlKey, wsUrl)
    }

    fun getWsUrl(): String = WxpSaveService.get(WsUrlKey, "wss://wxpusher.zjiecode.com")
}