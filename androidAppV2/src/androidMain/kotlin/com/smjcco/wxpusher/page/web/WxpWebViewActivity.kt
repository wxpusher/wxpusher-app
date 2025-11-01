package com.smjcco.wxpusher.page.web

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.WxpBaseActivity
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpToastUtils

class WxpWebViewActivity : WxpBaseActivity() {

    companion object Companion {
        const val EXTRA_URL = "extra_url"
        fun start(context: Context, url: String) {
            val intent = Intent(context, WxpWebViewActivity::class.java)
            intent.putExtra(EXTRA_URL, url)
            context.startActivity(intent)
        }
    }

    private var fragment: WxpWebViewFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        if (savedInstanceState != null) {
            return
        }
        val targetUrl = intent.getStringExtra(EXTRA_URL) ?: ""
        if (targetUrl.isEmpty()) {
            WxpToastUtils.showToast("无效的链接")
            WxpLogUtils.i(message = "打开无效的拦截,url=${targetUrl}")
            finish()
            return
        }
        fragment = WxpWebViewFragment.newInstance(targetUrl)
        supportFragmentManager.beginTransaction()
            .replace(R.id.webViewContainer, fragment!!)
            .commit()
    }

    override fun onBackPressed() {
        if (fragment?.onBackPressed() != true) {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return fragment?.onActivityCreateOptionsMenu(menu, menuInflater) ?: true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return fragment?.onActivityOptionsItemSelected(item) ?: true
    }
}
