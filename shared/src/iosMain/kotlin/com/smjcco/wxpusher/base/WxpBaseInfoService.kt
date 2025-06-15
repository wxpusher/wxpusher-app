package com.smjcco.wxpusher.base

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

actual fun WxpBaseInfoService_getAppVersionName(): String {
    return (NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as String)
}

actual fun WxpBaseInfoService_getDeviceName(): String {
    return UIDevice.currentDevice.name
}

actual fun WxpBaseInfoService_getPlatform(): String {
    //目前只有iphone，先暂时写死
    return "iOS"
}

