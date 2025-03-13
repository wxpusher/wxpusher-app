package com.smjcco.wxpusher.api

import androidx.annotation.Keep

@Keep
class BaseResp<T>(val code: Int, val msg: String, val data: T) {
    fun isSuccess(): Boolean {
        return code == 1000
    }
}