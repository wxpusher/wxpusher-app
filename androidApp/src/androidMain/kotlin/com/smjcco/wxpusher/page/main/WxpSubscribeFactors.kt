package com.smjcco.wxpusher.page.main

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.smjcco.wxpusher.biz.subscribe.WxpSubscribeIntentFactors
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 安装后还原用的设备多因素，口径和 H5 落地页一致：屏幕用 dp，竖屏短边为宽。
 */
object WxpSubscribeFactors {
    fun collect(context: Context): WxpSubscribeIntentFactors {
        val density = context.resources.displayMetrics.density.takeIf { it > 0 } ?: 1f
        //网页的 screen.width/height 是整块屏幕，这里也要用含状态栏、导航栏的物理尺寸
        val (pxW, pxH) = realScreenPixels(context)
        val w = (min(pxW, pxH) / density).roundToInt()
        val h = (max(pxW, pxH) / density).roundToInt()
        return WxpSubscribeIntentFactors(
            os = "android",
            osVersion = Build.VERSION.RELEASE,
            model = Build.MODEL,
            screenW = w,
            screenH = h,
            dpr = density.toDouble(),
            netType = netType(context),
            lang = Locale.getDefault().toLanguageTag(),
            tz = TimeZone.getDefault().id,
        )
    }

    @Suppress("DEPRECATION")
    private fun realScreenPixels(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }

    private fun netType(context: Context): String? = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        when {
            caps == null -> null
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            else -> null
        }
    } catch (e: Throwable) {
        null
    }
}
