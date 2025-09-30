package com.smjcco.wxpusher.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import com.heytap.msp.push.HeytapPushManager
import com.hihonor.push.sdk.HonorPushClient
import com.huawei.hms.api.HuaweiApiAvailability
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.config.ConfigManager
import com.vivo.push.PushClient

object DeviceUtils {
    //是否是小米设备
    fun isMIUI(): Boolean {
        return ConfigManager.getCurrentConfig().xiaomiPush
                && "Xiaomi".equals(Build.MANUFACTURER, true)
    }

    /**
     * 是否支持华为推送
     * 验证HMS Core（APK）在设备上是否成功安装和集成，检查已经安装的HMS Core（APK）版本号是否为client所需要的版本号或比需要的更新。该类为抽象方法，需要子类实现。
     * https://developer.huawei.com/consumer/cn/doc/hmscore-common-References/huaweiapiavailability-0000001050121134#section9492524178
     */
    fun isHuaweiMobileServicesAvailable(): Boolean {
        return ConfigManager.getCurrentConfig().huaweiPush
                && 0 == HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(ApplicationUtils.getApplication())
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
            return false;
        }

        // Android Q版本对应MagicUI 4.0
        if (Build.VERSION.SDK_INT > 29) {
            return true;
        }
        // Android Q以下版本返回-1
        return false
    }

    fun isHonorPush(): Boolean {
        //这里很有问题，针对荣耀90/100（已知的），checkSupportHonorPush 为false，会走华为或者自建推送
        //查询官网，国内Magic UI 4.0及以上 支持荣耀推送 ，所以再判断一下是不是Magic UI 4.0
        //如果没有问题，后面判断isMagicOs的逻辑，可以删除掉
        val supportPush =
            HonorPushClient.getInstance().checkSupportHonorPush(ApplicationUtils.getApplication())
        WxpLogUtils.i(
            "honor",
            "isHonorDevice=${isHonorDevice()},isMagicOs=${isMagicOs()},checkSupportHonorPush=${supportPush}"
        )
        return ConfigManager.getCurrentConfig().honorPush
                && supportPush
    }

    fun isVivo(): Boolean {
        return ConfigManager.getCurrentConfig().vivoPush
                && PushClient.getInstance(ApplicationUtils.getApplication()).isSupport()
    }

    fun isOppo(): Boolean {
        return ConfigManager.getCurrentConfig().oppoPush
                && HeytapPushManager.isSupportPush(ApplicationUtils.getApplication())
    }

    fun getPlatform(): DevicePlatform {
//        if (isMIUI()) {
//            return DevicePlatform.Android_XIAOMI
//        } else if (isVivo()) {
//            return DevicePlatform.Android_VIVO
//        } else if (isOppo()) {
//            return DevicePlatform.Android_OPPO
//        } else if (isHonorPush()) {
//            //临时逻辑，判断是支持荣耀push，但是没有打开开关，就走自建，避免进入华为的逻辑
//            if (ConfigManager.getCurrentConfig().honorPush) {
//                return DevicePlatform.Android_HONOR
//            } else {
//                return DevicePlatform.Android
//            }
//        } else if (isHuaweiMobileServicesAvailable()) {
//            //华为需要放在最后面，因为安装了HCM就会识别成华为，后面需要处理一下
//            return DevicePlatform.Android_HUAWEI
//        }
        return DevicePlatform.Android
    }

    /**
     * 调用设备振动
     */
    fun vibrator(time: Int) {
        val vibrator = ApplicationUtils.getApplication()
            .getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(
            VibrationEffect.createOneShot(
                time.toLong(),
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
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