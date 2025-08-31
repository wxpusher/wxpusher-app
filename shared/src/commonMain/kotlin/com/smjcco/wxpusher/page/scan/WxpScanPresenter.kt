package com.smjcco.wxpusher.page.scan

import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.WxpBaseMvpPresenter
import com.smjcco.wxpusher.base.WxpDialogParams
import com.smjcco.wxpusher.base.WxpDialogUtils
import com.smjcco.wxpusher.base.WxpToastUtils
import com.smjcco.wxpusher.base.runAtMainSuspend
import com.smjcco.wxpusher.biz.common.WxpAppDataService

class WxpScanPresenter(view: IWxpScanView) :
    WxpBaseMvpPresenter<IWxpScanView, IWxpScanPresenter>(view),
    IWxpScanPresenter {
    private fun dealFollowResult(followResult: WxpFollowResult) {
        val params = WxpDialogParams()
        params.title = "订阅成功"
        params.message = followResult.msg
        params.leftText = "关闭"
        params.leftBlock = {
            view?.onClosePage()
        }
        params.rightText = "查看详情"
        params.rightBlock = {
            runAtMainSuspend {
                var openId = WxpAppDataService.getLoginInfo()?.openId
                if (openId.isNullOrEmpty()) {
                    openId = WxpApiService.getOpenId()
                    if (!openId.isNullOrEmpty()) {
                        WxpAppDataService.saveOpenId(openId)
                    }
                }
                if (openId.isNullOrEmpty()) {
                    WxpToastUtils.showToast("获取openId失败，请重试")
                    return@runAtMainSuspend
                }
                view?.onOpenWebPage("${WxpConfig.baseUrl}/wxuser/?openId=${openId}&subId=${followResult.subId}#/detail")
            }
        }
        WxpDialogUtils.showDialog(params)
    }

    override fun scan(data: String?) {
        if (data.isNullOrEmpty()) {
            WxpToastUtils.showToast("扫描数据为空")
            return
        }
        runAtMainSuspend {
            val scanResult = WxpApiService.getScanResult(data)
            if (scanResult == null) {
                view?.onClosePage()
                return@runAtMainSuspend
            }
            //是订阅内容
            if (scanResult.type == WxpScanQrcodeResp.TypeSubscribe) {
                if (scanResult.followResult != null) {
                    dealFollowResult(scanResult.followResult)
                    view?.onClosePage()
                } else {
                    WxpToastUtils.showToast("数据异常，请重试")
                    view?.onClosePage()
                }
            } else {
                val params = WxpDialogParams()
                params.title = "扫描内容"
                params.message = data
                params.leftText = "关闭"
                params.leftBlock = {
                    view?.onClosePage()
                }
                params.rightText = "复制"
                params.rightBlock = {
                    view?.onCopy(data)
                    view?.onClosePage()
                }
                WxpDialogUtils.showDialog(params)
            }
        }
    }

}