package com.smjcco.wxpusher.kmp.push

import android.app.Activity
import android.app.Application
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.kmp.common.utils.DeviceUtils
import com.smjcco.wxpusher.kmp.push.honor.HonorPushUtils
import com.smjcco.wxpusher.kmp.push.huawei.HuaweiPushUtils
import com.smjcco.wxpusher.kmp.push.oppo.OppoPushUtils
import com.smjcco.wxpusher.kmp.push.vivo.VIVOPushUtils
import com.smjcco.wxpusher.kmp.push.ws.WxpNotificationManager
import com.smjcco.wxpusher.kmp.push.ws.connect.WsManager
import com.smjcco.wxpusher.kmp.push.ws.WsUtils
import com.smjcco.wxpusher.kmp.push.ws.keepalive.KeepWsAliveServiceStarter
import com.smjcco.wxpusher.kmp.push.xiaomi.XiaomiUtils
import com.smjcco.wxpusher.kmp.common.utils.PermissionUtils

/**
 * 管理push的一堆事儿，对厂商和通道做抽象
 */
object PushManager {
    private val TAG = "PushManager"

    /**
     * 初始化推送
     */
    fun init(application: Application = ApplicationUtils.getApplication()) {
        if (!ApplicationUtils.isMainProcess()) {
            WxpLogUtils.i(TAG, "非主进程，不初始化")
            return
        }

        val platform = DeviceUtils.getPlatform()
        if (platform == DevicePlatform.Android_XIAOMI) {
            WxpLogUtils.i(TAG, "初始化小米推送")
            XiaomiUtils.init(application)
        } else if (platform == DevicePlatform.Android_VIVO) {
            WxpLogUtils.i(TAG, "初始化VIVO推送")
            VIVOPushUtils.init(ApplicationUtils.getApplication())
        } else if (platform == DevicePlatform.Android_HONOR) {
            WxpLogUtils.i(TAG, "初始化荣耀推送")
            HonorPushUtils.init(application)
        } else if (platform == DevicePlatform.Android_HUAWEI) {
            WxpLogUtils.i(TAG, "初始化华为推送")
            HuaweiPushUtils.init(application)
        } else if (platform == DevicePlatform.Android_OPPO) {
            WxpLogUtils.i(TAG, "初始化OPPO推送")
            OppoPushUtils.init(application)
        } else {
            WxpLogUtils.i(TAG, "初始化自建长链接")
            WxpNotificationManager.init()
            WsManager.init()
            //启动保活，必须在最后
            KeepWsAliveServiceStarter.start(application)
        }

    }

    /**
     * 当获取pushtoken失败的时候回调
     */
    fun onGetPushTokenFail(platform: DevicePlatform) {
        if (platform != DevicePlatform.Android) {
            WxpLogUtils.i(TAG, "获取厂商pushToken失败，初始化自建长链接")
            WsManager.init()
        }
    }

    /**
     * 当获取到推动token的时候，管理token的上报，更新
     */
    fun onGetPushToken(token: String, platform: DevicePlatform) {
        WxpLogUtils.i(TAG, "收到设备token，platform=${platform}, token=${token}")
        WxpAppDataService.savePushToken(token)
        WxpAppDataService.updateDeviceInfo()
    }

    /**
     * 显示打开通知提醒的弹窗
     */
    fun showOpenNoteRemindSettingDialog(activity: Activity) {
        //没登录不提醒
        if (WxpAppDataService.getLoginInfo()?.deviceToken.isNullOrEmpty()) {
            return
        }
        //没有推送id，不提醒
        if (WxpAppDataService.getPushToken().isNullOrEmpty()) {
            return
        }
        //没有推送权限不提醒
        if (!PermissionUtils.hasNotificationPermission(activity)) {
            return
        }
        val platform = DeviceUtils.getPlatform()
        if (platform == DevicePlatform.Android_XIAOMI) {
            XiaomiUtils.showSettingGuide(activity)
        } else if (platform == DevicePlatform.Android_VIVO) {
            CommonUtils.showSettingGuide(activity)
        } else if (platform == DevicePlatform.Android_HONOR) {
            CommonUtils.showSettingGuide(activity)
        } else if (platform == DevicePlatform.Android_HUAWEI) {
            CommonUtils.showSettingGuide(activity)
        } else if (platform == DevicePlatform.Android_OPPO) {
            CommonUtils.showSettingGuide(activity)
        } else {
            WsUtils.showSettingGuide(activity)
        }
    }

    fun getGuidePageUrl(): String {
        val platform = DeviceUtils.getPlatform()
        return "https://wxpusher.zjiecode.com/docs/open-app-note/index.html?brand=%s".format(
            platform.getPlatform()
        )
    }

}