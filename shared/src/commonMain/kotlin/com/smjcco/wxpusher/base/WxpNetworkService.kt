package com.smjcco.wxpusher.base

import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.biz.common.WxpAppDataService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.URLBuilder
import io.ktor.http.buildUrl
import io.ktor.http.encodedPath
import io.ktor.http.headers
import io.ktor.http.parseUrl
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object WxpNetworkService {
    private val client = HttpClient() {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            })
            headers {
                append("deviceToken", WxpAppDataService.getLoginInfo()?.deviceToken ?: "")
                append("versionName", WxpBaseInfoService.getAppVersionName())
            }
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

