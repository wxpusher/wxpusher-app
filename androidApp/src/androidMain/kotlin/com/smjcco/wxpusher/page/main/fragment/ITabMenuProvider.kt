package com.smjcco.wxpusher.page.main.fragment

import android.view.Menu
import android.view.MenuItem

/**
 * Tab菜单提供者接口
 * 每个Fragment可以实现此接口来定义自己的菜单
 */
interface ITabMenuProvider {
    /**
     * 获取菜单资源ID
     * @return 菜单资源ID，如果返回0表示不显示菜单
     */
    fun getMenuResId(): Int
    
    /**
     * 处理菜单项点击事件
     * @param item 被点击的菜单项
     * @return true表示已处理，false表示未处理
     */
    fun onMenuItemSelected(item: MenuItem): Boolean
    
    /**
     * 在菜单创建后可以对菜单进行额外配置
     * @param menu 创建的菜单
     */
    fun onMenuCreated(menu: Menu) {
        // 默认空实现
    }
}
