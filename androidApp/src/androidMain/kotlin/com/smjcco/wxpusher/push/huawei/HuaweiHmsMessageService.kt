package com.smjcco.wxpusher.push.huawei

import com.huawei.hms.push.HmsMessageService
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.push.PushManager


class HuaweiHmsMessageService : HmsMessageService() {
    private val TAG = "Huawei"
    override fun onNewToken(s: String?) {
        if (s.isNullOrEmpty()) {
            WxPusherLog.w(TAG, "华为推送-onNewToken=null")
            return
        }
        WxPusherLog.i(TAG, "华为推送-通过HuaweiHmsMessageService获取token")
        PushManager.onGetPushToken(s, DevicePlatform.Android_HUAWEI)
    }
}