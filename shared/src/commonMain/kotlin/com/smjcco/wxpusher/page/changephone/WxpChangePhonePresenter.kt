package com.smjcco.wxpusher.page.changephone

import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.WxpLoadingUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.base.common.runAtMainSuspend
import kotlinx.coroutines.delay

class WxpChangePhonePresenter(view: IWxpChangePhoneView) :
    WxpBaseMvpPresenter<IWxpChangePhoneView, IWxpChangePhonePresenter>(view),
    IWxpChangePhonePresenter {
    //是否可以发送验证码
    private var canSendVerifyCode = true

    fun sendTimeWait() {
        runAtMainSuspend {
            canSendVerifyCode = false
            for (i in 120 downTo 0) {
                if (i <= 0) {
                    canSendVerifyCode = true
                    view?.onSendButtonText("发送验证码")
                    break
                }
                view?.onSendButtonText("${i}S")
                delay(1000)
            }
        }
    }

    /**
     * 发送验证码
     */
    override fun sendVerifyCode(phone: String?) {
        if (!canSendVerifyCode) {
            return
        }
        if (phone.isNullOrEmpty()) {
            WxpToastUtils.showToast("请输入手机号")
            return
        }
        if (phone == WxpAppDataService.getLoginInfo()?.phone) {
            WxpToastUtils.showToast("已经绑定到当前手机号码")
            return
        }
        runAtMainSuspend {
            view?.onSendButtonText("发送中")
            if (WxpApiService.sendVerifyCode(phone) == true) {
                WxpToastUtils.showToast("发送成功")
                sendTimeWait()
            } else {
                view?.onSendButtonText("发送验证码")
            }
        }
    }

    override fun bindPhone(phone: String?, verifyCode: String?) {
        if (phone.isNullOrEmpty()) {
            WxpToastUtils.showToast("请输入手机号")
            return
        }
        if (verifyCode.isNullOrEmpty()) {
            WxpToastUtils.showToast("请输入验证码")
            return
        }
        if (verifyCode.length != 6) {
            WxpToastUtils.showToast("验证码错误")
            return
        }
        WxpLogUtils.i(message = "换绑手机号，phone=${phone}")
        val req = WxpPhoneBindReq(phone = phone, code = verifyCode)

        WxpLoadingUtils.showLoading(msg = "处理中...")
        runAtMainSuspend {
            val result = WxpApiService.phoneBind(req)
            WxpLoadingUtils.dismissLoading()
            if (result == true) {
                WxpToastUtils.showToast("绑定手机号成功")
                //修改本地保存的手机号
                val loginInfo = WxpAppDataService.getLoginInfo()
                loginInfo?.let {
                    it.phone = phone
                    WxpAppDataService.saveLoginInfo(loginInfo)
                    view?.onChangPhoneFinish()
                }
            }
        }
    }
}