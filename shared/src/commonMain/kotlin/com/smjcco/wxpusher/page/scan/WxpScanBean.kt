package com.smjcco.wxpusher.page.scan

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class WxpScanQrcodeResp(
    val type: Int,
    val data: String?,
    val followResult: WxpFollowResult?
) {
    companion object {
        //显示原始内容
        const val TypeShowRaw = 0

        //扫码定位
        const val TypeSubscribe = 1
    }
}

@Serializable
@JsonIgnoreUnknownKeys
data class WxpFollowResult(
    val code: Int?,
    val extraQrcode: Boolean?,//是否是通过参数二维码订阅
    val type: Int?,
    val String: String?,
    val subId: Int?,
    val msg: String?,
) {

    companion object {
        const val FollowCodeResultSuccess: Int = 1 //正常
        const val FollowCodeResultReject: Int = 2 //被拉黑了
        const val FollowCodeResultOverMaxUserCount: Int = 3 //超过了最大数量限制
        const val TypeApp: Int = 1
        const val TypeTopic: Int = 2
    }
}
