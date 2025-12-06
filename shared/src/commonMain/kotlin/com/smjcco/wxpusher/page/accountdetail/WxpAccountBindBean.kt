package com.smjcco.wxpusher.page.accountdetail

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys


/**
 * 微信绑定
 */
@Serializable
@JsonIgnoreUnknownKeys
data class WxpWeixinBindReq(val code: String?)

/**
 * 苹果绑定
 */
@Serializable
@JsonIgnoreUnknownKeys
data class WxpAppleBindReq(val jwtCode: String?, val name: String?)