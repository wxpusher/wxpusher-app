package com.smjcco.wxpusher.base.biz


interface IWxpAppPageService {
    fun jumpToLogin()
}

object WxpAppPageService {
    private lateinit var pageService: IWxpAppPageService

    fun init(service: IWxpAppPageService) {
        pageService = service
    }

    /**
     * 身份过期等，跳转到到登录页面
     */
    fun jumpToLogin() {
        pageService.jumpToLogin()
    }
}