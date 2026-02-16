package com.smjcco.wxpusher.wxapi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smjcco.wxpusher.base.common.WxpLogUtils

/**
 * 微信回调Activity，主要是分享
 *
 * 注意事项：
 * 1. 此Activity必须放在wxapi包名下
 * 2. 此Activity必须在AndroidManifest.xml中注册
 * 3. 不要添加intent-filter，避免Android 13无法回跳
 */
open class WXEntryActivity : AppCompatActivity() {

    private val TAG = "WXEntryActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WxpLogUtils.d(TAG, "WXEntryActivity onCreate")

        // 将回调传递给WxpWeixinOpenManager处理
        WxpWeixinOpenManager.getApi()?.handleIntent(intent, WxpWeixinOpenManager)

        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        WxpLogUtils.d(TAG, "WXEntryActivity onNewIntent")
        setIntent(intent)

        // 将回调传递给WxpWeixinOpenManager处理
        WxpWeixinOpenManager.getApi()?.handleIntent(intent, WxpWeixinOpenManager)
        finish()
    }
}