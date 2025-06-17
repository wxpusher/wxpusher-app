package com.smjcco.wxpusher.base

import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

expect fun Double.toDateTimeString(): String


object WxpDateTimeUtils {
    @OptIn(ExperimentalTime::class)

    fun getRelativeDateTime(timeStamp: Double): String {
        val nowInSeconds = Clock.System.now().toEpochMilliseconds()
        val duration = (abs(nowInSeconds - timeStamp) / 1000).toInt()
        return when {
            duration < 60 -> "刚刚"
            duration < 3600 -> "${duration / 60}分钟前"
            duration < 86400 -> "${duration / 3600}小时前"
            duration < 604800 -> "${duration / 86400}天前"
            else -> {
                timeStamp.toDateTimeString()
            }
        }
    }

}