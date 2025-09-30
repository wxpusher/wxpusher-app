package com.smjcco.wxpusher.app

import com.smjcco.wxpusher.base.common.IWxpBaseInfoServiceListener
import com.smjcco.wxpusher.utils.DeviceUtils

class WxpBaseInfoServiceImpl : IWxpBaseInfoServiceListener {
    override fun getPlatform(): String {
        return DeviceUtils.getPlatform().getPlatform()
    }
}