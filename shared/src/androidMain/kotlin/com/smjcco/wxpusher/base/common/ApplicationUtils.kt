package com.smjcco.wxpusher.base.common

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import android.os.Process


@SuppressLint("StaticFieldLeak")
object ApplicationUtils : ActivityLifecycleCallbacks {
    private lateinit var application: Application
    private val TAG = "AppInit"

    // 当前Activity的引用

    private var currentActivity: Activity? = null

    /**
     * 初始化ApplicationUtils
     * @param app Application实例
     */
    fun init(app: Application) {
        application = app
        // 注册Activity生命周期回调
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * 获取当前Activity
     */
    fun getCurrentActivity(): Activity? {
        return currentActivity
    }

    fun getApplication(): Application {
        return application
    }

    /**
     * 判断是不是主进程
     * 感觉判断的有问题，先不用这个方法
     */
    fun isMainProcess(): Boolean {
        val am = (application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        val processInfos = am.runningAppProcesses
        val mainProcessName: String = application.getPackageName()
        val myPid = Process.myPid()
        for (info in processInfos) {
            if (info.pid == myPid && mainProcessName == info.processName) {
                return true
            }
        }
        return false
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    /**
     * 记录当前显示的Activity
     */
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}