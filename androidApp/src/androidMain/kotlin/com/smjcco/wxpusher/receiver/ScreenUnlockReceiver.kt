package com.smjcco.wxpusher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smjcco.wxpusher.ws.WsManager


class ScreenUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (Intent.ACTION_USER_PRESENT == intent.action) {
            // 屏幕解锁时触发任务
            Log.i(WsManager.TAG, "屏幕解锁，检查WS长链接")
            WsManager.connect()
        }
    }
}