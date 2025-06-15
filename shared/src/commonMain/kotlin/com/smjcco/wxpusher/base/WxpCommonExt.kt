package com.smjcco.wxpusher.base

public inline fun Any?.letOnNotEmpty(run: (Any) -> Unit) {
    if (this == null) {
        return
    }
    if (this is String && this.isEmpty()) {
        return
    }
    run(this)
}