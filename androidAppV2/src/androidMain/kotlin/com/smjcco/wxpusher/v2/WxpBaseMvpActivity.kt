package com.smjcco.wxpusher.v2

import android.os.Bundle
import androidx.activity.ComponentActivity

abstract class WxpBaseMvpActivity<P> : WxpBaseActivity() {
    protected val presenter: P = createPresenter()

    abstract fun createPresenter(): P

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

}