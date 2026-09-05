package com.smjcco.wxpusher.push.ws.alert

import androidx.annotation.RawRes
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.page.notificationsound.WxpNotificationSoundOption
import com.smjcco.wxpusher.page.notificationsound.WxpNotificationSoundOptions

/**
 * WS 通道收到消息时的提醒方式设置。
 *
 * 只对当前设备生效，不同步服务端：WS 的通知是本机构建的，震动和闪光灯本来也无法由
 * 服务端下发，存本地还能离线读取、不受登录态影响。
 *
 * 震动、闪光灯、响铃共用同一个 [getDurationSeconds]，避免三个不一致的时长互相打架。
 */
object WsAlertStore {
    private const val KEY_DURATION = "WsAlert_DurationSeconds"
    private const val KEY_VIBRATE = "WsAlert_VibrateEnabled"
    private const val KEY_TORCH = "WsAlert_TorchEnabled"
    private const val KEY_SOUND = "WsAlert_SoundEnabled"
    private const val KEY_SOUND_KEY = "WsAlert_SoundKey"
    private const val KEY_FORCE_LOUD = "WsAlert_ForceLoud"

    /** 提醒时长上限，再长就只是折磨用户和耗电了。 */
    const val DURATION_MAX = 60

    /** 滑块步长。默认值是 1 秒，所以必须是 1 秒粒度。 */
    const val DURATION_STEP = 1

    /**
     * 默认响一次就停。
     *
     * 这个默认值配合默认开启的震动和「跟随系统默认」提示音，观感与旧版本（通知渠道
     * 自带铃声 + 震动）基本一致，老用户升级后不会觉得提醒变了。
     */
    private const val DURATION_DEFAULT = 1

    /**
     * 时长为 0 表示不做任何额外提醒，只留一条静默的通知栏消息。
     */
    fun getDurationSeconds(): Int =
        WxpSaveService.get(KEY_DURATION, DURATION_DEFAULT).coerceIn(0, DURATION_MAX)

    fun setDurationSeconds(seconds: Int) {
        WxpSaveService.set(KEY_DURATION, seconds.coerceIn(0, DURATION_MAX))
    }

    fun isVibrateEnabled(): Boolean = WxpSaveService.get(KEY_VIBRATE, true)

    fun setVibrateEnabled(enabled: Boolean) {
        WxpSaveService.set(KEY_VIBRATE, enabled)
    }

    fun isTorchEnabled(): Boolean = WxpSaveService.get(KEY_TORCH, false)

    fun setTorchEnabled(enabled: Boolean) {
        WxpSaveService.set(KEY_TORCH, enabled)
    }

    fun isSoundEnabled(): Boolean = WxpSaveService.get(KEY_SOUND, true)

    fun setSoundEnabled(enabled: Boolean) {
        WxpSaveService.set(KEY_SOUND, enabled)
    }

    /** 当前提示音的 key，未知值由 [WsAlertTones.find] 回落到「跟随系统默认」。 */
    fun getSoundKey(): String =
        WxpSaveService.get(KEY_SOUND_KEY, WxpNotificationSoundOptions.DEFAULT_KEY)

    fun setSoundKey(key: String) {
        WxpSaveService.set(KEY_SOUND_KEY, key)
    }

    /**
     * 打开后按闹钟音频属性播放，手机静音或勿扰时也会响。默认关闭，跟随系统行为。
     */
    fun isForceLoud(): Boolean = WxpSaveService.get(KEY_FORCE_LOUD, false)

    fun setForceLoud(force: Boolean) {
        WxpSaveService.set(KEY_FORCE_LOUD, force)
    }

    /** 是否需要 App 自己执行提醒。三个开关全关时等同于时长为 0。 */
    fun hasAnyAlert(): Boolean =
        getDurationSeconds() > 0
            && (isVibrateEnabled() || isTorchEnabled() || isSoundEnabled())

    /** 推送通道设置页入口行展示的一句话摘要。 */
    fun summary(): String {
        if (!hasAnyAlert()) {
            return "仅通知栏提示"
        }
        val parts = mutableListOf("${getDurationSeconds()} 秒")
        if (isVibrateEnabled()) {
            parts.add("震动")
        }
        if (isSoundEnabled()) {
            parts.add(WsAlertTones.find(getSoundKey()).name)
        }
        if (isTorchEnabled()) {
            parts.add("闪光灯")
        }
        return parts.joinToString(" · ")
    }
}

/**
 * Android 可用的提示音清单。
 *
 * 名称和描述直接复用 shared 里的 [WxpNotificationSoundOptions]，与 iOS 同一份文案。
 * iOS 的 long3~long20 不在其中：Android 的提醒时长由 [WsAlertStore] 统一控制，靠循环
 * 重播短音实现，不需要预渲染的长音频。
 */
object WsAlertTones {
    /** 与 shared 清单里的 key 对应，顺序即页面展示顺序。 */
    private val RAW_BY_KEY: Map<String, Int?> = linkedMapOf(
        WxpNotificationSoundOptions.DEFAULT_KEY to null,
        "ding" to R.raw.wxp_ding,
        "bell" to R.raw.wxp_bell,
        "chime" to R.raw.wxp_chime,
        "alarm" to R.raw.wxp_alarm,
        "drop" to R.raw.wxp_drop,
    )

    val all: List<WxpNotificationSoundOption> =
        RAW_BY_KEY.keys.map { WxpNotificationSoundOptions.find(it) }

    /** 未知 key 回落到「跟随系统默认」，与 shared 的行为一致。 */
    fun find(key: String?): WxpNotificationSoundOption {
        if (key == null || !RAW_BY_KEY.containsKey(key)) {
            return all.first()
        }
        return WxpNotificationSoundOptions.find(key)
    }

    /** 返回 res/raw 资源 id；为空表示用系统默认通知音。 */
    @RawRes
    fun rawResOf(key: String?): Int? = RAW_BY_KEY[key]
}
