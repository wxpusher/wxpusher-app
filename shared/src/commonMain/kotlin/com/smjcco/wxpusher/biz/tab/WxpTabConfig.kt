package com.smjcco.wxpusher.biz.tab

import com.smjcco.wxpusher.base.common.WxpSaveService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

/**
 * 底部 tab 显隐配置。「消息列表」「我的」为核心 tab 恒显示，此处仅管理两个可配置 tab。
 * 与 H5（app-fe common/TabConfig.ts）约定同一份 JSON 结构，缺省全部显示。
 */
@Serializable
@JsonIgnoreUnknownKeys
data class WxpTabConfig(
    // 消息市场
    val market: Boolean = true,
    // 扩展功能
    val extFunc: Boolean = true
)

object WxpTabConfigStore {
    // 与 H5 约定的存储 key（原生与 H5 共享同一 WxpSaveService，无前缀）
    const val TAB_CONFIG_KEY = "tab_config"

    fun read(): WxpTabConfig {
        val raw = WxpSaveService.get(TAB_CONFIG_KEY, "")
        if (raw.isEmpty()) {
            return WxpTabConfig()
        }
        return try {
            Json.decodeFromString(raw)
        } catch (e: Exception) {
            WxpTabConfig()
        }
    }
}
