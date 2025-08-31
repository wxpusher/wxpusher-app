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
            view?.onClosePage()
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
                } else {
                    WxpToastUtils.showToast("数据异常，请重试")
                    view?.onClosePage()
                }
            } else if (scanResult.type == WxpScanQrcodeResp.TypeOpenUrlWithConfirm) {
                if (scanResult.data.isNullOrEmpty()) {
                    view?.onClosePage()
                    WxpToastUtils.showToast("返回数据错误，无法打开")
                } else {
                    val params = WxpDialogParams()
                    params.title = "扫描成功"
                    params.message = scanResult.data
                    params.leftText = "关闭"
                    params.leftBlock = {
                        view?.onClosePage()
                    }
                    params.rightText = "打开"
                    params.rightBlock = {
                        view?.onClosePage()
                        view?.onOpenWebPage(scanResult.data)
                    }
                    WxpDialogUtils.showDialog(params)
                }
            } else if (scanResult.type == WxpScanQrcodeResp.TypeOpenUrl) {
                if (scanResult.data.isNullOrEmpty()) {
                    view?.onClosePage()
                    WxpToastUtils.showToast("返回数据错误，无法打开")
                } else {
                    view?.onClosePage()
                    view?.onOpenWebPage(scanResult.data)
                }
            } else {
                val params = WxpDialogParams()
                params.title = "扫描成功"
                params.message = scanResult.data
                params.leftText = "关闭"
                params.leftBlock = {
                    view?.onClosePage()
                }
                params.rightText = "复制"
                params.rightBlock = {
                    view?.onCopy(scanResult.data ?: "")
                    view?.onClosePage()
                }
                WxpDialogUtils.showDialog(params)
            }
        }
    }

}