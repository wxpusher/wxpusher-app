package com.smjcco.wxpusher.api

import com.smjcco.wxpusher.base.BaseResp
import com.smjcco.wxpusher.base.WxpNetworkService
import com.smjcco.wxpusher.base.WxpToastUtils
import com.smjcco.wxpusher.page.login.WxpLoginSendVerifyCodeReq
import com.smjcco.wxpusher.page.login.WxpLoginSendVerifyCodeResp
import com.smjcco.wxpusher.page.messagelist.WxpMessageListMessage
import com.smjcco.wxpusher.page.messagelist.WxpMessageListReq
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
            if (toastError) {
                WxpToastUtils.showToast(resp.msg)
            }
            errorBlock?.invoke(BizError(resp.code, resp.msg))
        } catch (e: Throwable) {
            e.printStackTrace()
            if (toastError) {
                if (e is IOException) {
                    WxpToastUtils.showToast("请检查网络")
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
     * 获取消息列表的数据
     */
    suspend fun fetchMessageList(req: WxpMessageListReq): List<WxpMessageListMessage>? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .get(WxpNetworkService.getUrl("/api/need-login/device/message/list-v2")) {
                    parameter("lastUserReceiveRecordId", req.lastUserReceiveRecordId)
                    parameter("key", req.key)
                }.body()
        })
    }

    /**
     * 标记消息已读状态
     * @param id 消息的消息id，不传表示标记用户的所有消息
     * @param read 是否标记为已读状态
     */
    suspend fun markMessageReadStatus(
        id: Long? = null,
        read: Boolean,
        successBlock: (() -> Unit)
    ): Unit? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .put(WxpNetworkService.getUrl("/api/need-login/device/message/read-mark")) {
                    id?.let {
                        parameter("id", id)
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
        id: Long,
        successBlock: (() -> Unit)
    ): Unit? {
        return commonRespDeal(block = {
            return@commonRespDeal WxpNetworkService.getWxpHttpClient()
                .delete(WxpNetworkService.getUrl("/api/need-login/device/message/delete")) {
                    parameter("id", id)
                }.body()
        }, successBlock = {
            successBlock.invoke()
        }
        )
    }
}