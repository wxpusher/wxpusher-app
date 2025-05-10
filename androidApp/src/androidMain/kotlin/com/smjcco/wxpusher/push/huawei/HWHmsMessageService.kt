package com.smjcco.wxpusher.push.huawei

import android.util.Log
import com.huawei.hms.push.HmsMessageService


class HWHmsMessageService : HmsMessageService() {
    private val TAG = "HWHmsMessageService"

    override fun onNewToken(s: String) {
        Log.d(TAG, "收到HW-token = [$s]")
    }
}