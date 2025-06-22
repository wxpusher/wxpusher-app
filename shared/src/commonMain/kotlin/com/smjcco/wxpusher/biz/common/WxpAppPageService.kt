package com.smjcco.wxpusher.biz.common

expect fun WxpAppPageService_jumpToLogin()

object WxpAppPageService {
    /**
     * 身份过期等，跳转到到登录页面
     */
    fun jumpToLogin() {
        WxpAppPageService_jumpToLogin()
    }
}