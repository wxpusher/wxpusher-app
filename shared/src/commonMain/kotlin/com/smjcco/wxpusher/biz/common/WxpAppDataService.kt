package com.smjcco.wxpusher.biz.common

import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.WxpBaseInfoService
import com.smjcco.wxpusher.base.WxpLogUtils
import com.smjcco.wxpusher.base.WxpSaveService
import com.smjcco.wxpusher.base.letOnNotEmpty
import com.smjcco.wxpusher.base.runAtMainSuspend
import com.smjcco.wxpusher.biz.bean.WxpLoginInfo
import com.smjcco.wxpusher.biz.bean.WxpPlatformEnum
import com.smjcco.wxpusher.biz.bean.WxpUpdateInfoReq
import com.smjcco.wxpusher.page.messagelist.WxpMessageListMessage
import kotlinx.serialization.json.Json

object WxpAppDataService {
    private const val MessageListCacheKey = "WxpMessageList_MessageSaveCacheKey"
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
        WxpLogUtils.d(message = "开始迁移iOS数据")
        if (WxpBaseInfoService.getPlatform() != WxpPlatformEnum.iOS.platform) {
            WxpLogUtils.d(message = "开始迁移iOS数据，非iOS")
            return
        }
        if (WxpSaveService.get(mergeIOSDataHasRun, false)) {
            WxpLogUtils.d(message = "开始迁移iOS数据，已经迁移过")
            return
        }

        //把iOS的数据，读取出来，存档到新的方式里面，避免用户重新登录
        val uid = WxpSaveService.get("sp_uid", "")
        val deviceId = WxpSaveService.get("deviceId", "")
        val deviceToken = WxpSaveService.get("deviceToken", "")
        val pushToken = WxpSaveService.get("pushToken", "")
        WxpLogUtils.d(message = "开始迁移iOS数据，读取数据 deviceToken=" + deviceToken)
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
            val updateInfoReq = WxpUpdateInfoReq(loginInfo?.deviceId, getPushToken())
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
     * 解除手机号绑定
     */
    fun unbindPhone() {
        runAtMainSuspend {
            WxpApiService.unbindPhone {
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
     * 获取缓存的消息列表数据
     */
    fun getCacheMessageList(): List<WxpMessageListMessage>? {
        val messageDataStr = WxpSaveService.get(MessageListCacheKey, "")
        if (messageDataStr.isEmpty()) {
            return null
        }
        return Json.decodeFromString(messageDataStr)
    }

    /**
     * 保存消息列表缓存
     */
    fun setCacheMessageList(messageList: List<WxpMessageListMessage>?) {
        if (messageList.isNullOrEmpty()) {
            WxpSaveService.set(MessageListCacheKey, "")
            return
        }
        val dataStr = Json.encodeToString(messageList)
        WxpSaveService.set(MessageListCacheKey, dataStr)
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