package com.smjcco.wxpusher.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

fun runAtMainSuspend(block: suspend () -> Unit) {
    WxpScopeUtils.getMainScope().launch {
        block()
    }
}

fun runAtIOSuspend(block: suspend () -> Unit) {
    WxpScopeUtils.getIoScopeScope().launch {
        block()
    }
}


object WxpScopeUtils {
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun getMainScope(): CoroutineScope = mainScope
    fun getIoScopeScope(): CoroutineScope = ioScope

}
