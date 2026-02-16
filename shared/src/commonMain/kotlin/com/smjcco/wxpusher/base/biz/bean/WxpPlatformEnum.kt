package com.smjcco.wxpusher.base.biz.bean

enum class WxpPlatformEnum(val platform: String) {
    iOS("iOS"),
    Android("Android"),
    Android_XIAOMI("Android_Xiaomi"),
    Android_HUAWEI("Android_Huawei"),
    Android_VIVO("Android_Vivo"),
    Android_HONOR("Android_Honor"),
    Android_OPPO("Android_Oppo"),
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


    fun isThis(p: String?): Boolean {
        return platform == p
    }

    companion object {
        /**
         * 验证平台是否OK
         */
        fun verify(name: String?): Boolean {
            if (name.isNullOrEmpty()) {
                return false
            }
            val values = entries.toTypedArray()
            for (value in values) {
                if (value.platform == name) {
                    return true
                }
            }
            if (name.startsWith("Chrome-") || name.startsWith("Safari-")) {
                return true
            }
            return false
        }

        /**
         * 是否是chrome设备
         */
        fun isChrome(platform: String?): Boolean {
            return platform != null && platform.startsWith("Chrome-")
        }
    }
}