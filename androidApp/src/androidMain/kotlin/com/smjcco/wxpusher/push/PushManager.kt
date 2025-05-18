package com.smjcco.wxpusher.push

import android.app.Application
import com.smjcco.wxpusher.api.DeviceApi
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.push.honor.HonorPushUtils
import com.smjcco.wxpusher.push.huawei.HuaweiPushUtils
import com.smjcco.wxpusher.push.vivo.VIVOPushUtils
import com.smjcco.wxpusher.push.ws.WsManager
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.DeviceUtils
import com.xiaomi.mipush.sdk.MiPushClient

/**
 * 管理push的一堆事儿，对厂商和通道做抽象
 */
object PushManager {
    private val TAG = "PushManager"

    /**
     * 初始化推送
     */
    fun init(application: Application) {
        if (!ApplicationUtils.isMainProcess()) {
            WxPusherLog.i(TAG, "非主进程，不初始化")
            return
        }

        val platform = DeviceUtils.getPlatform()
        if (platform == DevicePlatform.Android_XIAOMI) {
            WxPusherLog.i(TAG, "初始化小米推送")
            MiPushClient.registerPush(
                application,
                "2882303761520373007",
                "5932037320007"
            )
        } else if (platform == DevicePlatform.Android_VIVO) {
            WxPusherLog.i(TAG, "初始化VIVO推送")
            VIVOPushUtils.init(ApplicationUtils.application)
        } else if (platform == DevicePlatform.Android_HONOR) {
            WxPusherLog.i(TAG, "初始化荣耀推送")
            HonorPushUtils.init(application)
        } else if (platform == DevicePlatform.Android_HUAWEI) {
            WxPusherLog.i(TAG, "初始化华为推送")
            HuaweiPushUtils.init(application)
        } else {
            WxPusherLog.i(TAG, "初始化自建长链接")
            WsManager.init()
        }

//        if (DeviceUtils.isMIUI()) {
//            WxPusherLog.i(TAG, "初始化小米推送")
//            MiPushClient.registerPush(
//                application,
//                "2882303761520373007",
//                "5932037320007"
//            )
//        } else if (PushClient.getInstance(ApplicationUtils.application).isSupport()) {
//            WxPusherLog.i(TAG, "初始化VIVO推送")
//            VIVOPushUtils.init(ApplicationUtils.application)
//        } else if (DeviceUtils.isHuaweiMobileServicesAvailable()) {
//            WxPusherLog.i(TAG, "初始化华为推送")
//            HmsMessaging.getInstance(application).isAutoInitEnabled = true
//            HuaweiPushUtils.init(application)
//        } else {
//            WxPusherLog.i(TAG, "初始化自建长链接")
//            WsManager.init()
//        }
    }

    /**
     * 当获取pushtoken失败的时候回调
     */
    fun onGetPushTokenFail(platform: DevicePlatform) {
        if (platform != DevicePlatform.Android) {
            WxPusherLog.i(TAG, "获取厂商pushToken失败，初始化自建长链接")
            WsManager.init()
        }
    }

    /**
     * 当获取到推动token的时候，管理token的上报，更新
     */
    fun onGetPushToken(token: String, platform: DevicePlatform) {
        WxPusherLog.i(TAG, "收到设备token，platform=${platform}, token=${token}")
        AppDataUtils.savePushToken(token)
        DeviceApi.updateDeviceInfoAsync(platform)
    }


}