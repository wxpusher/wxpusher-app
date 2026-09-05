package com.smjcco.wxpusher.push

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * 监听 Activity 启动，并触发设备活跃信息检查。
 *
 * onActivityStarted 在每次页面切换时都会触发，并不等价于应用回到前台；协调器会通过最近
 * 一次成功上报时间统一限制一小时内不再发起网络请求，因此这里不额外维护 Activity 数量状态。
 */
internal object PushActiveReportLifecycle : Application.ActivityLifecycleCallbacks {
    private var initialized = false

    /** 注册全局 Activity 生命周期监听，同一进程只注册一次。 */
    fun init(application: Application) {
        if (initialized) {
            return
        }
        initialized = true
        application.registerActivityLifecycleCallbacks(this)
    }

    /** 任意 Activity 启动时检查是否需要补充上报设备活跃信息。 */
    override fun onActivityStarted(activity: Activity) {
        PushChannelCoordinator.reportActiveIfNeeded()
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}
