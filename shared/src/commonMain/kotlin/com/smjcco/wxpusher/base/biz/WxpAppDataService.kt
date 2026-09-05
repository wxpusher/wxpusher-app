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

    /** 相同设备信息的最小重复上报间隔。 */
    private const val DEVICE_INFO_REPORT_INTERVAL_MILLIS = 60 * 60 * 1000L

    //上报的信息 ，避免重复上报
    private var hasUpdateInfoData: WxpUpdateInfoReq? = null
    private var hasUpdateInfoDataTime: Long = 0L

    // 正在上报中的内容，用于挡掉冷启动时多个入口发起的完全相同的请求。
    private var reportingInfoData: WxpUpdateInfoReq? = null

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
        if (WxpBaseInfoService.getClientPlatform() != WxpPlatformEnum.iOS.platform) {
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
     * 上传当前已经生效的平台和 pushToken。
     *
     * Android 的通道路由由 PushChannelCoordinator 统一同步；本方法保留给 iOS 等固定
     * 平台调用，并确保 platform 为空时也使用当前生效平台，不能再单独覆盖 token。
     *
     * @param platform 需要上报的推送路由平台，为空时使用当前生效平台。
     * @param silent 后台自动上报传 true，失败时不弹 toast，也不会跳转登录页。
     */
    fun updateDeviceInfo(platform: String? = null, silent: Boolean = false) {
        runAtIOSuspend {
            val loginInfo = getLoginInfo()
            val effectivePlatform = platform ?: WxpBaseInfoService.getEffectivePushPlatform()
            val updateInfoReq = WxpUpdateInfoReq(
                loginInfo?.deviceId,
                getPushToken(),
                effectivePlatform,
            )
            // 相同内容一小时内不重复上报；token 或平台变化时仍然立即上报。
            if (hasUpdateInfoData == updateInfoReq && !isDeviceInfoReportExpired()) {
                return@runAtIOSuspend
            }
            // 冷启动时多个入口可能同时发起完全相同的请求，在途的直接跳过。
            if (reportingInfoData == updateInfoReq) {
                return@runAtIOSuspend
            }

            reportingInfoData = updateInfoReq
            try {
                WxpApiService.updateDeviceInfo(updateInfoReq, silent = silent) {
                    // 只有上报成功后才记录去重状态，失败请求允许后续继续重试。
                    recordDeviceInfoReportSuccess(updateInfoReq)
                    WxpLogUtils.i(message = "更新pushToken成功,updateInfoReq=${updateInfoReq}")
                }
            } finally {
                // 期间可能已经有更新的内容开始上报，不能把它的在途标记清掉。
                if (reportingInfoData == updateInfoReq) {
                    reportingInfoData = null
                }
            }
        }
    }

    /**
     * iOS 应用进入前台时按需重新上报 APNs token 和设备活跃信息。
     *
     * APNs 返回 400 或 410 后，服务端会把设备标记为异常；客户端使用仍然有效的 token
     * 重新上报后，服务端会恢复正常状态。进程存活期间使用一小时间隔避免频繁请求；冷启动
     * 会重新上报，失败不会更新时间，后续进入前台仍可重试。
     *
     * deviceId、pushToken 和相同内容去重都由 [updateDeviceInfo] 和接口层统一校验，
     * 这里只判断登录态和上报间隔，避免同一条规则散落在多处。
     */
    fun reportIOSActiveIfNeeded() {
        if (getLoginInfo()?.deviceToken.isNullOrEmpty() || !isDeviceInfoReportExpired()) {
            return
        }

        WxpLogUtils.i(message = "iOS进入前台，重新上报APNs token和设备活跃信息")
        updateDeviceInfo(silent = true)
    }

    /**
     * 判断距离最近一次成功上报是否已经超过间隔。
     *
     * Android 和 iOS 共用同一套一小时规则。尚未成功上报过，或系统时间回拨导致间隔为负时，
     * 都允许立即上报，避免异常时间让设备长期无法恢复正常状态。
     */
    fun isDeviceInfoReportExpired(): Boolean {
        val elapsed = WxpDateTimeUtils.getTimestamp() - hasUpdateInfoDataTime
        return elapsed < 0L || elapsed >= DEVICE_INFO_REPORT_INTERVAL_MILLIS
    }

    /**
     * 记录最近一次成功上报的内容和时间。
     *
     * 只能在服务端明确返回成功后调用；失败请求不能更新，否则会阻止后续前台重试。
     * Android 协调器自行构造请求体，因此必须把上报内容一起传入，保证内容去重和时间节流
     * 两个状态始终成对更新。
     */
    fun recordDeviceInfoReportSuccess(reportedInfo: WxpUpdateInfoReq) {
        hasUpdateInfoData = reportedInfo
        hasUpdateInfoDataTime = WxpDateTimeUtils.getTimestamp()
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
     fun getLoginInfoStr(): String? {
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
