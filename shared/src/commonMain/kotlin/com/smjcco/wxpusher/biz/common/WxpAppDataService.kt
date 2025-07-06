package com.smjcco.wxpusher.biz.common

import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.WxpBaseInfoService
import com.smjcco.wxpusher.base.WxpSaveService
import com.smjcco.wxpusher.base.runAtMainSuspend
import com.smjcco.wxpusher.biz.bean.WxpLoginInfo
import com.smjcco.wxpusher.biz.bean.WxpPlatformEnum
import com.smjcco.wxpusher.biz.bean.WxpUpdateInfoReq
import kotlinx.serialization.json.Json

object WxpAppDataService {
    private const val SaveLoginInfoKey = "SaveLoginInfoKey"
    private const val PushTokenKey = "PushTokenKey"
    private const val ApiUrl = "ApiUrl"
    private const val WebKey = "WebKey"
    private const val WsUrlKey = "WsUrlKey"

    private const val mergeIOSDataHasRun = "mergeIOSDataHasRun"

    /**
     * 针对iOS，第一次启动的时候，进行一次数据迁移，避免用户重新登录
     */
    fun mergeIOSData() {
        if (WxpBaseInfoService.getPlatform() != WxpPlatformEnum.iOS.platform) {
            return
        }
        if (WxpSaveService.get(mergeIOSDataHasRun, false)) {
            return
        }
        //把iOS的数据，读取出来，存档到新的方式里面，避免用户重新登录
        val uid = WxpSaveService.get("sp_uid", "")
        val deviceId = WxpSaveService.get("deviceId", "")
        val deviceToken = WxpSaveService.get("deviceToken", "")
        val pushToken = WxpSaveService.get("pushToken", "")
        saveLoginInfo(WxpLoginInfo(deviceToken, deviceId, uid))
        savePushToken(pushToken)
        WxpSaveService.set(mergeIOSDataHasRun, true)
    }

    /**
     * 上传设备信息到服务器
     * 重点是pushToken和设备的绑定关系
     */
    fun updateDeviceInfo() {
        runAtMainSuspend {
            val loginInfo = getLoginInfo()
            val updateInfoReq = WxpUpdateInfoReq(loginInfo?.deviceId, loginInfo?.deviceToken)
            WxpApiService.updateDeviceInfo(updateInfoReq) {
                println("更新pushToken成功,updateInfoReq=${updateInfoReq}")
            }
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        runAtMainSuspend {
            WxpApiService.logout {
                //删除本地的deviceToken
                getLoginInfo()?.let {
                    it.deviceToken = null
                    saveLoginInfo(it)
                }
                WxpAppPageService.jumpToLogin()
            }
        }
    }

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
    private fun getLoginInfoStr(): String? {
        return WxpSaveService.get(SaveLoginInfoKey, "")
    }

//    /**
//     * 保存登陆信息
//     */
//    fun saveLoginInfo(loginInfoStr: String?) {
//        WxpSaveService.set(SaveLoginInfoKey, loginInfoStr)
//    }

    fun saveLoginInfo(loginInfo: WxpLoginInfo) {
        WxpSaveService.set(SaveLoginInfoKey, Json.encodeToString(loginInfo))
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