package com.smjcco.wxpusher.utils

import com.hihonor.push.sdk.HonorPushClient
import com.huawei.hms.api.HuaweiApiAvailability
import com.smjcco.wxpusher.bean.DevicePlatform
import com.vivo.push.PushClient

object DeviceUtils {
    //是否是小米设备
    fun isMIUI(): Boolean {
        return "Xiaomi".equals(android.os.Build.MANUFACTURER, true)
    }

    /**
     * 是否支持华为推送
     * 验证HMS Core（APK）在设备上是否成功安装和集成，检查已经安装的HMS Core（APK）版本号是否为client所需要的版本号或比需要的更新。该类为抽象方法，需要子类实现。
     * https://developer.huawei.com/consumer/cn/doc/hmscore-common-References/huaweiapiavailability-0000001050121134#section9492524178
     */
    fun isHuaweiMobileServicesAvailable(): Boolean {
        return 0 == HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(ApplicationUtils.application)
    }

    fun getPlatform(): DevicePlatform {
        if (isMIUI()) {
            return DevicePlatform.Android_XIAOMI
        } else if (PushClient.getInstance(ApplicationUtils.application).isSupport()) {
            return DevicePlatform.Android_VIVO
        } else if (HonorPushClient.getInstance().checkSupportHonorPush(ApplicationUtils.application)) {
            return DevicePlatform.Android_HONOR
        } else if (isHuaweiMobileServicesAvailable()) {
            return DevicePlatform.Android_HUAWEI
        }
        return DevicePlatform.Android
    }

}