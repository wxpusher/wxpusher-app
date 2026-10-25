package com.smjcco.wxpusher.base.biz.bean

import kotlinx.serialization.Serializable


@Serializable
data class WxpUpdateInfoReq(
    val deviceUuid: String? = null,
    val pushToken: String? = null,
    //是否更新平台，如果不传就不更新
    val platform: String? = null,
    //厂商订阅通道授权状态（目前只有小米），不传时服务端保留原值
    val vendorSubState: String? = null,
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as WxpUpdateInfoReq

        if (deviceUuid != other.deviceUuid) return false
        if (pushToken != other.pushToken) return false
        if (platform != other.platform) return false
        if (vendorSubState != other.vendorSubState) return false

        return true
    }
}