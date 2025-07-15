package com.smjcco.wxpusher.base

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone


private const val UNIX_TO_REFERENCE_OFFSET: Double = 978307200.0
actual fun Double.toDateTimeString(): String {
    // 将毫秒级时间戳转换为秒（iOS NSDate 使用秒级时间戳）
    val secondsSince1970 = this / 1000.0
    val secondsSinceReference = secondsSince1970 - UNIX_TO_REFERENCE_OFFSET
    val date = NSDate(timeIntervalSinceReferenceDate = secondsSinceReference)

    return NSDateFormatter().apply {
        timeZone = NSTimeZone.localTimeZone
        dateFormat = "yyyy-MM-dd HH:mm:ss"
    }.stringFromDate(date)
}

actual fun Long.toDate(): String {
    // 将毫秒级时间戳转换为秒（iOS NSDate 使用秒级时间戳）
    val secondsSince1970 = this / 1000.0
    val secondsSinceReference = secondsSince1970 - UNIX_TO_REFERENCE_OFFSET
    val date = NSDate(timeIntervalSinceReferenceDate = secondsSinceReference)
    return NSDateFormatter().apply {
        timeZone = NSTimeZone.localTimeZone
        dateFormat = "yyyy-MM-dd"
    }.stringFromDate(date)
}