package com.smjcco.wxpusher.app

import com.smjcco.wxpusher.base.biz.IWxpAppPageService
import com.smjcco.wxpusher.utils.WxpJumpPageUtils

class WxpAppPageServiceImpl : IWxpAppPageService {

    override fun jumpToLogin() {
        WxpJumpPageUtils.jumpToLogin()
    }
}