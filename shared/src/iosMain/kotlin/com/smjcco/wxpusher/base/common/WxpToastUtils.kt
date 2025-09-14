package com.smjcco.wxpusher.base.common

import cocoapods.Toaster.Toast
import cocoapods.Toaster.ToastView
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual fun ExpWxpToastUtils_showToast(msg: String){
    ToastView.appearance().setBottomOffsetPortrait(450.0)
    Toast(text = msg, delay = 0.0, duration = 2.0).show()
//    val toast = Toast()
//    toast.setText(msg)
//    toast.show()
}
