package com.smjcco.wxpusher

import com.smjcco.wxpusher.utils.AppDataUtils

object WxPusherConfig {
    var ApiUrl = "https://wxpusher.zjiecode.com"
    var WebUrl = "https://static.zjiecode.com/wxpusher/web-app"
    var WsUrl = "wss://wxpusher.zjiecode.com"
    fun init() {
        ApiUrl = AppDataUtils.getApiUrl()
        WebUrl = AppDataUtils.getWebUrl()
        WsUrl = AppDataUtils.getWsUrl()
    }

}
