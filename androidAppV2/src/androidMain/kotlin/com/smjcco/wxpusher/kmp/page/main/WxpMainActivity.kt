package com.smjcco.wxpusher.kmp.page.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.kmp.base.WxpBaseActivity
import com.smjcco.wxpusher.kmp.common.WxpSaveKey
import com.smjcco.wxpusher.kmp.page.login.WxpLoginActivity
import com.smjcco.wxpusher.kmp.page.main.fragment.MessageListFragment
import com.smjcco.wxpusher.kmp.page.main.fragment.ProfileFragment

/**
 * app首页 - 使用ViewPager2 + TabLayout实现Tab切换
 */
class WxpMainActivity : WxpBaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        // 初始化视图
        initViews()

        if (checkAppStatus()) {
            //检查没有通过，不走后面的构建页面流程了
            finish()
            return
        }

        // 设置ViewPager和TabLayout
        setupViewPager()
        title = "消息列表"
    }

    /**
     * 检查状态，没有通过就返回false，不走后面流程
     */
    private fun checkAppStatus(): Boolean {
        //没有同意隐私协议
        if (WxpSaveService.get(WxpSaveKey.UserHasAgreement, false)) {
            // TODO: 打开隐私协议页面
//            startActivity(Intent(this, LoginActivity::class.java))
            return true;
        }
        val token = WxpAppDataService.getLoginInfo()?.deviceToken
        if (token.isNullOrEmpty()) {
            startActivity(Intent(this, WxpLoginActivity::class.java))
            return true
        }
        return false
    }

    private fun initViews() {
        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)
    }

    private fun checkPermissionsAndLogin() {
        // TODO: 实现权限检查和登录状态检查
        // 对应iOS中的隐私协议检查和登录检查
        // 这里应该检查：
        // 1. 用户是否同意隐私协议
        // 2. 用户是否已登录
        // 3. 如果未登录，跳转到登录页面
    }

    private fun setupViewPager() {
        viewPager.adapter = MainPagerAdapter(this)

        // 连接TabLayout和ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "消息列表"
                    tab.setIcon(R.drawable.ic_paperplane)
                }

                1 -> {
                    tab.text = "我的"
                    tab.setIcon(R.drawable.ic_person)
                }
            }
        }.attach()

        // 监听Tab切换，更新Toolbar标题
        tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    title = it.text
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // 不需要处理
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // 不需要处理
            }
        })
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_action_bar, menu)
        return true
    }

    fun setMenu(){

    }
    /**
     * ViewPager2适配器
     */
    private inner class MainPagerAdapter(activity: AppCompatActivity) :
        FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MessageListFragment()
                1 -> ProfileFragment()
                else -> MessageListFragment()
            }
        }
    }
}