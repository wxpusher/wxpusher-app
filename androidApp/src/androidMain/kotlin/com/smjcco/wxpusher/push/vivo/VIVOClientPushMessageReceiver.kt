package com.smjcco.wxpusher.push.vivo

import com.vivo.push.sdk.OpenClientPushMessageReceiver
import com.vivo.push.subscribedialog.ChannelOperation

class VIVOClientPushMessageReceiver : OpenClientPushMessageReceiver() {

    /** 用户在 vivo 订阅弹窗里的选择 */
    override fun onSubscribeDialogResult(operateResult: Int, operations: MutableList<ChannelOperation>?) {
        super.onSubscribeDialogResult(operateResult, operations)
        VivoSubscribeManager.onSubscribeDialogResult(operateResult, operations)
    }
}
