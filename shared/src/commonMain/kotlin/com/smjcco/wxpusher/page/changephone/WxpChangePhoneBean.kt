package com.smjcco.wxpusher.page.changephone

import kotlinx.serialization.Serializable


/**
 * 换绑手机号
 */
@Serializable
data class WxpPhoneBindReq(val phone: String?, val code: String?)