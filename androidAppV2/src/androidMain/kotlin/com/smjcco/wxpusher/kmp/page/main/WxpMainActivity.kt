package com.smjcco.wxpusher.kmp.page.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import com.smjcco.wxpusher.kmp.page.main.fragment.ITabMenuProvider
import com.smjcco.wxpusher.kmp.page.main.fragment.MessageListFragment
import com.smjcco.wxpusher.kmp.page.main.fragment.ProfileFragment

/**
 * app首页 - 使用ViewPager2 + TabLayout实现Tab切换
 */
class WxpMainActivity : WxpBaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var fragmentList: List<Fragment>
    private var currentMenuProvider: ITabMenuProvider? = null
    private var currentMenu: Menu? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        fragmentList = listOf<Fragment>(MessageListFragment(), ProfileFragment())
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
        viewPager.adapter = MainPagerAdapter(this, fragmentList)

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

        // 监听Tab切换，更新Toolbar标题和菜单
        tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    title = it.text
                    // 更新菜单
                    updateMenuForCurrentTab(tab.position)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // 不需要处理
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // 不需要处理
            }
        })

        // 监听ViewPager页面切换（处理手势滑动切换）
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 延迟更新菜单，确保Fragment已经加载完成
                viewPager.post {
                    updateMenuForCurrentTab(position)
                }
            }
        })

        // 延迟初始化第一个tab的菜单，确保Fragment已创建
        viewPager.post {
            updateMenuForCurrentTab(0)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 清空之前的菜单
        menu.clear()
        currentMenu = menu

        // 根据当前Fragment设置菜单
        currentMenuProvider?.let { menuProvider ->
            val menuResId = menuProvider.getMenuResId()
            if (menuResId != 0) {
                menuInflater.inflate(menuResId, menu)
                menuProvider.onMenuCreated(menu)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 将菜单点击事件委托给当前Fragment处理
        currentMenuProvider?.let { menuProvider ->
            if (menuProvider.onMenuItemSelected(item)) {
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 根据当前tab位置更新菜单
     */
    private fun updateMenuForCurrentTab(position: Int) {
        val fragment = fragmentList[position]
        if (fragment is ITabMenuProvider) {
            currentMenuProvider = fragment
            // 重新创建菜单
            invalidateOptionsMenu()
        } else {
            currentMenuProvider = null
            invalidateOptionsMenu()
        }
    }

    /**
     * ViewPager2适配器
     */
    private inner class MainPagerAdapter(
        activity: AppCompatActivity,
        var fragmentList: List<Fragment>
    ) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = fragmentList.size

        override fun createFragment(position: Int): Fragment {
            return fragmentList[position]
        }
    }
}