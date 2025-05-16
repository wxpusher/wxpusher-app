package com.smjcco.wxpusher.bean

/**
 * 设备平台枚举，决定用啥推送通道
 */
enum class DevicePlatform(private val platform: String) {
    iOS("iOS"),
    Android("Android"),
    Android_XIAOMI("Android_Xiaomi"),
    Android_HUAWEI("Android_Huawei"),
    Android_VIVO("Android_Vivo"),
    Mac("Mac"),
    Windows("Windows"),
    Linux("Linux"),
    Web("Web"),
    Chrome_Windows("Chrome-Windows"),
    Chrome_Mac("Chrome-Mac"),
    Chrome_Android("Chrome-Android"),
    Chrome_Other("Chrome-Other"),
    Safari_MacOS("Safari-MacOS"),
    Safari_iOS("Safari-iOS"),
    Wecom("Wecom");

    fun getPlatform() = platform
}