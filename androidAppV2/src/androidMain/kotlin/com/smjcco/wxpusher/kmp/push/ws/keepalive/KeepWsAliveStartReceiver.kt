package com.smjcco.wxpusher.kmp.push.ws.keepalive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class KeepWsAliveStartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            KeepWsAliveService.start()
        }
    }
}
