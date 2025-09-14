package com.smjcco.wxpusher.base

expect fun WxpBaseInfoService_getAppVersionName(): String
expect fun WxpBaseInfoService_getDeviceName(): String

interface IWxpBaseInfoServiceListener {
    /**
     * 获取平台
     */
    fun getPlatform(): String
}

object WxpBaseInfoService {
    private lateinit var baseInfoListener: IWxpBaseInfoServiceListener
    fun init(listener: IWxpBaseInfoServiceListener) {
        this.baseInfoListener = listener
    }

    /**
     * 获取app版本信息
     */
    fun getAppVersionName() = WxpBaseInfoService_getAppVersionName()

    /**
     * 获取设备名称
     */
    fun getDeviceName() = WxpBaseInfoService_getDeviceName()

    /**
     * 获取平台信息
     */
    fun getPlatform() = baseInfoListener.getPlatform()

}