package com.smjcco.wxpusher.base.common

expect fun ExpWxpSaveService_get(key: String): String?
expect fun ExpWxpSaveService_set(key: String, value: String?)


expect fun ExpWxpSaveService_getDouble(key: String): Double?
expect fun ExpWxpSaveService_setDouble(key: String, value: Double)

expect fun ExpWxpSaveService_remove(key: String)
expect fun ExpWxpSaveService_init()

/**
 * 数据存储服务，提供基础的数据持久化存储
 */
object WxpSaveService {

    fun init() {
        ExpWxpSaveService_init()
    }

    fun get(key: String, value: String): String {
        return ExpWxpSaveService_get(key) ?: value
    }

    fun set(key: String, value: String?) {
        ExpWxpSaveService_set(key, value)
    }

    fun get(key: String, value: Boolean): Boolean {
        val bStr = get(key, "")
        if (bStr.isEmpty()) {
            return value
        }
        return bStr.toBoolean()
    }

    fun set(key: String, value: Boolean) {
        ExpWxpSaveService_set(key, value.toString())
    }

    fun get(key: String, value: Int): Int {
        val bStr = get(key, "")
        if (bStr.isEmpty()) {
            return value
        }
        return bStr.toInt()
    }

    fun set(key: String, value: Int) {
        ExpWxpSaveService_set(key, value.toString())
    }

    fun get(key: String, value: Double): Double {
        return ExpWxpSaveService_getDouble(key) ?: value
    }

    fun set(key: String, value: Double) {
        ExpWxpSaveService_setDouble(key, value)

    }

}