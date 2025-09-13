package com.smjcco.wxpusher.base

import android.content.pm.PackageManager
import android.os.Build

actual fun WxpBaseInfoService_getAppVersionName(): String {
    try {
        // 获取PackageManager实例
        val packageManager = ApplicationUtils.application.packageManager
        // 获取当前应用的PackageInfo
        val packageInfo =
            packageManager.getPackageInfo(ApplicationUtils.application.packageName, 0)
        // 获取版本号
        return packageInfo.versionName ?: ""
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        return ""
    }
}

actual fun WxpBaseInfoService_getDeviceName(): String = Build.BRAND + " " + Build.MODEL
actual fun WxpBaseInfoService_getPlatform(): String {
    return ""
}

