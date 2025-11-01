package com.smjcco.wxpusher.thrid.weixin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.Keep
import androidx.core.graphics.scale
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXImageObject
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXTextObject
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.modelpay.PayResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import java.io.ByteArrayOutputStream


// 回调接口定义
typealias AuthCompletion = (WeChatAuthResponse?, WeChatError?) -> Unit
typealias ShareCompletion = (Boolean, WeChatError?) -> Unit
typealias PayCompletion = (PayResp?, WeChatError?) -> Unit

/**
 * 微信OpenSDK管理类
 *
 * 功能包括：
 * - 微信授权登录
 * - 分享功能（文本、图片、网页）
 * - 微信支付
 */
object WxpWeixinOpenManager : IWXAPIEventHandler {

    private const val TAG = "WxpWeixinOpenManager"

    // 微信AppID（从iOS代码中获取）
    private const val APP_ID = "wx906c2d32c1b1a115"

    // 回调变量
    private var authCompletion: AuthCompletion? = null
    private var shareCompletion: ShareCompletion? = null
    private var payCompletion: PayCompletion? = null

    // IWXAPI实例
    private var api: IWXAPI? = null

    /**
     * 初始化微信SDK
     * 应该在Application的onCreate中调用
     */
    fun init(context: Context) {
        if (api == null) {
            // 通过WXAPIFactory工厂，获取IWXAPI的实例
            api = WXAPIFactory.createWXAPI(context, APP_ID, true)
            // 将应用的appId注册到微信
            val isSuccess = api?.registerApp(APP_ID) ?: false
            WxpLogUtils.i(TAG, "微信SDK注册结果: ${isSuccess}")
        }
    }

    /**
     * 获取IWXAPI实例
     */
    fun getApi(): IWXAPI? = api

    /**
     * 检查是否安装了微信
     */
    fun isWeChatInstalled(): Boolean {
        return api?.isWXAppInstalled() ?: false
    }

    /**
     * 获取微信API支持版本
     */
    fun getWXAppSupportAPI(): Int? {
        return api?.wxAppSupportAPI
    }

    // ========== 授权登录相关 ==========

    /**
     * 发起微信授权登录
     * @param scope 授权范围，默认snsapi_userinfo
     * @param state 状态参数
     * @param completion 完成回调
     */
    fun requestAuth(
        scope: String = "snsapi_userinfo",
        state: String = "wechat_auth",
        completion: AuthCompletion
    ) {
        if (!isWeChatInstalled()) {
            completion(null, WeChatError.NotInstalled)
            return
        }

        authCompletion = completion
        val req = SendAuth.Req().apply {
            this.scope = scope
            this.state = state
        }

        val success = api?.sendReq(req) ?: false
        if (!success) {
            authCompletion = null
            completion(null, WeChatError.SendRequestFailed)
        }
    }

    // ========== 分享相关 ==========

    /**
     * 分享文本到微信
     * @param text 分享文本
     * @param scene 分享场景：0-聊天界面，1-朋友圈，2-收藏 (SendMessageToWX.Req.WXScenexxx)
     * @param completion 完成回调
     */
    fun shareText(text: String, scene: Int, completion: ShareCompletion) {
        shareCompletion = completion
        val textObj = WXTextObject().apply {
            this.text = text
        }

        val msg = WXMediaMessage(textObj).apply {
            this.description = text
        }

        val req = SendMessageToWX.Req().apply {
            this.scene = scene
            this.message = msg
        }

        val success = api?.sendReq(req) ?: false
        if (!success) {
            shareCompletion = null
            completion(false, WeChatError.SendRequestFailed)
        }
    }

    /**
     * 分享图片到微信
     * @param imageBitmap 图片Bitmap
     * @param scene 分享场景：0-聊天界面，1-朋友圈，2-收藏 (SendMessageToWX.Req.WXScenexxx)
     * @param completion 完成回调
     */
    fun shareImage(imageBitmap: Bitmap, scene: Int, completion: ShareCompletion) {
        shareCompletion = completion


        // 将Bitmap转换为byte数组
        val outputStream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val imageData = outputStream.toByteArray()
        outputStream.close()

        val imageObject = WXImageObject().apply {
            this.imageData = imageData
        }

        // 创建缩略图
        val thumbBmp = imageBitmap.scale(150, 150)

        val message = WXMediaMessage().apply {
            mediaObject = imageObject
            setThumbImage(thumbBmp)
        }

        val req = SendMessageToWX.Req().apply {
            this.message = message
            this.scene = scene
        }

        val success = api?.sendReq(req) ?: false
        if (!success) {
            shareCompletion = null
            completion(false, WeChatError.SendRequestFailed)
        }
    }

    /**
     * 分享图片到微信（通过图片路径）
     * @param imagePath 图片路径
     * @param scene 分享场景：0-聊天界面，1-朋友圈，2-收藏
     * @param completion 完成回调
     */
    fun shareImage(imagePath: String, scene: Int, completion: ShareCompletion) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        if (bitmap == null) {
            completion(false, WeChatError.InvalidImage)
            return
        }
        shareImage(bitmap, scene, completion)
    }

    /**
     * 分享网页到微信
     * @param url 网页链接
     * @param title 标题
     * @param description 描述
     * @param thumbBitmap 缩略图Bitmap
     * @param scene 分享场景：0-聊天界面，1-朋友圈，2-收藏
     * @param completion 完成回调
     */
    fun shareWebPage(
        url: String,
        title: String,
        description: String,
        thumbBitmap: Bitmap,
        scene: Int,
        completion: ShareCompletion
    ) {
        shareCompletion = completion

        val webPageObject = WXWebpageObject().apply {
            webpageUrl = url
        }

        val message = WXMediaMessage().apply {
            mediaObject = webPageObject
            this.title = title
            this.description = description

            // 设置缩略图
            val thumbBmp = thumbBitmap.scale(150, 150)
            setThumbImage(thumbBmp)
        }

        val req = SendMessageToWX.Req().apply {
            this.message = message
            this.scene = scene
        }

        val success = api?.sendReq(req) ?: false
        if (!success) {
            shareCompletion = null
            completion(false, WeChatError.SendRequestFailed)
        }
    }

    // ========== 支付相关 ==========

    /**
     * 发起微信支付
     * @param paymentParams 支付参数Map
     * @param completion 完成回调
     */
    fun requestPayment(paymentParams: Map<String, Any?>, completion: PayCompletion) {
        // 验证必要的参数
        val prepayId = paymentParams["prepayid"] as? String
        val sign = paymentParams["paySign"] as? String
        val timeStampString = paymentParams["timeStamp"] as? String
        val nonceStr = paymentParams["nonceStr"] as? String
        val partnerId = paymentParams["partnerid"] as? String
        val packageValue = paymentParams["package"] as? String

        if (prepayId.isNullOrEmpty() || sign.isNullOrEmpty() ||
            timeStampString.isNullOrEmpty() || nonceStr.isNullOrEmpty() ||
            partnerId.isNullOrEmpty() || packageValue.isNullOrEmpty()
        ) {
            completion(null, WeChatError.UnknownError("支付参数格式错误"))
            return
        }

        val timeStamp: UInt = timeStampString.toUIntOrNull() ?: run {
            completion(null, WeChatError.UnknownError("时间戳格式错误"))
            return
        }

        // 检查微信是否已安装和支持
        if (!isWeChatInstalled()) {
            completion(null, WeChatError.NotInstalled)
            return
        }

        payCompletion = completion

        val payReq = PayReq().apply {
            this.appId = APP_ID
            this.partnerId = partnerId
            this.prepayId = prepayId
            this.nonceStr = nonceStr
            this.timeStamp = timeStamp.toString()
            this.packageValue = packageValue
            this.sign = sign
        }

        val success = api?.sendReq(payReq) ?: false
        if (!success) {
            payCompletion = null
            completion(null, WeChatError.SendRequestFailed)
        }
    }

    // ========== IWXAPIEventHandler 实现 ==========

    /**
     * 收到微信的请求
     * 注意：此方法需要在WXEntryActivity中调用api.handleIntent()
     */
    override fun onReq(req: BaseReq?) {
        WxpLogUtils.d(TAG, "收到微信请求: $req")
    }

    /**
     * 收到微信的响应
     * 注意：此方法需要在WXEntryActivity中调用api.handleIntent()
     */
    override fun onResp(resp: BaseResp?) {
        WxpLogUtils.d(
            TAG,
            "收到微信响应: $resp, errCode: ${resp?.errCode}, errStr: ${resp?.errStr}"
        )

        when (resp) {
            is SendAuth.Resp -> handleAuthResponse(resp)
            is SendMessageToWX.Resp -> handleShareResponse(resp)
            is PayResp -> handlePayResponse(resp)
            else -> WxpLogUtils.d(TAG, "未知类型的微信响应")
        }
    }

    /**
     * 处理授权响应
     */
    private fun handleAuthResponse(response: SendAuth.Resp) {
        if (response.errCode == 0 && !response.code.isNullOrEmpty()) {
            val authResponse = WeChatAuthResponse(
                code = response.code!!,
                state = response.state,
                url = response.url,
                lang = response.lang,
                country = response.country
            )
            authCompletion?.let {
                it.invoke(authResponse, null)
                authCompletion = null
            }
        } else {
            val error = WeChatError.fromErrorCode(response.errCode, response.errStr)
            authCompletion?.let {
                it.invoke(null, error)
                authCompletion = null
            }
        }
    }

    /**
     * 处理分享响应
     */
    private fun handleShareResponse(response: SendMessageToWX.Resp) {
        if (response.errCode == 0) {
            shareCompletion?.let {
                it.invoke(true, null)
                shareCompletion = null
            }
        } else {
            val error = WeChatError.fromErrorCode(response.errCode, response.errStr)
            shareCompletion?.let {
                it.invoke(false, error)
                shareCompletion = null
            }
        }
    }

    /**
     * 处理支付响应
     */
    private fun handlePayResponse(response: PayResp) {
        if (response.errCode == 0) {
            payCompletion?.let {
                it.invoke(response, null)
                payCompletion = null
            }
        } else {
            val error = WeChatError.fromErrorCode(response.errCode, response.errStr)
            payCompletion?.let {
                it.invoke(null, error)
                payCompletion = null
            }
        }
    }
}

// ========== 数据模型 ==========

/**
 * 微信授权响应
 */
@Keep
data class WeChatAuthResponse(
    val code: String,
    val state: String? = null,
    val url: String? = null,
    val lang: String? = null,
    val country: String? = null
)

/**
 * 微信错误类型
 */
@Keep
sealed class WeChatError(message: String) : Exception(message) {
    object NotInstalled : WeChatError("未安装微信")

    object UnsupportedVersion : WeChatError("微信版本过低")
    object SendRequestFailed : WeChatError("发送请求失败")
    object UserCancel : WeChatError("用户取消")
    object AuthDenied : WeChatError("授权被拒绝")
    object InvalidImage : WeChatError("图片无效")
    object PaymentFailed : WeChatError("支付失败")
    class UnknownError(msg: String) : WeChatError(msg)

    companion object {
        fun fromErrorCode(code: Int, errorMessage: String?): WeChatError {
            return when (code) {
                -2 -> UserCancel
                -4 -> AuthDenied
                -6 -> UnknownError("签名错误: $errorMessage")
                else -> UnknownError(errorMessage ?: "未知错误: $code")
            }
        }
    }
}

