package com.smjcco.wxpusher.base

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone

actual fun Double.toDateTimeString(): String {
    val date = NSDate(this)
    val formatter = NSDateFormatter()
    formatter.timeZone = NSTimeZone.localTimeZone
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
    return formatter.stringFromDate(date)
}