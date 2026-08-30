package com.smjcco.wxpusher.base.common

expect fun WxpBaseInfoService_getAppVersionName(): String
expect fun WxpBaseInfoService_getDeviceName(): String

interface IWxpBaseInfoServiceListener {
    /**
     * 获取客户端操作系统平台。
     *
     * 该值只描述应用运行环境，例如 Android、iOS、HarmonyOS，不能用于判断
     * Android 当前选择的是厂商推送还是 WxPusher 自建链接。
     */
    fun getClientPlatform(): String

    /**
     * 获取当前用于后端消息分发的推送路由平台。
     *
     * Android 会根据实际生效通道返回 Android_Xiaomi、Android_Huawei 或 Android 等；
     * iOS 和 HarmonyOS 分别固定返回 iOS、HarmonyOS。
     */
    fun getEffectivePushPlatform(): String
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

    /** 获取客户端操作系统平台，不包含具体推送通道信息。 */
    fun getClientPlatform() = baseInfoListener.getClientPlatform()

    /** 获取当前实际生效、用于后端消息分发的推送路由平台。 */
    fun getEffectivePushPlatform() = baseInfoListener.getEffectivePushPlatform()

}
