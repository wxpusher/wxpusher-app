package com.smjcco.wxpusher.base

expect fun WxpBaseInfoService_getAppVersionName(): String
expect fun WxpBaseInfoService_getDeviceName(): String

object WxpBaseInfoService {
    /**
     * 获取app版本信息
     */
    fun getAppVersionName() = WxpBaseInfoService_getAppVersionName()

    /**
     * 获取设备名称
     */
    fun getDeviceName() = WxpBaseInfoService_getDeviceName()

}