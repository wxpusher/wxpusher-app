package com.smjcco.wxpusher_app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform