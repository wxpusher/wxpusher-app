package com.smjcco.wxpusher.api


class BaseResp<T>(val code: Int, val msg: String, val data: T) {
    fun isSuccess(): Boolean {
        return code == 1000
    }
}