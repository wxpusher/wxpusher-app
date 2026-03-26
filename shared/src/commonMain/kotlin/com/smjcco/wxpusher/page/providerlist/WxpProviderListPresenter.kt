package com.smjcco.wxpusher.page.providerlist

import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.common.WxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.base.common.runAtMainSuspend
import com.smjcco.wxpusher.base.biz.WxpAppDataService

class WxpProviderListPresenter(view: IWxpProviderListView) :
    WxpBaseMvpPresenter<IWxpProviderListView, IWxpProviderListPresenter>(view),
    IWxpProviderListPresenter {
    override fun loadPage() {
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
            view?.onLoadPage("${WxpConfig.appFeUrl}/app#/market")
        }
    }
}