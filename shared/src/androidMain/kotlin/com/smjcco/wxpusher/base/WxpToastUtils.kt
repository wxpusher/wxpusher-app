package com.smjcco.wxpusher.base

import android.widget.Toast

actual fun ExpWxpToastUtils_showToast(msg: String) {
    Toast.makeText(ApplicationUtils.application, msg, Toast.LENGTH_LONG).show()
}
