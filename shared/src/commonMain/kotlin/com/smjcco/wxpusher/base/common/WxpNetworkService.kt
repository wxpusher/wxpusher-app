package com.smjcco.wxpusher.base.common

import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object WxpNetworkService {
    private val client = HttpClient {
        defaultRequest {
            contentType(ContentType.Application.Json)
            header("deviceToken", WxpAppDataService.getLoginInfo()?.deviceToken ?: "")
            header("version", WxpBaseInfoService.getAppVersionName())
            header("platform", WxpBaseInfoService.getPlatform())
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                explicitNulls = false
                coerceInputValues = true
            })
        }

        install(Logging) {
            level = LogLevel.ALL
            logger = Logger.DEFAULT
        }
    }

    /**
     * 获取一个私有域的可以发送http网络请求client，并且附带token之类的
     */
    fun getWxpHttpClient(): HttpClient = client

    fun getUrl(path: String): String {
        return URLBuilder(WxpConfig.baseUrl).apply {
            encodedPath = path
        }.buildString()
    }


}

/**
 * 网络返回的基本数据结构
 */
@Serializable
data class BaseResp<T>(val code: Int, val msg: String, val data: T)

