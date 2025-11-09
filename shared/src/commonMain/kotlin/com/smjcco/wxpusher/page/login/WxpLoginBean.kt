package com.smjcco.wxpusher.page.login

import kotlinx.serialization.Serializable

@Serializable
data class WxpLoginSendVerifyCodeReq(
    val phone: String,
    val code: String,
    val deviceId: String?,
    val deviceName: String?,
    val pushToken: String?,
)

@Serializable
data class WxpLoginSendVerifyCodeResp(
    val phoneHasRegister: Boolean,
    //电话号码没有注册，可以和公众号绑定或者注册，通过这个code可以验证用户手机号，（手机号+验证码的base64编码）
    val phoneVerifyCode: String?,
    val deviceToken: String?,
    val deviceId: String?,
    val uid: String?,
    val openId: String?,
)

/**
 * 微信登录
 */
@Serializable
data class WxpWeixinLoginReq(
    val code: String,
    val bindCode: String?,//如果是绑定手机号的
    val deviceId: String?,
    val deviceName: String?,
    val pushToken: String?,
)

/**
 * 微信登录结果
 */
@Serializable
data class WxpWeixinLoginResp(
    val deviceToken: String?,
    val deviceId: String?,
    val uid: String?,
    val openId: String?,
)