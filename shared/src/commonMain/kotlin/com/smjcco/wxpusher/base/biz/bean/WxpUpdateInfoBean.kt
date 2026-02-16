package com.smjcco.wxpusher.base.biz.bean

import kotlinx.serialization.Serializable


@Serializable
data class WxpUpdateInfoReq(
    val deviceUuid: String? = null,
    val pushToken: String? = null,
    //是否更新平台，如果不传就不更新
    val platform: String? = null
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as WxpUpdateInfoReq

        if (deviceUuid != other.deviceUuid) return false
        if (pushToken != other.pushToken) return false
        if (platform != other.platform) return false

        return true
    }
}