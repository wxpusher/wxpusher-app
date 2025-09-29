package com.smjcco.wxpusher.kmp.keepalive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.smjcco.wxpusher.push.PushManager

class KeepWsAliveStartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            KeepWsAliveService.start()
        }
    }
}
