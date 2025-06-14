package com.smjcco.wxpusher.base

import platform.Foundation.NSUserDefaults

actual fun ExpWxpSaveService_get(key: String): String? {
    return NSUserDefaults.standardUserDefaults.stringForKey(key)
}

actual fun ExpWxpSaveService_set(key: String, value: String?) {
    NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
    NSUserDefaults.standardUserDefaults.synchronize()
}

actual fun ExpWxpSaveService_init() {

}

actual fun ExpWxpSaveService_remove(key: String) {
    NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
    NSUserDefaults.standardUserDefaults.synchronize()
}