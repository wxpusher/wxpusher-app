package com.smjcco.wxpusher.ad

import android.content.Context
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.smjcco.wxpusher.BuildConfig
import com.smjcco.wxpusher.base.common.WxpLogUtils

/**
 * 穿山甲（CSJ/Pangle）广告 SDK 初始化封装。
 *
 * 对照 iOS [com.smjcco.wxpusher] 端的 WxpPangleAdManager：单例 + doInit 幂等模式。
 * App ID 与 iOS 一致（[APP_ID]），代码位 ID 由各广告 View 自行写死。
 *
 * 合规要点：必须在用户同意隐私政策之后再调用 [doInit] 初始化 SDK / 加载广告，
 * 当前与微信、推送等 SDK 一样在 Application.onCreate 中初始化（启动前已有同意流程）。
 */
object WxpPangleAdManager {
    private const val TAG = "WxpAd"

    /** 穿山甲「应用 App ID」 */
    private const val APP_ID = "5838363"

    @Volatile
    private var started = false

    /** 初始化穿山甲 SDK。需在用户同意隐私政策后调用，重复调用无副作用。 */
    fun doInit(context: Context) {
        if (started) return
        started = true

        val config = TTAdConfig.Builder()
            .appId(APP_ID)
            .appName("WxPusher")
            // 线下（offline）环境开启 SDK 调试日志，线上关闭
            .debug(!BuildConfig.online)
            .allowShowNotify(true)
            .supportMultiProcess(false)
            .build()

        TTAdSdk.init(context.applicationContext, config)
        TTAdSdk.start(object : TTAdSdk.Callback {
            override fun success() {
                WxpLogUtils.d(TAG, "穿山甲SDK初始化成功")
            }

            override fun fail(code: Int, msg: String?) {
                WxpLogUtils.d(TAG, "穿山甲SDK初始化失败 code=$code msg=$msg")
            }
        })
    }

    /** SDK 是否已初始化（加载广告前判断），对照 iOS isSupported 的就绪判断。 */
    fun isReady(): Boolean = started

    /**
     * 按当前界面深/浅色设置广告主题，需在加载广告前调用，影响后续渲染的广告创意。
     * 对照 iOS applyAdTheme。
     */
    fun applyAdTheme(isDark: Boolean) {
        if (!started) return
        runCatching {
            TTAdSdk.getAdManager().setThemeStatus(if (isDark) 1 else 0)
        }.onFailure {
            WxpLogUtils.w(TAG, "设置广告主题失败", it)
        }
    }
}
