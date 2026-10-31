package com.smjcco.wxpusher.base.biz.bean

import kotlinx.serialization.Serializable


@Serializable
data class WxpUpdateInfoReq(
    val deviceUuid: String? = null,
    val pushToken: String? = null,
    //是否更新平台，如果不传就不更新
    val platform: String? = null,
    //厂商推送的设备状态，服务端只覆盖上报了的字段，不传时保留原值
    val vendorState: WxpVendorState? = null,
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as WxpUpdateInfoReq

        if (deviceUuid != other.deviceUuid) return false
        if (pushToken != other.pushToken) return false
        if (platform != other.platform) return false
        if (vendorState != other.vendorState) return false

        return true
    }
}

/**
 * 厂商推送的设备状态，服务端以 JSON 存在 user_device.vendor_state。
 * 不跟具体厂商绑定，以后其他厂商需要上报设备级状态时在这里加字段。
 */
@Serializable
data class WxpVendorState(
    //订阅通道的授权状态：GRANTED / DISABLED / DENIED / UNSUPPORTED，目前只有小米
    val subscribe: String? = null,
)
