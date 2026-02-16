package com.smjcco.wxpusher.base.common

import android.widget.Toast

actual fun ExpWxpToastUtils_showToast(msg: String) {
    Toast.makeText(ApplicationUtils.getApplication(), msg, Toast.LENGTH_LONG).show()
}
