package com.smjcco.wxpusher.api


data class SubscribeListItem(
    val type: Int,
    val id: Long,
    val name: String
) {
    fun getChannelId(): String {
        return if (type == 1) "T${id}" else "A${id}"
    }
}