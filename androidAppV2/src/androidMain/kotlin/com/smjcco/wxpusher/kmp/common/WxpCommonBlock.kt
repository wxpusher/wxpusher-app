package com.smjcco.wxpusher.kmp.common

import android.app.Activity
import com.smjcco.wxpusher.base.common.ApplicationUtils

fun withActivity(activity: Activity? = null, run: ((activity: Activity) -> Unit)?) {
    if (run == null) {
        return
    }
    var currentActivity = activity
    if (currentActivity == null) {
        currentActivity = ApplicationUtils.getCurrentActivity()
    }
    if (currentActivity == null) {
        return
    }
    run(currentActivity)
}