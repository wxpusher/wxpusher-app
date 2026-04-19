package com.smjcco.wxpusher.biz.version

import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.common.WxpDateTimeUtils
import com.smjcco.wxpusher.base.common.WxpDialogParams
import com.smjcco.wxpusher.base.common.WxpDialogUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.base.common.runAtIOSuspend

/**
 * 统一的 App 版本升级检测入口。
 *
 * - App 启动 / 从后台切前台时调用 [onAppForeground] 触发检测；
 * - 节流：默认 3 小时内只检测一次；只要接口返回成功（无论是否有更新）就记录本次检查时间；
 * - 发现新版本时统一弹窗（普通升级可取消 / 强制升级不可取消），点击「去升级」调 [WxpAppMarketNavigator.jumpToMarket]；
 * - 跳转路径由两端各自的 Navigator 实现分流（Android 分厂商市场或 TBS，iOS 打开 downloadUrl）。
 */
object WxpVersionCheckManager {
    private const val TAG = "VersionCheck"
    private const val KEY_LAST_CHECK_TS = "app_version_last_check_ts"
    private const val THROTTLE_MILLIS = 3L * 60 * 60 * 1000
    private var navigator: WxpAppMarketNavigator? = null

    fun setNavigator(navigator: WxpAppMarketNavigator) {
        this.navigator = navigator
    }

    /**
     * @param force true 时绕过节流（用于 Profile 页手动点"软件更新"）
     */
    fun onAppForeground(force: Boolean = false) {
        val now = WxpDateTimeUtils.getTimestamp()
        if (!force) {
            val last = WxpSaveService.get(KEY_LAST_CHECK_TS, 0.0).toLong()
            if (now - last in 0 until THROTTLE_MILLIS) {
                WxpLogUtils.d(TAG, "skip: throttled, last=$last now=$now")
                return
            }
        }

        runAtIOSuspend {
            val resp = WxpApiService.checkAppVersion() ?: return@runAtIOSuspend
            // 只要接口检查成功，就记录本次检查时间；后续所有分支共用这一次写入，
            // 也能避免 TBS 等外部升级页关闭后再触发 onResume 导致弹窗反复弹出。
            WxpSaveService.set(KEY_LAST_CHECK_TS, now.toDouble())
            if (!resp.hasUpdate) {
                if (force) {
                    WxpToastUtils.showToast("已经是最新版本")
                }
                return@runAtIOSuspend
            }
            val nav = navigator
            if (nav != null && nav.willShowInternalDialog(resp.downgradeToTbs)) {
                // 跳转通道自带升级弹窗（例如 Android TBS），跳过 shared 弹窗，避免用户点两次
                WxpLogUtils.i(TAG, "skip shared dialog, delegate to navigator internal dialog")
                nav.jumpToMarket(resp.downloadUrl, resp.downgradeToTbs)
            } else {
                showUpdateDialog(resp)
            }
        }
    }

    private fun showUpdateDialog(resp: AppVersionCheckResp) {
        val params = WxpDialogParams(
            title = resp.title.ifEmpty { "发现新版本" },
            message = resp.content,
            rightText = "去升级",
            rightBlock = {
                navigator?.jumpToMarket(resp.downloadUrl, resp.downgradeToTbs)
            }
        )
        if (resp.forceUpdate) {
            params.cancelable = false
        } else {
            params.leftText = "稍后"
        }
        WxpDialogUtils.showDialog(params)
    }
}
