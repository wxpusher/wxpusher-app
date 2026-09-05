package com.smjcco.wxpusher.page.notificationsound

import kotlinx.serialization.Serializable

/**
 * 查询当前设备铃声的响应
 */
@Serializable
data class WxpNotificationSoundResp(
    val sound: String? = null
)

/**
 * 修改当前设备铃声的请求
 */
@Serializable
data class WxpUpdateNotificationSoundReq(
    val sound: String
)

/**
 * 一个可选铃声。
 *
 * fileName 为 null 表示"跟随系统默认"，客户端没有对应的音频文件，也就无法试听。
 */
data class WxpNotificationSoundOption(
    //跨端统一的铃声标志，与服务端 NotificationSound 枚举一一对应
    val key: String,
    val name: String,
    val des: String,
    //iOS Bundle 内的音频文件名，不含后缀
    val fileName: String?
)

/**
 * 可选铃声清单。
 *
 * 这份清单必须和服务端 com.zjiecode.wxpusher.common.bean.NotificationSound 保持一致，
 * 而且音频文件是随客户端打包的 —— 所以新增铃声必须发版，不能只改服务端。
 *
 * 目前只有 iOS 用：iOS 的 aps.sound 只认 App Bundle 里的文件名，服务端下发文件名即可生效。
 * 安卓的铃声由通知渠道决定，服务端无法统一下发，方案另行讨论。
 */
object WxpNotificationSoundOptions {
    const val DEFAULT_KEY = "default"

    val all: List<WxpNotificationSoundOption> = listOf(
        WxpNotificationSoundOption(DEFAULT_KEY, "跟随系统默认", "使用系统的默认通知提示音", null),
        WxpNotificationSoundOption("ding", "叮咚", "经典门铃，两声下行", "wxp_ding"),
        WxpNotificationSoundOption("bell", "清脆", "金属钟声，明亮干净", "wxp_bell"),
        WxpNotificationSoundOption("chime", "提示", "三音上行，柔和不突兀", "wxp_chime"),
        WxpNotificationSoundOption("alarm", "警报", "双音交替，适合重要消息", "wxp_alarm"),
        WxpNotificationSoundOption("drop", "水滴", "短促轻快，打扰感最低", "wxp_drop"),
        WxpNotificationSoundOption("long3", "持续 3 秒", "三音提示循环 3 次，音量渐强", "wxp_long3"),
        WxpNotificationSoundOption("long5", "持续 5 秒", "三音提示循环 4 次，音量渐强", "wxp_long5"),
        WxpNotificationSoundOption("long10", "持续 10 秒", "循环 7 次，节奏渐密、音量渐强", "wxp_long10"),
        WxpNotificationSoundOption("long15", "持续 15 秒", "循环 9 次，节奏渐密、音量渐强", "wxp_long15"),
        WxpNotificationSoundOption("long20", "持续 20 秒", "循环 11 次，节奏渐密、音量渐强", "wxp_long20")
    )

    /**
     * 未知/空值一律回落到默认，避免服务端加了新铃声而老客户端显示空白
     */
    fun find(key: String?): WxpNotificationSoundOption {
        return all.firstOrNull { it.key == key } ?: all.first()
    }
}
