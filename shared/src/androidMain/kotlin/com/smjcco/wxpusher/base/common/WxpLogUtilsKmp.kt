package com.smjcco.wxpusher.base.common

import android.util.Log

/**
 * Android平台的日志实现
 */
actual fun platformLog(level: WxpLogUtils.LogLevel, tag: String, message: String, throwable: Throwable?) {
    when (level) {
        WxpLogUtils.LogLevel.DEBUG -> {
            if (throwable != null) {
                Log.d(tag, message, throwable)
            } else {
                Log.d(tag, message)
            }
        }
        WxpLogUtils.LogLevel.INFO -> {
            if (throwable != null) {
                Log.i(tag, message, throwable)
            } else {
                Log.i(tag, message)
            }
        }
        WxpLogUtils.LogLevel.WARN -> {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
        WxpLogUtils.LogLevel.ERROR -> {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
}