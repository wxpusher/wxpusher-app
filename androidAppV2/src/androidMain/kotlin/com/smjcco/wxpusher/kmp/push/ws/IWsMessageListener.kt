package com.smjcco.wxpusher.kmp.push.ws

interface IWsMessageListener<T: BaseWsMsg> {
    fun onMessage(message: T);
}