package com.smjcco.wxpusher.utils

import com.smjcco.wxpusher.bean.LoginInfo

/**
 * 保存app常用数据
 */
object AppDataUtils {
    private const val SaveLoginInfoKey = "SaveLoginInfoKey"
    private const val PushTokenKey = "PushTokenKey"


    /**
     * 获取登陆信息
     */
    fun getLoginInfo(): LoginInfo? {
        val infoStr = getLoginInfoStr()
        return GsonUtils.toObj(infoStr, LoginInfo::class)
    }

    /**
     * 返回string类型，方便给到容器
     */
    fun getLoginInfoStr(): String? {
        return SaveUtils.getByKey(SaveLoginInfoKey)
    }

    /**
     * 保存登陆信息
     */
    fun saveLoginInfo(loginInfoStr: String?) {
        SaveUtils.setKeyValue(SaveLoginInfoKey, loginInfoStr)
    }

    /**
     * 获取pushToken
     */
    fun getPushToken(): String? = SaveUtils.getByKey(PushTokenKey)

    /**
     * 保存pushToken
     */
    fun savePushToken(pushToken: String?) {
        SaveUtils.setKeyValue(PushTokenKey, pushToken)
    }
}