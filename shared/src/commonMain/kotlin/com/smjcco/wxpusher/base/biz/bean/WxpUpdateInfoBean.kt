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
    //订阅通道的授权状态：GRANTED / DISABLED / DENIED / UNSUPPORTED，小米、vivo
    val subscribe: String? = null,
    //以下是授权诊断信息，只用来统计和排查，服务端不按它们路由
    //最后一次授权操作的结果，取值见 Android 端 VendorSubscribeReason
    val reason: String? = null,
    //系统授权接口最后一次返回的原始错误码
    val code: String? = null,
    //本地订阅通道：NONE 不存在、CLOSED 存在但关着、OPEN 开着
    val channel: String? = null,
    //应用通知权限是否打开
    val notify: Boolean? = null,
    //说明弹窗累计显示次数
    val guide: Int? = null,
    //系统授权弹窗累计拉起次数
    val request: Int? = null,
    //最后一次授权操作的时间，秒
    val at: Long? = null,
)
