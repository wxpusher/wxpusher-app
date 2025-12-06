package com.smjcco.wxpusher.page.login

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class WxpLoginSendVerifyCodeReq(
    //不绑定现有账号，直接创建账号登录
    val justCreateAccount: Boolean,
    val phone: String,
    val code: String,
    val deviceId: String?,
    val deviceName: String?,
    val pushToken: String?,
)

/**
 * 无论那种登录方式，都一定会返回的信息
 */
@Serializable
@JsonIgnoreUnknownKeys
open class WxpBaseLoginResp {
    constructor()
    constructor(
        deviceToken: String?,
        deviceId: String?,
        uid: String?,
        spt: String?,
        openId: String?,
        nickName: String?,
        phone: String?,
        wxBind: Boolean?,
        appleBind: Boolean?
    ) {
        this.deviceToken = deviceToken
        this.deviceId = deviceId
        this.uid = uid
        this.spt = spt
        this.openId = openId
        this.nickName = nickName
        this.phone = phone
        this.wxBind = wxBind
        this.appleBind = appleBind
    }

    var deviceToken: String? = null
    var deviceId: String? = null
    var uid: String? = null
    var spt: String? = null
    var openId: String? = null

    //用户昵称
    var nickName: String? = null

    //绑定的电话号码
    var phone: String? = null

    //是否绑定了微信
    var wxBind: Boolean? = null

    //是否绑定了苹果
    var appleBind: Boolean? = null
}

@Serializable
@JsonIgnoreUnknownKeys
class WxpLoginSendVerifyCodeResp : WxpBaseLoginResp {
    constructor() : super()

    constructor(
        deviceToken: String? = null,
        deviceId: String? = null,
        uid: String? = null,
        spt: String? = null,
        openId: String? = null,
        nickName: String? = null,
        phone: String? = null,
        wxBind: Boolean? = null,
        appleBind: Boolean? = null,
        phoneHasRegister: Boolean? = null,
        phoneVerifyCode: String? = null
    ) : super(deviceToken, deviceId, uid, spt, openId, nickName, phone, wxBind, appleBind) {
        this.phoneHasRegister = phoneHasRegister
        this.phoneVerifyCode = phoneVerifyCode
    }

    var phoneHasRegister: Boolean? = null

    //电话号码没有注册，可以和公众号绑定或者注册，通过这个code可以验证用户手机号，（手机号+验证码的base64编码）
    var phoneVerifyCode: String? = null
}

/**
 * 微信登录
 */
@Serializable
@JsonIgnoreUnknownKeys
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
@JsonIgnoreUnknownKeys
class WxpWeixinLoginResp : WxpBaseLoginResp {
    constructor() : super()

    constructor(
        deviceToken: String? = null,
        deviceId: String? = null,
        uid: String? = null,
        spt: String? = null,
        openId: String? = null,
        nickName: String? = null,
        phone: String? = null,
        wxBind: Boolean? = null,
        appleBind: Boolean? = null,
    ) : super(deviceToken, deviceId, uid, spt, openId, nickName, phone, wxBind, appleBind)

}

/**
 * 苹果登录
 */
@Serializable
@JsonIgnoreUnknownKeys
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
@JsonIgnoreUnknownKeys
class WxpAppleLoginResp : WxpBaseLoginResp {
    constructor() : super()

    constructor(
        deviceToken: String? = null,
        deviceId: String? = null,
        uid: String? = null,
        spt: String? = null,
        openId: String? = null,
        nickName: String? = null,
        phone: String? = null,
        wxBind: Boolean? = null,
        appleBind: Boolean? = null,
        hasRegister: Boolean? = null,
    ) : super(deviceToken, deviceId, uid, spt, openId, nickName, phone, wxBind, appleBind) {
        this.hasRegister = hasRegister
    }

    var hasRegister: Boolean? = null
}