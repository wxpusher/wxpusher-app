package com.smjcco.wxpusher

import com.smjcco.wxpusher.base.common.WxpSaveService

object WxpConfig {
    //和服务器BaseLoginResp 模型的版本号对齐，否则会发生登录信息补全
    const val UserLoginInfoVersion = 1

    //后端地址
    var baseUrl: String = "https://wxpusher.zjiecode.com"
//    var baseUrl: String = "http://wxpusher.test.zjiecode.com"

    //ws的地址，iOS用不上
    var wsUrl: String = "wss://wxpusher.zjiecode.com"

    fun init() {
        baseUrl = WxpSaveService.get("baseUrl", baseUrl)
        baseUrl = "http://127.0.0.1:6100"
        wsUrl = WxpSaveService.get("wsUrl", wsUrl)
    }

    fun saveBaseUrl(baseUrl: String) {
        WxpSaveService.set("baseUrl", baseUrl)
    }

    fun saveWsUrl(wsUrl: String) {
        WxpSaveService.set("wsUrl", wsUrl)
    }
}