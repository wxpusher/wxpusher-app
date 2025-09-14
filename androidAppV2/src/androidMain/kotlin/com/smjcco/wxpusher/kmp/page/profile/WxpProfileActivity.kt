package com.smjcco.wxpusher.kmp.page.profile

import android.os.Bundle
import com.smjcco.wxpusher.kmp.base.WxpBaseActivity
import com.smjcco.wxpusher.kmp.page.main.fragment.ProfileFragment

/**
 * Profile Activity - 个人中心页面
 * 
 * 注意：实际使用中，个人中心功能已迁移到 ProfileFragment
 * 这个 Activity 保留作为备用，如果未来需要独立的个人中心页面
 * 可以在这里加载 ProfileFragment
 */
class WxpProfileActivity : WxpBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 如果需要独立的 Activity，可以在这里加载 ProfileFragment
        // 约定：目前主要使用 ProfileFragment 在主页面的 Tab 中
        // TODO: 如果需要独立 Activity，可以在这里实现
        finish()
    }
}