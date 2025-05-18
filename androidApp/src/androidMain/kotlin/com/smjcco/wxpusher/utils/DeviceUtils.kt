package com.smjcco.wxpusher.utils

import com.heytap.msp.push.HeytapPushManager
import com.hihonor.push.sdk.HonorPushClient
import com.huawei.hms.api.HuaweiApiAvailability
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.config.ConfigManager
import com.vivo.push.PushClient

object DeviceUtils {
    //是否是小米设备
    fun isMIUI(): Boolean {
        return ConfigManager.getCurrentConfig().xiaomiPush
                && "Xiaomi".equals(android.os.Build.MANUFACTURER, true)
    }

    /**
     * 是否支持华为推送
     * 验证HMS Core（APK）在设备上是否成功安装和集成，检查已经安装的HMS Core（APK）版本号是否为client所需要的版本号或比需要的更新。该类为抽象方法，需要子类实现。
     * https://developer.huawei.com/consumer/cn/doc/hmscore-common-References/huaweiapiavailability-0000001050121134#section9492524178
     */
    fun isHuaweiMobileServicesAvailable(): Boolean {
        return ConfigManager.getCurrentConfig().huaweiPush
                && 0 == HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(ApplicationUtils.application)
    }

    fun isHonor(): Boolean {
        return ConfigManager.getCurrentConfig().honorPush
                && HonorPushClient.getInstance()
            .checkSupportHonorPush(ApplicationUtils.application)
    }

    fun isVivo(): Boolean {
        return ConfigManager.getCurrentConfig().vivoPush
                && PushClient.getInstance(ApplicationUtils.application).isSupport()
    }

    fun isOppo(): Boolean {
        return ConfigManager.getCurrentConfig().vivoPush
                && HeytapPushManager.isSupportPush(ApplicationUtils.application)
    }

    fun getPlatform(): DevicePlatform {
        if (isMIUI()) {
            return DevicePlatform.Android_XIAOMI
        } else if (isVivo()) {
            return DevicePlatform.Android_VIVO
        } else if (isHonor()) {
            return DevicePlatform.Android_HONOR
        } else if (isHuaweiMobileServicesAvailable()) {
            return DevicePlatform.Android_HUAWEI
        } else if (isOppo()) {
            return DevicePlatform.Android_OPPO
        }
        return DevicePlatform.Android
    }

}