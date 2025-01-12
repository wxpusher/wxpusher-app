package com.smjcco.wxpusher.utils

import android.content.pm.PackageManager
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object WxPusherUtils {
    private val mainScope = CoroutineScope(Dispatchers.Main)
    fun toast(toast: String?) {
        toast?.let {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toast.makeText(ApplicationUtils.application, it, Toast.LENGTH_LONG).show()
            } else {
                mainScope.launch {
                    Toast.makeText(ApplicationUtils.application, it, Toast.LENGTH_LONG).show()
                }


            }
        }
    }

    fun getVersionName(): String {
        try {
            // 获取PackageManager实例
            val packageManager = ApplicationUtils.application.packageManager
            // 获取当前应用的PackageInfo
            val packageInfo =
                packageManager.getPackageInfo(ApplicationUtils.application.packageName, 0)
            // 获取版本号
            return packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return ""
        }
    }

    fun getVersionCode(): Int {
        try {
            // 获取PackageManager实例
            val packageManager = ApplicationUtils.application.packageManager
            // 获取当前应用的PackageInfo
            val packageInfo =
                packageManager.getPackageInfo(ApplicationUtils.application.packageName, 0)
            // 获取版本号
            return packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return 0
        }
    }
}