package com.smjcco.wxpusher.kmp.common.utils

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import com.smjcco.wxpusher.base.common.ApplicationUtils

object VibratorUtils {

    /**
     * 进行一次震动
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
}