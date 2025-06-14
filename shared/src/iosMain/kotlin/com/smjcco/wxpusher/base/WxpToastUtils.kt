package com.smjcco.wxpusher.base

import cocoapods.Toaster.Toast
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual fun ExpWxpToastUtils_showToast(msg: String){
    Toast(text = msg, delay = 0.0, duration = 3.0).show()
//    val toast = Toast()
//    toast.setText(msg)
//    toast.show()
}
