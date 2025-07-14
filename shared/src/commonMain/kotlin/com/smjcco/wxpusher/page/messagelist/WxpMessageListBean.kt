package com.smjcco.wxpusher.page.messagelist

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
data class WxpMessageListReq(
    val messageId: Long,
    val key: String?,
)


@Serializable
@JsonIgnoreUnknownKeys
data class WxpMessageListMessage(
    val messageId: Long,
    val url: String,
    val sourceUrl: String?,
    val summary: String,
    val name: String?,
    var read: Boolean,
    val createTime: Long,
)
