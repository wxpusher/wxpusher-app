package com.smjcco.wxpusher.page

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.smjcco.wxpusher.page.main.WxpMainActivity


class WebViewActivity : ComponentActivity() {
    companion object {
        const val INTENT_KEY_URL = "url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addOnNewIntentListener {
            openPageFromIntent(it)
        }
        openPageFromIntent(intent)
    }

    //主要是为了兼容oppo打开的协议，后期线上没有这种打开方式以后，就可以删除这个WebViewActivity
    private fun openPageFromIntent(intent: Intent?) {
        val url = intent?.getStringExtra(INTENT_KEY_URL)
        if (url.isNullOrEmpty()) {
            return
        }
        //OPPO 推送的 action_parameters 会作为 extra 传进来，原文链接一起转给首页
        val sourceUrl = intent?.getStringExtra(WxpMainActivity.INTENT_KEY_SOURCE_URL)
        val intent = Intent(this, WxpMainActivity::class.java)
        intent.putExtra(WxpMainActivity.INTENT_KEY_URL, url)
        intent.putExtra(WxpMainActivity.INTENT_KEY_SOURCE_URL, sourceUrl)
        startActivity(intent)
        finish()
    }
}