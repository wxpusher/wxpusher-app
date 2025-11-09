package com.smjcco.wxpusher.page.login

import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.base.common.runAtMainSuspend
import com.smjcco.wxpusher.base.biz.bean.WxpLoginInfo
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import kotlinx.coroutines.delay

class WxpLoginPresenter(view: IWxpLoginView) :
    WxpBaseMvpPresenter<IWxpLoginView, IWxpLoginPresenter>(view),
    IWxpLoginPresenter {
    //是否可以发送验证码
    private var canSendVerifyCode = true
    override fun init() {
        view?.onSendButtonText("发送验证码", false)
    }

    fun sendTimeWait() {
        runAtMainSuspend {
            canSendVerifyCode = false
            for (i in 120 downTo 0) {
                if (i <= 0) {
                    canSendVerifyCode = true
                    view?.onSendButtonText("发送验证码", false)
                    break
                }
                view?.onSendButtonText("${i}S", false)
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
        runAtMainSuspend {
            view?.onSendButtonText("发送中", true)
            if (WxpApiService.sendVerifyCode(phone) == true) {
                WxpToastUtils.showToast("发送成功")
                sendTimeWait()
            } else {
                view?.onSendButtonText("发送验证码", false)
            }
        }
    }

    /**
     * 通过验证码登录
     */
    override fun verifyCodeLogin(
        phone: String?,
        verifyCode: String?
    ) {
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

        val req = WxpLoginSendVerifyCodeReq(
            phone = phone,
            code = verifyCode,
            deviceId = WxpAppDataService.getLoginInfo()?.deviceId,
            deviceName = WxpBaseInfoService.getDeviceName(),
            pushToken = WxpAppDataService.getPushToken()
        )

        runAtMainSuspend {
            val loginData = WxpApiService.verifyCodeLogin(req)
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
                    view?.onGoBind(phone = phone, code = verifyCode, data = it)
                }
            }
        }
    }

    override fun wexinLogin(code: String?) {
        if (code.isNullOrEmpty()) {
            WxpToastUtils.showToast("微信授权码错误")
            return
        }
        val weixinLoginReq = WxpWeixinLoginReq(
            code, null,
            deviceId = WxpAppDataService.getLoginInfo()?.deviceId,
            deviceName = WxpBaseInfoService.getDeviceName(),
            pushToken = WxpAppDataService.getPushToken()
        )
        
        runAtMainSuspend {
            val loginData = WxpApiService.weixinLogin(weixinLoginReq)
            loginData?.let {
                val loginInfo = WxpLoginInfo(
                    deviceId = it.deviceId,
                    deviceToken = it.deviceToken,
                    uid = it.uid,
                    openId = it.openId
                )
                WxpAppDataService.saveLoginInfo(loginInfo)
                WxpAppDataService.updateDeviceInfo()
                view?.onGoMain()
            }
        }

    }
}