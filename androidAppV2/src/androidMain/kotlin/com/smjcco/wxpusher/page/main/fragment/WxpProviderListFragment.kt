package com.smjcco.wxpusher.page.main.fragment

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.page.providerlist.IWxpProviderListPresenter
import com.smjcco.wxpusher.page.providerlist.IWxpProviderListView
import com.smjcco.wxpusher.page.providerlist.WxpProviderListPresenter
import com.smjcco.wxpusher.page.web.WxpWebViewFragment

class WxpProviderListFragment : WxpWebViewFragment(), IWxpProviderListView {
    private var presenter: IWxpProviderListPresenter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = createPresenter()
        presenter?.loadPage()
    }

    override fun setupUI(view: View) {
        super.setupUI(view)
        val closeButton: ImageButton = view.findViewById(R.id.closeButton)
        closeButton.setImageResource(R.drawable.ic_home)
    }

    //覆盖关闭按钮为打开首页
    override fun onCloseButtonClicked() {
        presenter?.loadPage()
    }

    override fun onLoadPage(url: String) {
        loadWebContent(url)
    }

    override fun createPresenter(): IWxpProviderListPresenter {
        return WxpProviderListPresenter(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter?.onDestroy()
    }
}