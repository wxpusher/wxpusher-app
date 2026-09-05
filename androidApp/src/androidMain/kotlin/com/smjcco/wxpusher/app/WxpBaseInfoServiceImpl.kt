package com.smjcco.wxpusher.app

import com.smjcco.wxpusher.base.biz.bean.WxpPlatformEnum
import com.smjcco.wxpusher.base.common.IWxpBaseInfoServiceListener
import com.smjcco.wxpusher.push.PushPlatformState

class WxpBaseInfoServiceImpl : IWxpBaseInfoServiceListener {
    /** 获取 Android 客户端平台，不包含当前推送通道信息。 */
    override fun getClientPlatform(): String {
        return WxpPlatformEnum.Android.platform
    }

    /** 获取当前已经生效或首次注册阶段默认使用的后端推送路由平台。 */
    override fun getEffectivePushPlatform(): String {
        return PushPlatformState.getEffectivePushPlatform().getPlatform()
    }
}
