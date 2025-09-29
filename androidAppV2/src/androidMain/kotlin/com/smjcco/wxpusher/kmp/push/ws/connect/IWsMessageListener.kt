package com.smjcco.wxpusher.kmp.push.ws.connect

interface IWsMessageListener<T: BaseWsMsg> {
    fun onMessage(message: T);
}