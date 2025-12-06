package com.smjcco.wxpusher.page.messagelist

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class WxpMessageListReq(
    val messageId: Long,
    val key: String?,
    val scene: Int?,
) {
    companion object {
        val SceneManual = 1 //手动下拉刷新
        val SceneAutoRefresh = 2 //打开的时候自动刷新
        val SceneFetchResume = 3 //后台会前台自动刷新
        val SceneSearch = 4 //搜索
        val SceneLoadMore = 5 //加载更多
    }
}


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
