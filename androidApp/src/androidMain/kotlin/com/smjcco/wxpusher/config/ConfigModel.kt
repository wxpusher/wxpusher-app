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
    val huaweiPushJustHcm: Boolean = false,//只通过hcm进行判断，如果线上有华为设备判断失误，可以通过这个降级
    val oppoPush: Boolean = true,
    val honorPush: Boolean = true,
    val meizuPush: Boolean = true,
    //是否引导小米用户开启资讯订阅通道「订阅的通知提醒」，出问题时可以远程关掉
    val xiaomiSubscribe: Boolean = true,
    //小米个人订阅通道 135072 的关停时间
    val xiaomiLegacyCutoff: String = "2026-12-31T00:00:00+08:00",
    //是否引导 vivo 用户开启订阅渠道「订阅内容更新」，出问题时可以远程关掉
    val vivoSubscribe: Boolean = true,
    //vivo 不走模板的订阅提醒消息被拦截的时间
    val vivoLegacyCutoff: String = "2027-04-01T00:00:00+08:00",
)

/**
 * 配置列表响应
 */
@Keep
data class ConfigResponse(
    val configs: List<ConfigItem>
) 