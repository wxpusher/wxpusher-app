package com.smjcco.wxpusher.base

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class WxpNetworkService {
    companion object {
        private val client = HttpClient() {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                })
            }
        }

        /**
         * 获取一个私有域的可以发送http网络请求client，并且附带token之类的
         */
        fun getWxpHttpClient(): HttpClient = client
    }


}

/**
 * 网络返回的基本数据结构
 */
@Serializable
data class BaseResp<T>(val code: Int, val msg: String, val data: T)

