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
    Android_HONOR("Android_Honor"),
    Android_OPPO("Android_Oppo"),
    Android_MEIZU("Android_Meizu"),
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

    companion object {
        /** 根据后端保存的平台字符串恢复枚举，无法识别时返回空。 */
        fun find(platform: String?): DevicePlatform? =
            entries.firstOrNull { it.platform == platform }
    }
}
