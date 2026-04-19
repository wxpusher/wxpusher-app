package com.smjcco.wxpusher.base.common


expect fun WxpDialogUtils_showDialog(params: WxpDialogParams)
data class WxpDialogParams(
    var title: String? = null,
    var message: String? = null,
    var leftText: String? = null,
    var leftBlock: (() -> Unit)? = null,
    var rightText: String? = null,
    var rightBlock: (() -> Unit)? = null,
    var cancelable: Boolean = true
)

object WxpDialogUtils {

    fun showDialog(params: WxpDialogParams?) {
        if (params == null) {
            return
        }

        runAtMainSuspend {
            WxpDialogUtils_showDialog(params)
        }
    }
}