package com.smjcco.wxpusher.push.ws

interface IWsMessageListener<T: BaseWsMsg> {
    fun onMessage(message: T);
}