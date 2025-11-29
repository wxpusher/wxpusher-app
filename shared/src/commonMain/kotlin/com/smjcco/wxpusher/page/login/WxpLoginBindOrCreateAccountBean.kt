package com.smjcco.wxpusher.page.login

import kotlinx.serialization.Serializable


/**
 * 手机号登录，返回的数据，转到选择注册方式页面使用
 */
@Serializable
data class WxpPhoneBind(
    val phone: String,
    val code: String,
    val phoneVerifyCode: String?//用于发送给公众号的bindcode
)

/**
 * 苹果登录，返回的数据，转到选择注册方式页面使用
 */
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
