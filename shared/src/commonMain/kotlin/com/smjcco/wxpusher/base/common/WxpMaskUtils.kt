package com.smjcco.wxpusher.base.common

object WxpMaskUtils {
    /**
     *  对数据进行脱敏，中间用 * 填充
     *  @param text 原始文本
     *  @param pre 前面保留的长度
     *  @param end 后面保留的长度，
     */
    fun mask(text: String, pre: Int, end: Int): String {
        val length = text.length
        var preStr = ""
        var endStr = ""

        if (length > pre) {
            preStr = text.substring(0, pre)
        }

        if (length > end) {
            endStr = text.substring(length - end)
        }

        val rest = length - preStr.length - endStr.length
        val sb = StringBuilder(preStr)
        for (i in 0 until rest) {
            sb.append("*")
        }
        sb.append(endStr)
        return sb.toString()
    }
}

