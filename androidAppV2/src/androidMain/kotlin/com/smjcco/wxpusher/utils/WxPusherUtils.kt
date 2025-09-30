package com.smjcco.wxpusher.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import android.widget.Toast
import com.smjcco.wxpusher.base.common.ApplicationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object WxPusherUtils {
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun getMainScope(): CoroutineScope = mainScope
    fun getIoScopeScope(): CoroutineScope = ioScope


    fun toast(toast: String?) {
        toast?.let {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toast.makeText(ApplicationUtils.getApplication(), it, Toast.LENGTH_LONG).show()
            } else {
                mainScope.launch {
                    Toast.makeText(ApplicationUtils.getApplication(), it, Toast.LENGTH_LONG).show()
                }


            }
        }
    }

    fun getVersionName(): String {
        try {
            // 获取PackageManager实例
            val packageManager = ApplicationUtils.getApplication().packageManager
            // 获取当前应用的PackageInfo
            val packageInfo =
                packageManager.getPackageInfo(ApplicationUtils.getApplication().packageName, 0)
            // 获取版本号
            return packageInfo.versionName ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return ""
        }
    }

    fun getVersionCode(): Int {
        try {
            // 获取PackageManager实例
            val packageManager = ApplicationUtils.getApplication().packageManager
            // 获取当前应用的PackageInfo
            val packageInfo =
                packageManager.getPackageInfo(ApplicationUtils.getApplication().packageName, 0)
            // 获取版本号
            return packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return 0
        }
    }


}