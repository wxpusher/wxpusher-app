package com.smjcco.wxpusher.kmp.push.honor

import android.app.Application
import com.hihonor.push.sdk.HonorPushCallback
import com.hihonor.push.sdk.HonorPushClient
import com.huawei.hms.common.ApiException
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpScopeUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.kmp.push.PushManager
import kotlinx.coroutines.launch


object HonorPushUtils {
    private val TAG = "Honor"
    fun init(application: Application) {
        HonorPushClient.getInstance().init(application, true);
        WxpScopeUtils.getIoScopeScope().launch {
            try {
                HonorPushClient.getInstance().getPushToken(object : HonorPushCallback<String?> {
                    override fun onSuccess(pushToken: String?) {
                        if (pushToken.isNullOrEmpty()) {
                            WxpLogUtils.w(TAG, "荣耀推送-init-onNewToken=null")
                            return
                        }
                        PushManager.onGetPushToken(pushToken, DevicePlatform.Android_HONOR)

                        //打开通知栏消息状态
                        HonorPushClient.getInstance()
                            .turnOnNotificationCenter(object : HonorPushCallback<Void?> {
                                override fun onSuccess(aVoid: Void?) {
                                    WxpLogUtils.i(TAG, "荣耀推送-init-打开通知栏目推送成功")
                                }

                                override fun onFailure(errorCode: Int, errorString: String) {
                                    WxpLogUtils.w(
                                        TAG,
                                        "荣耀推送-init-turnOnNotificationCenter失败，errorCode=$errorCode,errorString=$errorString"
                                    )
                                }
                            })
                    }

                    override fun onFailure(errorCode: Int, errorString: String) {
                        WxpLogUtils.w(
                            TAG,
                            "荣耀推送-init-失败，errorCode=$errorCode,errorString=$errorString"
                        )
                    }
                })

            } catch (e: ApiException) {
                WxpLogUtils.e(TAG, "荣耀推送-init- 获取token失败", e)
                PushManager.onGetPushTokenFail(DevicePlatform.Android_HUAWEI)
            }
        }
    }
}