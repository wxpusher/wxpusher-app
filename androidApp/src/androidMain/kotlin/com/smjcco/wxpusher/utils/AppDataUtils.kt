package com.smjcco.wxpusher.utils

import com.smjcco.wxpusher.bean.LoginInfo

/**
 * 保存app常用数据
 */
object AppDataUtils {
    private const val SaveLoginInfoKey = "SaveLoginInfoKey"
    private const val PushTokenKey = "PushTokenKey"
    private const val ApiUrl = "ApiUrl"
    private const val WebKey = "WebKey"
    private const val WsUrlKey = "WsUrlKey"


    /**
     * 获取登陆信息
     */
    fun getLoginInfo(): LoginInfo? {
        val infoStr = getLoginInfoStr()
        return GsonUtils.toObj(infoStr, LoginInfo::class)
    }

    /**
     * 返回string类型，方便给到容器
     */
    fun getLoginInfoStr(): String? {
        return SaveUtils.getByKey(SaveLoginInfoKey)
    }

    /**
     * 保存登陆信息
     */
    fun saveLoginInfo(loginInfoStr: String?) {
        SaveUtils.setKeyValue(SaveLoginInfoKey, loginInfoStr)
    }

    /**
     * 获取pushToken
     */
    fun getPushToken(): String? = SaveUtils.getByKey(PushTokenKey)

    /**
     * 保存pushToken
     */
    fun savePushToken(pushToken: String?) {
        SaveUtils.setKeyValue(PushTokenKey, pushToken)
    }

    /**
     * 保存后端api接口
     */
    fun saveApiUrl(pushToken: String?) {
        SaveUtils.setKeyValue(ApiUrl, pushToken)
    }

    fun getApiUrl(): String = SaveUtils.getByKey(ApiUrl) ?: "https://wxpusher.zjiecode.com"


    /**
     * 保存web加载地址
     */
    fun saveWebUrl(pushToken: String?) {
        SaveUtils.setKeyValue(WebKey, pushToken)
    }

    fun getWebUrl(): String =
        SaveUtils.getByKey(WebKey) ?: "https://static.zjiecode.com/wxpusher/web-app"

    /**
     * 保存websocket连接地址
     */
    fun saveWsUrl(wsUrl: String?) {
        SaveUtils.setKeyValue(WsUrlKey, wsUrl)
    }

    fun getWsUrl(): String =
        SaveUtils.getByKey(WsUrlKey) ?: "wss://wxpusher.zjiecode.com"
}