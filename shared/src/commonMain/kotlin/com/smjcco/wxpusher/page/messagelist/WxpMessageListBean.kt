package com.smjcco.wxpusher.page.messagelist

import kotlinx.serialization.Serializable

@Serializable
data class WxpMessageListReq(
    val lastUserReceiveRecordId: Long,
    val key: String?,
)


@Serializable
data class WxpMessageListMessage(
    val id: Long,
    val url: String,
    val sourceUrl: String?,
    val summary: String,
    val name: String?,
    var read: Boolean,
    val createTime: Long,
)
