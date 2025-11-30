package com.smjcco.wxpusher.base.biz.bean

import com.smjcco.wxpusher.page.login.WxpBaseLoginResp
import com.smjcco.wxpusher.page.login.WxpLoginSendVerifyCodeResp
import kotlinx.serialization.Serializable
import kotlin.String


/**
 * 登陆信息
 */
@Serializable
data class WxpLoginInfo(
    var deviceToken: String? = null, //设备身份信息
    val deviceId: String? = null, //设备id
    val uid: String? = null, //用户uid
    val spt: String? = null, //用户spt
    var openId: String? = null, //用户的openid，可能不存在
    var nickName: String? = null,
    var phone: String? = null,
    var wxBind: Boolean? = null,
    var appleBind: Boolean? = null
) {
    constructor(baseResp: WxpBaseLoginResp) : this(
        baseResp.deviceToken,
        baseResp.deviceId,
        baseResp.uid,
        baseResp.spt,
        baseResp.openId,
        baseResp.nickName,
        baseResp.phone,
        baseResp.wxBind,
        baseResp.appleBind
    )
}
