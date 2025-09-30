package com.smjcco.wxpusher.base.common

import kotlin.random.Random

object RandomUtils {
    fun generateRandomString(length: Int): String {
        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { charPool[Random.nextInt(0, charPool.size)] }
            .joinToString("")
    }
}