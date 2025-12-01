package com.smjcco.wxpusher.page.accountdetail

import kotlinx.serialization.Serializable


/**
 * 微信绑定
 */
@Serializable
data class WxpWeixinBindReq(val code: String?)

/**
 * 苹果绑定
 */
@Serializable
data class WxpAppleBindReq(val jwtCode: String?, val name: String?)