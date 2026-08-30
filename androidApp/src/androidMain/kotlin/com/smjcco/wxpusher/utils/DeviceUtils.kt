package com.smjcco.wxpusher.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.smjcco.wxpusher.base.common.ApplicationUtils

object DeviceUtils {

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
