package com.smjcco.wxpusher.page.main.fragment

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.page.web.WxpWebViewFragment

/**
 * 扩展功能 tab —— 原生 WebView 容器加载 app-fe 的九宫格入口页。
 * 与消息市场 tab（WxpProviderListFragment）结构一致：隐藏返回按钮、关闭按钮改为回到本页。
 */
class WxpExtFuncFragment : WxpWebViewFragment() {

    private val pageUrl: String
        get() = "${WxpConfig.appFeUrl}/app/#/ext-func"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadWebContent(pageUrl)
    }

    override fun setupUI(view: View) {
        super.setupUI(view)
        //默认隐藏底部操作栏：布局默认 VISIBLE，避免打开瞬间闪一下再隐藏（UI 线程调用会同步生效）
        setBottomBarVisibleOverride(false)
        //隐藏左上角的返回按钮
        getActivityHost()?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        //覆盖webview默认的关闭按钮，改成home按钮
        val closeButton: ImageButton = view.findViewById(R.id.closeButton)
        closeButton.setImageResource(R.drawable.ic_home)
    }

    //覆盖关闭按钮为回到扩展功能首页
    override fun onCloseButtonClicked() {
        loadWebContent(pageUrl)
    }
}
