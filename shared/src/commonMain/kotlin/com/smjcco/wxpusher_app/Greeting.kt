package com.smjcco.wxpusher_app

import com.smjcco.wxpusher.base.BaseResp
import com.smjcco.wxpusher.base.TestTesp
import com.smjcco.wxpusher.base.WxpNetworkService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class Greeting {
    private val client = HttpClient()

    suspend fun greeting(): BaseResp<TestTesp?> {
        return WxpNetworkService.getWxpHttpClient()
            .get("https://wxpusher.zjiecode.com/api/need-login/device/message-list-v2").body()
    }

    private val platform: Platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}