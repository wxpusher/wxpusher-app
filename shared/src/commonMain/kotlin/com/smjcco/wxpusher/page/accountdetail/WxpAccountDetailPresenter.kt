package com.smjcco.wxpusher.page.accountdetail

import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.biz.WxpAppPageService
import com.smjcco.wxpusher.base.common.WxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.WxpDialogParams
import com.smjcco.wxpusher.base.common.WxpDialogUtils
import com.smjcco.wxpusher.base.common.WxpLoadingUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.base.common.runAtMainSuspend

class WxpAccountDetailPresenter(view: IWxpAccountDetailView) :
    WxpBaseMvpPresenter<IWxpAccountDetailView, IWxpAccountDetailPresenter>(view),
    IWxpAccountDetailPresenter {
    override fun weixinBind(code: String?) {
        WxpLogUtils.i(message = "绑定微信账号，code=${code}")
        if (code.isNullOrEmpty()) {
            WxpToastUtils.showToast("微信授权码为空")
            return
        }
        val weixinBindReq = WxpWeixinBindReq(code)

        runAtMainSuspend {
            WxpLoadingUtils.showLoading(msg = "绑定中...")
            val success = WxpApiService.weixinBind(weixinBindReq)
            WxpLoadingUtils.dismissLoading()
            success?.let {
                val loginInfo = WxpAppDataService.getLoginInfo()
                loginInfo?.let {
                    it.weiXinBind = true
                    WxpAppDataService.saveLoginInfo(it)
                    view?.onWeixinBindSuccess()
                }
            }
        }
    }


    override fun appleBind(code: String?, userId: String?, email: String?, name: String?) {
        WxpLogUtils.i(message = "绑定苹果账号，code=${code}")
        if (code.isNullOrEmpty()) {
            WxpToastUtils.showToast("苹果授权为空")
            return
        }
        val appleBindReq = WxpAppleBindReq(code, name)

        runAtMainSuspend {
            WxpLoadingUtils.showLoading(msg = "绑定中...")
            val success = WxpApiService.appleBind(appleBindReq)
            WxpLoadingUtils.dismissLoading()
            success?.let {
                val loginInfo = WxpAppDataService.getLoginInfo()
                loginInfo?.let {
                    it.appleBind = true
                    WxpAppDataService.saveLoginInfo(it)
                    view?.onAppleBindSuccess()
                }
            }
        }
    }

    override fun logout() {
        val params = WxpDialogParams()
        params.title = "退出当前账号吗？"
        params.message = "退出后需要重新登录才可以接收消息"
        params.leftText = "取消"
        params.rightText = "退出账号"
        params.rightBlock = {
            runAtMainSuspend {
                WxpLoadingUtils.showLoading(msg = "退出中...")
                WxpApiService.logout {
                    WxpLoadingUtils.dismissLoading()
                    //删除本地的deviceToken
                    WxpAppDataService.getLoginInfo()?.let {
                        it.deviceToken = null
                        WxpAppDataService.saveLoginInfo(it)
                    }
                    WxpAppPageService.jumpToLogin()
                }
            }
        }
        WxpDialogUtils.showDialog(params)
    }
}