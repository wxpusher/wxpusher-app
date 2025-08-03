package com.smjcco.wxpusher.base

import platform.Foundation.NSLog

/**
 * iOS平台的日志实现
 */
actual fun platformLog(level: WxpLogUtils.LogLevel, tag: String, message: String, throwable: Throwable?) {
    val logMessage = if (throwable != null) {
        "$message\nException: ${throwable.message}\nStackTrace: ${throwable.stackTraceToString()}"
    } else {
        message
    }
    
    // iOS使用NSLog输出日志
    NSLog(logMessage)
}