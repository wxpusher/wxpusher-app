package com.smjcco.wxpusher.push

import android.app.Activity
import android.app.Application
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.honor.HonorPushUtils
import com.smjcco.wxpusher.push.huawei.HuaweiPushUtils
import com.smjcco.wxpusher.push.meizu.MeizuPushUtils
import com.smjcco.wxpusher.push.oppo.OppoPushUtils
import com.smjcco.wxpusher.push.vivo.VIVOPushUtils
import com.smjcco.wxpusher.push.xiaomi.XiaomiUtils
import com.smjcco.wxpusher.utils.PermissionUtils

interface IPushTokenChangedListener {
    fun onPushToken(platform: DevicePlatform, pushToken: String)
}

/**
 * 各厂商推送 SDK 的统一适配入口。
 *
 * 通道决策由 [PushChannelCoordinator] 负责，本类只发起厂商注册并转发 token 回调。
 */
object PushManager {
    private val TAG = "PushManager"

    private val pushTokenChangedListenerList: MutableList<IPushTokenChangedListener> =
        mutableListOf()

    /**
     * 初始化推送
     */
    fun init(application: Application = ApplicationUtils.getApplication()) {
        if (!ApplicationUtils.isMainProcess()) {
            WxpLogUtils.i(TAG, "非主进程，不初始化")
            return
        }

        PushChannelCoordinator.init(application)
        PushActiveReportLifecycle.init(application)
    }

    /** 由协调器调用，只负责发起当前设备对应厂商 SDK 的注册。 */
    internal fun startVendorRegistration(application: Application, platform: DevicePlatform) {
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
        } else if (platform == DevicePlatform.Android_MEIZU) {
            WxpLogUtils.i(TAG, "初始化魅族推送")
            MeizuPushUtils.init(application)
        } else {
            WxpLogUtils.i(TAG, "当前设备没有可注册的厂商推送，platform=$platform")
        }
    }

    /** 将厂商 token 获取失败事件交给通道协调器处理。 */
    fun onGetPushTokenFail(platform: DevicePlatform) {
        PushChannelCoordinator.onVendorTokenFailed(platform)
    }

    /** 根据 token 来源分发给厂商通道或 WebSocket 通道。 */
    fun onGetPushToken(token: String, platform: DevicePlatform) {
        WxpLogUtils.i(TAG, "收到设备token，platform=${platform}, token=${token}")
        if (platform == DevicePlatform.Android) {
            PushChannelCoordinator.onWsToken(token)
        } else {
            PushChannelCoordinator.onVendorToken(token, platform)
        }
    }

    /** 仅在新通道真正生效后通知旧有业务监听器。 */
    internal fun notifyEffectiveTokenChanged(platform: DevicePlatform, token: String) {
        for (listener in pushTokenChangedListenerList.toList()) {
            listener.onPushToken(platform, token)
        }
    }

    fun addPushTokenChangedListener(listener: IPushTokenChangedListener) {
        pushTokenChangedListenerList.add(listener)
    }

    fun removePushTokenChangedListener(listener: IPushTokenChangedListener) {
        pushTokenChangedListenerList.remove(listener)
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
        val platform = PushPlatformState.getEffectivePushPlatform()
        if (platform == DevicePlatform.Android_XIAOMI) {
            XiaomiUtils.showSettingGuide(activity)
        } else if (platform == DevicePlatform.Android_VIVO) {
            CommonUtils.showSettingGuide(activity)
        } else if (platform == DevicePlatform.Android_HONOR) {
            CommonUtils.showSettingGuide(activity)
        } else if (platform == DevicePlatform.Android_HUAWEI) {
            //华为打开app的时候，不让弹窗，因此先注释调
//            CommonUtils.showSettingGuide(activity)
        } else if (platform == DevicePlatform.Android_OPPO) {
            CommonUtils.showSettingGuide(activity)
        } else if (platform == DevicePlatform.Android_MEIZU) {
            MeizuPushUtils.showSettingGuide(activity)
        }
    }

    fun getGuidePageUrl(): String {
        val platform = PushPlatformResolver.detectVendorPushPlatform()
        return "https://wxpusher.zjiecode.com/docs/open-app-note/index.html?brand=%s".format(
            platform.getPlatform()
        )
    }


}
