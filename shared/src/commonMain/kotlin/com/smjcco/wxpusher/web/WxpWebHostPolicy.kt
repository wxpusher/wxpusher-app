package com.smjcco.wxpusher.web

import io.ktor.http.URLBuilder

object WxpWebHostPolicy {
    val DEFAULT_WHITELIST_HOSTS = setOf(
        "wxpusher.zjiecode.com",
        "wxpusher.test.zjiecode.com",
        "10.0.0.11",
        "10.0.2.2",
        "127.0.0.1"
    )

    fun isHostInWhitelist(host: String?): Boolean {
        return host != null && DEFAULT_WHITELIST_HOSTS.contains(host)
    }

    fun isUrlInWhitelist(url: String?): Boolean {
        if (url.isNullOrBlank()) {
            return false
        }
        val host = runCatching {
            URLBuilder(url).build().host
        }.getOrNull()
        return isHostInWhitelist(host)
    }
}
