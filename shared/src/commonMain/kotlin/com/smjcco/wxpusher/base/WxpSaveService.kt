package com.smjcco.wxpusher.base

expect fun WxpSaveService_get(key: String): String?
expect fun WxpSaveService_set(key: String, value: String?)
expect fun WxpSaveService_init()

/**
 * 数据存储服务，提供基础的数据持久化存储
 */
object WxpSaveService {

    fun init() {
        WxpSaveService_init()
    }

    fun get(key: String, value: String): String {
        return WxpSaveService_get(key) ?: value
    }

    fun set(key: String, value: String?) {
        WxpSaveService_set(key, value)
    }

    fun get(key: String, value: Boolean): Boolean {
        val bStr = get(key, "")
        if (bStr.isEmpty()) {
            return value
        }
        return bStr.toBoolean()
    }

    fun set(key: String, value: Boolean) {
        WxpSaveService_set(key, value.toString())
    }

    fun get(key: String, value: Int): Int {
        val bStr = get(key, "")
        if (bStr.isEmpty()) {
            return value
        }
        return bStr.toInt()
    }

    fun set(key: String, value: Int) {
        WxpSaveService_set(key, value.toString())
    }

}