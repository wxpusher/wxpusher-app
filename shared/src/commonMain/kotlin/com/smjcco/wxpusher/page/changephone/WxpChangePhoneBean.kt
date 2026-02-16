package com.smjcco.wxpusher.page.changephone

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys


/**
 * 换绑手机号
 */
@Serializable
@JsonIgnoreUnknownKeys
data class WxpPhoneBindReq(val phone: String?, val code: String?)