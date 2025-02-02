package com.smjcco.wxpusher

object WxPusherConfig {
    const val Offline = true

    private const val Host = "10.0.0.10"

    const val WsUrl = "ws://${Host}:6104"
    const val ApiUrl = "http://${Host}:6100"
    const val WebUrl = "http://${Host}:3000/home"
}
