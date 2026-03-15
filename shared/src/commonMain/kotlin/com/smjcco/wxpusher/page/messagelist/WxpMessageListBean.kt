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


@Serializable
@JsonIgnoreUnknownKeys
data class WxpCheckAppMsgReasonResp(
    //错误码
    val code: Int = 0,
    //列表是否可以接受消息
    val hasMsg: Boolean = true,
    //是否可以推送通知
    val hasPush: Boolean = true,
    //具体收不到消息的原因
    val reason: String? = null,
    //解释的url
    val url: String? = null,
)


@Serializable
@JsonIgnoreUnknownKeys
data class WxpListBannerResp(
    val id: Int? = null,
    //标题，必填，否则不会显示
    val title: String? = null,
    //如果存在desc，点击标题后，弹出弹出解释说明，否则点击后直接打开url，如果 也没有url，就不可点击
    val desc: String? = null,
    //如果存在url，点击后打开这个url
    val url: String? = null,
)
