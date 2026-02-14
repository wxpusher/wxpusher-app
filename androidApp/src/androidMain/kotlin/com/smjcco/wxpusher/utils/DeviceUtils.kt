package com.smjcco.wxpusher.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.heytap.msp.push.HeytapPushManager
import com.hihonor.push.sdk.HonorPushClient
import com.huawei.hms.api.HuaweiApiAvailability
import com.meizu.cloud.pushsdk.PushManager
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.config.ConfigManager
import com.vivo.push.PushClient

object DeviceUtils {

    //运行的时候，如果有降级，就使用这个新的设备类型
    var runtimePlatform: DevicePlatform? = null

    //是否是小米设备
    fun isMIUI(): Boolean {
        return "Xiaomi".equals(Build.MANUFACTURER, true)
    }

    /**
     * 是否支持华为推送
     * 验证HMS Core（APK）在设备上是否成功安装和集成，检查已经安装的HMS Core（APK）版本号是否为client所需要的版本号或比需要的更新。该类为抽象方法，需要子类实现。
     * https://developer.huawei.com/consumer/cn/doc/hmscore-common-References/huaweiapiavailability-0000001050121134#section9492524178
     */
    fun isHuaweiMobileServicesAvailable(): Boolean {
        return 0 == HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(ApplicationUtils.getApplication())
    }

    /**
     * 是否支持华为推送
     * 华为或者荣耀的制造商，并且HCM可用
     */
    fun isHuawei(): Boolean {
        //因为安装了HCM就会识别成华为，所以判断一下制造商
        return (Build.MANUFACTURER.equals("huawei", true)
                || Build.MANUFACTURER.equals("HONOR", true))
                && isHuaweiMobileServicesAvailable()
    }

    /**
     * 是否是荣耀设备
     */
    fun isHonorDevice(): Boolean {
        return Build.MANUFACTURER.equals("HONOR", true)
    }

    /**
     * 国内Magic UI 4.0及以上 支持荣耀推送
     */
    fun isMagicOs(): Boolean {
        if (!isHonorDevice()) {
            // 非荣耀设备，暂不支持
            return false
        }

        // Android Q版本对应MagicUI 4.0
        if (Build.VERSION.SDK_INT > 29) {
            return true
        }
        // Android Q以下版本返回-1
        return false
    }

    fun isHonorPush(): Boolean {
        val supportPush =
            HonorPushClient.getInstance().checkSupportHonorPush(ApplicationUtils.getApplication())
        return supportPush
    }

    fun isVivo(): Boolean {
        return PushClient.getInstance(ApplicationUtils.getApplication()).isSupport
    }

    fun isOppo(): Boolean {
        return HeytapPushManager.isSupportPush(ApplicationUtils.getApplication())
    }

    fun getPlatform(): DevicePlatform {
        //如果运行过程中有降级，比如注册华为推送失败，最后走了ws，就用降级后的设备类型
        if (runtimePlatform != null) {
            return runtimePlatform!!
        }
        if (isMIUI() && ConfigManager.getCurrentConfig().xiaomiPush) {
            return DevicePlatform.Android_XIAOMI
        } else if (isVivo() && ConfigManager.getCurrentConfig().vivoPush) {
            return DevicePlatform.Android_VIVO
        } else if (isOppo() && ConfigManager.getCurrentConfig().oppoPush) {
            return DevicePlatform.Android_OPPO
        } else if (isHonorPush() && ConfigManager.getCurrentConfig().honorPush) {
            return DevicePlatform.Android_HONOR
        } else if (isHuawei() && ConfigManager.getCurrentConfig().huaweiPush) {
            return DevicePlatform.Android_HUAWEI
        } else if (isHuaweiMobileServicesAvailable() && ConfigManager.getCurrentConfig().huaweiPushJustHcm) {
            //华为需要放在最后面，因为安装了HCM就会识别成华为，后面需要处理一下
            return DevicePlatform.Android_HUAWEI
        } else if (PushManager.isBrandMeizu() && ConfigManager.getCurrentConfig().meizuPush) {
            return DevicePlatform.Android_MEIZU
        }
        return DevicePlatform.Android
    }

    fun setPlatform(platform: DevicePlatform) {
        runtimePlatform = platform
    }

    /**
     * 调用设备振动
     */
    fun vibrator(time: Int = 50) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = ApplicationUtils.getApplication()
                .getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager?
            val vibrator = vibratorManager?.defaultVibrator

            if (time < 100) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(time.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } else {
            val vibrator = ApplicationUtils.getApplication()
                .getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(
                VibrationEffect.createOneShot(
                    time.toLong(),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    }

    /**
     * 是否忽略了电池优化🔋？
     * Check if battery optimization is enabled, see https://stackoverflow.com/a/49098293/1440785
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val context = ApplicationUtils.getApplication()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val appName = context.packageName
        return powerManager.isIgnoringBatteryOptimizations(appName)
    }

    /**
     * 检查设备是否连接到网络
     *
     * @return 如果设备连接到网络，则返回true；否则返回false
     */
    fun isNetworkConnected(): Boolean {
        val connectivityManager =
            ApplicationUtils.getApplication()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities != null && (
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }

}