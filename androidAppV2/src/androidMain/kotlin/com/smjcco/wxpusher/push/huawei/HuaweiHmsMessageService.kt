package com.smjcco.wxpusher.push.huawei

import com.huawei.hms.push.HmsMessageService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.utils.DeviceUtils


class HuaweiHmsMessageService : HmsMessageService() {
    private val TAG = "Huawei"
    override fun onNewToken(s: String?) {
        if (s.isNullOrEmpty()) {
            WxpLogUtils.w(TAG, "华为推送-onNewToken=null")
            PushManager.onGetPushTokenFail(DevicePlatform.Android_HONOR)
            return
        }
        WxpLogUtils.i(TAG, "华为推送-通过HuaweiHmsMessageService获取token=" + s)
        if (DeviceUtils.getPlatform() == DevicePlatform.Android_HONOR) {
            //不知道为啥，在荣耀的手机【HONOR FRI-AN00】上，初始化荣耀推送，华为这里会进行一次回调，导致pushToken被覆盖了
            WxpLogUtils.i(TAG, "华为推送-但是是荣耀设备，忽略华为token=" + s)
        } else {
            PushManager.onGetPushToken(s, DevicePlatform.Android_HUAWEI)
        }
    }
}