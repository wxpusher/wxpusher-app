package com.smjcco.wxpusher.api

import com.smjcco.wxpusher.base.BaseResp
import com.smjcco.wxpusher.base.WxpNetworkService
import com.smjcco.wxpusher.base.WxpToastUtils
import com.smjcco.wxpusher.page.login.WxpLoginSendVerifyCodeReq
import com.smjcco.wxpusher.page.login.WxpLoginSendVerifyCodeResp
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class BizError(val code: Int, val msg: String) : RuntimeException()
object WxpApiService {

    suspend fun <T> commonRespDeal(
        block: suspend () -> BaseResp<T>,
        toastError: Boolean = true,
        errorBlock: ((e: Throwable) -> Unit)? = null
    ): T? {
        try {
            val resp = withContext(Dispatchers.IO) {
                block()
            }
            if (resp.code == 1000) {
                return resp.data
            }
            if (toastError) {
                WxpToastUtils.showToast(resp.msg)
            } else {
                errorBlock?.invoke(BizError(resp.code, resp.msg))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            if (toastError) {
                WxpToastUtils.showToast(e.message)
            } else {
                errorBlock?.invoke(e)
            }
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
}