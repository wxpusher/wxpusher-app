package com.smjcco.wxpusher.biz.ad

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

/**
 * 是否展示广告的响应。用对象包装，便于后续扩展（如广告位类型、刷新间隔等字段）。
 */
@Serializable
@JsonIgnoreUnknownKeys
data class WxpShowAdResp(
    // 是否展示广告
    val showAd: Boolean = false,
)
