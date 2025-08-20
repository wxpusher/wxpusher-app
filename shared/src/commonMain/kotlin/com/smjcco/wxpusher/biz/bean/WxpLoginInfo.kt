package com.smjcco.wxpusher.biz.bean

import kotlinx.serialization.Serializable


/**
 * 登陆信息
 */
@Serializable
data class WxpLoginInfo(
    var deviceToken: String?, //设备身份信息
    val deviceId: String?, //设备id
    val uid: String?, //用户uid
    val openId: String? //用户的openid
)
