package com.smjcco.wxpusher.base.common

inline fun <T> T?.letOnNotEmpty(run: (T) -> Unit) {
    if (this == null) {
        return
    }
    if (this is String && this.isEmpty()) {
        return
    }
    run(this)
}