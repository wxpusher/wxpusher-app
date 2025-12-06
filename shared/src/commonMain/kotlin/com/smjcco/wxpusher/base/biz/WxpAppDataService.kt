package com.smjcco.wxpusher.base.biz

import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.base.common.runAtIOSuspend
import com.smjcco.wxpusher.base.common.runAtMainSuspend
import com.smjcco.wxpusher.base.biz.bean.WxpLoginInfo
import com.smjcco.wxpusher.base.biz.bean.WxpPlatformEnum
import com.smjcco.wxpusher.base.biz.bean.WxpUpdateInfoReq
import com.smjcco.wxpusher.base.common.WxpDateTimeUtils
import com.smjcco.wxpusher.base.common.WxpLoadingUtils
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

    //上报的信息 ，避免重复上报
    private var hasUpdateInfoData: WxpUpdateInfoReq? = null
    private var hasUpdateInfoDataTime: Long = 0L

    /**
     * 数据模块初始化
     */
    fun init() {
        getUserDeviceInfo()
    }

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
        saveLoginInfo(
            WxpLoginInfo(
                deviceToken = deviceToken,
                deviceId = deviceId,
                uid = uid,
                openId = null
            )
        )
        savePushToken(pushToken)
        WxpSaveService.set(mergeIOSDataHasRun, true)
    }

    /**
     * 上传设备信息到服务器
     * 重点是pushToken和设备的绑定关系
     */
    fun updateDeviceInfo(platform: String? = null) {
        runAtIOSuspend {
            val loginInfo = getLoginInfo()
            val updateInfoReq = WxpUpdateInfoReq(loginInfo?.deviceId, getPushToken(), platform)
            if (hasUpdateInfoData != null && hasUpdateInfoData == updateInfoReq
                && WxpDateTimeUtils.getTimestamp() - hasUpdateInfoDataTime < 3600000 //距离上次上报小于1小时，就不上报了
            ) {
                return@runAtIOSuspend
            }
            hasUpdateInfoData = updateInfoReq
            hasUpdateInfoDataTime = WxpDateTimeUtils.getTimestamp()
            WxpApiService.updateDeviceInfo(updateInfoReq) {
                WxpLogUtils.i(message = "更新pushToken成功,updateInfoReq=${updateInfoReq}")
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
     * 删除账号
     */
    fun removeAccount() {
        runAtMainSuspend {
            WxpLoadingUtils.showLoading(msg = "处理中", canDismiss = false)
            val result = WxpApiService.removeAccount()
            WxpLoadingUtils.dismissLoading()
            if (result == true) {
                WxpSaveService.set(SaveLoginInfoKey, "")
                WxpAppPageService.jumpToLogin()
            }
        }
    }

    /**
     * 补全用户数据
     * 因为早期的用户等，登录的时候，数据完整度不够，因此如果数据和服务器版本不一致，就调用这个接口进行一次补全
     */
    fun getUserDeviceInfo() {
        //没有登录不补全
        if (getLoginInfo()?.deviceToken.isNullOrEmpty()) {
            return
        }
        //用户数据版本和最新版本数据一致，就不用进行补全
        if (getLoginInfo()?.version == WxpConfig.UserLoginInfoVersion) {
            return
        }
        runAtIOSuspend {
            val result = WxpApiService.getUserDeviceInfo()
            result?.let {
                saveLoginInfo(WxpLoginInfo(result))
                WxpLogUtils.i(message = "补全用户数据完成")
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

    fun saveOpenId(openId: String?) {
        if (openId.isNullOrEmpty()) {
            return
        }
        runAtIOSuspend {
            val loginInfo = getLoginInfo()
            loginInfo?.openId = openId
            if (loginInfo != null) {
                saveLoginInfo(loginInfo)
            }
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