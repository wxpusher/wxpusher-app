package com.smjcco.wxpusher.ad

import android.os.Handler
import android.os.Looper
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.smjcco.wxpusher.BuildConfig
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.common.WxpSaveKey

/**
 * 穿山甲（CSJ/Pangle）广告 SDK 初始化封装。
 *
 * 懒加载：App 启动时不初始化，只有在真正需要展示广告时（后端开关放行后）才通过
 * [ensureStarted] 初始化，付费/白名单等不展示广告的用户全程不会初始化 SDK。
 * App ID 与 iOS 一致（[APP_ID]），代码位 ID 由各广告 View 自行写死。
 *
 * 合规要点：未同意隐私政策时 [ensureStarted] 直接回调失败，不会初始化。
 */
object WxpPangleAdManager {
    private const val TAG = "WxpAd"

    /** 穿山甲「应用 App ID」 */
    private const val APP_ID = "5838363"

    private enum class State { IDLE, STARTING, READY }

    private val mainHandler = Handler(Looper.getMainLooper())

    // 以下状态只在主线程读写
    private var state = State.IDLE
    private val pendingCallbacks = mutableListOf<(Boolean) -> Unit>()

    /**
     * 确保 SDK 已初始化完成，结果通过 [callback] 在主线程回调（true=可加载广告）。
     * 需在主线程调用；初始化中重复调用会排队等待同一次结果，失败后下次调用会重新初始化。
     */
    fun ensureStarted(callback: (Boolean) -> Unit) {
        if (!WxpSaveService.get(WxpSaveKey.UserHasAgreement, false)) {
            WxpLogUtils.w(TAG, "未同意隐私政策，不初始化穿山甲SDK")
            callback(false)
            return
        }
        when (state) {
            State.READY -> callback(true)
            State.STARTING -> pendingCallbacks.add(callback)
            State.IDLE -> {
                pendingCallbacks.add(callback)
                start()
            }
        }
    }

    private fun start() {
        state = State.STARTING
        WxpLogUtils.d(TAG, "开始初始化穿山甲SDK")
        val config = TTAdConfig.Builder()
            .appId(APP_ID)
            .appName("WxPusher")
            // 线下（offline）环境开启 SDK 调试日志，线上关闭
            .debug(!BuildConfig.online)
            .allowShowNotify(true)
            .supportMultiProcess(false)
            .build()

        try {
            TTAdSdk.init(ApplicationUtils.getApplication(), config)
            TTAdSdk.start(object : TTAdSdk.Callback {
                override fun success() {
                    WxpLogUtils.d(TAG, "穿山甲SDK初始化成功")
                    mainHandler.post { finishStart(true) }
                }

                override fun fail(code: Int, msg: String?) {
                    WxpLogUtils.d(TAG, "穿山甲SDK初始化失败 code=$code msg=$msg")
                    mainHandler.post { finishStart(false) }
                }
            })
        } catch (t: Throwable) {
            WxpLogUtils.w(TAG, "穿山甲SDK初始化异常", t)
            finishStart(false)
        }
    }

    private fun finishStart(success: Boolean) {
        state = if (success) State.READY else State.IDLE
        val callbacks = pendingCallbacks.toList()
        pendingCallbacks.clear()
        callbacks.forEach { it(success) }
    }

    /** SDK 是否已初始化完成（加载广告前判断）。 */
    fun isReady(): Boolean = state == State.READY

    /**
     * 按当前界面深/浅色设置广告主题，需在加载广告前调用，影响后续渲染的广告创意。
     * 对照 iOS applyAdTheme。
     */
    fun applyAdTheme(isDark: Boolean) {
        if (!isReady()) return
        runCatching {
            TTAdSdk.getAdManager().setThemeStatus(if (isDark) 1 else 0)
        }.onFailure {
            WxpLogUtils.w(TAG, "设置广告主题失败", it)
        }
    }
}
