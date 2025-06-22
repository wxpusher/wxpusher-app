package com.smjcco.wxpusher.biz.common

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual fun WxpAppPageService_jumpToLogin(){
    platform.swift.com.smjcco.wxpusher.KtSwiftJumpPageUtils.jumpToLogin()
}