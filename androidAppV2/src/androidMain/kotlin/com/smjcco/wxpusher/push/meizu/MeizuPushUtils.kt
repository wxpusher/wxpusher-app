package com.smjcco.wxpusher.push.meizu

import android.app.Application
import com.meizu.cloud.pushsdk.PushManager
import com.smjcco.wxpusher.base.common.WxpLogUtils

object MeizuPushUtils {
    /**
     * 初始化魅族推送
     */
    fun init(application: Application) {
        WxpLogUtils.i("MEIZU", "魅族SDK version=" + PushManager.getSdkVersion())
        PushManager.register(application, "155986", "b8bd81b459d840779feea5b87d33714f");
    }
}