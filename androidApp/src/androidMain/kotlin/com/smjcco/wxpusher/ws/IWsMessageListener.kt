package com.smjcco.wxpusher.ws

interface IWsMessageListener<T:BaseWsMsg> {
    fun onMessage(message: T);
}