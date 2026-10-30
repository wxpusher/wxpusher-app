package com.smjcco.wxpusher.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.smjcco.wxpusher.base.common.WxpLogUtils

interface IAppForegroundListener {
    fun onAppForeground()
}

/**
 * 监听 App 从后台回到前台。
 *
 * 基于 ProcessLifecycleOwner：最后一个 Activity 停止后再等 700ms 才判定进入后台，
 * 切换深色模式、改字体大小导致 Activity 重建时不会被当成一次前后台切换，不需要自己统计 Activity 数。
 * 回调都在主线程。
 */
object WxpAppForegroundObserver {
    private const val TAG = "AppForeground"

    private var initialized = false
    private val listenerList: MutableList<IAppForegroundListener> = mutableListOf()

    /** 注册进程生命周期监听，同一进程只注册一次，必须在主线程调用。 */
    fun init() {
        if (initialized) {
            return
        }
        initialized = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                WxpLogUtils.d(TAG, "App回到前台，listener=${listenerList.size}")
                listenerList.toList().forEach { it.onAppForeground() }
            }
        })
    }

    fun addListener(listener: IAppForegroundListener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener)
        }
    }

    fun removeListener(listener: IAppForegroundListener) {
        listenerList.remove(listener)
    }
}
