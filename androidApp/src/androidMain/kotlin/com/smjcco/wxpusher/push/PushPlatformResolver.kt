package com.smjcco.wxpusher.push

import android.os.Build
import com.heytap.msp.push.HeytapPushManager
import com.hihonor.push.sdk.HonorPushClient
import com.huawei.hms.api.HuaweiApiAvailability
import com.meizu.cloud.pushsdk.PushManager as MeizuPushManager
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.config.ConfigManager
import com.vivo.push.PushClient

/**
 * Android 推送平台识别器。
 *
 * 本类只负责根据设备能力和远程配置识别厂商推送平台，不保存当前生效通道，
 * 也不执行注册、降级或后端上报。
 */
object PushPlatformResolver {

    /**
     * 仅根据设备和厂商 SDK 能力识别系统推送平台。
     *
     * 此方法不应用厂商推送总开关，适合设置页展示手机实际支持的系统推送类型。
     * 判断顺序与原有逻辑保持一致，避免多个厂商 SDK 同时声明可用时改变识别结果。
     */
    fun detectVendorPushPlatform(): DevicePlatform {
        if (isXiaomiDevice()) {
            return DevicePlatform.Android_XIAOMI
        } else if (isVivoPushSupported()) {
            return DevicePlatform.Android_VIVO
        } else if (isOppoPushSupported()) {
            return DevicePlatform.Android_OPPO
        } else if (isHonorPushSupported()) {
            return DevicePlatform.Android_HONOR
        } else if (isHuaweiDevice()) {
            return DevicePlatform.Android_HUAWEI
        } else if (isHuaweiMobileServicesAvailable()) {
            return DevicePlatform.Android_HUAWEI
        } else if (MeizuPushManager.isBrandMeizu()) {
            return DevicePlatform.Android_MEIZU
        }
        return DevicePlatform.Android
    }

    /**
     * 识别当前远程配置允许使用的默认厂商推送平台。
     *
     * 首次安装尚未形成有效通道状态时使用该结果作为默认路由。判断条件和顺序
     * 完整保留原有实现，不能改成“先识别厂商再判断开关”，否则可能改变兼容机型行为。
     */
    internal fun resolveConfigAllowedVendorPushPlatform(): DevicePlatform {
        val config = ConfigManager.getCurrentConfig()
        if (isXiaomiDevice() && config.xiaomiPush) {
            return DevicePlatform.Android_XIAOMI
        } else if (isVivoPushSupported() && config.vivoPush) {
            return DevicePlatform.Android_VIVO
        } else if (isOppoPushSupported() && config.oppoPush) {
            return DevicePlatform.Android_OPPO
        } else if (isHonorPushSupported() && config.honorPush) {
            return DevicePlatform.Android_HONOR
        } else if (isHuaweiDevice() && config.huaweiPush) {
            return DevicePlatform.Android_HUAWEI
        } else if (isHuaweiMobileServicesAvailable() && config.huaweiPushJustHcm) {
            // 华为能力放在靠后位置，避免仅安装 HMS Core 的设备被优先识别成华为设备。
            return DevicePlatform.Android_HUAWEI
        } else if (MeizuPushManager.isBrandMeizu() && config.meizuPush) {
            return DevicePlatform.Android_MEIZU
        }
        return DevicePlatform.Android
    }

    /**
     * 判断指定厂商平台当前是否被 ConfigManager 允许使用。
     *
     * 该方法用于协调器处理系统强制降级，判断规则与原协调器逻辑保持一致。
     */
    internal fun isVendorPushAllowed(platform: DevicePlatform): Boolean {
        val config = ConfigManager.getCurrentConfig()
        return when (platform) {
            DevicePlatform.Android_XIAOMI -> config.xiaomiPush
            // 兼容旧逻辑：HMS 能力兜底开关可以独立启用华为通道。
            DevicePlatform.Android_HUAWEI -> config.huaweiPush || config.huaweiPushJustHcm
            DevicePlatform.Android_VIVO -> config.vivoPush
            DevicePlatform.Android_HONOR -> config.honorPush
            DevicePlatform.Android_OPPO -> config.oppoPush
            DevicePlatform.Android_MEIZU -> config.meizuPush
            else -> false
        }
    }

    /** 判断平台是否属于 Android 厂商系统推送，而不是通用 Android WS 通道。 */
    internal fun isVendorPushPlatform(platform: DevicePlatform): Boolean {
        return platform != DevicePlatform.Android && platform.name.startsWith("Android_")
    }

    /** 判断当前设备是否为小米设备。 */
    private fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", true)
    }

    /** 判断当前设备是否安装并支持所需版本的 HMS Core。 */
    private fun isHuaweiMobileServicesAvailable(): Boolean {
        return HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(ApplicationUtils.getApplication()) == 0
    }

    /** 判断当前设备是否为支持 HMS 推送的华为或荣耀设备。 */
    private fun isHuaweiDevice(): Boolean {
        val isHuaweiOrHonor = Build.MANUFACTURER.equals("huawei", true)
            || Build.MANUFACTURER.equals("HONOR", true)
        return isHuaweiOrHonor && isHuaweiMobileServicesAvailable()
    }

    /** 判断荣耀推送 SDK 是否支持当前设备。 */
    private fun isHonorPushSupported(): Boolean {
        return HonorPushClient.getInstance()
            .checkSupportHonorPush(ApplicationUtils.getApplication())
    }

    /** 判断 VIVO 推送 SDK 是否支持当前设备。 */
    private fun isVivoPushSupported(): Boolean {
        return PushClient.getInstance(ApplicationUtils.getApplication()).isSupport
    }

    /** 判断 OPPO 推送 SDK 是否支持当前设备。 */
    private fun isOppoPushSupported(): Boolean {
        return HeytapPushManager.isSupportPush(ApplicationUtils.getApplication())
    }
}
