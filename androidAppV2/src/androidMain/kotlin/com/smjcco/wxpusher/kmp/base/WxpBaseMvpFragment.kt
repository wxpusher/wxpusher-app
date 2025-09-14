package com.smjcco.wxpusher.kmp.base

import android.os.Bundle
import com.smjcco.wxpusher.base.common.IWxpBaseMvpPresenter

abstract class WxpBaseMvpFragment<P : IWxpBaseMvpPresenter<*, *>> : WxpBaseFragment() {
    protected lateinit var presenter: P

    abstract fun createPresenter(): P

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = createPresenter()
    }

    override fun onResume() {
        super.onResume()
        presenter.onShow()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

}