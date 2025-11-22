package com.smjcco.wxpusher.api

import com.smjcco.wxpusher.base.common.BaseResp
import com.smjcco.wxpusher.base.common.WxpNetworkService
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.base.biz.bean.WxpUpdateInfoReq
import com.smjcco.wxpusher.base.biz.WxpAppPageService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.page.login.WxpAppleLoginReq
import com.smjcco.wxpusher.page.login.WxpAppleLoginResp
import com.smjcco.wxpusher.page.login.WxpLoginSendVerifyCodeReq
import com.smjcco.wxpusher.page.login.WxpLoginSendVerifyCodeResp
import com.smjcco.wxpusher.page.login.WxpWeixinLoginReq
import com.smjcco.wxpusher.page.login.WxpWeixinLoginResp
import com.smjcco.wxpusher.page.messagelist.WxpMessageListMessage
import com.smjcco.wxpusher.page.messagelist.WxpMessageListReq
import com.smjcco.wxpusher.page.scan.WxpScanQrcodeResp
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

class BizError(val code: Int, val msg: String) : RuntimeException()
object WxpApiService {

    suspend fun <T> commonRespDeal(
        block: suspend () -> BaseResp<T>,
        toastError: Boolean = true,
        successBlock: ((data: T) -> Unit)? = null,
        errorBlock: ((e: Throwable) -> Unit)? = null
    ): T? {
        try {
            val resp = withContext(Dispatchers.IO) {
                block()
            }
            if (resp.code == 1000) {
                successBlock?.invoke(resp.data)
                return resp.data
            }

            if (resp.code == 1002) {
                WxpAppPageService.jumpToLogin()
                return null
            }
            if (toastError) {
                WxpToastUtils.showToast(resp.msg)
            }
            errorBlock?.invoke(BizError(resp.code, resp.msg))
        } catch (e: Throwable) {
            e.printStackTrace()
            if (toastError) {
                if (e is IOException) {
                    WxpToastUtils.showToast("请检查网络")
                } else if (e is NoTransformationFoundException) {
                    WxpToastUtils.showToast("数据反序列化异常，可能是服务器出现问题，请稍后再试")
                } else {
                    WxpToastUtils.showToast(e.message)
                }
            }
            errorBlock?.invoke(e)
        }
        return null
    }

    /**
     * 发送验证码
     * @return 发送是否成功，true表示成功，其他表示异常
     */
    suspend fun sendVerifyCode(phone: String): Boolean? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .post(WxpNetworkService.getUrl("/api/device/send-verify-code")) {
                    setBody(mapOf("phone" to phone))
                }.body()
        })
    }

    suspend fun verifyCodeLogin(req: WxpLoginSendVerifyCodeReq): WxpLoginSendVerifyCodeResp? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .post(WxpNetworkService.getUrl("/api/device/verify-code-login")) {
                    setBody(req)
                }.body()
        })
    }

    /**
     * 更新设备的pushToken信息
     */
    suspend fun logout(
        successBlock: (() -> Unit)? = null
    ): Boolean? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .post(WxpNetworkService.getUrl("/api/need-login/device/logout")) {

                }.body()
        }, errorBlock = {
            WxpToastUtils.showToast("失败," + it.message)
        }, successBlock = { successBlock?.invoke() })
    }

    /**
     * 解绑手机号
     */
    suspend fun unbindPhone(
        successBlock: (() -> Unit)? = null
    ): Boolean? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .post(WxpNetworkService.getUrl("/api/need-login/device/unbind")) {

                }.body()
        }, errorBlock = {
            WxpToastUtils.showToast("失败," + it.message)
        }, successBlock = { successBlock?.invoke() })
    }

    /**
     * 通过微信授权码进行登录
     */
    suspend fun weixinLogin(req: WxpWeixinLoginReq): WxpWeixinLoginResp? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .post(WxpNetworkService.getUrl("/api/device/weixin-login")) {
                    setBody(req)
                }.body()
        })
    }

    /**
     * 苹果登录
     */
    suspend fun appleLogin(req: WxpAppleLoginReq): WxpAppleLoginResp? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .post(WxpNetworkService.getUrl("/api/device/apple-login")) {
                    setBody(req)
                }.body()
        })
    }

    /**
     * 更新设备的pushToken信息
     */
    suspend fun updateDeviceInfo(
        req: WxpUpdateInfoReq,
        successBlock: (() -> Unit)? = null
    ): Boolean? {
        if (req.deviceUuid.isNullOrEmpty() || req.pushToken.isNullOrEmpty()) {
            return false
        }
        WxpLogUtils.d(message = "上报设备信息-updateDeviceInfo")
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .put(WxpNetworkService.getUrl("/api/need-login/device/update-device-info")) {
                    setBody(req)
                }.body()
        }, successBlock = { successBlock?.invoke() })
    }

    /**
     * 获取消息列表的数据
     */
    suspend fun fetchMessageList(req: WxpMessageListReq): List<WxpMessageListMessage>? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .get(WxpNetworkService.getUrl("/api/need-login/device/message/list-v2")) {
                    parameter("messageId", req.messageId)
                    parameter("key", req.key)
                    parameter("scene", req.scene)
                }.body()
        })
    }

    /**
     * 标记消息已读状态
     * @param id 消息的消息id，不传表示标记用户的所有消息
     * @param read 是否标记为已读状态
     */
    suspend fun markMessageReadStatus(
        messageId: Long? = null,
        read: Boolean,
        successBlock: (() -> Unit)
    ): Unit? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .put(WxpNetworkService.getUrl("/api/need-login/device/message/read-mark")) {
                    messageId?.let {
                        parameter("messageId", messageId)
                    }
                    parameter("read", read)
                }.body()
        }, successBlock = {
            successBlock.invoke()
        }
        )
    }

    /**
     * 删除消息记录
     * @param id 消息的消息id
     */
    suspend fun deleteMessageById(
        messageId: Long,
        successBlock: (() -> Unit)
    ): Unit? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .delete(WxpNetworkService.getUrl("/api/need-login/device/message/delete")) {
                    parameter("messageId", messageId)
                }.body()
        }, successBlock = {
            successBlock.invoke()
        }
        )
    }

    /**
     * 在用户没有openid的时候，查询一下用户的openid
     */
    suspend fun getOpenId(): String? {
        val data: Map<String?, String?>? = commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .get(WxpNetworkService.getUrl("/api/need-login/device/openid"))
                .body()
        }
        )
        if (data?.isNotEmpty() == true) {
            return data.get("openId")
        }
        return null
    }

    /**
     * 查询扫码结果
     */
    suspend fun getScanResult(data: String): WxpScanQrcodeResp? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .post(WxpNetworkService.getUrl("/api/need-login/device/scan")) {
                    val body = mapOf("data" to data)
                    setBody(body)
                }
                .body()
        }
        )
    }
}