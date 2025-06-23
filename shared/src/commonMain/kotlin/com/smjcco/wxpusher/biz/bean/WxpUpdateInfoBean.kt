package com.smjcco.wxpusher.biz.bean


data class WxpUpdateInfoReq(
    val deviceUuid: String? = null,
    val pushToken: String? = null,
    //是否更新平台，如果不传就不更新
    val platform: String? = null
)