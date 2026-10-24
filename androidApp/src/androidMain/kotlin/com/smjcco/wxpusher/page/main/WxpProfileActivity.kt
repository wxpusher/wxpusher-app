package com.smjcco.wxpusher.page.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.WxpBaseActivity
import com.smjcco.wxpusher.page.main.fragment.ProfileFragment

/**
 * 账号与推送设置（原来在底部 Tab 的「我的」）。
 *
 * 定制版把底部 Tab 藏掉了，班级页面要整屏显示，所以「我的」改成一个独立页面，
 * 从班级页右下角的悬浮按钮进入 —— 否则退出登录、推送设置这些入口就找不到了。
 */
class WxpProfileActivity : WxpBaseActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, WxpProfileActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        title = "账号与推送设置"
        //带返回箭头的标题栏，避免学生进来以后不知道怎么回班级页
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.profileContainer, ProfileFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
