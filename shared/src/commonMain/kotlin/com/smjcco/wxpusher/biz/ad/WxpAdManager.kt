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
     * 查询广告配置（是否展示 + 信息流插入位置）。结果对象通过 [callback] 回调（已切回主线程）。
     * 接口失败/异常时回调 null（按不展示处理）。Banner 只需读 [WxpShowAdResp.showAd]。
     *
     * @param slotId   代码位id
     * @param callback 回调，返回完整响应对象（含 showAd / position）
     */
    fun fetchAdConfig(slotId: String, callback: (WxpShowAdResp?) -> Unit) {
        runAtIOSuspend {
            val resp = WxpApiService.fetchAdConfig(slotId)
            WxpLogUtils.d(TAG, "fetchAdConfig slotId=$slotId resp=$resp")
            runAtMainSuspend {
                callback(resp)
            }
        }
    }
}
