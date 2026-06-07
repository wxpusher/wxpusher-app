package com.smjcco.wxpusher.biz.ad

import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.runAtIOSuspend
import com.smjcco.wxpusher.base.common.runAtMainSuspend

/**
 * 广告展示控制的 Swift/平台层入口。
 *
 * 展示广告前先请求后端开关接口，由服务端结合付费订阅状态与白名单决定是否放行。
 * 代码位id（slotId）由各平台写死后传入；本类只负责请求与回调。
 */
object WxpAdManager {
    private const val TAG = "WxpAd"

    /**
     * 查询指定广告位是否展示广告。结果通过 [callback] 回调（已切回主线程）。
     * 接口失败/异常时按不展示（false）处理。
     *
     * @param slotId   代码位id
     * @param callback 回调，true=展示广告，false=不展示
     */
    fun shouldShowAd(slotId: String, callback: (Boolean) -> Unit) {
        runAtIOSuspend {
            val show = WxpApiService.shouldShowAd(slotId)?.showAd ?: false
            WxpLogUtils.d(TAG, "shouldShowAd slotId=$slotId result=$show")
            runAtMainSuspend {
                callback(show)
            }
        }
    }
}
