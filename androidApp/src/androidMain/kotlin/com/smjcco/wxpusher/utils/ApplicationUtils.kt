package com.smjcco.wxpusher.utils

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process

object ApplicationUtils {
    lateinit var application: Application
    private val TAG = "AppInit"

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
}