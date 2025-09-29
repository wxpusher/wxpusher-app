package com.smjcco.wxpusher.kmp.app

import com.smjcco.wxpusher.base.biz.IWxpAppPageService
import com.smjcco.wxpusher.kmp.common.utils.WxpJumpPageUtils

class WxpAppPageServiceImpl : IWxpAppPageService {

    override fun jumpToLogin() {
        WxpJumpPageUtils.jumpToLogin()
    }
}