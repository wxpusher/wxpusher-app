package com.smjcco.wxpusher.bean

/**
 * 登陆信息
 */
data class LoginInfo(
    val deviceToken: String, //设备身份信息
    val deviceId: String, //设备id
    val uid: String //用户uid
)
