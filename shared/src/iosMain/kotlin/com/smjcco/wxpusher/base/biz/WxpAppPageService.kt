package com.smjcco.wxpusher.base.biz

import kotlinx.cinterop.ExperimentalForeignApi
import platform.swift.com.smjcco.wxpusher.KtSwiftJumpPageUtils

@OptIn(ExperimentalForeignApi::class)
actual fun WxpAppPageService_jumpToLogin(){
    KtSwiftJumpPageUtils.jumpToLogin()
}