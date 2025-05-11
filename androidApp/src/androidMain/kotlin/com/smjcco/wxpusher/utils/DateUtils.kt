package com.smjcco.wxpusher.utils

import java.text.SimpleDateFormat
import java.util.Date

object DateUtils {
    fun getDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        return dateFormat.format(Date()) // 格式化为字符串
    }
}