package com.smjcco.wxpusher.page.bind

import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.base.common.runAtMainSuspend
import com.smjcco.wxpusher.base.biz.bean.WxpLoginInfo
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.page.login.WxpLoginSendVerifyCodeReq

class WxpBindPresenter(view: IWxpBindView) :
    WxpBaseMvpPresenter<IWxpBindView, IWxpBindPresenter>(view),
    IWxpBindPresenter {

    override fun queryBindStatus(phone: String?, verifyCode: String?) {
        if (phone.isNullOrEmpty()) {
            WxpToastUtils.showToast("手机号为空，请重新登录")
            return
        }
        if (verifyCode.isNullOrEmpty()) {
            WxpToastUtils.showToast("验证码为空，请重新登录")
            return
        }

        val req = WxpLoginSendVerifyCodeReq(
            phone = phone,
            code = verifyCode,
            deviceId = WxpAppDataService.getLoginInfo()?.deviceId,
            deviceName = WxpBaseInfoService.getDeviceName(),
            pushToken = WxpAppDataService.getPushToken()
        )

        runAtMainSuspend {
            view?.showLoading(true)
            val loginData = WxpApiService.verifyCodeLogin(req)
            view?.showLoading(false)
            loginData?.let {
                if (it.phoneHasRegister) {
                    val loginInfo = WxpLoginInfo(
                        deviceId = it.deviceId,
                        deviceToken = it.deviceToken,
                        uid = it.uid,
                        openId = it.openId
                    )
                    WxpAppDataService.saveLoginInfo(loginInfo)
                    WxpAppDataService.updateDeviceInfo()
                    view?.onGoMain()
                } else {
                    WxpToastUtils.showToast("绑定未完成，请先按步骤绑定 ")
                }
            }
        }
    }
}