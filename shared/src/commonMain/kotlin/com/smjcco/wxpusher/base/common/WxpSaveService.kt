package com.smjcco.wxpusher.base.common

expect fun ExpWxpSaveService_get(key: String): String?
expect fun ExpWxpSaveService_set(key: String, value: String?)


expect fun ExpWxpSaveService_getDouble(key: String): Double?
expect fun ExpWxpSaveService_setDouble(key: String, value: Double)

expect fun ExpWxpSaveService_remove(key: String)
expect fun ExpWxpSaveService_init()

/**
 * 数据存储服务，提供基础的数据持久化存储。
 * 原生与 H5（通过桥 getByKey/setKeyValue）共享同一份存储、同一 key 空间。
 *
 * 内置变更观察者：任意 set 写入后按 key 通知 listener（主线程回调），供底部 tab 显隐等场景订阅。
 */
object WxpSaveService {

    private val listeners = mutableMapOf<Int, (String) -> Unit>()
    private var nextListenerId = 0

    fun init() {
        ExpWxpSaveService_init()
    }

    fun get(key: String, value: String): String {
        return ExpWxpSaveService_get(key) ?: value
    }

    fun set(key: String, value: String?) {
        ExpWxpSaveService_set(key, value)
        notifyChanged(key)
    }

    fun get(key: String, value: Boolean): Boolean {
        val bStr = get(key, "")
        if (bStr.isEmpty()) {
            return value
        }
        return bStr.toBoolean()
    }

    fun set(key: String, value: Boolean) {
        set(key, value.toString())
    }

    fun get(key: String, value: Int): Int {
        val bStr = get(key, "")
        if (bStr.isEmpty()) {
            return value
        }
        return bStr.toInt()
    }

    fun set(key: String, value: Int) {
        set(key, value.toString())
    }

    fun get(key: String, value: Double): Double {
        return ExpWxpSaveService_getDouble(key) ?: value
    }

    fun set(key: String, value: Double) {
        ExpWxpSaveService_setDouble(key, value)
        notifyChanged(key)
    }

    /**
     * 无重载歧义的字符串读写：供 iOS 桥等 ObjC/Swift 互操作场景直接调用。
     * get/set 的 String/Boolean/Int/Double 多个重载导出到 Swift 后，"干净"的 get(key:value:)/set(key:value:)
     * 选择器被分配给 Bool 重载，直接用 String 参数会编译报错，故提供唯一命名的别名。
     */
    fun getStringValue(key: String, defaultValue: String): String = get(key, defaultValue)

    fun setStringValue(key: String, value: String?) = set(key, value)

    /**
     * 注册存储变更监听，返回用于注销的 id。须在主线程调用；回调在主线程执行，参数为变更的 key。
     */
    fun addListener(listener: (String) -> Unit): Int {
        val id = nextListenerId++
        listeners[id] = listener
        return id
    }

    fun removeListener(id: Int) {
        listeners.remove(id)
    }

    private fun notifyChanged(key: String) {
        // 无 listener 时零开销（早期 init 也走这里，安全跳过）
        if (listeners.isEmpty()) {
            return
        }
        // 切主线程回调：注册/注销/通知都在主线程访问 listeners，无需锁（commonMain 无 @Synchronized）
        runAtMainSuspend {
            if (listeners.isNotEmpty()) {
                listeners.values.toList().forEach { it(key) }
            }
        }
    }
}
