package com.smjcco.wxpusher.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.biz.version.WxpAppMarketNavigator
import com.tencent.upgrade.core.UpgradeManager
import com.tencent.upgrade.core.UpgradeReqCallbackForUserManualCheck

/**
 * Android 端应用市场跳转实现。
 *
 * 分流策略：
 * - 已上架厂商（华为/荣耀/小米/OPPO/VIVO/魅族）→ 厂商应用商店；
 * - 其他未识别厂商 → TBS 升级；
 * - 服务端下发 downgradeToTbs=true → 本次跳转直接走 TBS（不再尝试厂商市场）；
 * - 厂商市场拉起失败（包未安装 / ActivityNotFoundException）→ 二级兜底 TBS。
 */
object AppMarketNavigator : WxpAppMarketNavigator {

    private const val TAG = "AppMarketNavigator"

    override fun willShowInternalDialog(downgradeToTbs: Boolean): Boolean {
        // 服务端下发降级 或 未识别厂商 → 走 TBS，TBS 自己会弹升级窗
        if (downgradeToTbs) return true
        return vendorMarketPkg(DeviceUtils.getPlatform()) == null
    }

    override fun jumpToMarket(downloadUrl: String, downgradeToTbs: Boolean) {
        val ctx: Context = ApplicationUtils.getCurrentActivity() ?: ApplicationUtils.getApplication()

        if (downgradeToTbs) {
            WxpLogUtils.i(TAG, "服务端下发降级，走TBS")
            openTbs()
            return
        }

        val platform = DeviceUtils.getPlatform()
        val marketPkg = vendorMarketPkg(platform)
        if (marketPkg != null) {
            if (tryOpenVendorMarket(ctx, marketPkg)) {
                return
            }
            WxpLogUtils.w(TAG, "厂商市场拉起失败，兜底TBS: platform=$platform pkg=$marketPkg")
        } else {
            WxpLogUtils.i(TAG, "未识别厂商，走TBS: platform=$platform")
        }
        openTbs()
    }

    private fun vendorMarketPkg(platform: DevicePlatform): String? = when (platform) {
        DevicePlatform.Android_XIAOMI -> "com.xiaomi.market"
        DevicePlatform.Android_HUAWEI -> "com.huawei.appmarket"
        DevicePlatform.Android_HONOR -> "com.hihonor.appmarket"
        DevicePlatform.Android_OPPO -> "com.heytap.market"
        DevicePlatform.Android_VIVO -> "com.bbk.appstore"
        DevicePlatform.Android_MEIZU -> "com.meizu.mstore"
        else -> null
    }

    private fun tryOpenVendorMarket(ctx: Context, marketPkg: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ctx.packageName}"))
                .setPackage(marketPkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            WxpLogUtils.w(TAG, "厂商市场未安装: $marketPkg", e)
            false
        } catch (e: Exception) {
            WxpLogUtils.w(TAG, "厂商市场拉起异常: $marketPkg", e)
            false
        }
    }

    private fun openTbs() {
        try {
            // checkUpgrade 第三个回调参数不能为 null
            UpgradeManager.getInstance()
                .checkUpgrade(true, null, object : UpgradeReqCallbackForUserManualCheck() {})
        } catch (e: Exception) {
            WxpLogUtils.w(TAG, "TBS 升级检测异常", e)
        }
    }
}
