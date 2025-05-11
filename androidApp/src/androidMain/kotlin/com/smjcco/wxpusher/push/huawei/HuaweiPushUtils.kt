package com.smjcco.wxpusher.push.huawei

import android.app.Application
import android.text.TextUtils
import android.util.Log
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.utils.WxPusherUtils
import kotlinx.coroutines.launch

object HuaweiPushUtils {
    private final val TAG = "Huawei"
    fun initPushToken(application: Application) {
        WxPusherUtils.getIoScopeScope().launch {
            try {
                val token = HmsInstanceId.getInstance(application)
                    .getToken("114073793", "HCM")
                Log.i(TAG, "Get token: $token")
                if (!TextUtils.isEmpty(token)) {
                    PushManager.onGetPushToken(token, DevicePlatform.Android_HUAWEI)
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Get token failed, $e")
            }
        }
    }
}