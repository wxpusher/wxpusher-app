package com.smjcco.wxpusher.page.login

import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.biz.bean.WxpLoginInfo
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.WxpLoadingUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.base.common.runAtIOSuspend
import com.smjcco.wxpusher.base.common.runAtMainSuspend
import kotlinx.coroutines.delay

class WxpLoginPresenter(view: IWxpLoginView) :
    WxpBaseMvpPresenter<IWxpLoginView, IWxpLoginPresenter>(view),
    IWxpLoginPresenter {
    //是否可以发送验证码
    private var canSendVerifyCode = true
    override fun init() {
        view?.onSendButtonText("发送验证码", false)

        //在iOS上，打开登录页面的时候，发送一个网络请求，触发网络访问授权弹出，避免登录的时候再弹出，打断登录流程
        if (WxpBaseInfoService.getPlatform() == "iOS") {
            runAtIOSuspend {
                WxpApiService.sendLoginPing()
            }
        }
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
        WxpLogUtils.i(message = "手机登录，phone=${phone}")
        val req = WxpLoginSendVerifyCodeReq(
            justCreateAccount = false,
            phone = phone,
            code = verifyCode,
            deviceId = WxpAppDataService.getLoginInfo()?.deviceId,
            deviceName = WxpBaseInfoService.getDeviceName(),
            pushToken = WxpAppDataService.getPushToken()
        )

        WxpLoadingUtils.showLoading(msg = "登录中...")
        runAtMainSuspend {
            val loginData = WxpApiService.verifyCodeLogin(req)
            WxpLoadingUtils.dismissLoading()
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
                    WxpLogUtils.i(message = "手机登录，用户未注册")
                    view?.onGoBindOrCreateAccount(
                        WxpBindPageData(
                            phoneLogin = WxpPhoneBind(
                                phone, verifyCode, it.phoneVerifyCode
                            )
                        )
                    )
                }
            }
        }
    }

    override fun wexinLogin(code: String?) {
        WxpLogUtils.i(message = "微信登录，code=${code}")
        if (code.isNullOrEmpty()) {
            WxpToastUtils.showToast("微信授权码错误")
            return
        }
        val weixinLoginReq = WxpWeixinLoginReq(
            code,
            bindCode = null,
            appleLoginJwtCode = null,
            appleName = null,
            deviceId = WxpAppDataService.getLoginInfo()?.deviceId,
            deviceName = WxpBaseInfoService.getDeviceName(),
            pushToken = WxpAppDataService.getPushToken()
        )

        runAtMainSuspend {
            WxpLoadingUtils.showLoading(msg = "验证中...")
            val loginData = WxpApiService.weixinLogin(weixinLoginReq)
            WxpLoadingUtils.dismissLoading()
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

    override fun appleLogin(code: String?, userId: String?, email: String?, name: String?) {
        WxpLogUtils.i(message = "苹果登录，code=${code}")
        if (code.isNullOrEmpty()) {
            WxpToastUtils.showToast("苹果登录信息为空")
            return
        }
        val appleLoginReq = WxpAppleLoginReq(
            justCreateAccount = false,
            code = code,
            name = name,
            deviceId = WxpAppDataService.getLoginInfo()?.deviceId,
            deviceName = WxpBaseInfoService.getDeviceName(),
            pushToken = WxpAppDataService.getPushToken()
        )

        runAtMainSuspend {
            WxpLoadingUtils.showLoading(msg = "验证中...")
            val loginData = WxpApiService.appleLogin(appleLoginReq)
            WxpLoadingUtils.dismissLoading()
            loginData?.let {
                if (it.hasRegister == true) {
                    WxpLogUtils.i(message = "苹果登录，用户已经注册")
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
                    WxpLogUtils.i(message = "苹果登录，用户未注册")
                    view?.onGoBindOrCreateAccount(
                        WxpBindPageData(
                            appleLogin = WxpAppleBind(
                                code,
                                name
                            )
                        )
                    )
                }

            }
        }
    }
}