package com.smjcco.wxpusher.kmp.base

import android.os.Bundle
import com.smjcco.wxpusher.base.IWxpBaseMvpPresenter

abstract class WxpBaseMvpActivity<P : IWxpBaseMvpPresenter<*, *>> : WxpBaseActivity() {
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