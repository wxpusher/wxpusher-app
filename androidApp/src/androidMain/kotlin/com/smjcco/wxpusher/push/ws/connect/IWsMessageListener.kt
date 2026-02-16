package com.smjcco.wxpusher.push.ws.connect

interface IWsMessageListener<T: BaseWsMsg> {
    fun onMessage(message: T);
}