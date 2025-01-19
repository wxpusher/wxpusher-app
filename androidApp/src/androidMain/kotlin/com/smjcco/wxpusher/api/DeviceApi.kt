package com.smjcco.wxpusher.api

import android.util.Log
import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.GsonUtils
import com.smjcco.wxpusher.utils.WxPusherUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

object DeviceApi {
    private const val TAG = "DeviceApi"
    private val client = OkHttpClient()
    private const val BASE_URL = WxPusherConfig.ApiUrl // 替换为你的服务器地址

    //记录一下上报的数据，如果变化，就不用再次上报
    private var updateDataCache = "";

    fun updateDeviceInfoAsync() {
        WxPusherUtils.getIoScopeScope().launch {
            updateDeviceInfo()
        }
    }

    /**
     * 更新设备token
     */
    private suspend fun updateDeviceInfo(): Boolean {
        val deviceToken = AppDataUtils.getLoginInfo()?.deviceToken
        if (deviceToken.isNullOrEmpty()) {
            Log.d(TAG, "updateDeviceInfo: 没有deviceToken")
            return false
        }
        val pushToken = AppDataUtils.getPushToken()
        if (pushToken.isNullOrEmpty()) {
            Log.d(TAG, "updateDeviceInfo: 没有pushToken")
            return false
        }
        val deviceUuid = AppDataUtils.getLoginInfo()?.deviceId
        if (deviceUuid.isNullOrEmpty()) {
            Log.d(TAG, "updateDeviceInfo: 没有deviceUuid")
            return false
        }
        val reqBody = GsonUtils.toJson(UpdateDeviceInfoReq(deviceUuid, pushToken))
        if (reqBody == updateDataCache) {
            Log.d(TAG, "updateDeviceInfo: 数据为变更，无需上报")
            return false
        }
        try {
            return withContext(Dispatchers.IO) {
                val url = "$BASE_URL/api/need-login/device/update-device-info"
                val requestBody = reqBody.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .header("deviceToken", deviceToken)
                    .put(requestBody)
                    .build()
                Log.d(TAG, "updateDeviceInfo: 开始上报更新PT,${reqBody}")
                client.newCall(request).execute().use { response: Response ->
                    val status = response.isSuccessful
                    if (!status) {
                        return@use false
                    }
                    val bodyByte = response.body?.bytes()
                    val bodyStr = bodyByte?.let { String(it) }
                    Log.d(TAG, "updateDeviceInfo: 上报更新PT,结果=${bodyStr}")
                    val respData = GsonUtils.toObj(bodyStr, BaseResp::class)
                    if (respData?.isSuccess() == true) {
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

    data class UpdateDeviceInfoReq(
        val deviceUuid: String,
        val pushToken: String
    )
}