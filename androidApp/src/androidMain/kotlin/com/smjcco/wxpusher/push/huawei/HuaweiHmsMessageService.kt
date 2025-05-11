package com.smjcco.wxpusher.push.huawei

import com.huawei.hms.push.HmsMessageService
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.PushManager


class HuaweiHmsMessageService : HmsMessageService() {

    override fun onNewToken(s: String) {
        PushManager.onGetPushToken(s, DevicePlatform.Android_HUAWEI)
    }
}