package com.smjcco.wxpusher

import com.smjcco.wxpusher.base.common.WxpSaveService

object WxpConfig {
    //后端地址
    var baseUrl: String = "https://wxpusher.zjiecode.com"
//    var baseUrl: String = "http://wxpusher.test.zjiecode.com"

    //ws的地址，iOS用不上
    var wsUrl: String = "wss://wxpusher.zjiecode.com"

    fun init() {
        baseUrl = WxpSaveService.get("baseUrl", baseUrl)
        baseUrl = "http://192.168.110.71:6100"
        wsUrl = WxpSaveService.get("wsUrl", wsUrl)
    }

    fun saveBaseUrl(baseUrl: String) {
        WxpSaveService.set("baseUrl", baseUrl)
    }

    fun saveWsUrl(wsUrl: String) {
        WxpSaveService.set("wsUrl", wsUrl)
    }
}