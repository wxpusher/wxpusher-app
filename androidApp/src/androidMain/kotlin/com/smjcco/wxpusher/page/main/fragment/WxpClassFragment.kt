package com.smjcco.wxpusher.page.main.fragment

import android.os.Bundle
import android.view.View
import com.smjcco.wxpusher.page.web.WxpWebViewFragment

/**
 * 班级管理系统页（定制版）。
 *
 * 站点 `wangan.xdyzf.cn` 不是 WxPusher 官方域名，**没有加入** WxpWebHostPolicy 白名单，
 * 因此顶部会照常显示「当前内容由第三方提供，与WxPusher无关」的提示条，点击仍弹出官方免责声明
 * —— 这是官方要求保留的提醒，定制版不做屏蔽。
 *
 * 代价是底部浏览器操作栏（后退/前进/刷新/关闭）默认也会出现。这里按官方的既有做法
 * （见 WxpProviderListFragment / WxpExtFuncFragment）在 setupUI 里单独关掉，
 * 不依赖白名单、也不改动官方白名单逻辑。
 */
class WxpClassFragment : WxpWebViewFragment() {

    companion object {
        fun newInstance(url: String): WxpClassFragment {
            val fragment = WxpClassFragment()
            val args = Bundle()
            args.putString(WxpWebViewFragment.EXTRA_URL, url)
            args.putBoolean(WxpWebViewFragment.EXTRA_SHOW_AD, false)
            fragment.arguments = args
            return fragment
        }
    }

    override fun setupUI(view: View) {
        super.setupUI(view)
        //默认隐藏底部操作栏：布局默认 VISIBLE，避免打开瞬间闪一下再隐藏（UI 线程调用会同步生效）
        setBottomBarVisibleOverride(false)
    }
}
