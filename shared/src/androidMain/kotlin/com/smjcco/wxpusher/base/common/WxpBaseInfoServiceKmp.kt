package com.smjcco.wxpusher.base.common

import android.content.pm.PackageManager
import android.os.Build

actual fun WxpBaseInfoService_getAppVersionName(): String {
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

actual fun WxpBaseInfoService_getDeviceName(): String = Build.BRAND + " " + Build.MODEL
