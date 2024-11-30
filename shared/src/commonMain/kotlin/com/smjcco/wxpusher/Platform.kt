package com.smjcco.wxpusher

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform