package com.smjcco.wxpusher.api

import android.util.Log
import androidx.annotation.Keep
import com.google.gson.reflect.TypeToken
import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.DateUtils
import com.smjcco.wxpusher.utils.GsonUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.utils.WxPusherUtils
import com.smjcco.wxpusher.web.WxPusherWebInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.Collections

object DeviceApi {
    private const val TAG = "DeviceApi"
    private const val UpDateTokenDate = "UpDateTokenDate"
    private val client = OkHttpClient()

    //记录一下上报的数据，如果变化，就不用再次上报
    private var updateDataCache = "";

    fun updateDeviceInfoAsync(platform: DevicePlatform?) {
        WxPusherUtils.getIoScopeScope().launch {
            updateDeviceInfo(platform)
        }
    }

    /**
     * 获取订单的列表
     * 主要用来创建通知的channel
     */
    suspend fun getSubscribeList(): List<SubscribeListItem> {
        val deviceToken = AppDataUtils.getLoginInfo()?.deviceToken
        if (deviceToken.isNullOrEmpty()) {
            WxPusherLog.i(TAG, "getSubscribeList: 没有deviceToken")
            return Collections.emptyList()
        }

        try {
            return withContext(Dispatchers.IO) {
                val url = "${WxPusherConfig.ApiUrl}/api/need-login/device/subscribe-list"
                val request = Request.Builder()
                    .url(url)
                    .method("GET", null)
                    .header("deviceToken", deviceToken)
                    .build()
                client.newCall(request).execute().use { response: Response ->
                    val status = response.isSuccessful
                    if (!status) {
                        return@use Collections.emptyList()
                    }
                    val bodyByte = response.body?.bytes()
                    val bodyStr = bodyByte?.let { String(it) }
                    WxPusherLog.i(TAG, "获取订阅列表,结果=${bodyStr}")
                    val type = object : TypeToken<BaseResp<List<SubscribeListItem>>>() {}.type
                    val respData: BaseResp<List<SubscribeListItem>>? =
                        GsonUtils.toObj(bodyStr, type)
                    if (respData?.isSuccess() == true) {
                        return@use respData.data
                    }
                    return@use Collections.emptyList()
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return Collections.emptyList()
    }

    /**
     * 更新设备token
     */
    private suspend fun updateDeviceInfo(platform: DevicePlatform?): Boolean {
        val deviceToken = AppDataUtils.getLoginInfo()?.deviceToken
        if (deviceToken.isNullOrEmpty()) {
            WxPusherLog.i(TAG, "updateDeviceInfo: 没有deviceToken")
            return false
        }
        val pushToken = AppDataUtils.getPushToken()
        if (pushToken.isNullOrEmpty()) {
            WxPusherLog.i(TAG, "updateDeviceInfo: 没有pushToken")
            return false
        }
        val deviceUuid = AppDataUtils.getLoginInfo()?.deviceId
        if (deviceUuid.isNullOrEmpty()) {
            WxPusherLog.i(TAG, "updateDeviceInfo: 没有deviceUuid")
            return false
        }
        val reqBody = GsonUtils.toJson(
            UpdateDeviceInfoReq(
                deviceUuid, pushToken,
                platform?.getPlatform()
            )
        )
        if (reqBody == updateDataCache && SaveUtils.getByKey(UpDateTokenDate) == DateUtils.getDate()) {
            WxPusherLog.i(TAG, "updateDeviceInfo: 数据为变更，无需上报")
            return false
        }
        try {
            return withContext(Dispatchers.IO) {
                val url = "${WxPusherConfig.ApiUrl}/api/need-login/device/update-device-info"
                val requestBody = reqBody.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .header("deviceToken", deviceToken)
                    .put(requestBody)
                    .build()
                WxPusherLog.i(TAG, "updateDeviceInfo: 开始上报更新PT,${reqBody}")
                client.newCall(request).execute().use { response: Response ->
                    val status = response.isSuccessful
                    if (!status) {
                        return@use false
                    }
                    val bodyByte = response.body?.bytes()
                    val bodyStr = bodyByte?.let { String(it) }
                    WxPusherLog.i(TAG, "updateDeviceInfo: 上报更新PT,结果=${bodyStr}")
                    val respData = GsonUtils.toObj(bodyStr, BaseResp::class)
                    if (respData?.isSuccess() == true) {
                        SaveUtils.setKeyValue(UpDateTokenDate, DateUtils.getDate())
                        updateDataCache = reqBody
                        return@use true
                    }
                    return@use false
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return false
    }

    @Keep
    data class UpdateDeviceInfoReq(
        val deviceUuid: String,
        val pushToken: String,
        val platform: String?
    )
}