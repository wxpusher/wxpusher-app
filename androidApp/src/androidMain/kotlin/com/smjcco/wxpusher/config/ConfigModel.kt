package com.smjcco.wxpusher.config

import androidx.annotation.Keep


/**
 * 配置数据模型
 */
@Keep
data class ConfigItem(
    val version: String = "0.0.0", // 最低支持的应用版本号
    val xiaomiPush: Boolean = true,
    val vivoPush: Boolean = true,
    val huaweiPush: Boolean = true,
    val oppoPush: Boolean = false,
    val honorPush: Boolean = false,
)

/**
 * 配置列表响应
 */
@Keep
data class ConfigResponse(
    val configs: List<ConfigItem>
) 