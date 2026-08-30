package com.smjcco.wxpusher.push.huawei

import android.os.Build
import com.huawei.hms.push.HmsMessageService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.push.PushPlatformResolver


class HuaweiHmsMessageService : HmsMessageService() {
    private val TAG = "Huawei"
    override fun onNewToken(s: String?) {
        if (s.isNullOrEmpty()) {
            WxpLogUtils.w(TAG, "华为推送-onNewToken=null")
            // 其他品牌安装 HMS 后也可能收到空回调，必须与成功分支使用相同的厂商守卫。
            if (PushPlatformResolver.detectVendorPushPlatform() == DevicePlatform.Android_HUAWEI) {
                PushManager.onGetPushTokenFail(DevicePlatform.Android_HUAWEI)
            }
            return
        }
        WxpLogUtils.i(TAG, "华为推送-通过HuaweiHmsMessageService获取token=" + s)
        // 其他品牌安装 HMS 后也可能收到回调，因此按设备厂商能力过滤，不能按当前推送通道过滤。
        if (PushPlatformResolver.detectVendorPushPlatform() == DevicePlatform.Android_HUAWEI) {
            PushManager.onGetPushToken(s, DevicePlatform.Android_HUAWEI)
        } else {
            // 非华为推送设备忽略该 token，避免覆盖设备真正的厂商 token。
            WxpLogUtils.i(TAG, "华为推送-但是是[" + Build.MANUFACTURER + "]设备，忽略华为token=" + s)
        }
    }
}
