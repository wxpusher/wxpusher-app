package com.smjcco.wxpusher.biz.bean

import kotlinx.serialization.Serializable


/**
 * 登陆信息
 */
@Serializable
data class WxpLoginInfo(
    var deviceToken: String? = null , //设备身份信息
    val deviceId: String? = null , //设备id
    val uid: String? = null , //用户uid
    var openId: String? = null //用户的openid，可能不存在
)
