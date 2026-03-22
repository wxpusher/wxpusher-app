package com.smjcco.wxpusher.page.main

import androidx.fragment.app.Fragment

/**
 * Activity 实现此接口后，可告知调用方当前选中的 Tab 是否对应传入的 Fragment。
 * 用于在 ViewPager+Tab 场景下，仅当该 Fragment 为当前页时才更新标题，避免后台 Tab 覆盖前台标题。
 */
interface CurrentTabProvider {

    /**
     * @return 若当前由 ViewPager 选中的页面对应 [fragment] 则返回 true，否则返回 false
     */
    fun isFragmentCurrentTab(fragment: Fragment): Boolean
}
