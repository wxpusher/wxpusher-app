package com.smjcco.wxpusher.web

import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.common.WxpDateTimeUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpNetworkService
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.base.common.WxpScopeUtils
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object AppFeVersionManager {
    private const val VERSION_FILE_NAME = "/app/version.txt"
    private const val VERSION_KEY_PREFIX = "app_fe_version_"
    private const val LAST_REQ_KEY_PREFIX = "app_fe_version_last_req_"
    private const val PENDING_REFRESH_KEY_PREFIX = "app_fe_pending_refresh_"
    private const val REQUEST_TIMEOUT_MS = 1800L
    private const val MIN_REQUEST_INTERVAL_MS =  60L * 60L * 1000L

    /**
     * 启动时调用一次即可，内部异步执行且包含 1 小时节流，失败会自动吞掉。
     */
    fun refreshOnAppLaunch() {
        WxpScopeUtils.getIoScopeScope().launch {
            val origin = normalizeOrigin(WxpConfig.appFeUrl) ?: return@launch
            val now = WxpDateTimeUtils.getTimestamp()
            val lastReqAt = getLastRequestAt(origin)
            if (now - lastReqAt < MIN_REQUEST_INTERVAL_MS) {
                return@launch
            }
            saveLastRequestAt(origin, now)

            val remoteVersion = fetchRemoteVersion(origin) ?: return@launch
            val localVersion = getLocalVersion(origin)
            if (localVersion.isNullOrBlank() || localVersion != remoteVersion) {
                saveLocalVersion(origin, remoteVersion)
                markPendingRefresh(origin, remoteVersion)
            }
        }
    }

    fun appendVersionParam(url: String, version: String?): String {
        if (version.isNullOrBlank()) {
            return url
        }
        return runCatching {
            URLBuilder(url).apply {
                parameters.remove("_appfev")
                parameters.append("_appfev", version)
            }.buildString()
        }.getOrDefault(url)
    }

    /**
     * 同步接口：WebView 打开时读取并消费一次待刷新版本。
     * 返回 null 表示不需要清理缓存；返回版本号表示需要清理并可用于 URL 打标。
     */
    fun consumePendingRefreshVersion(url: String): String? {
        val origin = normalizeOrigin(url) ?: return null
        val key = buildPendingRefreshKey(origin)
        val pendingVersion = WxpSaveService.get(key, "").ifBlank { null } ?: return null
        WxpSaveService.set(key, "")
        return pendingVersion
    }

    private suspend fun fetchRemoteVersion(origin: String): String? {
        return runCatching {
            withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                val versionUrl = URLBuilder(origin).apply {
                    encodedPath = "$VERSION_FILE_NAME"
                    parameters.append("t", WxpDateTimeUtils.getTimestamp().toString())
                }.buildString()

                WxpNetworkService.getWxpHttpClient()
                    .get(versionUrl) {
                        header("Cache-Control", "no-cache, no-store, max-age=0")
                        header("Pragma", "no-cache")
                    }
                    .bodyAsText()
                    .trim()
                    .ifBlank { null }
            }
        }.onFailure {
            WxpLogUtils.w(message = "请求version.txt失败,url=$origin", throwable = it)
        }.getOrNull()
    }

    private fun normalizeOrigin(url: String): String? {
        return runCatching {
            val parsed = URLBuilder(url).build()
            val protocol = parsed.protocol.name
            val host = parsed.host
            val hasCustomPort = parsed.port != parsed.protocol.defaultPort
            val portPart = if (hasCustomPort) ":${parsed.port}" else ""
            "$protocol://$host$portPart"
        }.getOrNull()
    }

    private fun buildVersionKey(origin: String): String = VERSION_KEY_PREFIX + origin
    private fun buildLastReqKey(origin: String): String = LAST_REQ_KEY_PREFIX + origin
    private fun buildPendingRefreshKey(origin: String): String = PENDING_REFRESH_KEY_PREFIX + origin

    private fun getLocalVersion(origin: String): String? {
        val version = WxpSaveService.get(buildVersionKey(origin), "")
        return version.ifBlank { null }
    }

    private fun saveLocalVersion(origin: String, version: String) {
        WxpSaveService.set(buildVersionKey(origin), version)
    }

    private fun getLastRequestAt(origin: String): Long {
        val raw = WxpSaveService.get(buildLastReqKey(origin), "")
        return raw.toLongOrNull() ?: 0L
    }

    private fun saveLastRequestAt(origin: String, timestamp: Long) {
        WxpSaveService.set(buildLastReqKey(origin), timestamp.toString())
    }

    private fun markPendingRefresh(origin: String, version: String) {
        WxpSaveService.set(buildPendingRefreshKey(origin), version)
    }

}
