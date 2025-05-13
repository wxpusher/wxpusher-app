package com.smjcco.wxpusher.push

import android.app.Application
import android.util.Log
import com.huawei.hms.push.HmsMessaging
import com.smjcco.wxpusher.api.DeviceApi
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.push.huawei.HuaweiPushUtils
import com.smjcco.wxpusher.push.ws.WsManager
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.ApplicationUtils.application
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
//        if (!ApplicationUtils.isMainProcess()) {
//            WxPusherLog.i(TAG, "非主进程，不初始化")
//            return
//        }
        if (DeviceUtils.isHuaweiMobileServicesAvailable()) {
            WxPusherLog.i(TAG, "初始化华为推送")
            HmsMessaging.getInstance(application).isAutoInitEnabled = true
            HuaweiPushUtils.initPushToken(application)
        } else if (DeviceUtils.isMIUI()) {
            WxPusherLog.i(TAG, "初始化小米推送")
            MiPushClient.registerPush(
                application,
                "2882303761520373007",
                "5932037320007"
            )
        } else {
            WxPusherLog.i(TAG, "初始化自建长链接")
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

    /**
     * 反注册push，感觉应该用不到
     */
    fun disablePush() {
        if (DeviceUtils.isMIUI()) {
            MiPushClient.unregisterPush(application)
        } else if (DeviceUtils.isHuaweiMobileServicesAvailable()) {
            HmsMessaging.getInstance(application).turnOffPush()
        } else {
            WsManager.disconnect()
        }
    }
}