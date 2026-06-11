package com.smjcco.wxpusher.ad

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdDislike
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTNativeExpressAd
import com.smjcco.wxpusher.base.common.WxpLogUtils

/**
 * 穿山甲 Banner（模版渲染 / Express）广告封装。
 *
 * 对照 iOS 的 WxpBannerAdView：代码位 ID 写死在本类中（消息详情页底部 Banner），
 * 通过 [Callback] 回调渲染结果，由宿主（WebView 页）决定容器展开/收起。
 */
class WxpBannerAdView(context: Context) : FrameLayout(context) {

    /** 渲染结果回调，对照 iOS WxpBannerAdViewDelegate */
    interface Callback {
        /** 模版渲染成功，host 据此将容器展开到 [heightPx] 高度 */
        fun onAdRendered(adView: WxpBannerAdView, heightPx: Int)

        /** 加载或渲染失败，host 据此保持/收起隐藏 */
        fun onAdFailed(adView: WxpBannerAdView)

        /** 用户关闭（dislike），host 据此收起隐藏 */
        fun onAdClosed(adView: WxpBannerAdView)
    }

    companion object {
        /** 消息详情页底部 Banner 代码位 ID（Android 专属，与 iOS 不同） */
        const val messageDetailSlotId = "983728419"

        private const val TAG = "WxpAd"

        /** Banner 模版比例（宽:高 = 600:150），与 iOS 一致 */
        private const val RATIO = 150f / 600f
    }

    private val activity: Activity = context as? Activity
        ?: throw IllegalArgumentException("WxpBannerAdView 需要 Activity 上下文")

    var callback: Callback? = null

    private var expressAd: TTNativeExpressAd? = null

    /** 按当前可用宽度（px）加载一条 Banner 广告 */
    fun loadAd(slotId: String, widthPx: Int) {
        if (widthPx <= 0 || !WxpPangleAdManager.isReady()) {
            callback?.onAdFailed(this)
            return
        }
        val widthDp = pxToDp(widthPx.toFloat())
        val heightDp = widthDp * RATIO

        val adSlot = AdSlot.Builder()
            .setCodeId(slotId)
            .setAdCount(1)
            .setExpressViewAcceptedSize(widthDp, heightDp)
            .build()

        val adNative = TTAdSdk.getAdManager().createAdNative(activity)
        adNative.loadBannerExpressAd(adSlot, object : TTAdNative.NativeExpressAdListener {
            override fun onError(code: Int, message: String?) {
                WxpLogUtils.d(TAG, "Banner拉取失败 code=$code msg=$message")
                callback?.onAdFailed(this@WxpBannerAdView)
            }

            override fun onNativeExpressAdLoad(ads: List<TTNativeExpressAd>?) {
                val ad = ads?.firstOrNull()
                if (ad == null) {
                    callback?.onAdFailed(this@WxpBannerAdView)
                    return
                }
                bindAd(ad)
            }
        })
    }

    private fun bindAd(ad: TTNativeExpressAd) {
        expressAd = ad
        ad.setExpressInteractionListener(object : TTNativeExpressAd.ExpressAdInteractionListener {
            override fun onAdClicked(view: View?, type: Int) {
                // 点击广告，无需额外处理
            }

            override fun onAdShow(view: View?, type: Int) {
                // 广告展示，无需额外处理
            }

            override fun onRenderFail(view: View?, msg: String?, code: Int) {
                WxpLogUtils.d(TAG, "Banner渲染失败 code=$code msg=$msg")
                callback?.onAdFailed(this@WxpBannerAdView)
            }

            override fun onRenderSuccess(view: View?, width: Float, height: Float) {
                val adView = view ?: return
                removeAllViews()
                addView(adView)
                val heightPx = dpToPx(height).toInt()
                callback?.onAdRendered(this@WxpBannerAdView, heightPx)
            }
        })
        // 用户「不喜欢」关闭回调
        ad.setDislikeCallback(activity, object : TTAdDislike.DislikeInteractionCallback {
            override fun onShow() {}

            override fun onSelected(position: Int, value: String?, enforce: Boolean) {
                callback?.onAdClosed(this@WxpBannerAdView)
            }

            override fun onCancel() {}
        })
        // 按当前界面深/浅色设置广告主题，使创意与页面背景协调
        WxpPangleAdManager.applyAdTheme(isDarkMode())
        ad.render()
    }

    private fun isDarkMode(): Boolean {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    fun destroy() {
        expressAd?.destroy()
        expressAd = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        destroy()
    }

    private fun pxToDp(px: Float): Float = px / resources.displayMetrics.density

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}
