package com.smjcco.wxpusher.api

import com.smjcco.wxpusher.base.WxpNetworkService
import io.ktor.client.request.post

object WxpApiService {
    suspend fun sendVerifyCode(phone: String) {
        WxpNetworkService.getWxpHttpClient().post()
    }
}