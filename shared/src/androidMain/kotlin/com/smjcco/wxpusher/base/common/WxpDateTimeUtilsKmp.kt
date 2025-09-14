package com.smjcco.wxpusher.base.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


actual fun Double.toDateTimeString(): String {
    val date = Date(this.toLong())
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return format.format(date)
}

actual fun Long.toDate(): String {
    val date = Date(this)
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return format.format(date)
}