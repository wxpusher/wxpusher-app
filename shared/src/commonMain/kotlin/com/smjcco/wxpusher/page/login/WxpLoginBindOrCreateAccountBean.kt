package com.smjcco.wxpusher.page.login

import kotlinx.serialization.Serializable

@Serializable
data class WxpPhoneBind(
    val phone: String,
    val code: String,
    val phoneVerifyCode: String?//用于发送给公众号的bindcode
)

@Serializable
data class WxpAppleBind(
    //苹果登录的jwt code
    val code: String?,
    //用户昵称
    val name: String?
)


@Serializable
data class WxpBindPageData(
    //苹果登录，绑定或者创建账号的数据
    val appleLogin: WxpAppleBind? = null,
    //手机号验证码登录，绑定或者创建账号的数据
    val phoneLogin: WxpPhoneBind? = null
)
