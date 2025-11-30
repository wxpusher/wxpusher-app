package com.smjcco.wxpusher.page.login

import kotlinx.serialization.Serializable

@Serializable
data class WxpLoginSendVerifyCodeReq(
    //不绑定现有账号，直接创建账号登录
    val justCreateAccount: Boolean,
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
    //微信登录，绑定手机号
    val bindCode: String?,
    //绑定苹果账号，苹果的jwt
    val appleLoginJwtCode: String?,
    //用户名，首次授权才有
    val appleName: String?,

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

/**
 * 苹果登录
 */
@Serializable
data class WxpAppleLoginReq(
    //不绑定现有账号，直接创建账号登录
    val justCreateAccount: Boolean,
    //苹果登录的jwt code
    val code: String?,
    //苹果用户昵称
    val name: String?,

    val deviceId: String?,
    val deviceName: String?,
    val pushToken: String?,
)

/**
 * 苹果登录结果
 */
@Serializable
data class WxpAppleLoginResp(
    val deviceToken: String?,
    val deviceId: String?,
    val uid: String?,
    val openId: String?,
    val hasRegister: Boolean?
)