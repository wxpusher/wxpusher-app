package com.smjcco.wxpusher.biz.version

import kotlinx.serialization.Serializable

/**
 * 服务端 /api/device/version-update 的响应结构，字段与后端 AppVersionCheckResp 严格对齐。
 */
@Serializable
data class AppVersionCheckResp(
    val hasUpdate: Boolean = false,
    val forceUpdate: Boolean = false,
    val title: String = "",
    val content: String = "",
    val latestVersion: String = "",
    val downloadUrl: String = "",
    val downgradeToTbs: Boolean = false,
)
